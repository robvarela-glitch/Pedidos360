package cl.duoc.pedidos360.rabbitadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validación del JWT emitido por Microsoft Entra ID (IDaaS). Es la misma clase en
 * todos los microservicios de Pedidos360, para que todos apliquen las mismas reglas.
 *
 * Valida:
 *   1. Firma   -> claves públicas (JWKS) del tenant: pedidos360.security.jwk-set-uri
 *   2. Expiración (exp / nbf)
 *   3. Issuer  -> debe ser el tenant de Pedidos360 (se aceptan los formatos v1 y v2 de Entra ID)
 *   4. Audience -> el token debe estar emitido para la API de Pedidos360
 *                  (api://<client-id> en tokens v1 o <client-id> en tokens v2)
 *   5. Scope   -> el token delegado debe incluir el scope "access_as_user" (claim scp)
 *
 * Además traduce:
 *   - claim "roles" (App roles de Entra ID: Admin, Operador, Cliente) -> ROLE_Admin, ROLE_Operador, ROLE_Cliente
 *   - claim "scp" -> SCOPE_access_as_user
 *   - identidad del usuario -> claim "oid" (identificador estable del usuario en el tenant);
 *     si no viene, se usa "sub".
 */
@Component
public class JwtSecuritySupport {

    private final String jwkSetUri;
    private final Set<String> issuers;
    private final Set<String> audiences;
    private final String requiredScope;
    private final String defaultRole;

    public JwtSecuritySupport(
            @Value("${pedidos360.security.jwk-set-uri}") String jwkSetUri,
            @Value("${pedidos360.security.issuers}") List<String> issuers,
            @Value("${pedidos360.security.audiences}") List<String> audiences,
            @Value("${pedidos360.security.required-scope:}") String requiredScope,
            @Value("${pedidos360.security.default-role:}") String defaultRole) {
        this.jwkSetUri = jwkSetUri;
        this.issuers = trimAll(issuers);
        this.audiences = trimAll(audiences);
        this.requiredScope = requiredScope == null ? "" : requiredScope.trim();
        this.defaultRole = defaultRole == null ? "" : defaultRole.trim();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator(),
                audienceValidator(),
                scopeValidator()));
        return decoder;
    }

    /** Convierte el JWT ya validado en una autenticación de Spring Security. */
    public Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt), userId(jwt));
    }

    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        } else if (!defaultRole.isEmpty()) {
            // Usuario del tenant sin App role asignado: se le trata como el rol por defecto
            // (configurable con DEFAULT_ROLE; dejar vacío para exigir rol explícito).
            authorities.add(new SimpleGrantedAuthority("ROLE_" + defaultRole));
        }
        for (String scope : scopes(jwt)) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        return authorities;
    }

    public static String userId(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return (oid != null && !oid.isBlank()) ? oid : jwt.getSubject();
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            String iss = jwt.getClaimAsString("iss");
            if (iss != null && issuers.contains(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("El token no fue emitido por el tenant de Pedidos360 (issuer)");
        };
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> {
            List<String> tokenAudiences = jwt.getAudience();
            if (tokenAudiences != null && tokenAudiences.stream().anyMatch(audiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("El token no fue emitido para la API de Pedidos360 (audience)");
        };
    }

    private OAuth2TokenValidator<Jwt> scopeValidator() {
        return jwt -> {
            if (requiredScope.isEmpty() || scopes(jwt).contains(requiredScope)) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("El token no incluye el scope requerido: " + requiredScope);
        };
    }

    private static Set<String> scopes(Jwt jwt) {
        String scp = jwt.getClaimAsString("scp");
        if (scp == null || scp.isBlank()) {
            return Set.of();
        }
        return new java.util.HashSet<>(java.util.Arrays.asList(scp.trim().split("\\s+")));
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }

    private static Set<String> trimAll(List<String> values) {
        return values.stream().map(String::trim).filter(v -> !v.isEmpty()).collect(Collectors.toSet());
    }
}
