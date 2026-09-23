package cl.duoc.pedidos360.report.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Mismo contrato JSON que orders.messaging.OrderEvent (topic Kafka "orders.events"). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent implements Serializable {
    private Long orderId;
    private String customerId;
    private String eventType;
    private String status;
    private Double total;
    private LocalDateTime occurredAt;
}
