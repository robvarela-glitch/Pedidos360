package cl.duoc.pedidos360.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Configuración de seguridad del BFF.
 *
 * Valida los JWT emitidos por Entra ID y aplica autorización
 * según los roles definidos en el claim "roles".
 *
 * Validaciones del JWT:
 *   1. Firma
 *   2. Issuer
 *   3. Expiración
 *   4. Audience
 *
 * Si el JWT falla cualquiera de las validaciones -> 401.
 * Si el JWT es válido pero falta el rol requerido -> 403.
 */
@Configuration
public class SecurityConfig {

    private final String issuerUri;
    private final String audience;

    public SecurityConfig(Environment env) {
        this.issuerUri = env.getProperty(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );

        this.audience = env.getProperty(
            "pedidos360.security.audience"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http
    ) throws Exception {

        http
            .csrf(
                AbstractHttpConfigurer::disable
            )

            .sessionManagement(
                sm -> sm.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(
                auth -> auth

                    // Endpoints públicos
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/api/*/publico/**"
                    )
                    .permitAll()

                    // -------------------------
                    // PEDIDOS
                    // -------------------------

                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/orders/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Orders.Read",
                        "ROLE_Orders.Write",
                        "ROLE_Admin"
                    )

                    .requestMatchers(
                        "/api/orders/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Orders.Write",
                        "ROLE_Admin"
                    )

                    // -------------------------
                    // CATÁLOGO
                    // -------------------------

                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/catalog/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Catalog.Read",
                        "ROLE_Catalog.Write",
                        "ROLE_Admin"
                    )

                    .requestMatchers(
                        "/api/catalog/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Catalog.Write",
                        "ROLE_Admin"
                    )

                    // -------------------------
                    // REPORTES
                    // -------------------------

                    .requestMatchers(
                        "/api/reports/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Reports.Read",
                        "ROLE_Admin"
                    )

                    // -------------------------
                    // AUDITORÍA
                    // -------------------------

                    .requestMatchers(
                        "/api/audit/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Audit.Read",
                        "ROLE_Admin"
                    )

                    // -------------------------
                    // NOTIFICACIONES
                    // -------------------------

                    .requestMatchers(
                        "/api/notify/**"
                    )
                    .hasAnyAuthority(
                        "ROLE_Notify.Read",
                        "ROLE_Admin"
                    )

                    // Cualquier otra ruta requiere JWT válido
                    .anyRequest()
                    .authenticated()
            )

            .oauth2ResourceServer(
                oauth2 -> oauth2

                    .jwt(
                        jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(
                                jwtAuthenticationConverter()
                            )
                    )

                    // 401 cuando el token es inválido
                    .authenticationEntryPoint(
                        new BearerTokenAuthenticationEntryPoint()
                    )

                    // 403 cuando falta el rol
                    .accessDeniedHandler(
                        new BearerTokenAccessDeniedHandler()
                    )
            );

        return http.build();
    }

    /**
     * Construye el JwtDecoder que valida:
     *
     * - Firma
     * - Issuer
     * - Expiración
     * - Audience
     */
    @Bean
    public JwtDecoder jwtDecoder() {

        NimbusJwtDecoder decoder =
            (NimbusJwtDecoder)
            JwtDecoders.fromIssuerLocation(
                issuerUri
            );

        OAuth2TokenValidator<Jwt> defaultValidators =
            JwtValidators.createDefaultWithIssuer(
                issuerUri
            );

        OAuth2TokenValidator<Jwt> audienceValidator =
            jwt -> {

                List<String> audiences =
                    jwt.getAudience();

                if (
                    audiences != null &&
                    audiences.contains(audience)
                ) {
                    return OAuth2TokenValidatorResult.success();
                }

                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                        "invalid_token",
                        "El token no fue emitido para esta API (audience)",
                        null
                    )
                );
            };

        decoder.setJwtValidator(
            new DelegatingOAuth2TokenValidator<>(
                defaultValidators,
                audienceValidator
            )
        );

        return decoder;
    }

    /**
     * Traduce el claim "roles" del JWT
     * a authorities con prefijo ROLE_.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName(
            "roles"
        );

        authoritiesConverter.setAuthorityPrefix(
            "ROLE_"
        );

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
        );

        return converter;
    }
}