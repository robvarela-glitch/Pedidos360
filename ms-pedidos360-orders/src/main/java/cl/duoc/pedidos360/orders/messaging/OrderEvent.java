package cl.duoc.pedidos360.orders.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Evento de dominio de un pedido. Viaja como JSON por RabbitMQ:
 *   - exchange direct -> cola de notificaciones (ms-notify)
 *   - exchange topic  -> colas de reportes (ms-report) y auditoría (ms-audit)
 * Los consumidores usan una clase equivalente (mismo contrato JSON).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {
    private Long orderId;
    private String customerId;
    private String customerName;
    private String eventType; // ORDER_CREATED, ORDER_STATUS_UPDATED, ORDER_CANCELLED
    private String status;
    private Double total;
    private LocalDateTime occurredAt;
}
