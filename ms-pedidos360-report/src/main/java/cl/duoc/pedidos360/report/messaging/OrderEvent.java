package cl.duoc.pedidos360.report.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Mismo contrato JSON que orders.messaging.OrderEvent (cola pedidos360.report.orders). */
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
        if (orderId == null || customerId == null || eventType == null || status == null || total == null) {
            throw new NonRecoverableMessageException(
                    "OrderEvent inválido: orderId, customerId, eventType, status y total son obligatorios");
        }
    }
}
