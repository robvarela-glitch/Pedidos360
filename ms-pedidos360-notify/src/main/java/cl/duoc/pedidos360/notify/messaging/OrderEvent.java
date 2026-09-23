package cl.duoc.pedidos360.notify.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Debe mantener el mismo contrato JSON que cl.duoc.pedidos360.orders.messaging.OrderEvent
 * (es el mensaje que llega por la cola RabbitMQ "orders.notifications").
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {
    private Long orderId;
    private String customerId;
    private String eventType;
    private String status;
    private Double total;
    private LocalDateTime occurredAt;
}
