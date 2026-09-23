package cl.duoc.pedidos360.report.controller;

import cl.duoc.pedidos360.report.dto.ReportSummary;
import cl.duoc.pedidos360.report.model.OrderReportEntry;
import cl.duoc.pedidos360.report.repository.OrderReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ReportSummary> summary() {
        ReportSummary summary = new ReportSummary(
                reportRepository.countPedidosCreados(),
                reportRepository.countPedidosCancelados(),
                reportRepository.totalVendido());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/entries")
    public ResponseEntity<List<OrderReportEntry>> findAll() {
        return ResponseEntity.ok(reportRepository.findAll());
    }

    @GetMapping("/entries/cliente/{customerId}")
    public ResponseEntity<List<OrderReportEntry>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(reportRepository.findByCustomerId(customerId));
    }
}
