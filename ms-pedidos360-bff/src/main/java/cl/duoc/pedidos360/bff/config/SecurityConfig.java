package cl.duoc.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static cl.duoc.pedidos360.bff.config.Roles.ADMIN;
import static cl.duoc.pedidos360.bff.config.Roles.CLIENTE;
import static cl.duoc.pedidos360.bff.config.Roles.OPERADOR;

/**
 * Seguridad del BFF (único punto de entrada del frontend, detrás del API Manager).
 *
 * - Valida el JWT de Entra ID: firma, issuer, expiración, audience y scope (JwtSecuritySupport).
 * - Autoriza por App role (Admin / Operador / Cliente) con una lista EXPLÍCITA de método + ruta.
 *   Cualquier combinación que no esté en la lista se rechaza (denyAll).
 * - CORS solo para los orígenes configurados (pedidos360.cors.allowed-origins).
 *
 * Sin token / token inválido -> 401. Token válido sin el rol requerido -> 403.
 * La propiedad del recurso (un Cliente solo ve SUS pedidos) la aplica cada microservicio.
 */
@Configuration
public class SecurityConfig {

    /**
     * Métricas para Prometheus: solo se aceptan desde la red interna de Docker
     * (172.16.0.0/12 y 192.168.0.0/16) o desde la propia máquina. Desde Internet -> 403.
     */
    private static final WebExpressionAuthorizationManager INTERNAL_NETWORK = new WebExpressionAuthorizationManager(
            "hasIpAddress('172.16.0.0/12') or hasIpAddress('192.168.0.0/16') or hasIpAddress('127.0.0.1')");

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${pedidos360.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtSecuritySupport jwtSupport) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ---- Públicos por diseño: health checks y ping de cada microservicio ----
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/*/publico/ping").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Métricas (Prometheus): solo red interna
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").access(INTERNAL_NETWORK)
                // Página de error interna de Spring: si queda bloqueada, cualquier fallo
                // interno (500) se transforma en un 403 vacío y se oculta la causa real.
                .requestMatchers("/error").permitAll()

                // ---- Identidad del usuario autenticado (claims del JWT validado) ----
                .requestMatchers(HttpMethod.GET, "/api/me").hasAnyRole(ADMIN, OPERADOR, CLIENTE)

                // ---- Catálogo ----
                .requestMatchers(HttpMethod.GET, "/api/catalog/products", "/api/catalog/products/*").hasAnyRole(ADMIN, OPERADOR, CLIENTE)
                .requestMatchers(HttpMethod.POST, "/api/catalog/products").hasRole(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/catalog/products/*").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/catalog/products/*").hasRole(ADMIN)

                // ---- Pedidos (el microservicio filtra por dueño para el rol Cliente) ----
                .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/*").hasAnyRole(ADMIN, OPERADOR, CLIENTE)
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole(ADMIN, OPERADOR, CLIENTE)
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyRole(ADMIN, OPERADOR)
                .requestMatchers(HttpMethod.DELETE, "/api/orders/*").hasAnyRole(ADMIN, OPERADOR, CLIENTE)

                // ---- Notificaciones (filtradas por dueño en ms-notify) ----
                .requestMatchers(HttpMethod.GET, "/api/notify/notifications", "/api/notify/notifications/**").hasAnyRole(ADMIN, OPERADOR, CLIENTE)

                // ---- Reportes y auditoría: solo Administrador ----
                .requestMatchers(HttpMethod.GET, "/api/reports/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/audit/**").hasRole(ADMIN)

                // ---- Administración de RabbitMQ: solo Administrador ----
                .requestMatchers(HttpMethod.GET, "/api/rabbitadmin/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/rabbitadmin/**").hasRole(ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/rabbitadmin/**").hasRole(ADMIN)

                // Todo lo demás (otros métodos o rutas) se rechaza
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtSupport.authenticationConverter()))
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );

        return http.build();
    }

    /**
     * CORS: orígenes explícitos (sin comodín). El frontend envía el token en el header
     * Authorization (no usa cookies), por eso allowCredentials queda en false.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("WWW-Authenticate"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
