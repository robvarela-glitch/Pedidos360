package cl.duoc.pedidos360.orders.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración RabbitMQ del PRODUCTOR de eventos de pedidos.
 *
 * ms-orders declara los exchanges a los que publica (las colas las declara cada
 * consumidor, que es su dueño). Los nombres vienen de RabbitTopologyProperties.
 *
 * Además activa publisher confirms y returns para registrar en el log si un
 * mensaje no llegó al broker o no tenía ninguna cola de destino.
 */
@Slf4j
@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange ordersDirectExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.directExchange(props.getExchanges().getDirect()).durable(true).build();
    }

    @Bean
    public TopicExchange eventsTopicExchange(RabbitTopologyProperties props) {
        return ExchangeBuilder.topicExchange(props.getExchanges().getTopic()).durable(true).build();
    }

    /** Serializa los eventos como JSON (fechas ISO-8601) usando el ObjectMapper de Spring Boot. */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplateCustomizer publisherConfirmsCustomizer() {
        return template -> {
            template.setMandatory(true);
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    log.error("RabbitMQ NO confirmó la publicación del mensaje {}: {}", correlation, cause);
                }
            });
            template.setReturnsCallback(returned -> log.error(
                    "Mensaje sin cola de destino: exchange={}, routingKey={}, motivo={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        };
    }
}
