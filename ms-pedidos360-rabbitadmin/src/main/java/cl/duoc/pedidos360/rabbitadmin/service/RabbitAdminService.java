package cl.duoc.pedidos360.rabbitadmin.service;

import cl.duoc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.OperationResponse;
import cl.duoc.pedidos360.rabbitadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.rabbitadmin.exception.BrokerOperationException;
import cl.duoc.pedidos360.rabbitadmin.exception.ProtectedResourceException;
import cl.duoc.pedidos360.rabbitadmin.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Toda la lógica de administración de RabbitMQ: crear y eliminar colas (con su DLQ),
 * exchanges y bindings, consultar su estado y publicar mensajes de prueba.
 *
 * El controlador solo llama a estos métodos de alto nivel: no conoce AmqpAdmin,
 * RabbitTemplate ni la API HTTP de RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementClient managementClient;
    private final RabbitAdminProperties props;

    // ------------------------------------------------------------------ colas

    public QueueInfoResponse createQueue(CreateQueueRequest request) {
        String name = request.name();
        if (amqpAdmin.getQueueInfo(name) != null) {
            throw new BrokerOperationException("La cola '" + name + "' ya existe", null);
        }
        QueueBuilder builder = request.durableOrDefault() ? QueueBuilder.durable(name) : QueueBuilder.nonDurable(name);
        String dlqName = request.withDeadLetterQueueOrDefault() ? name + ".dlq" : null;
        if (dlqName != null) {
            ensureDeadLetterExchange();
            Queue dlq = QueueBuilder.durable(dlqName).build();
            run("crear la DLQ " + dlqName, () -> amqpAdmin.declareQueue(dlq));
            run("enlazar la DLQ " + dlqName, () -> amqpAdmin.declareBinding(
                    new Binding(dlqName, Binding.DestinationType.QUEUE, props.getDeadLetterExchange(), dlqName, null)));
            builder.deadLetterExchange(props.getDeadLetterExchange()).deadLetterRoutingKey(dlqName);
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs().intValue());
        }
        if (request.maxLength() != null) {
            builder.maxLength(request.maxLength());
        }
        Queue queue = builder.build();
        run("crear la cola " + name, () -> amqpAdmin.declareQueue(queue));
        log.info("[ADMIN] Cola creada: {} (DLQ: {})", name, dlqName);
        return new QueueInfoResponse(name, 0, 0, dlqName);
    }

    public QueueInfoResponse getQueue(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        String dlq = amqpAdmin.getQueueInfo(name + ".dlq") != null ? name + ".dlq" : null;
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount(), dlq);
    }

    public OperationResponse deleteQueue(String name, boolean includeDeadLetterQueue) {
        assertNotProtected(name);
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        run("eliminar la cola " + name, () -> amqpAdmin.deleteQueue(name));
        String detail = "Cola eliminada";
        if (includeDeadLetterQueue && amqpAdmin.getQueueInfo(name + ".dlq") != null) {
            assertNotProtected(name + ".dlq");
            run("eliminar la DLQ " + name + ".dlq", () -> amqpAdmin.deleteQueue(name + ".dlq"));
            detail = "Cola y su DLQ eliminadas";
        }
        log.info("[ADMIN] {}: {}", detail, name);
        return OperationResponse.of("queue", name, "DELETED", detail);
    }

    public OperationResponse purgeQueue(String name) {
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        amqpAdmin.purgeQueue(name, false);
        log.info("[ADMIN] Cola vaciada: {}", name);
        return OperationResponse.of("queue", name, "PURGED", "Mensajes eliminados de la cola");
    }

    public List<Map<String, Object>> listQueues() {
        return managementClient.listQueues();
    }

    // -------------------------------------------------------------- exchanges

    public OperationResponse createExchange(CreateExchangeRequest request) {
        Exchange exchange = new ExchangeBuilder(request.name(), request.type().amqpType())
                .durable(request.durableOrDefault())
                .build();
        run("crear el exchange " + request.name(), () -> amqpAdmin.declareExchange(exchange));
        log.info("[ADMIN] Exchange creado: {} ({})", request.name(), request.type());
        return OperationResponse.of("exchange", request.name(), "CREATED", "Exchange de tipo " + request.type());
    }

    public OperationResponse deleteExchange(String name) {
        assertNotProtected(name);
        boolean deleted = run("eliminar el exchange " + name, () -> amqpAdmin.deleteExchange(name));
        if (!deleted) {
            throw new ResourceNotFoundException("El exchange '" + name + "' no existe");
        }
        log.info("[ADMIN] Exchange eliminado: {}", name);
        return OperationResponse.of("exchange", name, "DELETED", "Exchange eliminado");
    }

    public List<Map<String, Object>> listExchanges() {
        return managementClient.listExchanges();
    }

    // --------------------------------------------------------------- bindings

    public OperationResponse createBinding(BindingRequest request) {
        requireQueue(request.queue());
        Binding binding = new Binding(request.queue(), Binding.DestinationType.QUEUE,
                request.exchange(), request.routingKey(), null);
        run("crear el binding (verifique que el exchange '" + request.exchange() + "' exista)",
                () -> amqpAdmin.declareBinding(binding));
        log.info("[ADMIN] Binding creado: {} --[{}]--> {}", request.exchange(), request.routingKey(), request.queue());
        return OperationResponse.of("binding", request.exchange() + " -> " + request.queue(), "CREATED",
                "routingKey=" + request.routingKey());
    }

    public OperationResponse deleteBinding(BindingRequest request) {
        assertNotProtected(request.exchange());
        assertNotProtected(request.queue());
        Binding binding = new Binding(request.queue(), Binding.DestinationType.QUEUE,
                request.exchange(), request.routingKey(), null);
        run("eliminar el binding", () -> amqpAdmin.removeBinding(binding));
        log.info("[ADMIN] Binding eliminado: {} --[{}]--> {}", request.exchange(), request.routingKey(), request.queue());
        return OperationResponse.of("binding", request.exchange() + " -> " + request.queue(), "DELETED",
                "routingKey=" + request.routingKey());
    }

    public List<Map<String, Object>> listBindings() {
        return managementClient.listBindings();
    }

    // ---------------------------------------------------------- clúster/test

    public List<Map<String, Object>> listNodes() {
        return managementClient.listNodes();
    }

    public OperationResponse publish(PublishMessageRequest request) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("x-source-service", "ms-pedidos360-rabbitadmin");
        if (request.headers() != null) {
            request.headers().forEach(properties::setHeader);
        }
        Message message = new Message(request.payload().toString().getBytes(StandardCharsets.UTF_8), properties);
        run("publicar en el exchange " + request.exchange(), () -> {
            rabbitTemplate.send(request.exchange(), request.routingKey(), message);
            return null;
        });
        log.info("[ADMIN] Mensaje de prueba publicado en {} con routing key {}", request.exchange(), request.routingKey());
        return OperationResponse.of("message", request.exchange(), "PUBLISHED", "routingKey=" + request.routingKey());
    }

    // ---------------------------------------------------------------- apoyo

    private void ensureDeadLetterExchange() {
        DirectExchange dlx = new DirectExchange(props.getDeadLetterExchange(), true, false);
        run("declarar el exchange de dead-letter", () -> {
            amqpAdmin.declareExchange(dlx);
            return null;
        });
    }

    private void requireQueue(String queue) {
        if (amqpAdmin.getQueueInfo(queue) == null) {
            throw new ResourceNotFoundException("La cola '" + queue + "' no existe");
        }
    }

    private void assertNotProtected(String name) {
        if (props.getProtectedNames().stream().map(String::trim).anyMatch(name::equals)) {
            throw new ProtectedResourceException(name);
        }
    }

    private <T> T run(String action, java.util.function.Supplier<T> operation) {
        try {
            return operation.get();
        } catch (AmqpException e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String cause = root.getMessage() != null ? root.getMessage() : e.getMessage();
            throw new BrokerOperationException("RabbitMQ rechazó la operación: no se pudo " + action + ". " + cause, e);
        }
    }

    private void run(String action, Runnable operation) {
        run(action, () -> {
            operation.run();
            return null;
        });
    }
}
