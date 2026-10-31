import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';

import { Order, formatPrice } from '../../core/models/order.model';
import { OrdersService } from '../../core/services/orders.service';
import { BackofficeService, ReportSummary } from '../../core/services/backoffice.service';
import { describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/**
 * Reportes (solo Administrador).
 *  - Resumen de ventas: ms-report (eventos order.* recibidos por RabbitMQ y guardados en Oracle).
 *  - Distribución, ventas mensuales y productos: calculados con los pedidos reales de ms-orders.
 */
@Component({
  selector: 'app-reports',
  imports: [
    CommonModule,
    Sidebar
  ],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports implements OnInit {

  private readonly ordersService = inject(OrdersService);
  private readonly backoffice = inject(BackofficeService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = true;
  errorMessage = '';
  period = new Intl.DateTimeFormat('es-CL', { month: 'long', year: 'numeric' }).format(new Date());

  summary: ReportSummary | null = null;
  totalOrders = 0;
  pendingOrders = 0;
  dispatchedOrders = 0;
  cancelledOrders = 0;
  totalSales = 0;
  averageOrder = 0;
  deliveryRate = 0;
  statusChanges = 0;

  salesData: { month: string; value: number }[] = [];
  orderStatuses: { name: string; value: number; percentage: number; className: string }[] = [];
  topProducts: { name: string; category: string; sales: number; revenue: number }[] = [];
  operationalIndicators: { label: string; value: number; description: string; className: string }[] = [];

  ngOnInit(): void {
    forkJoin({
      summary: this.backoffice.getReportSummary(),
      orders: this.ordersService.getOrders()
    }).subscribe({
      next: ({ summary, orders }) => {
        this.summary = summary;
        this.compute(summary, orders);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = describeHttpError(err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private compute(summary: ReportSummary, orders: Order[]): void {
    const count = (s: string) => orders.filter((o) => o.status === s).length;
    this.totalOrders = summary.pedidosCreados;
    this.totalSales = summary.totalNeto;
    this.statusChanges = summary.cambiosDeEstado;
    this.cancelledOrders = summary.pedidosCancelados;
    this.pendingOrders = count('CREADO') + count('EN_PREPARACION');
    this.dispatchedOrders = count('DESPACHADO');
    const active = orders.filter((o) => o.status !== 'CANCELADO');
    this.averageOrder = active.length ? Math.round(active.reduce((t, o) => t + o.total, 0) / active.length) : 0;
    this.deliveryRate = active.length ? Math.round((count('ENTREGADO') / active.length) * 100) : 0;

    const defs = [
      { status: 'CREADO', name: 'Creado', className: 'created' },
      { status: 'EN_PREPARACION', name: 'En preparación', className: 'preparing' },
      { status: 'DESPACHADO', name: 'Despachado', className: 'dispatched' },
      { status: 'ENTREGADO', name: 'Entregado', className: 'delivered' },
      { status: 'CANCELADO', name: 'Cancelado', className: 'cancelled' }
    ];
    this.orderStatuses = defs.map((d) => {
      const value = count(d.status);
      return {
        name: d.name,
        value,
        percentage: orders.length ? Math.round((value / orders.length) * 100) : 0,
        className: d.className
      };
    });

    const months: { key: string; month: string; value: number }[] = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push({
        key: d.getFullYear() + '-' + d.getMonth(),
        month: new Intl.DateTimeFormat('es-CL', { month: 'short' }).format(d).replace('.', ''),
        value: 0
      });
    }
    for (const o of active) {
      const d = new Date(o.createdAt);
      const bucket = months.find((m) => m.key === d.getFullYear() + '-' + d.getMonth());
      if (bucket) {
        bucket.value += o.total;
      }
    }
    this.salesData = months.map(({ month, value }) => ({ month, value }));

    const byProduct = new Map<string, { sales: number; revenue: number }>();
    for (const o of active) {
      for (const item of o.items) {
        const acc = byProduct.get(item.productName) ?? { sales: 0, revenue: 0 };
        acc.sales += item.quantity;
        acc.revenue += item.quantity * item.unitPrice;
        byProduct.set(item.productName, acc);
      }
    }
    this.topProducts = [...byProduct.entries()]
      .sort((a, b) => b[1].revenue - a[1].revenue)
      .slice(0, 4)
      .map(([name, v]) => ({ name, category: 'Pedidos', sales: v.sales, revenue: v.revenue }));

    this.operationalIndicators = [
      { label: 'Pedidos entregados', value: count('ENTREGADO'), description: 'pedidos completados', className: 'green' },
      { label: 'Pedidos pendientes', value: this.pendingOrders, description: 'requieren atención', className: 'orange' },
      { label: 'En despacho', value: this.dispatchedOrders, description: 'en distribución', className: 'purple' },
      { label: 'Cancelaciones', value: summary.pedidosCancelados, description: 'eventos ORDER_CANCELLED', className: 'red' }
    ];
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }

  getBarHeight(value: number): number {
    const max = Math.max(1, ...this.salesData.map((item) => item.value));
    return (value / max) * 100;
  }

  getStatusClass(className: string): string {
    return className;
  }

  getTotalStatusOrders(): number {
    return this.orderStatuses.reduce((total, status) => total + status.value, 0);
  }

  getProductRank(index: number): string {
    return `0${index + 1}`;
  }

  getRevenuePercentage(revenue: number): number {
    const max = Math.max(1, ...this.topProducts.map((product) => product.revenue));
    return (revenue / max) * 100;
  }
}
