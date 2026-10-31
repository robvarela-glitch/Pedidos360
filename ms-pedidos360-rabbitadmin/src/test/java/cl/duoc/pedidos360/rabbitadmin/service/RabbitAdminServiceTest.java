package cl.duoc.pedidos360.rabbitadmin.service;

import cl.duoc.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import cl.duoc.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitadmin.dto.ExchangeKind;
import cl.duoc.pedidos360.rabbitadmin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.rabbitadmin.exception.BrokerOperationException;
import cl.duoc.pedidos360.rabbitadmin.exception.ProtectedResourceException;
import cl.duoc.pedidos360.rabbitadmin.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        RabbitAdminProperties props = new RabbitAdminProperties();
        props.setDeadLetterExchange("pedidos360.dlx");
        props.setProtectedNames(List.of("pedidos360.notify.orders", "pedidos360.events.topic"));
        service = new RabbitAdminService(amqpAdmin, mock(RabbitTemplate.class), mock(RabbitManagementClient.class), props);
    }

    @Test
    void crearCola_conDlq_declaraColaDlqYBinding() {
        QueueInfoResponse response = service.createQueue(new CreateQueueRequest("pruebas.cola", true, true, null, null));

        ArgumentCaptor<Queue> queues = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin, atLeastOnce()).declareQueue(queues.capture());
        assertThat(queues.getAllValues()).extracting(Queue::getName).containsExactly("pruebas.cola.dlq", "pruebas.cola");
        Queue principal = queues.getAllValues().get(1);
        assertThat(principal.getArguments()).containsEntry("x-dead-letter-exchange", "pedidos360.dlx")
                .containsEntry("x-dead-letter-routing-key", "pruebas.cola.dlq");
        verify(amqpAdmin).declareBinding(any(Binding.class));
        assertThat(response.deadLetterQueue()).isEqualTo("pruebas.cola.dlq");
    }

    @Test
    void crearCola_queYaExiste_esRechazada() {
        when(amqpAdmin.getQueueInfo("pruebas.cola")).thenReturn(new QueueInformation("pruebas.cola", 0, 0));

        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("pruebas.cola", true, true, null, null)))
                .isInstanceOf(BrokerOperationException.class);
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void eliminarCola_protegida_esRechazada() {
        assertThatThrownBy(() -> service.deleteQueue("pedidos360.notify.orders", true))
                .isInstanceOf(ProtectedResourceException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarCola_inexistente_404() {
        assertThatThrownBy(() -> service.deleteQueue("no.existe", true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminarCola_existente_eliminaColaYDlq() {
        when(amqpAdmin.getQueueInfo("pruebas.cola")).thenReturn(new QueueInformation("pruebas.cola", 0, 0));
        when(amqpAdmin.getQueueInfo("pruebas.cola.dlq")).thenReturn(new QueueInformation("pruebas.cola.dlq", 0, 0));

        service.deleteQueue("pruebas.cola", true);

        verify(amqpAdmin).deleteQueue("pruebas.cola");
        verify(amqpAdmin).deleteQueue("pruebas.cola.dlq");
    }

    @Test
    void crearExchangeTopic_declaraTipoCorrecto() {
        service.createExchange(new CreateExchangeRequest("pruebas.topic", ExchangeKind.TOPIC, true));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("topic");
        assertThat(captor.getValue().getName()).isEqualTo("pruebas.topic");
    }

    @Test
    void eliminarExchange_protegido_esRechazado() {
        assertThatThrownBy(() -> service.deleteExchange("pedidos360.events.topic"))
                .isInstanceOf(ProtectedResourceException.class);
    }

    @Test
    void crearBinding_aColaInexistente_404() {
        assertThatThrownBy(() -> service.createBinding(new BindingRequest("pruebas.topic", "no.existe", "order.*")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearBinding_valido_declaraBinding() {
        when(amqpAdmin.getQueueInfo("pruebas.cola")).thenReturn(new QueueInformation("pruebas.cola", 0, 0));

        service.createBinding(new BindingRequest("pruebas.topic", "pruebas.cola", "order.*"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertThat(captor.getValue().getExchange()).isEqualTo("pruebas.topic");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("order.*");
    }
}
