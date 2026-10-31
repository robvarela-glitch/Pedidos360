package cl.duoc.pedidos360.report.messaging;

/**
 * Error NO recuperable: el mensaje está mal formado o no cumple el contrato.
 * Reintentarlo no sirve, así que se envía directamente a la DLQ.
 */
public class NonRecoverableMessageException extends RuntimeException {
    public NonRecoverableMessageException(String message) {
        super(message);
    }

    public NonRecoverableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
