package cl.duoc.pedidos360.notify.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mismo contrato JSON que cl.duoc.pedidos360.orders.messaging.OrderEvent
 * (mensaje que llega por la cola pedidos360.notify.orders).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent implements Serializable {
    private Long orderId;
    private String customerId;
    private String customerName;
    private String eventType;
    private String status;
    private Double total;
    private LocalDateTime occurredAt;

    /** Valida el contrato mínimo; si falla, el mensaje va directo a la DLQ. */
    public void validate() {
        if (orderId == null || customerId == null || customerId.isBlank() || eventType == null || eventType.isBlank()) {
            throw new NonRecoverableMessageException(
                    "OrderEvent inválido: orderId, customerId y eventType son obligatorios");
        }
    }
}
