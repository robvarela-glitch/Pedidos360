import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';

import {
  RabbitAdminService, RabbitBinding, RabbitExchange, RabbitNode, RabbitQueue
} from '../../core/services/rabbit-admin.service';
import { describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

/**
 * Mensajería (solo Administrador): estado del clúster RabbitMQ y administración de
 * colas, exchanges y bindings mediante ms-pedidos360-rabbitadmin (vía BFF / API Gateway).
 */
@Component({
  selector: 'app-messaging',
  imports: [FormsModule, Sidebar],
  templateUrl: './messaging.html',
  styleUrl: './messaging.css'
})
export class Messaging implements OnInit {

  private readonly rabbit = inject(RabbitAdminService);
  private readonly cdr = inject(ChangeDetectorRef);

  nodes: RabbitNode[] = [];
  queues: RabbitQueue[] = [];
  exchanges: RabbitExchange[] = [];
  bindings: RabbitBinding[] = [];
  loading = true;
  working = false;
  errorMessage = '';
  successMessage = '';

  newQueue = { name: 'pruebas.demo', withDlq: true };
  newExchange = { name: 'pruebas.topic', type: 'TOPIC' };
  newBinding = { exchange: 'pruebas.topic', queue: 'pruebas.demo', routingKey: 'demo.#' };
  testMessage = { exchange: 'pedidos360.orders.direct', routingKey: 'notify.order', failure: 'fatal' };

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    forkJoin({
      nodes: this.rabbit.nodes(),
      queues: this.rabbit.queues(),
      exchanges: this.rabbit.exchanges(),
      bindings: this.rabbit.bindings()
    }).subscribe({
      next: (data) => {
        this.nodes = data.nodes;
        this.queues = data.queues;
        this.exchanges = data.exchanges.filter((e) => !e.name.startsWith('amq.'));
        this.bindings = data.bindings;
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

  createQueue(): void {
    this.run(this.rabbit.createQueue(this.newQueue.name.trim(), this.newQueue.withDlq), 'Cola creada');
  }

  deleteQueue(name: string): void {
    this.run(this.rabbit.deleteQueue(name), 'Cola eliminada: ' + name);
  }

  createExchange(): void {
    this.run(this.rabbit.createExchange(this.newExchange.name.trim(), this.newExchange.type), 'Exchange creado');
  }

  deleteExchange(name: string): void {
    this.run(this.rabbit.deleteExchange(name), 'Exchange eliminado: ' + name);
  }

  createBinding(): void {
    const b = this.newBinding;
    this.run(this.rabbit.createBinding(b.exchange.trim(), b.queue.trim(), b.routingKey.trim()), 'Binding creado');
  }

  deleteBinding(b: RabbitBinding): void {
    this.run(this.rabbit.deleteBinding(b.source, b.destination, b.routing_key), 'Binding eliminado');
  }

  /** Publica un evento de prueba con x-simulate-failure para demostrar reintentos y DLQ. */
  publishTest(): void {
    const headers: Record<string, string> = {};
    if (this.testMessage.failure) {
      headers['x-simulate-failure'] = this.testMessage.failure;
    }
    const payload = {
      orderId: 999999, customerId: 'demo', eventType: 'ORDER_CREATED', status: 'CREADO', total: 1,
      occurredAt: new Date().toISOString().substring(0, 19)
    };
    this.run(this.rabbit.publish(this.testMessage.exchange, this.testMessage.routingKey, payload, headers),
      'Mensaje publicado. Revise la DLQ en unos segundos (Actualizar).');
  }

  isDlq(name: string): boolean {
    return name.endsWith('.dlq');
  }

  private run(request: Observable<unknown>, message: string): void {
    this.working = true;
    this.errorMessage = '';
    this.successMessage = '';
    request.subscribe({
      next: () => {
        this.working = false;
        this.successMessage = message;
        this.cdr.markForCheck();
        setTimeout(() => this.refresh(), 800);
      },
      error: (err) => {
        this.working = false;
        this.errorMessage = describeHttpError(err);
        this.cdr.markForCheck();
      }
    });
  }
}
