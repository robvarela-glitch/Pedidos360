package cl.duoc.pedidos360.catalog.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Evento de dominio del catálogo (exchange topic, routing key product.*). Lo consume ms-audit. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {
    private Long productId;
    private String sku;
    private String name;
    private String eventType; // PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DEACTIVATED
    private Double price;
    private Integer stock;
    private Boolean active;
    private String performedBy;
    private LocalDateTime occurredAt;
}
