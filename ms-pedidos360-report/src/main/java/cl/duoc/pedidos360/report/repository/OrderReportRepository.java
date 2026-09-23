package cl.duoc.pedidos360.report.repository;

import cl.duoc.pedidos360.report.model.OrderReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderReportRepository extends JpaRepository<OrderReportEntry, Long> {

    List<OrderReportEntry> findByCustomerId(String customerId);

    @Query("SELECT COUNT(r) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CREATED'")
    long countPedidosCreados();

    @Query("SELECT COALESCE(SUM(r.total), 0) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CREATED'")
    Double totalVendido();

    @Query("SELECT COUNT(r) FROM OrderReportEntry r WHERE r.eventType = 'ORDER_CANCELLED'")
    long countPedidosCancelados();
}
