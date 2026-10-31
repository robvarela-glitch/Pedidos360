package cl.duoc.pedidos360.notify.messaging;

import cl.duoc.pedidos360.notify.config.RabbitTopologyProperties;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Rutas del ACK manual: éxito -> ACK, error no recuperable -> DLQ, recuperable -> reintento y luego DLQ. */
class ManualAckHandlerTest {

    private static final String QUEUE = "pedidos360.notify.orders";

    private RabbitTemplate rabbitTemplate;
    private Channel channel;
    private ManualAckHandler handler;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        channel = mock(Channel.class);
        RabbitTopologyProperties props = new RabbitTopologyProperties();
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setBackoffMs(0);
        meterRegistry = new SimpleMeterRegistry();
        handler = new ManualAckHandler(rabbitTemplate, props, meterRegistry);
    }

    @Test
    void procesamientoCorrecto_haceAck() throws Exception {
        handler.handle(QUEUE, message(0), channel, msg -> { });

        verify(channel).basicAck(7L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
        assertEquals(1.0, counted("ack"));
    }

    @Test
    void errorNoRecuperable_vaDirectoALaDlq() throws Exception {
        handler.handle(QUEUE, message(0), channel, msg -> {
            throw new NonRecoverableMessageException("evento inválido");
        });

        verify(channel).basicNack(7L, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        assertEquals(1.0, counted("dlq"));
    }

    @Test
    void errorRecuperable_primerIntento_seRepublicaEnLaMismaCola() throws Exception {
        handler.handle(QUEUE, message(0), channel, msg -> {
            throw new IllegalStateException("Oracle no disponible");
        });

        verify(rabbitTemplate).send(eq(""), eq(QUEUE), any(Message.class));
        verify(channel).basicAck(7L, false);
        assertEquals(1.0, counted("retry"));
    }

    @Test
    void errorRecuperable_reintentosAgotados_vaALaDlq() throws Exception {
        handler.handle(QUEUE, message(2), channel, msg -> {
            throw new IllegalStateException("Oracle no disponible");
        });

        verify(channel).basicNack(7L, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        assertEquals(1.0, counted("dlq"));
    }

    /** Valor de la métrica pedidos360.rabbit.messages para la cola de prueba y el resultado indicado. */
    private double counted(String result) {
        Counter c = meterRegistry.find(ManualAckHandler.METRIC).tags("queue", QUEUE, "result", result).counter();
        return c == null ? 0.0 : c.count();
    }

    private static Message message(int previousRetries) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        if (previousRetries > 0) {
            props.setHeader(ManualAckHandler.RETRY_HEADER, previousRetries);
        }
        return new Message("{\"orderId\":1}".getBytes(StandardCharsets.UTF_8), props);
    }
}
