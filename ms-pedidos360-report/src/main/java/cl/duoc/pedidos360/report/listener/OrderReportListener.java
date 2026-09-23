package cl.duoc.pedidos360.report.listener;

import cl.duoc.pedidos360.report.messaging.OrderEvent;
import cl.duoc.pedidos360.report.model.OrderReportEntry;
import cl.duoc.pedidos360.report.repository.OrderReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReportListener {

    private final OrderReportRepository reportRepository;

    @KafkaListener(topics = "orders.events", groupId = "ms-pedidos360-report")
    public void handleOrderEvent(OrderEvent event) {
        log.info("📊 Registrando en reportes el evento {} del pedido {}", event.getEventType(), event.getOrderId());

        OrderReportEntry entry = OrderReportEntry.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .total(event.getTotal())
                .processedAt(LocalDateTime.now())
                .build();

        reportRepository.save(entry);
    }
}
