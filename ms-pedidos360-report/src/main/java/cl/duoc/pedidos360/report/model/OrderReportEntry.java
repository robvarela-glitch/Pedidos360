package cl.duoc.pedidos360.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de reporte por cada evento de pedido recibido desde Kafka.
 * Sirve tanto para el detalle como para armar agregados (total vendido,
 * cantidad de pedidos por estado, etc.) desde /api/reports.
 */
@Entity
@Table(name = "PED360_ORDER_REPORTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReportEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private String customerId;

    @Column(name = "EVENT_TYPE", nullable = false)
    private String eventType;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "TOTAL", nullable = false)
    private Double total;

    @Column(name = "PROCESSED_AT", nullable = false)
    private LocalDateTime processedAt;
}
