import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Order, OrderStatus, formatPrice, statusLabel } from '../../core/models/order.model';
import { OrdersService } from '../../core/services/orders.service';
import { AuthService, describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

type Stage = 'CREADO' | 'EN_PREPARACION' | 'DESPACHADO' | 'ENTREGADO' | 'CANCELADO';

interface StageDef {
  status: Stage;
  label: string;
  next?: OrderStatus;
  action?: string;
}

/**
 * Bandeja de trabajo del Operador: pedidos agrupados por etapa (los más antiguos
 * primero) con la acción siguiente a mano. Cada cambio llama a
 * PATCH /api/orders/{id}/status, que valida rol y transición en el backend.
 */
@Component({
  selector: 'app-operations',
  imports: [RouterLink, Sidebar],
  templateUrl: './operations.html',
  styleUrl: './operations.css'
})
export class Operations implements OnInit {

  readonly auth = inject(AuthService);
  private readonly ordersService = inject(OrdersService);

  readonly stages: StageDef[] = [
    { status: 'CREADO', label: 'Por preparar', next: 'EN_PREPARACION', action: 'Comenzar preparación' },
    { status: 'EN_PREPARACION', label: 'En preparación', next: 'DESPACHADO', action: 'Marcar despachado' },
    { status: 'DESPACHADO', label: 'En camino', next: 'ENTREGADO', action: 'Confirmar entrega' },
    { status: 'ENTREGADO', label: 'Entregados' },
    { status: 'CANCELADO', label: 'Cancelados' }
  ];

  readonly orders = signal<Order[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly feedback = signal<{ text: string; ok: boolean } | null>(null);
  readonly working = signal<number | null>(null);
  readonly confirmingCancel = signal<number | null>(null);
  readonly stage = signal<Stage>('CREADO');
  readonly search = signal('');
  readonly updatedAt = signal<Date | null>(null);

  readonly counts = computed(() => {
    const result: Record<string, number> = {};
    for (const s of this.stages) {
      result[s.status] = this.orders().filter((o) => o.status === s.status).length;
    }
    return result;
  });

  readonly currentStage = computed(() => this.stages.find((s) => s.status === this.stage())!);

  readonly visible = computed(() => {
    const q = this.search().toLowerCase().trim();
    const active = this.stage() !== 'ENTREGADO' && this.stage() !== 'CANCELADO';
    return this.orders()
      .filter((o) => o.status === this.stage())
      .filter((o) => !q || ('ped-' + o.id).includes(q) || String(o.id) === q || o.customer.toLowerCase().includes(q))
      // Pendientes: el más antiguo primero. Cerrados: el más reciente primero.
      .sort((a, b) => active
        ? a.createdAt.localeCompare(b.createdAt)
        : (b.updatedAt ?? b.createdAt).localeCompare(a.updatedAt ?? a.createdAt));
  });

  readonly pendingTotal = computed(() =>
    this.counts()['CREADO'] + this.counts()['EN_PREPARACION'] + this.counts()['DESPACHADO']);

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.ordersService.getOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.updatedAt.set(new Date());
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(describeHttpError(err));
        this.loading.set(false);
      }
    });
  }

  selectStage(stage: Stage): void {
    this.stage.set(stage);
    this.confirmingCancel.set(null);
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  advance(order: Order): void {
    const next = this.currentStage().next;
    if (next) {
      this.run(order, this.ordersService.updateOrderStatus(order.id, next),
        'PED-' + order.id + ' pasó a ' + statusLabel(next) + '.');
    }
  }

  cancel(order: Order): void {
    this.run(order, this.ordersService.cancelOrder(order.id), 'PED-' + order.id + ' fue cancelado.');
  }

  private run(order: Order, request: ReturnType<OrdersService['cancelOrder']>, message: string): void {
    this.working.set(order.id);
    this.feedback.set(null);
    request.subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((o) => (o.id === updated.id ? updated : o)));
        this.working.set(null);
        this.confirmingCancel.set(null);
        this.feedback.set({ text: message, ok: true });
      },
      error: (err) => {
        this.working.set(null);
        this.feedback.set({ text: describeHttpError(err), ok: false });
      }
    });
  }

  itemsSummary(order: Order): string {
    return order.items.map((i) => i.quantity + ' × ' + i.productName).join(', ');
  }

  waiting(order: Order): string {
    const minutes = Math.max(0, Math.round((Date.now() - new Date(order.createdAt).getTime()) / 60000));
    if (minutes < 60) {
      return 'hace ' + minutes + ' min';
    }
    const hours = Math.round(minutes / 60);
    if (hours < 48) {
      return 'hace ' + hours + ' h';
    }
    return 'hace ' + Math.round(hours / 24) + ' días';
  }

  time(date: Date | null): string {
    return date ? new Intl.DateTimeFormat('es-CL', { hour: '2-digit', minute: '2-digit' }).format(date) : '';
  }

  price(value: number): string {
    return formatPrice(value);
  }
}
