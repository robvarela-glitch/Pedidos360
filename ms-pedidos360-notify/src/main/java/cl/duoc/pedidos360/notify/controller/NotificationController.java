package cl.duoc.pedidos360.notify.controller;

import cl.duoc.pedidos360.notify.config.JwtSecuritySupport;
import cl.duoc.pedidos360.notify.config.Roles;
import cl.duoc.pedidos360.notify.model.Notification;
import cl.duoc.pedidos360.notify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notificaciones. Admin y Operador ven todas; un Cliente solo las suyas
 * (filtradas por el claim "oid" del JWT, nunca por un parámetro enviado por el cliente).
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-notify activo");
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<List<Notification>> findAll(JwtAuthenticationToken auth) {
        if (canSeeAll(auth)) {
            return ResponseEntity.ok(notificationRepository.findAllByOrderBySentAtDesc());
        }
        return ResponseEntity.ok(notificationRepository.findByCustomerIdOrderBySentAtDesc(userId(auth)));
    }

    @GetMapping("/notifications/pedido/{orderId}")
    @PreAuthorize("hasAnyRole('Admin','Operador','Cliente')")
    public ResponseEntity<List<Notification>> findByOrder(@PathVariable Long orderId, JwtAuthenticationToken auth) {
        if (canSeeAll(auth)) {
            return ResponseEntity.ok(notificationRepository.findByOrderIdOrderBySentAtDesc(orderId));
        }
        return ResponseEntity.ok(notificationRepository.findByOrderIdAndCustomerIdOrderBySentAtDesc(orderId, userId(auth)));
    }

    /** Consulta por cliente: solo para Administrador (soporte). */
    @GetMapping("/notifications/cliente/{customerId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<Notification>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationRepository.findByCustomerIdOrderBySentAtDesc(customerId));
    }

    private static boolean canSeeAll(JwtAuthenticationToken auth) {
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_" + Roles.ADMIN) || a.getAuthority().equals("ROLE_" + Roles.OPERADOR));
    }

    private static String userId(JwtAuthenticationToken auth) {
        return JwtSecuritySupport.userId(auth.getToken());
    }
}
