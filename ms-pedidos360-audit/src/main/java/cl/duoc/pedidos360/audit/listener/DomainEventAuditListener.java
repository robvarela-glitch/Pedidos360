package cl.duoc.pedidos360.audit.listener;

import cl.duoc.pedidos360.audit.messaging.ManualAckHandler;
import cl.duoc.pedidos360.audit.messaging.NonRecoverableMessageException;
import cl.duoc.pedidos360.audit.model.AuditLog;
import cl.duoc.pedidos360.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Consumidor del dominio AUDITORÍA: cola pedidos360.audit.events (exchange topic, patrón #).
 * Recibe TODOS los eventos de dominio (order.* desde ms-orders y product.* desde ms-catalog)
 * y deja un registro inmutable en Oracle. ACK / reintentos / DLQ: ManualAckHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventAuditListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ManualAckHandler ackHandler;

    @RabbitListener(queues = "${pedidos360.rabbit.queues.audit}")
    public void onDomainEvent(Message message, Channel channel) throws IOException {
        String queue = message.getMessageProperties().getConsumerQueue();
        ackHandler.handle(queue, message, channel, msg -> {
            JsonNode event = objectMapper.readTree(msg.getBody());
            String routingKey = msg.getMessageProperties().getReceivedRoutingKey();
            AuditLog auditLog = toAuditLog(event, routingKey, (String) msg.getMessageProperties().getHeaders().get("x-source-service"));
            auditLogRepository.save(auditLog);
            log.info("Auditoría: {} sobre la entidad {} (routing key {})",
                    auditLog.getEventType(), auditLog.getEntityId(), routingKey);
        });
    }

    AuditLog toAuditLog(JsonNode event, String routingKey, String sourceHeader) {
        if (event == null || !event.isObject()) {
            throw new NonRecoverableMessageException("El evento de auditoría debe ser un objeto JSON");
        }
        String eventType = text(event, "eventType");
        String entityId = event.hasNonNull("orderId") ? event.get("orderId").asText()
                : event.hasNonNull("productId") ? event.get("productId").asText() : null;
        if (eventType == null || entityId == null) {
            throw new NonRecoverableMessageException("Evento sin eventType o sin identificador de entidad (orderId/productId)");
        }
        String source = sourceHeader != null ? sourceHeader
                : (routingKey != null && routingKey.startsWith("product.")) ? "ms-pedidos360-catalog" : "ms-pedidos360-orders";
        String payload = event.toString();
        return AuditLog.builder()
                .sourceService(source)
                .entityId(entityId)
                .eventType(eventType)
                .routingKey(routingKey)
                .actor(text(event, "customerName") != null ? text(event, "customerName") : text(event, "performedBy"))
                .payload(payload.length() > 1000 ? payload.substring(0, 1000) : payload)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
