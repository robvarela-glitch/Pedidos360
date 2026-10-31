package cl.duoc.pedidos360.audit.controller;

import cl.duoc.pedidos360.audit.model.AuditLog;
import cl.duoc.pedidos360.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Bitácora de auditoría: solo Administrador. */
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
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<AuditLog>> findAll() {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByRecordedAtDesc());
    }

    @GetMapping("/logs/pedido/{orderId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<AuditLog>> findByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(auditLogRepository.findByEntityId(orderId));
    }
}
