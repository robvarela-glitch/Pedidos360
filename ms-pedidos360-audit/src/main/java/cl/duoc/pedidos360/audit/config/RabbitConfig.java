package cl.duoc.pedidos360.audit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso "Auditoría":
 *
 *   pedidos360.events.topic (topic) --[#]--> pedidos360.audit.events
 *   pedidos360.audit.events --(rechazo)--> pedidos360.dlx --> pedidos360.audit.events.dlq
 *
 * El patrón # recibe TODOS los eventos de dominio: pedidos (order.*) y productos (product.*).
 *
 * ms-audit es el dueño de su cola y de su DLQ, por eso las declara aquí.
 * Los nombres y el patrón de binding vienen de RabbitTopologyProperties (application.properties).
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventsTopicExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.topicExchange(props.getExchanges().getTopic()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.directExchange(props.getExchanges().getDeadLetter()).durable(true).build();
    }

    @Bean
    public Queue auditQueue(RabbitTopologyProperties props) {
        String queue = props.getQueues().getAudit();
        return QueueBuilder.durable(queue)
                .deadLetterExchange(props.getExchanges().getDeadLetter())
                .deadLetterRoutingKey(props.dlqOf(queue))
                .build();
    }

    @Bean
    public Queue auditDeadLetterQueue(RabbitTopologyProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getAudit())).build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange eventsTopicExchange, RabbitTopologyProperties props) {
        return BindingBuilder.bind(auditQueue).to(eventsTopicExchange).with(props.getBindings().getAudit());
    }

    @Bean
    public Binding auditDeadLetterBinding(Queue auditDeadLetterQueue, DirectExchange deadLetterExchange,
                                          RabbitTopologyProperties props) {
        return BindingBuilder.bind(auditDeadLetterQueue).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getAudit()));
    }

    // Sin MessageConverter JSON a propósito: el listener recibe el Message crudo y lo
    // deserializa él mismo (objectMapper.readValue) para decidir ACK / reintento / DLQ.
    // Con Jackson2JsonMessageConverter, Spring intentaba convertir el mensaje ANTES de
    // llamar al listener usando el header __TypeId__ del productor
    // (cl.duoc.pedidos360.orders.messaging.OrderEvent), clase que no existe en este
    // servicio -> ClassNotFoundException y todos los eventos terminaban en la DLQ.
}
