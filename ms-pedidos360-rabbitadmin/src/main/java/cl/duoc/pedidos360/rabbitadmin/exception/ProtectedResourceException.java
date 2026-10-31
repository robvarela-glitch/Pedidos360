package cl.duoc.pedidos360.rabbitadmin.exception;

/** Se intentó eliminar un recurso que forma parte de la topología base de Pedidos360. */
public class ProtectedResourceException extends RuntimeException {
    public ProtectedResourceException(String name) {
        super("El recurso '" + name + "' es parte de la topología base de Pedidos360 y no se puede eliminar desde la API");
    }
}
