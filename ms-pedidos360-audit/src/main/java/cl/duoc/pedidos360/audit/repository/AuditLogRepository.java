package cl.duoc.pedidos360.audit.repository;

import cl.duoc.pedidos360.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityId(String entityId);
    List<AuditLog> findBySourceService(String sourceService);
}
