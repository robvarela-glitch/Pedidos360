package cl.duoc.pedidos360.bff.controller;

import cl.duoc.pedidos360.bff.config.JwtSecuritySupport;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Devuelve la identidad que el backend obtuvo del JWT ya validado.
 * Sirve para el frontend (qué rol tiene el usuario) y como evidencia en la
 * presentación: muestra issuer, audience, scope y roles SIN exponer el token.
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(JwtAuthenticationToken authentication) {
        var jwt = authentication.getToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", JwtSecuritySupport.userId(jwt));
        body.put("name", jwt.getClaimAsString("name"));
        body.put("username", firstNonNull(jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("upn"), jwt.getClaimAsString("unique_name")));
        body.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList());
        body.put("rolesInToken", jwt.getClaimAsStringList("roles") != null ? jwt.getClaimAsStringList("roles") : List.of());
        body.put("scope", jwt.getClaimAsString("scp"));
        body.put("issuer", jwt.getClaimAsString("iss"));
        body.put("audience", jwt.getAudience());
        body.put("tokenVersion", jwt.getClaimAsString("ver"));
        body.put("expiresAt", jwt.getExpiresAt());
        return body;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
