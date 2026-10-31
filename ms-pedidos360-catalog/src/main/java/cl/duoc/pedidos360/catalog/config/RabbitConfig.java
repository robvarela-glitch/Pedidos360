package cl.duoc.pedidos360.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ms-catalog es productor: publica eventos product.* en el exchange topic de dominio. */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventsTopicExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.topicExchange(props.getExchanges().getTopic()).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
