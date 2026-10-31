package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.client.CatalogClient;
import cl.duoc.pedidos360.orders.client.CatalogProduct;
import cl.duoc.pedidos360.orders.dto.CreateOrderRequest;
import cl.duoc.pedidos360.orders.dto.OrderItemRequest;
import cl.duoc.pedidos360.orders.exception.InvalidOrderException;
import cl.duoc.pedidos360.orders.exception.InvalidStatusTransitionException;
import cl.duoc.pedidos360.orders.exception.OrderAccessDeniedException;
import cl.duoc.pedidos360.orders.messaging.OrderEventPublisher;
import cl.duoc.pedidos360.orders.model.Order;
import cl.duoc.pedidos360.orders.model.OrderStatus;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import cl.duoc.pedidos360.orders.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de identidad y propiedad: Ana (cliente) no puede ver ni cancelar pedidos de Pedro. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private CatalogClient catalogClient;
    @InjectMocks
    private OrderService orderService;

    private final CurrentUser ana = new CurrentUser("oid-ana", "Ana", false, false, "token-ana");
    private final CurrentUser operador = new CurrentUser("oid-op", "Olga", false, true, "token-op");
    private Order pedidoDePedro;

    @BeforeEach
    void setUp() {
        pedidoDePedro = Order.builder().id(10L).customerId("oid-pedro").status(OrderStatus.CREADO)
                .total(1000.0).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void crearPedido_usaIdentidadDelTokenYPrecioDelCatalogo() {
        when(catalogClient.getProduct(1L, "token-ana"))
                .thenReturn(new CatalogProduct(1L, "TEC-001", "Teclado", 100.0, 10, true));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order creado = orderService.createOrder(request(1L, 3), ana);

        assertThat(creado.getCustomerId()).isEqualTo("oid-ana");
        assertThat(creado.getTotal()).isEqualTo(300.0);
        assertThat(creado.getItems().get(0).getUnitPrice()).isEqualTo(100.0);
        verify(eventPublisher).publish(creado, OrderEventPublisher.ORDER_CREATED);
    }

    @Test
    void crearPedido_sinStock_esRechazado() {
        when(catalogClient.getProduct(1L, "token-ana"))
                .thenReturn(new CatalogProduct(1L, "TEC-001", "Teclado", 100.0, 1, true));

        assertThatThrownBy(() -> orderService.createOrder(request(1L, 5), ana))
                .isInstanceOf(InvalidOrderException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cliente_soloVeSusPropiosPedidos() {
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc("oid-ana")).thenReturn(List.of());

        orderService.findVisibleOrders(ana, null);

        verify(orderRepository).findByCustomerIdOrderByCreatedAtDesc("oid-ana");
        verify(orderRepository, never()).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void cliente_noPuedeVerPedidoAjeno() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pedidoDePedro));

        assertThatThrownBy(() -> orderService.findVisibleOrder(10L, ana))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void cliente_noPuedeCancelarPedidoAjeno() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pedidoDePedro));

        assertThatThrownBy(() -> orderService.cancelOrder(10L, ana))
                .isInstanceOf(OrderAccessDeniedException.class);
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void operador_puedeVerPedidoDeCualquierCliente() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pedidoDePedro));

        assertThat(orderService.findVisibleOrder(10L, operador).getCustomerId()).isEqualTo("oid-pedro");
    }

    @Test
    void operador_noPuedeSaltarseEstados() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pedidoDePedro));

        assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.ENTREGADO, operador))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void operador_cambiaEstadoValido_publicaEvento() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(pedidoDePedro));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order actualizado = orderService.updateStatus(10L, OrderStatus.EN_PREPARACION, operador);

        assertThat(actualizado.getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
        verify(eventPublisher).publish(any(Order.class), eq(OrderEventPublisher.ORDER_STATUS_UPDATED));
    }

    @Test
    void cliente_noPuedeCambiarEstado() {
        assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.EN_PREPARACION, ana))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    private static CreateOrderRequest request(Long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));
        return request;
    }
}
