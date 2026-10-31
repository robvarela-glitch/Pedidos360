package cl.duoc.pedidos360.report.repository;

import cl.duoc.pedidos360.report.model.OrderReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderReportRepository extends JpaRepository<OrderReportEntry, Long> {

    List<OrderReportEntry> findByCustomerId(String customerId);

    List<OrderReportEntry> findAllByOrderByProcessedAtDesc();

    @Query("SELECT COUNT(r) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CREATED'")
    long countPedidosCreados();

    @Query("SELECT COUNT(r) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CANCELLED'")
    long countPedidosCancelados();

    @Query("SELECT COUNT(r) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_STATUS_UPDATED'")
    long countCambiosDeEstado();

    @Query("SELECT COALESCE(SUM(r.total), 0.0) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CREATED'")
    double totalVendido();

    @Query("SELECT COALESCE(SUM(r.total), 0.0) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CANCELLED'")
    double totalCancelado();
}
