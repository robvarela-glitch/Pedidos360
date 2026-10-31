package cl.duoc.pedidos360.report.listener;

import cl.duoc.pedidos360.report.messaging.ManualAckHandler;
import cl.duoc.pedidos360.report.messaging.OrderEvent;
import cl.duoc.pedidos360.report.model.OrderReportEntry;
import cl.duoc.pedidos360.report.repository.OrderReportRepository;
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
 * Consumidor del dominio REPORTES: cola pedidos360.report.orders
 * (exchange topic, patrón order.*). Registra cada evento de pedido en Oracle
 * para calcular totales y conteos. ACK / reintentos / DLQ: ManualAckHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReportListener {

    private final OrderReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final ManualAckHandler ackHandler;

    @RabbitListener(queues = "${pedidos360.rabbit.queues.report}")
    public void onOrderEvent(Message message, Channel channel) throws IOException {
        String queue = message.getMessageProperties().getConsumerQueue();
        ackHandler.handle(queue, message, channel, msg -> {
            OrderEvent event = objectMapper.readValue(msg.getBody(), OrderEvent.class);
            event.validate();
            log.info("Reporte: evento {} del pedido {} (routing key {})", event.getEventType(),
                    event.getOrderId(), msg.getMessageProperties().getReceivedRoutingKey());
            register(event);
        });
    }

    void register(OrderEvent event) {
        reportRepository.save(OrderReportEntry.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .total(event.getTotal())
                .processedAt(LocalDateTime.now())
                .build());
    }
}
