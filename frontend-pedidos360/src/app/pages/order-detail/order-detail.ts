import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { NEXT_STATUSES, Order, OrderStatus, formatPrice, statusClass, statusLabel } from '../../core/models/order.model';
import { OrdersService, formatDate } from '../../core/services/orders.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { BackofficeService, NotificationEntry } from '../../core/services/backoffice.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/**
 * Detalle de un pedido.
 *  - Operador / Administrador: pueden avanzar el estado (según transiciones válidas).
 *  - Cliente: ve solo sus pedidos y puede cancelarlos mientras estén en CREADO.
 * Si un cliente intenta abrir un pedido ajeno, el backend responde 403.
 */
@Component({
  selector: 'app-order-detail',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css'
})
export class OrderDetail implements OnInit {

  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ordersService = inject(OrdersService);
  private readonly backoffice = inject(BackofficeService);
  private readonly cdr = inject(ChangeDetectorRef);

  order: Order | undefined;
  notifications: NotificationEntry[] = [];
  loading = true;
  updating = false;
  errorMessage = '';
  successMessage = '';

  statusSteps: OrderStatus[] = ['CREADO', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO'];

  ngOnInit(): void {
    this.auth.loadMe().subscribe(() => this.cdr.markForCheck());
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadOrder(id);
  }

  loadOrder(id: number): void {
    this.ordersService.getOrderById(id).subscribe({
      next: (data) => {
        this.order = data;
        this.loading = false;
        this.loadNotifications(id);
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.errorMessage = error?.status === 403
          ? 'Este pedido pertenece a otro cliente: el backend denegó el acceso (403).'
          : describeHttpError(error);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadNotifications(id: number): void {
    this.backoffice.getOrderNotifications(id).subscribe({
      next: (list) => {
        this.notifications = list;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notifications = [];
      }
    });
  }

  /** Estados a los que el Operador/Admin puede mover el pedido. */
  get nextStatuses(): OrderStatus[] {
    return this.order && this.auth.canManageOrders() ? NEXT_STATUSES[this.order.status] : [];
  }

  get canClientCancel(): boolean {
    return !!this.order && !this.auth.canManageOrders() && this.order.status === 'CREADO';
  }

  changeStatus(status: OrderStatus): void {
    if (!this.order) {
      return;
    }
    this.run(status === 'CANCELADO'
      ? this.ordersService.cancelOrder(this.order.id)
      : this.ordersService.updateOrderStatus(this.order.id, status),
      'Estado actualizado a ' + statusLabel(status) + '.');
  }

  cancelOrder(): void {
    if (this.order) {
      this.run(this.ordersService.cancelOrder(this.order.id), 'Pedido cancelado.');
    }
  }

  private run(request: ReturnType<OrdersService['cancelOrder']>, message: string): void {
    this.updating = true;
    this.errorMessage = '';
    this.successMessage = '';
    request.subscribe({
      next: (updated) => {
        this.order = updated;
        this.updating = false;
        this.successMessage = message;
        this.cdr.markForCheck();
        // La notificación la genera ms-notify al consumir el evento de RabbitMQ
        setTimeout(() => this.loadNotifications(updated.id), 1500);
      },
      error: (error) => {
        this.updating = false;
        this.errorMessage = describeHttpError(error);
        this.cdr.markForCheck();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/orders']);
  }

  getStatusClass(status: OrderStatus): string {
    return statusClass(status);
  }

  getStatusLabel(status: OrderStatus): string {
    return statusLabel(status);
  }

  getStepIndex(status: OrderStatus): number {
    return this.statusSteps.indexOf(status);
  }

  isStepCompleted(status: OrderStatus): boolean {
    if (!this.order) {
      return false;
    }
    const currentIndex = this.getStepIndex(this.order.status);
    const stepIndex = this.getStepIndex(status);
    return stepIndex <= currentIndex && currentIndex >= 0;
  }

  isCurrentStep(status: OrderStatus): boolean {
    return this.order?.status === status;
  }

  formatPrice(price: number): string {
    return formatPrice(price);
  }

  formatDate(value: string): string {
    return formatDate(value);
  }
}
