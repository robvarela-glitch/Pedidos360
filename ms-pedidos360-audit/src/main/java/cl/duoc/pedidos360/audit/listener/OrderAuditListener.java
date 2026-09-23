package cl.duoc.pedidos360.audit.listener;

import cl.duoc.pedidos360.audit.messaging.OrderEvent;
import cl.duoc.pedidos360.audit.model.AuditLog;
import cl.duoc.pedidos360.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAuditListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @KafkaListener(topics = "orders.events", groupId = "ms-pedidos360-audit")
    public void handleOrderEvent(OrderEvent event) {
        log.info("🕵️ Auditando evento {} del pedido {}", event.getEventType(), event.getOrderId());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            payloadJson = event.toString();
        }

        AuditLog auditLog = AuditLog.builder()
                .sourceService("ms-pedidos360-orders")
                .entityId(String.valueOf(event.getOrderId()))
                .eventType(event.getEventType())
                .payload(payloadJson)
                .recordedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
