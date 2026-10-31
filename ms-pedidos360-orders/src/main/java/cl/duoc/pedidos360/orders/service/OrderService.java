package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.client.CatalogClient;
import cl.duoc.pedidos360.orders.client.CatalogProduct;
import cl.duoc.pedidos360.orders.dto.CreateOrderRequest;
import cl.duoc.pedidos360.orders.dto.OrderItemRequest;
import cl.duoc.pedidos360.orders.exception.InvalidOrderException;
import cl.duoc.pedidos360.orders.exception.InvalidStatusTransitionException;
import cl.duoc.pedidos360.orders.exception.OrderAccessDeniedException;
import cl.duoc.pedidos360.orders.exception.OrderNotFoundException;
import cl.duoc.pedidos360.orders.messaging.OrderEventPublisher;
import cl.duoc.pedidos360.orders.model.Order;
import cl.duoc.pedidos360.orders.model.OrderItem;
import cl.duoc.pedidos360.orders.model.OrderStatus;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import cl.duoc.pedidos360.orders.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de negocio de pedidos. La identidad SIEMPRE viene del JWT (CurrentUser):
 *  - Cliente: crea pedidos a su nombre, ve y cancela solo los suyos (si están en CREADO).
 *  - Operador / Admin: ven todos los pedidos y cambian su estado según OrderStatus.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final CatalogClient catalogClient;

    @Transactional
    public Order createOrder(CreateOrderRequest request, CurrentUser user) {
        Order order = Order.builder()
                .customerId(user.id())
                .customerName(user.name())
                .status(OrderStatus.CREADO)
                .createdAt(LocalDateTime.now())
                .total(0.0)
                .build();

        double total = 0.0;
        for (OrderItemRequest itemReq : request.getItems()) {
            CatalogProduct product = catalogClient.getProduct(itemReq.getProductId(), user.token());
            if (Boolean.FALSE.equals(product.active())) {
                throw new InvalidOrderException("El producto " + product.name() + " no está disponible");
            }
            if (product.stock() != null && product.stock() < itemReq.getQuantity()) {
                throw new InvalidOrderException("Stock insuficiente para " + product.name()
                        + " (disponible: " + product.stock() + ")");
            }
            if (product.price() == null) {
                throw new InvalidOrderException("El producto " + product.name() + " no tiene precio");
            }
            OrderItem item = OrderItem.builder()
                    .productId(String.valueOf(product.id()))
                    .productName(product.name())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.price())
                    .build();
            order.addItem(item);
            total += itemReq.getQuantity() * product.price();
        }
        order.setTotal(total);

        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, OrderEventPublisher.ORDER_CREATED);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> findVisibleOrders(CurrentUser user, OrderStatus status) {
        if (user.canManageAllOrders()) {
            return status == null
                    ? orderRepository.findAll(NEWEST_FIRST)
                    : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        List<Order> own = orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.id());
        return status == null ? own : own.stream().filter(o -> o.getStatus() == status).toList();
    }

    @Transactional(readOnly = true)
    public Order findVisibleOrder(Long id, CurrentUser user) {
        Order order = findById(id);
        checkAccess(order, user);
        return order;
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus, CurrentUser user) {
        if (!user.canManageAllOrders()) {
            throw new OrderAccessDeniedException(id);
        }
        Order order = findById(id);
        if (!order.getStatus().canChangeTo(newStatus)) {
            throw new InvalidStatusTransitionException(order.getStatus(), newStatus);
        }
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, newStatus == OrderStatus.CANCELADO
                ? OrderEventPublisher.ORDER_CANCELLED
                : OrderEventPublisher.ORDER_STATUS_UPDATED);
        return saved;
    }

    @Transactional
    public Order cancelOrder(Long id, CurrentUser user) {
        Order order = findById(id);
        checkAccess(order, user);
        if (!user.canManageAllOrders() && order.getStatus() != OrderStatus.CREADO) {
            throw new InvalidStatusTransitionException(
                    "El cliente solo puede cancelar pedidos en estado CREADO (estado actual: " + order.getStatus() + ")");
        }
        if (!order.getStatus().canChangeTo(OrderStatus.CANCELADO)) {
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.CANCELADO);
        }
        order.setStatus(OrderStatus.CANCELADO);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, OrderEventPublisher.ORDER_CANCELLED);
        return saved;
    }

    private Order findById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Un cliente solo puede acceder a sus propios pedidos. */
    private void checkAccess(Order order, CurrentUser user) {
        if (!user.canManageAllOrders() && !order.getCustomerId().equals(user.id())) {
            throw new OrderAccessDeniedException(order.getId());
        }
    }
}
