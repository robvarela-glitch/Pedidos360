package cl.duoc.pedidos360.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro inmutable de auditoria: deja trazabilidad de TODO evento de negocio
 * ocurrido en Pedidos360 (por ahora, todos los eventos de orders via Kafka).
 */
@Entity
@Table(name = "PED360_AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SOURCE_SERVICE", nullable = false)
    private String sourceService;

    @Column(name = "ENTITY_ID", nullable = false)
    private String entityId;

    @Column(name = "EVENT_TYPE", nullable = false)
    private String eventType;

    @Column(name = "PAYLOAD", length = 1000)
    private String payload;

    @Column(name = "RECORDED_AT", nullable = false)
    private LocalDateTime recordedAt;
}
