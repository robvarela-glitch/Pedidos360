package cl.duoc.pedidos360.catalog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nombres de exchanges, colas, routing keys y bindings de RabbitMQ.
 * Se leen de application.properties (prefijo pedidos360.rabbit) y son los MISMOS
 * en todos los microservicios: no hay nombres escritos a mano en el código de negocio.
 *
 * Topología Pedidos360:
 *   pedidos360.orders.direct (direct) --[notify.order]--> pedidos360.notify.orders   (ms-notify)
 *   pedidos360.events.topic  (topic)  --[order.*]------> pedidos360.report.orders   (ms-report)
 *   pedidos360.events.topic  (topic)  --[#]------------> pedidos360.audit.events    (ms-audit)
 *   pedidos360.dlx           (direct) --[<cola>.dlq]---> <cola>.dlq                 (una DLQ por cola)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class RabbitTopologyProperties {

    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();
    private Bindings bindings = new Bindings();
    private Retry retry = new Retry();

    /** Nombre de la DLQ asociada a una cola: "<cola>.dlq". */
    public String dlqOf(String queue) {
        return queue + ".dlq";
    }

    @Getter
    @Setter
    public static class Exchanges {
        /** Exchange direct: entrega punto a punto (notificaciones al cliente). */
        private String direct = "pedidos360.orders.direct";
        /** Exchange topic: publicación de eventos de dominio por patrón (order.*, product.*). */
        private String topic = "pedidos360.events.topic";
        /** Exchange de dead-letter: recibe los mensajes rechazados de todas las colas. */
        private String deadLetter = "pedidos360.dlx";
    }

    @Getter
    @Setter
    public static class Queues {
        private String notify = "pedidos360.notify.orders";
        private String report = "pedidos360.report.orders";
        private String audit = "pedidos360.audit.events";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        /** Routing key del exchange direct hacia la cola de notificaciones. */
        private String notify = "notify.order";
        /** Prefijo de los eventos de pedidos en el exchange topic (order.created, order.cancelled...). */
        private String orderPrefix = "order";
        /** Prefijo de los eventos de productos en el exchange topic (product.created...). */
        private String productPrefix = "product";
    }

    @Getter
    @Setter
    public static class Bindings {
        /** Patrón de la cola de reportes: solo eventos de pedidos. */
        private String report = "order.*";
        /** Patrón de la cola de auditoría: todos los eventos de dominio. */
        private String audit = "#";
    }

    @Getter
    @Setter
    public static class Retry {
        /** Intentos totales antes de enviar el mensaje a la DLQ (errores recuperables). */
        private int maxAttempts = 3;
        /** Espera base entre reintentos (se multiplica por el número de intento). */
        private long backoffMs = 1000;
        /** Permite simular fallos con el header x-simulate-failure (solo para demostración). */
        private boolean allowFailureSimulation = true;
    }
}
