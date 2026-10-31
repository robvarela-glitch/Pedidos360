package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** La API no permite crear colas, exchanges ni bindings con nombres vacíos o inválidos. */
class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colaConNombreVacio_esInvalida() {
        assertThat(validator.validate(new CreateQueueRequest("", true, true, null, null))).isNotEmpty();
    }

    @Test
    void colaConPrefijoReservadoAmq_esInvalida() {
        assertThat(validator.validate(new CreateQueueRequest("amq.mi-cola", true, true, null, null))).isNotEmpty();
    }

    @Test
    void colaConEspacios_esInvalida() {
        assertThat(validator.validate(new CreateQueueRequest("mi cola", true, true, null, null))).isNotEmpty();
    }

    @Test
    void colaConTtlNegativo_esInvalida() {
        assertThat(validator.validate(new CreateQueueRequest("pruebas.cola", true, true, -5L, null))).isNotEmpty();
    }

    @Test
    void colaValida_noTieneErrores() {
        assertThat(validator.validate(new CreateQueueRequest("pruebas.cola", true, true, 60000L, 100))).isEmpty();
    }

    @Test
    void exchangeSinTipo_esInvalido() {
        assertThat(validator.validate(new CreateExchangeRequest("pruebas.topic", null, true))).isNotEmpty();
    }

    @Test
    void bindingConPatronTopicValido_noTieneErrores() {
        assertThat(validator.validate(new BindingRequest("pruebas.topic", "pruebas.cola", "order.*"))).isEmpty();
        assertThat(validator.validate(new BindingRequest("pruebas.topic", "pruebas.cola", "#"))).isEmpty();
    }

    @Test
    void bindingConRoutingKeyMalFormada_esInvalido() {
        assertThat(validator.validate(new BindingRequest("pruebas.topic", "pruebas.cola", "order..created"))).isNotEmpty();
        assertThat(validator.validate(new BindingRequest("pruebas.topic", "pruebas.cola", "order.cre*ated"))).isNotEmpty();
    }
}
