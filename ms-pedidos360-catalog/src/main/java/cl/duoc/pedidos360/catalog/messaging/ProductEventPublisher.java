package cl.duoc.pedidos360.catalog.messaging;

import cl.duoc.pedidos360.catalog.config.RabbitTopologyProperties;
import cl.duoc.pedidos360.catalog.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Publica los cambios del catálogo en el exchange topic (product.created, product.updated,
 * product.deactivated). La cola de auditoría (patrón #) los recibe; la de reportes (order.*) no.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_DEACTIVATED = "PRODUCT_DEACTIVATED";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    public void publish(Product product, String eventType, String performedBy) {
        ProductEvent event = ProductEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .eventType(eventType)
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .performedBy(performedBy)
                .occurredAt(LocalDateTime.now())
                .build();
        String routingKey = topology.getRoutingKeys().getProductPrefix() + "."
                + eventType.toLowerCase(Locale.ROOT).replaceFirst("^product_", "");
        try {
            rabbitTemplate.convertAndSend(topology.getExchanges().getTopic(), routingKey, event, message -> {
                message.getMessageProperties().setHeader("x-source-service", "ms-pedidos360-catalog");
                message.getMessageProperties().setHeader("x-event-type", eventType);
                return message;
            });
            log.info("Evento {} publicado para el producto {} (routing key {})", eventType, product.getId(), routingKey);
        } catch (AmqpException e) {
            // El catálogo sigue funcionando aunque RabbitMQ no esté disponible; queda registrado.
            log.error("No se pudo publicar el evento {} del producto {}: {}", eventType, product.getId(), e.getMessage());
        }
    }
}
