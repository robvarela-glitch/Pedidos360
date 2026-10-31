package cl.duoc.pedidos360.orders.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull(message = "productId es obligatorio")
    @Positive
    private Long productId;

    @NotNull(message = "quantity es obligatorio")
    @Positive
    @Max(value = 100, message = "Máximo 100 unidades por producto")
    private Integer quantity;
}
