package cl.duoc.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /api/rabbitadmin/exchanges */
public record CreateExchangeRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = ResourceNames.NAME_REGEX, message = ResourceNames.NAME_MESSAGE)
        String name,
        @NotNull(message = "es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)")
        ExchangeKind type,
        Boolean durable
) {
    public boolean durableOrDefault() {
        return durable == null || durable;
    }
}
