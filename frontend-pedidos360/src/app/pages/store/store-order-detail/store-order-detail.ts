import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Order, formatPrice, statusLabel } from '../../../core/models/order.model';
import { OrdersService, formatDate } from '../../../core/services/orders.service';
import { BackofficeService, NotificationEntry } from '../../../core/services/backoffice.service';
import { describeHttpError } from '../../../core/services/auth.service';
import { TRACKING_STEPS, stepIndex } from '../order-progress';

/**
 * Seguimiento de un pedido del cliente: etapas, productos, avisos (ms-notify)
 * y cancelación mientras el pedido esté recién recibido (estado CREADO).
 * Si el pedido es de otra persona, el backend responde 403.
 */
@Component({
  selector: 'app-store-order-detail',
  imports: [RouterLink],
  templateUrl: './store-order-detail.html',
  styleUrl: './store-order-detail.css'
})
export class StoreOrderDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly ordersService = inject(OrdersService);
  private readonly backoffice = inject(BackofficeService);

  readonly order = signal<Order | null>(null);
  readonly notifications = signal<NotificationEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly justCreated = signal(false);
  readonly cancelling = signal(false);
  readonly confirmCancel = signal(false);

  readonly steps = TRACKING_STEPS;
  readonly current = computed(() => (this.order() ? stepIndex(this.order()!.status) : -1));

  ngOnInit(): void {
    this.justCreated.set(this.route.snapshot.queryParamMap.has('nuevo'));
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.ordersService.getOrderById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        this.loadNotifications(order.id);
        if (this.justCreated()) {
          // El aviso lo genera ms-notify al consumir el evento de RabbitMQ: puede tardar un momento
          setTimeout(() => this.loadNotifications(order.id), 2500);
        }
      },
      error: (err) => {
        this.error.set(err?.status === 403
          ? 'Este pedido no pertenece a tu cuenta.'
          : err?.status === 404 ? 'No encontramos este pedido.' : describeHttpError(err));
        this.loading.set(false);
      }
    });
  }

  private loadNotifications(id: number): void {
    this.backoffice.getOrderNotifications(id).subscribe({
      next: (list) => this.notifications.set(list),
      error: () => this.notifications.set([])
    });
  }

  cancel(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    this.cancelling.set(true);
    this.actionError.set('');
    this.ordersService.cancelOrder(order.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.cancelling.set(false);
        this.confirmCancel.set(false);
        setTimeout(() => this.loadNotifications(updated.id), 1500);
      },
      error: (err) => {
        this.cancelling.set(false);
        this.actionError.set(describeHttpError(err));
      }
    });
  }

  price(value: number): string {
    return formatPrice(value);
  }

  status(value: string): string {
    return statusLabel(value);
  }

  date(value: string): string {
    return formatDate(value);
  }
}
