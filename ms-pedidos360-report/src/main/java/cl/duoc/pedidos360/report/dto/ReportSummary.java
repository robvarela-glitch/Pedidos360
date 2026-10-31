package cl.duoc.pedidos360.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportSummary {
    private long pedidosCreados;
    private long pedidosCancelados;
    private long cambiosDeEstado;
    private double totalVendido;
    /** Monto de pedidos cancelados (se descuenta del total vendido neto). */
    private double totalCancelado;

    public double getTotalNeto() {
        return totalVendido - totalCancelado;
    }
}
