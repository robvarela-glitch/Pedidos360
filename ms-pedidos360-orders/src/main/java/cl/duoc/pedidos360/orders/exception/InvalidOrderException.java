package cl.duoc.pedidos360.orders.exception;

/** Datos del pedido inválidos según las reglas de negocio (producto inexistente, inactivo o sin stock). */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
