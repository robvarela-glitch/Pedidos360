package cl.duoc.pedidos360.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {

    @NotBlank
    private String productId;

    @Positive
    private Integer quantity;

    @Positive
    private Double unitPrice;
}
