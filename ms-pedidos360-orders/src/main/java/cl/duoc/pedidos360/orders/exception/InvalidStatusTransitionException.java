package cl.duoc.pedidos360.orders.exception;

import cl.duoc.pedidos360.orders.model.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("No se puede cambiar el pedido de " + from + " a " + to);
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
