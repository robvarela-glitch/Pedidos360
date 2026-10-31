import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Sidebar } from '../../shared/components/sidebar/sidebar';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { OrdersService } from '../../core/services/orders.service';
import { CatalogService } from '../../core/services/catalog.service';
import { Order, formatPrice, statusClass, statusLabel } from '../../core/models/order.model';
import { Product } from '../../core/models/product.model';

/**
 * Dashboard con datos REALES del backend.
 *  - Administrador / Operador: visión global de todos los pedidos.
 *  - Cliente: resumen de SUS pedidos (el backend solo le devuelve los suyos).
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  readonly auth = inject(AuthService);
  private readonly ordersService = inject(OrdersService);
  private readonly catalogService = inject(CatalogService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = true;
  errorMessage = '';
  today = new Intl.DateTimeFormat('es-CL', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());

  orders: Order[] = [];
  products: Product[] = [];

  totalOrders = 0;
  pendingOrders = 0;
  dispatchedOrders = 0;
  cancelledOrders = 0;
  deliveredOrders = 0;
  totalSales = 0;
  stockProducts = 0;
  dispatchRate = 0;

  recentOrders: Order[] = [];
  orderStatusData: { label: string; value: number; percentage: number }[] = [];
  salesData: { month: string; value: number }[] = [];
  topProducts: { name: string; category: string; sold: number }[] = [];

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());

    forkJoin({
      orders: this.ordersService.getOrders(),
      products: this.catalogService.getProducts().pipe(catchError(() => of([] as Product[])))
    }).subscribe({
      next: ({ orders, products }) => {
        this.orders = orders;
        this.products = products;
        this.compute();
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

  get headerTitle(): string {
    return this.auth.canManageOrders() ? 'Dashboard' : 'Mi resumen';
  }

  get headerSubtitle(): string {
    if (this.auth.isAdmin()) {
      return 'Visión global de la operación: pedidos, catálogo y ventas.';
    }
    if (this.auth.isOperador()) {
      return 'Pedidos por gestionar y estado de la operación.';
    }
    return 'Estado de tus pedidos y productos disponibles.';
  }

  private compute(): void {
    const orders = this.orders;
    const count = (status: string) => orders.filter((o) => o.status === status).length;

    this.totalOrders = orders.length;
    this.pendingOrders = count('CREADO') + count('EN_PREPARACION');
    this.dispatchedOrders = count('DESPACHADO');
    this.deliveredOrders = count('ENTREGADO');
    this.cancelledOrders = count('CANCELADO');
    this.totalSales = orders.filter((o) => o.status !== 'CANCELADO').reduce((t, o) => t + o.total, 0);
    this.stockProducts = this.products.filter((p) => p.stock > 0).length;
    const active = this.totalOrders - this.cancelledOrders;
    this.dispatchRate = active > 0 ? Math.round(((this.dispatchedOrders + this.deliveredOrders) / active) * 100) : 0;

    this.recentOrders = orders.slice(0, 5);

    const statuses = ['ENTREGADO', 'EN_PREPARACION', 'DESPACHADO', 'CREADO', 'CANCELADO'];
    const labels: Record<string, string> = {
      ENTREGADO: 'Entregados', EN_PREPARACION: 'En preparación', DESPACHADO: 'Despachados',
      CREADO: 'Creados', CANCELADO: 'Cancelados'
    };
    this.orderStatusData = statuses.map((s) => {
      const value = count(s);
      return {
        label: labels[s],
        value,
        percentage: this.totalOrders > 0 ? Math.round((value / this.totalOrders) * 100) : 0
      };
    });

    // Ventas de los últimos 6 meses (pedidos no cancelados)
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
    for (const o of orders) {
      if (o.status === 'CANCELADO') {
        continue;
      }
      const d = new Date(o.createdAt);
      const bucket = months.find((m) => m.key === d.getFullYear() + '-' + d.getMonth());
      if (bucket) {
        bucket.value += o.total;
      }
    }
    this.salesData = months.map(({ month, value }) => ({ month, value }));

    // Productos más pedidos (suma de unidades en los pedidos)
    const sold = new Map<string, number>();
    for (const o of orders) {
      if (o.status === 'CANCELADO') {
        continue;
      }
      for (const item of o.items) {
        sold.set(item.productName, (sold.get(item.productName) ?? 0) + item.quantity);
      }
    }
    this.topProducts = [...sold.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 4)
      .map(([name, units]) => ({
        name,
        category: this.products.find((p) => p.name === name)?.category ?? '',
        sold: units
      }));
  }

  get maxSales(): number {
    return Math.max(1, ...this.salesData.map((item) => item.value));
  }

  axisLabel(fraction: number): string {
    const value = this.maxSales * fraction;
    if (value >= 1_000_000) {
      return '$' + (value / 1_000_000).toFixed(1).replace('.0', '') + 'M';
    }
    if (value >= 1_000) {
      return '$' + Math.round(value / 1_000) + 'K';
    }
    return '$' + Math.round(value);
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }

  getBarHeight(value: number): number {
    return (value / this.maxSales) * 100;
  }

  getStatusClass(status: string): string {
    return statusClass(status);
  }

  getStatusLabel(status: string): string {
    return statusLabel(status);
  }
}
