package cl.duoc.pedidos360.notify.listener;

import cl.duoc.pedidos360.notify.messaging.ManualAckHandler;
import cl.duoc.pedidos360.notify.messaging.OrderEvent;
import cl.duoc.pedidos360.notify.model.Notification;
import cl.duoc.pedidos360.notify.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Consumidor del dominio NOTIFICACIONES: cola pedidos360.notify.orders (exchange direct).
 * Convierte cada evento de pedido en una notificación para el cliente y la guarda en Oracle.
 * El ACK / reintento / DLQ lo decide ManualAckHandler (acknowledge-mode=manual).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final ManualAckHandler ackHandler;

    @RabbitListener(queues = "${pedidos360.rabbit.queues.notify}")
    public void onOrderEvent(Message message, Channel channel) throws IOException {
        String queue = message.getMessageProperties().getConsumerQueue();
        ackHandler.handle(queue, message, channel, msg -> {
            OrderEvent event = objectMapper.readValue(msg.getBody(), OrderEvent.class);
            event.validate();
            log.info("Notificación: evento {} del pedido {}", event.getEventType(), event.getOrderId());

            notificationRepository.save(Notification.builder()
                    .orderId(event.getOrderId())
                    .customerId(event.getCustomerId())
                    .eventType(event.getEventType())
                    .message(buildMessage(event))
                    .sentAt(LocalDateTime.now())
                    .build());
        });
    }

    private String buildMessage(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_CREATED" -> "Tu pedido #" + event.getOrderId() + " fue creado por un total de $" + event.getTotal();
            case "ORDER_STATUS_UPDATED" -> "Tu pedido #" + event.getOrderId() + " cambió de estado a " + event.getStatus();
            case "ORDER_CANCELLED" -> "Tu pedido #" + event.getOrderId() + " fue cancelado";
            default -> "Actualización de tu pedido #" + event.getOrderId();
        };
    }
}
