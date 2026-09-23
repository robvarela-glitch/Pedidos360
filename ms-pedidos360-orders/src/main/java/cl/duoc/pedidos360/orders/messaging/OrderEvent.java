package cl.duoc.pedidos360.orders.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Evento de dominio que viaja por Kafka (topic "orders.events", consumido por
 * report y audit) y por RabbitMQ (queue "orders.notifications", consumida por notify).
 * Los otros microservicios deben usar una clase equivalente (mismo contrato JSON).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {
    private Long orderId;
    private String customerId;
    private String eventType; // ORDER_CREATED, ORDER_UPDATED, ORDER_CANCELLED
    private String status;
    private Double total;
    private LocalDateTime occurredAt;
}
