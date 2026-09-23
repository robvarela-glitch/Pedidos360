package cl.duoc.pedidos360.orders.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("No se encontró el pedido con id " + id);
    }
}
