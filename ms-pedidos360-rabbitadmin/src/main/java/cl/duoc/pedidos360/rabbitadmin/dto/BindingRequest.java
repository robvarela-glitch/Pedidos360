package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** POST /api/rabbitadmin/bindings  y  DELETE /api/rabbitadmin/bindings */
public record BindingRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE)
        String exchange,
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE)
        String queue,
        @NotNull(message = "es obligatorio (puede ser vacío \"\" para fanout)")
        @Size(max = 255, message = "máximo 255 caracteres")
        @Pattern(regexp = ResourceNames.ROUTING_KEY_REGEX, message = ResourceNames.ROUTING_KEY_MESSAGE)
        String routingKey
) {
}
