package cl.duoc.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Mapea cada prefijo de ruta pública ("orders", "catalog", "notify",
 * "reports", "audit") a la URL base del microservicio interno real que lo
 * atiende. El {@link cl.duoc.pedidos360.bff.controller.ProxyController} usa
 * este mapa para saber a dónde reenviar cada petición ya autenticada.
 *
 * En docker-compose los nombres de servicio (ms-orders, ms-catalog, etc.)
 * resuelven por DNS interno; en local, cada variable cae por defecto a
 * localhost con el puerto que usa Agustín en cada application.properties.
 */
@Configuration
public class RoutingConfig {

    @Bean
    public Map<String, String> serviceBaseUrls(
            @Value("${services.orders.url:http://localhost:8081}") String ordersUrl,
            @Value("${services.catalog.url:http://localhost:8082}") String catalogUrl,
            @Value("${services.notify.url:http://localhost:8083}") String notifyUrl,
            @Value("${services.report.url:http://localhost:8084}") String reportUrl,
            @Value("${services.audit.url:http://localhost:8085}") String auditUrl
    ) {
        return Map.of(
                "orders", ordersUrl,
                "catalog", catalogUrl,
                "notify", notifyUrl,
                "reports", reportUrl,
                "audit", auditUrl
        );
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
