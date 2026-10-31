package cl.duoc.pedidos360.rabbitadmin.dto;

/** Reglas de nombres compartidas por los DTO del administrador. */
public final class ResourceNames {

    /**
     * Nombre de cola o exchange: 1 a 200 caracteres, letras, números, punto, guion o guion bajo,
     * debe empezar con letra o número y NO puede empezar con "amq." (reservado por RabbitMQ).
     */
    public static final String NAME_REGEX = "^(?!amq\\.)[A-Za-z0-9][A-Za-z0-9._-]{0,199}$";
    public static final String NAME_MESSAGE =
            "debe tener 1-200 caracteres (letras, números, '.', '-', '_'), empezar con letra o número y no usar el prefijo reservado 'amq.'";

    /** Routing key / patrón: palabras separadas por punto; en exchanges topic se permiten * y #. */
    public static final String ROUTING_KEY_REGEX = "^$|^([A-Za-z0-9_-]+|\\*|#)(\\.([A-Za-z0-9_-]+|\\*|#))*$";
    public static final String ROUTING_KEY_MESSAGE =
            "debe ser vacía o palabras (letras, números, '-', '_') separadas por '.', con comodines '*' o '#' como palabra completa";

    private ResourceNames() {
    }
}
