package cl.duoc.pedidos360.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotBlank
    private String customerId;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
}
