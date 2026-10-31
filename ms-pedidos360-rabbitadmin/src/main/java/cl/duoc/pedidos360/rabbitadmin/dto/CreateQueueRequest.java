package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/rabbitadmin/queues
 *
 * @param name               nombre de la cola
 * @param durable            sobrevive a reinicios del broker (por defecto true)
 * @param withDeadLetterQueue crea además "<name>.dlq" y la enlaza al DLX (por defecto true)
 * @param messageTtlMs       tiempo de vida de los mensajes (opcional, máx. 7 días)
 * @param maxLength          máximo de mensajes en la cola (opcional)
 */
public record CreateQueueRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE)
        String name,
        Boolean durable,
        Boolean withDeadLetterQueue,
        @Positive(message = "debe ser mayor que 0") @Max(value = 604_800_000L, message = "no puede superar 7 días")
        Long messageTtlMs,
        @Positive(message = "debe ser mayor que 0") @Max(value = 1_000_000L, message = "no puede superar 1.000.000")
        Integer maxLength
) {
    public boolean durableOrDefault() {
        return durable == null || durable;
    }

    public boolean withDeadLetterQueueOrDefault() {
        return withDeadLetterQueue == null || withDeadLetterQueue;
    }
}
