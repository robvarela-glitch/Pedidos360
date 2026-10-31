package cl.duoc.pedidos360.rabbitadmin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración del administrador (prefijo pedidos360.rabbitadmin en application.properties).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pedidos360.rabbitadmin")
public class RabbitAdminProperties {

    /** Exchange de dead-letter al que se enlazan las DLQ creadas por la API. */
    private String deadLetterExchange = "pedidos360.dlx";

    /** Recursos de la topología base que la API NO permite eliminar. */
    private List<String> protectedNames = new ArrayList<>();

    /** API HTTP de administración de RabbitMQ (plugin management), usada solo para listar. */
    private Management management = new Management();

    @Getter
    @Setter
    public static class Management {
        private String url = "http://localhost:15672";
        private String username = "guest";
        private String password = "guest";
    }
}
