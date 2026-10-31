package cl.duoc.pedidos360.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka es OPCIONAL en esta versión (pedidos360.kafka.enabled=true para activarlo).
 * La mensajería evaluada se implementa con RabbitMQ; Kafka se conserva como canal
 * adicional de eventos (topic orders.events) para quien quiera levantarlo con el
 * perfil "kafka" de docker-compose.
 */
@Configuration
@ConditionalOnProperty(name = "pedidos360.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic ordersEventsTopic(org.springframework.core.env.Environment env) {
        return TopicBuilder.name(env.getProperty("pedidos360.kafka.topic", "orders.events"))
                .partitions(3).replicas(1).build();
    }
}
