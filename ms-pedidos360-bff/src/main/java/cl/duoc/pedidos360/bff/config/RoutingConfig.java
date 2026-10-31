package cl.duoc.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Mapea cada prefijo de ruta pública ("orders", "catalog", "notify", "reports",
 * "audit", "rabbitadmin") a la URL base del microservicio interno que lo atiende.
 * El ProxyController usa este mapa para reenviar cada petición ya autenticada.
 *
 * En docker-compose los nombres de servicio (ms-orders, ms-catalog, etc.)
 * resuelven por DNS interno; en local, cada variable cae por defecto a localhost.
 */
@Configuration
public class RoutingConfig {

    @Bean
    public Map<String, String> serviceBaseUrls(
            @Value("${services.orders.url:http://localhost:8081}") String ordersUrl,
            @Value("${services.catalog.url:http://localhost:8082}") String catalogUrl,
            @Value("${services.notify.url:http://localhost:8083}") String notifyUrl,
            @Value("${services.report.url:http://localhost:8084}") String reportUrl,
            @Value("${services.audit.url:http://localhost:8085}") String auditUrl,
            @Value("${services.rabbitadmin.url:http://localhost:8086}") String rabbitAdminUrl
    ) {
        return Map.of(
                "orders", ordersUrl,
                "catalog", catalogUrl,
                "notify", notifyUrl,
                "reports", reportUrl,
                "audit", auditUrl,
                "rabbitadmin", rabbitAdminUrl
        );
    }

    /**
     * Cliente HTTP del proxy. Usa el HttpClient de Java 11+, que soporta todos los
     * métodos (incluido PATCH, que el cliente básico HttpURLConnection rechaza).
     * HTTP/1.1 explícito: los microservicios internos no usan HTTP/2.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory);
    }
}