package cl.duoc.pedidos360.report.controller;

import cl.duoc.pedidos360.report.dto.ReportSummary;
import cl.duoc.pedidos360.report.model.OrderReportEntry;
import cl.duoc.pedidos360.report.repository.OrderReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reportes de ventas: solo Administrador. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final OrderReportRepository reportRepository;

    @GetMapping("/publico/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ms-pedidos360-report activo");
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ReportSummary> summary() {
        ReportSummary summary = new ReportSummary(
                reportRepository.countPedidosCreados(),
                reportRepository.countPedidosCancelados(),
                reportRepository.countCambiosDeEstado(),
                reportRepository.totalVendido(),
                reportRepository.totalCancelado());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/entries")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<OrderReportEntry>> findAll() {
        return ResponseEntity.ok(reportRepository.findAllByOrderByProcessedAtDesc());
    }

    @GetMapping("/entries/cliente/{customerId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<OrderReportEntry>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(reportRepository.findByCustomerId(customerId));
    }
}
