package cl.duoc.pedidos360.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

/**
 * Este microservicio es un Resource Server: valida por sí mismo el JWT que le
 * reenvía el BFF (defensa en profundidad). Las reglas por rol se aplican en los
 * controladores con @PreAuthorize (ver Roles).
 *
 * Sin token o token inválido -> 401. Token válido sin el rol necesario -> 403.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Métricas para Prometheus: solo se aceptan desde la red interna de Docker
     * (172.16.0.0/12 y 192.168.0.0/16) o desde la propia máquina. Desde Internet -> 403.
     */
    private static final WebExpressionAuthorizationManager INTERNAL_NETWORK = new WebExpressionAuthorizationManager(
            "hasIpAddress('172.16.0.0/12') or hasIpAddress('192.168.0.0/16') or hasIpAddress('127.0.0.1')");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtSecuritySupport jwtSupport) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Métricas (Prometheus): solo red interna
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").access(INTERNAL_NETWORK)
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                        "/api/audit/publico/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtSupport.authenticationConverter()))
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );
        return http.build();
    }
}
