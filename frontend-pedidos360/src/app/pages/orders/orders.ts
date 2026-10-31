import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { ORDER_STATUSES, Order, OrderStatus, formatPrice, statusClass, statusLabel } from '../../core/models/order.model';
import { OrdersService } from '../../core/services/orders.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/**
 * Listado de pedidos desde Oracle (vía BFF).
 * Cliente: solo sus pedidos (filtrado por el backend según el claim oid del JWT).
 * Operador / Administrador: todos los pedidos.
 */
@Component({
  selector: 'app-orders',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class Orders implements OnInit {

  readonly auth = inject(AuthService);
  private readonly ordersService = inject(OrdersService);
  private readonly cdr = inject(ChangeDetectorRef);

  orders: Order[] = [];
  filteredOrders: Order[] = [];
  searchText = '';
  selectedStatus: OrderStatus | '' = '';
  statuses: OrderStatus[] = ORDER_STATUSES;
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.errorMessage = '';
    this.ordersService.getOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.filterOrders();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = describeHttpError(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  filterOrders(): void {
    const search = this.searchText.toLowerCase().trim();
    this.filteredOrders = this.orders.filter((order) => {
      const matchesSearch =
        order.id.toString().includes(search) ||
        order.customer.toLowerCase().includes(search);
      const matchesStatus = this.selectedStatus === '' || order.status === this.selectedStatus;
      return matchesSearch && matchesStatus;
    });
  }

  onSearch(event: Event): void {
    this.searchText = (event.target as HTMLInputElement).value;
    this.filterOrders();
  }

  onStatusChange(event: Event): void {
    this.selectedStatus = (event.target as HTMLSelectElement).value as OrderStatus | '';
    this.filterOrders();
  }

  getStatusClass(status: OrderStatus): string {
    return statusClass(status);
  }

  getStatusLabel(status: OrderStatus): string {
    return statusLabel(status);
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }

  getTotalOrders(): number {
    return this.orders.length;
  }

  getPendingOrders(): number {
    return this.orders.filter((o) => o.status === 'CREADO' || o.status === 'EN_PREPARACION').length;
  }

  getDispatchedOrders(): number {
    return this.orders.filter((o) => o.status === 'DESPACHADO').length;
  }

  getDeliveredOrders(): number {
    return this.orders.filter((o) => o.status === 'ENTREGADO').length;
  }

  getCancelledOrders(): number {
    return this.orders.filter((o) => o.status === 'CANCELADO').length;
  }

  getTotalSales(): number {
    return this.orders.filter((o) => o.status !== 'CANCELADO').reduce((total, o) => total + o.total, 0);
  }
}
