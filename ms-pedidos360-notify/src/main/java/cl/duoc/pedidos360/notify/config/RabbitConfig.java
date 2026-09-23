package cl.duoc.pedidos360.notify.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NOTIFY_QUEUE = "orders.notifications";

    // Declaración idempotente: si orders ya la creó, no pasa nada; si notify
    // arranca primero, la deja lista igual.
    @Bean
    public Queue notificationsQueue() {
        return new Queue(NOTIFY_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
