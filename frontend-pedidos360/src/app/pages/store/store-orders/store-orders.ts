import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Order, formatPrice, statusLabel } from '../../../core/models/order.model';
import { OrdersService } from '../../../core/services/orders.service';
import { describeHttpError } from '../../../core/services/auth.service';
import { TRACKING_STEPS, stepIndex } from '../order-progress';

/** Mis pedidos: el backend devuelve solo los pedidos del usuario autenticado (claim oid). */
@Component({
  selector: 'app-store-orders',
  imports: [RouterLink],
  templateUrl: './store-orders.html',
  styleUrl: './store-orders.css'
})
export class StoreOrders implements OnInit {

  private readonly ordersService = inject(OrdersService);

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly steps = TRACKING_STEPS;

  ngOnInit(): void {
    this.ordersService.getOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeHttpError(err));
        this.loading.set(false);
      }
    });
  }

  progress(order: Order): number {
    return stepIndex(order.status);
  }

  itemsSummary(order: Order): string {
    const names = order.items.map((i) => i.quantity + ' × ' + i.productName);
    return names.length > 2 ? names.slice(0, 2).join(', ') + ' y ' + (names.length - 2) + ' más' : names.join(', ');
  }

  price(value: number): string {
    return formatPrice(value);
  }

  status(value: string): string {
    return statusLabel(value);
  }
}
