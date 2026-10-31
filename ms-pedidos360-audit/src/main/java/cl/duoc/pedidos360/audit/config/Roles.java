package cl.duoc.pedidos360.audit.config;

/**
 * App roles definidos en Microsoft Entra ID para Pedidos360 (claim "roles" del JWT).
 * Se usan en @PreAuthorize como hasRole('Admin'), hasRole('Operador'), hasRole('Cliente').
 */
public final class Roles {
    public static final String ADMIN = "Admin";
    public static final String OPERADOR = "Operador";
    public static final String CLIENTE = "Cliente";

    private Roles() {
    }
}
