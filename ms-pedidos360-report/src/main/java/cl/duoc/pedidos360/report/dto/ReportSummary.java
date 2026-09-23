package cl.duoc.pedidos360.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportSummary {
    private long pedidosCreados;
    private long pedidosCancelados;
    private double totalVendido;
}
