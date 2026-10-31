package cl.duoc.pedidos360.audit.messaging;

import cl.duoc.pedidos360.audit.config.RabbitTopologyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Confirmación manual (ACK) y manejo de errores de los consumidores RabbitMQ.
 *
 * Rutas de un mensaje:
 *   - Procesado OK                         -> basicAck
 *   - Error NO recuperable (JSON inválido,  -> basicNack(requeue=false) -> DLX -> <cola>.dlq
 *     campos obligatorios faltantes)          (sin reintentos: reintentar no lo arreglaría)
 *   - Error recuperable (BD caída, timeout) -> se republica una copia en la MISMA cola con el
 *                                              header x-retry-count+1 y se hace ACK del original;
 *                                              al agotar pedidos360.rabbit.retry.max-attempts
 *                                              -> basicNack(requeue=false) -> DLQ
 *
 * Todo envío a la DLQ queda registrado en el log con el prefijo [DLQ].
 * Además, cada resultado incrementa la métrica pedidos360_rabbit_messages_total
 * (etiquetas queue y result = ack | retry | dlq), que Prometheus recoge y Grafana grafica.
 * Nunca se usa requeue=true, para no generar bucles infinitos de reproceso.
 */
@Slf4j
@Component
public class ManualAckHandler {

    public static final String RETRY_HEADER = "x-retry-count";
    public static final String SIMULATE_HEADER = "x-simulate-failure";

    public static final String METRIC = "pedidos360.rabbit.messages";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ManualAckHandler(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
        this.meterRegistry = meterRegistry;
    }

    /** Constructor para pruebas unitarias sin contexto de Spring. */
    public ManualAckHandler(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology) {
        this(rabbitTemplate, topology, new SimpleMeterRegistry());
    }

    private void count(String queue, String result) {
        Counter.builder(METRIC)
                .description("Mensajes procesados por el consumidor según su resultado")
                .tag("queue", queue)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    @FunctionalInterface
    public interface MessageWork {
        void process(Message message) throws Exception;
    }

    public void handle(String queue, Message message, Channel channel, MessageWork work) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        int attempt = retryCount(message) + 1;
        try {
            simulateFailureIfRequested(message);
            work.process(message);
            channel.basicAck(deliveryTag, false);
            count(queue, "ack");
            log.info("[ACK] cola={} intento={} mensaje procesado correctamente", queue, attempt);
        } catch (NonRecoverableMessageException | JsonProcessingException | IllegalArgumentException e) {
            log.error("[DLQ] cola={} -> {} | error NO recuperable: {} | payload={}",
                    queue, topology.dlqOf(queue), e.getMessage(), preview(message));
            channel.basicNack(deliveryTag, false, false);
            count(queue, "dlq");
        } catch (Exception e) {
            int maxAttempts = Math.max(1, topology.getRetry().getMaxAttempts());
            if (attempt < maxAttempts) {
                retry(queue, message, channel, deliveryTag, attempt, maxAttempts, e);
            } else {
                log.error("[DLQ] cola={} -> {} | reintentos agotados ({}/{}): {} | payload={}",
                        queue, topology.dlqOf(queue), attempt, maxAttempts, e.getMessage(), preview(message));
                channel.basicNack(deliveryTag, false, false);
                count(queue, "dlq");
            }
        }
    }

    private void retry(String queue, Message message, Channel channel, long deliveryTag,
                       int attempt, int maxAttempts, Exception cause) throws IOException {
        try {
            Thread.sleep(topology.getRetry().getBackoffMs() * attempt);
            message.getMessageProperties().setHeader(RETRY_HEADER, attempt);
            // Exchange por defecto ("") + routing key = nombre de la cola: la copia vuelve
            // SOLO a esta cola (no se duplica en las otras colas enlazadas al topic).
            rabbitTemplate.send("", queue, message);
            channel.basicAck(deliveryTag, false);
            count(queue, "retry");
            log.warn("[RETRY] cola={} intento {}/{} falló: {} -> se reintenta",
                    queue, attempt, maxAttempts, cause.getMessage());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            channel.basicNack(deliveryTag, false, false);
            count(queue, "dlq");
            log.error("[DLQ] cola={} -> {} | reintento interrumpido", queue, topology.dlqOf(queue));
        } catch (Exception republishError) {
            log.error("[DLQ] cola={} -> {} | no se pudo republicar el reintento: {}",
                    queue, topology.dlqOf(queue), republishError.getMessage());
            channel.basicNack(deliveryTag, false, false);
            count(queue, "dlq");
        }
    }

    /**
     * Solo para la demostración de reintentos y DLQ (desactivable con
     * RABBIT_ALLOW_FAILURE_SIMULATION=false):
     *   x-simulate-failure=transient -> error recuperable (reintentos y luego DLQ)
     *   x-simulate-failure=fatal     -> error no recuperable (DLQ inmediata)
     */
    private void simulateFailureIfRequested(Message message) throws Exception {
        if (!topology.getRetry().isAllowFailureSimulation()) {
            return;
        }
        Object mode = message.getMessageProperties().getHeaders().get(SIMULATE_HEADER);
        if ("fatal".equals(String.valueOf(mode))) {
            throw new NonRecoverableMessageException("Fallo NO recuperable simulado (x-simulate-failure=fatal)");
        }
        if ("transient".equals(String.valueOf(mode))) {
            throw new IllegalStateException("Fallo recuperable simulado (x-simulate-failure=transient)");
        }
    }

    private static int retryCount(Message message) {
        Object header = message.getMessageProperties().getHeaders().get(RETRY_HEADER);
        return header instanceof Number n ? n.intValue() : 0;
    }

    private static String preview(Message message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
