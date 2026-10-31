package cl.duoc.pedidos360.orders.security;

import cl.duoc.pedidos360.orders.config.JwtSecuritySupport;
import cl.duoc.pedidos360.orders.config.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Usuario autenticado, obtenido SIEMPRE del JWT validado (nunca del cuerpo de la petición).
 *
 * @param id     claim "oid" (o "sub"): identificador estable del usuario en Entra ID
 * @param name   nombre para mostrar (claim "name" o "preferred_username")
 * @param token  JWT original, para reenviarlo a otros microservicios (ej. catálogo)
 */
public record CurrentUser(String id, String name, boolean admin, boolean operador, String token) {

    /** Admin u Operador ven y gestionan todos los pedidos. */
    public boolean canManageAllOrders() {
        return admin || operador;
    }

    public static CurrentUser fromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No hay un usuario autenticado con JWT");
        }
        Jwt jwt = jwtAuth.getToken();
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        boolean admin = hasRole(auth, Roles.ADMIN);
        boolean operador = hasRole(auth, Roles.OPERADOR);
        return new CurrentUser(JwtSecuritySupport.userId(jwt), name, admin, operador, jwt.getTokenValue());
    }

    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }
}
