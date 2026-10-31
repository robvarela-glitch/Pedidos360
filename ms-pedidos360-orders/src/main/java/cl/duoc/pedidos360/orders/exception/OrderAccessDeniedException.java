package cl.duoc.pedidos360.orders.exception;

/** El usuario autenticado intenta ver o modificar un pedido que no le pertenece. */
public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException(Long orderId) {
        super("No tiene permiso para acceder al pedido " + orderId);
    }
}
