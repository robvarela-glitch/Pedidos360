package cl.duoc.pedidos360.rabbitadmin.exception;

/** RabbitMQ rechazó la operación (por ejemplo, la cola ya existe con otros argumentos). */
public class BrokerOperationException extends RuntimeException {
    public BrokerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
