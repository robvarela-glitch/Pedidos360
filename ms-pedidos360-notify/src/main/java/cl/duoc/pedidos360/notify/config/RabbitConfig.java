package cl.duoc.pedidos360.notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso "Notificar al cliente":
 *
 *   pedidos360.orders.direct (direct) --[notify.order]--> pedidos360.notify.orders
 *   pedidos360.notify.orders --(rechazo)--> pedidos360.dlx --[pedidos360.notify.orders.dlq]--> pedidos360.notify.orders.dlq
 *
 * ms-notify es el dueño de su cola y de su DLQ, por eso las declara aquí.
 * Los nombres vienen de RabbitTopologyProperties (application.properties).
 */
@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange ordersDirectExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.directExchange(props.getExchanges().getDirect()).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.directExchange(props.getExchanges().getDeadLetter()).durable(true).build();
    }

    @Bean
    public Queue notifyQueue(RabbitTopologyProperties props) {
        String queue = props.getQueues().getNotify();
        return QueueBuilder.durable(queue)
                .deadLetterExchange(props.getExchanges().getDeadLetter())
                .deadLetterRoutingKey(props.dlqOf(queue))
                .build();
    }

    @Bean
    public Queue notifyDeadLetterQueue(RabbitTopologyProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getNotify())).build();
    }

    @Bean
    public Binding notifyBinding(Queue notifyQueue, DirectExchange ordersDirectExchange, RabbitTopologyProperties props) {
        return BindingBuilder.bind(notifyQueue).to(ordersDirectExchange).with(props.getRoutingKeys().getNotify());
    }

    @Bean
    public Binding notifyDeadLetterBinding(Queue notifyDeadLetterQueue, DirectExchange deadLetterExchange,
                                           RabbitTopologyProperties props) {
        return BindingBuilder.bind(notifyDeadLetterQueue).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getNotify()));
    }

    // Sin MessageConverter JSON a propósito: el listener recibe el Message crudo y lo
    // deserializa él mismo (objectMapper.readValue) para decidir ACK / reintento / DLQ.
    // Con Jackson2JsonMessageConverter, Spring intentaba convertir el mensaje ANTES de
    // llamar al listener usando el header __TypeId__ del productor
    // (cl.duoc.pedidos360.orders.messaging.OrderEvent), clase que no existe en este
    // servicio -> ClassNotFoundException y todos los eventos terminaban en la DLQ.
}
