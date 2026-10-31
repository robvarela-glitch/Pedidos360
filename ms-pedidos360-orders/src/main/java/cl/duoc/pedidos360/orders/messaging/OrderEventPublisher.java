package cl.duoc.pedidos360.orders.messaging;

import cl.duoc.pedidos360.orders.config.RabbitTopologyProperties;
import cl.duoc.pedidos360.orders.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Publica los eventos de pedidos. El servicio de negocio solo llama a publish():
 * no conoce exchanges, colas ni routing keys (vienen de RabbitTopologyProperties).
 */
@Slf4j
@Component
public class OrderEventPublisher {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_UPDATED = "ORDER_STATUS_UPDATED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;
    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String kafkaTopic;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate,
                               RabbitTopologyProperties topology,
                               ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplate,
                               @Value("${pedidos360.kafka.enabled:false}") boolean kafkaEnabled,
                               @Value("${pedidos360.kafka.topic:orders.events}") String kafkaTopic) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
        this.kafkaTopic = kafkaTopic;
    }

    public void publish(Order order, String eventType) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .eventType(eventType)
                .status(order.getStatus().name())
                .total(order.getTotal())
                .occurredAt(LocalDateTime.now())
                .build();

        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setHeader("x-source-service", "ms-pedidos360-orders");
            message.getMessageProperties().setHeader("x-event-type", eventType);
            return message;
        };

        // 1) Exchange DIRECT -> cola de notificaciones (ms-notify)
        rabbitTemplate.convertAndSend(topology.getExchanges().getDirect(),
                topology.getRoutingKeys().getNotify(), event, headers);

        // 2) Exchange TOPIC -> reportes (order.*) y auditoría (#)
        String topicRoutingKey = topicRoutingKey(eventType);
        rabbitTemplate.convertAndSend(topology.getExchanges().getTopic(), topicRoutingKey, event, headers);

        // 3) Kafka opcional (desactivado por defecto)
        if (kafkaEnabled) {
            KafkaTemplate<String, Object> kafka = kafkaTemplate.getIfAvailable();
            if (kafka != null) {
                kafka.send(kafkaTopic, String.valueOf(order.getId()), event);
            }
        }

        log.info("Evento {} publicado para el pedido {} (direct: {}, topic: {})",
                eventType, order.getId(), topology.getRoutingKeys().getNotify(), topicRoutingKey);
    }

    /** ORDER_CREATED -> order.created, ORDER_STATUS_UPDATED -> order.status_updated ... */
    String topicRoutingKey(String eventType) {
        String suffix = eventType.toLowerCase(Locale.ROOT).replaceFirst("^order_", "");
        return topology.getRoutingKeys().getOrderPrefix() + "." + suffix;
    }
}
