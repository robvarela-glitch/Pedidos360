package cl.duoc.pedidos360.report.config;

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
 * Caso de uso "Reportes de ventas":
 *
 *   pedidos360.events.topic (topic) --[order.*]--> pedidos360.report.orders
 *   pedidos360.report.orders --(rechazo)--> pedidos360.dlx --> pedidos360.report.orders.dlq
 *
 * El patrón order.* recibe SOLO eventos de pedidos (order.created, order.cancelled...),
 * no los de productos (product.*).
 *
 * ms-report es el dueño de su cola y de su DLQ, por eso las declara aquí.
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
    public Queue reportQueue(RabbitTopologyProperties props) {
        String queue = props.getQueues().getReport();
        return QueueBuilder.durable(queue)
                .deadLetterExchange(props.getExchanges().getDeadLetter())
                .deadLetterRoutingKey(props.dlqOf(queue))
                .build();
    }

    @Bean
    public Queue reportDeadLetterQueue(RabbitTopologyProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getReport())).build();
    }

    @Bean
    public Binding reportBinding(Queue reportQueue, TopicExchange eventsTopicExchange, RabbitTopologyProperties props) {
        return BindingBuilder.bind(reportQueue).to(eventsTopicExchange).with(props.getBindings().getReport());
    }

    @Bean
    public Binding reportDeadLetterBinding(Queue reportDeadLetterQueue, DirectExchange deadLetterExchange,
                                          RabbitTopologyProperties props) {
        return BindingBuilder.bind(reportDeadLetterQueue).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getReport()));
    }

    // Sin MessageConverter JSON a propósito: el listener recibe el Message crudo y lo
    // deserializa él mismo (objectMapper.readValue) para decidir ACK / reintento / DLQ.
    // Con Jackson2JsonMessageConverter, Spring intentaba convertir el mensaje ANTES de
    // llamar al listener usando el header __TypeId__ del productor
    // (cl.duoc.pedidos360.orders.messaging.OrderEvent), clase que no existe en este
    // servicio -> ClassNotFoundException y todos los eventos terminaban en la DLQ.
}
