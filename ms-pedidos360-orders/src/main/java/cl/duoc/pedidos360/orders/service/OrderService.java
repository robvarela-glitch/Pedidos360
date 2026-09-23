package cl.duoc.pedidos360.orders.service;

import cl.duoc.pedidos360.orders.dto.CreateOrderRequest;
import cl.duoc.pedidos360.orders.dto.OrderItemRequest;
import cl.duoc.pedidos360.orders.exception.OrderNotFoundException;
import cl.duoc.pedidos360.orders.messaging.OrderEventPublisher;
import cl.duoc.pedidos360.orders.model.Order;
import cl.duoc.pedidos360.orders.model.OrderItem;
import cl.duoc.pedidos360.orders.model.OrderStatus;
import cl.duoc.pedidos360.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.CREADO)
                .createdAt(LocalDateTime.now())
                .total(0.0)
                .build();

        double total = 0.0;
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build();
            order.addItem(item);
            total += itemReq.getQuantity() * itemReq.getUnitPrice();
        }
        order.setTotal(total);

        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, "ORDER_CREATED");
        return saved;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = findById(id);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, "ORDER_UPDATED");
        return saved;
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = findById(id);
        order.setStatus(OrderStatus.CANCELADO);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        eventPublisher.publish(saved, "ORDER_CANCELLED");
    }
}
