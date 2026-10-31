package cl.duoc.pedidos360.report.listener;

import cl.duoc.pedidos360.report.messaging.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka OPCIONAL (pedidos360.kafka.enabled=true). La vía principal es
 * RabbitMQ (OrderReportListener). No activar ambos a la vez: cada evento se registraría dos veces.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pedidos360.kafka.enabled", havingValue = "true")
public class KafkaOrderReportListener {

    private final OrderReportListener rabbitListener;

    @KafkaListener(topics = "${pedidos360.kafka.topic:orders.events}", groupId = "ms-pedidos360-report")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Kafka: registrando en reportes el evento {} del pedido {}", event.getEventType(), event.getOrderId());
        rabbitListener.register(event);
    }
}
