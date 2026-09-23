package cl.duoc.pedidos360.audit.controller;

import cl.duoc.pedidos360.audit.model.AuditLog;
import cl.duoc.pedidos360.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-audit activo");
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> findAll() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/logs/pedido/{orderId}")
    public ResponseEntity<List<AuditLog>> findByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(auditLogRepository.findByEntityId(orderId));
    }
}
