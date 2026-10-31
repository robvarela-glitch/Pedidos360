package cl.duoc.pedidos360.rabbitadmin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * POST /api/rabbitadmin/messages: publica un mensaje de prueba (por ejemplo, para
 * demostrar reintentos y DLQ con el header x-simulate-failure=transient|fatal).
 */
public record PublishMessageRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE)
        String exchange,
        @NotNull(message = "es obligatorio")
        @Size(max = 255, message = "máximo 255 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "solo letras, números, '.', '-' y '_' (sin comodines al publicar)")
        String routingKey,
        @NotNull(message = "es obligatorio")
        JsonNode payload,
        @Size(max = 10, message = "máximo 10 headers")
        Map<@Pattern(regexp = "^x-[a-z0-9-]{1,40}$", message = "los headers deben tener formato x-nombre") String,
            @Size(max = 200) String> headers
) {
}
