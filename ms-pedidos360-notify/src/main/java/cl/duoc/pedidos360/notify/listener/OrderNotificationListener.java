package cl.duoc.pedidos360.notify.listener;

import cl.duoc.pedidos360.notify.messaging.OrderEvent;
import cl.duoc.pedidos360.notify.model.Notification;
import cl.duoc.pedidos360.notify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationRepository notificationRepository;

    /**
     * Escucha la cola "orders.notifications" que llena ms-pedidos360-orders
     * cada vez que se crea, actualiza o cancela un pedido.
     * Aquí "enviamos" la notificación (en este control, simulada con un log +
     * registro en Oracle; en producción sería email/SMS/push).
     */
    @RabbitListener(queues = "orders.notifications")
    public void handleOrderEvent(OrderEvent event) {
        String mensaje = construirMensaje(event);

        log.info("📩 Notificación para el cliente {}: {}", event.getCustomerId(), mensaje);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .eventType(event.getEventType())
                .message(mensaje)
                .sentAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    private String construirMensaje(OrderEvent event) {
        return switch (event.getEventType()) {
            case "ORDER_CREATED" -> "Tu pedido #" + event.getOrderId() + " fue creado por un total de $" + event.getTotal();
            case "ORDER_UPDATED" -> "Tu pedido #" + event.getOrderId() + " cambió de estado a " + event.getStatus();
            case "ORDER_CANCELLED" -> "Tu pedido #" + event.getOrderId() + " fue cancelado";
            default -> "Actualización de tu pedido #" + event.getOrderId();
        };
    }
}
