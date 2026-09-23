package cl.duoc.pedidos360.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BFF (Backend For Frontend) de Pedidos360.
 *
 * Único punto de entrada HTTP para el frontend Angular (Roberto). No tiene
 * base de datos propia ni lógica de negocio: su responsabilidad es
 *
 *   1. Validar el JWT emitido por el IDaaS (Azure AD / Entra ID) — firma,
 *      issuer, audience y expiración — antes de dejar pasar cualquier petición
 *      (ver {@link cl.duoc.pedidos360.bff.config.SecurityConfig}).
 *   2. Autorizar por rol qué método/ruta puede usar cada usuario.
 *   3. Reenviar la petición ya autenticada al microservicio interno que
 *      corresponda (ms-pedidos360-orders/catalog/notify/report/audit),
 *      incluyendo el mismo JWT (ver {@link cl.duoc.pedidos360.bff.controller.ProxyController}).
 */
@SpringBootApplication
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
