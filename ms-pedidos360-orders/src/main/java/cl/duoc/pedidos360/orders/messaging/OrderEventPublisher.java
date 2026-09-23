package cl.duoc.pedidos360.orders.messaging;

import cl.duoc.pedidos360.orders.config.MessagingConfig;
import cl.duoc.pedidos360.orders.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(Order order, String eventType) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .eventType(eventType)
                .status(order.getStatus().name())
                .total(order.getTotal())
                .occurredAt(LocalDateTime.now())
                .build();

        // RabbitMQ -> ms-pedidos360-notify
        rabbitTemplate.convertAndSend(
                MessagingConfig.NOTIFY_EXCHANGE,
                MessagingConfig.NOTIFY_ROUTING_KEY,
                event);

        // Kafka -> ms-pedidos360-report y ms-pedidos360-audit
        kafkaTemplate.send(MessagingConfig.ORDERS_EVENTS_TOPIC, order.getId().toString(), event);

        log.info("Evento publicado: {} para el pedido {}", eventType, order.getId());
    }
}
