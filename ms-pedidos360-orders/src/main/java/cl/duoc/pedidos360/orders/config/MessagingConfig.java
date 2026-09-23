package cl.duoc.pedidos360.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class MessagingConfig {

    public static final String NOTIFY_EXCHANGE = "orders.exchange";
    public static final String NOTIFY_QUEUE = "orders.notifications";
    public static final String NOTIFY_ROUTING_KEY = "orders.notify";

    public static final String ORDERS_EVENTS_TOPIC = "orders.events";

    // ---- RabbitMQ: orders -> notify ----
    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(notificationsQueue).to(ordersExchange).with(NOTIFY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ---- Kafka: orders -> report / audit ----
    @Bean
    public NewTopic ordersEventsTopic() {
        return TopicBuilder.name(ORDERS_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
