package cl.duoc.pedidos360.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Solicitud de creación de pedido.
 * NO incluye customerId ni precios: el cliente se toma del JWT y los precios del catálogo.
 * (Si el JSON trae esos campos, se ignoran.)
 */
@Getter
@Setter
public class CreateOrderRequest {

    @NotEmpty(message = "El pedido debe tener al menos un producto")
    @Size(max = 50, message = "Máximo 50 productos por pedido")
    @Valid
    private List<OrderItemRequest> items;
}
