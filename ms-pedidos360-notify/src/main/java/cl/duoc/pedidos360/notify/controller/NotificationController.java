package cl.duoc.pedidos360.notify.controller;

import cl.duoc.pedidos360.notify.model.Notification;
import cl.duoc.pedidos360.notify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<Notification>> findAll() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @GetMapping("/notifications/cliente/{customerId}")
    public ResponseEntity<List<Notification>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationRepository.findByCustomerId(customerId));
    }

    @GetMapping("/notifications/pedido/{orderId}")
    public ResponseEntity<List<Notification>> findByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(notificationRepository.findByOrderId(orderId));
    }
}
