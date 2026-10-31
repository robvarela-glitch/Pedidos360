import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { apiUrl } from '../config/app-config';

export interface RabbitNode { name: string; running: boolean; type: string; uptime: number; mem_used: number; }
export interface RabbitQueue { name: string; type: string; durable: boolean; messages: number; consumers: number; node: string; }
export interface RabbitExchange { name: string; type: string; durable: boolean; }
export interface RabbitBinding { source: string; destination: string; destination_type: string; routing_key: string; }

/** Cliente del microservicio administrador de RabbitMQ (ms-pedidos360-rabbitadmin, rol Admin). */
@Injectable({ providedIn: 'root' })
export class RabbitAdminService {

  private readonly http = inject(HttpClient);
  private readonly base = '/api/rabbitadmin';

  nodes(): Observable<RabbitNode[]> {
    return this.http.get<RabbitNode[]>(apiUrl(this.base + '/nodes'));
  }

  queues(): Observable<RabbitQueue[]> {
    return this.http.get<RabbitQueue[]>(apiUrl(this.base + '/queues'));
  }

  exchanges(): Observable<RabbitExchange[]> {
    return this.http.get<RabbitExchange[]>(apiUrl(this.base + '/exchanges'));
  }

  bindings(): Observable<RabbitBinding[]> {
    return this.http.get<RabbitBinding[]>(apiUrl(this.base + '/bindings'));
  }

  createQueue(name: string, withDeadLetterQueue: boolean): Observable<unknown> {
    return this.http.post(apiUrl(this.base + '/queues'), { name, withDeadLetterQueue });
  }

  deleteQueue(name: string): Observable<unknown> {
    return this.http.delete(apiUrl(this.base + '/queues/' + encodeURIComponent(name)));
  }

  createExchange(name: string, type: string): Observable<unknown> {
    return this.http.post(apiUrl(this.base + '/exchanges'), { name, type });
  }

  deleteExchange(name: string): Observable<unknown> {
    return this.http.delete(apiUrl(this.base + '/exchanges/' + encodeURIComponent(name)));
  }

  createBinding(exchange: string, queue: string, routingKey: string): Observable<unknown> {
    return this.http.post(apiUrl(this.base + '/bindings'), { exchange, queue, routingKey });
  }

  deleteBinding(exchange: string, queue: string, routingKey: string): Observable<unknown> {
    return this.http.delete(apiUrl(this.base + '/bindings'), { body: { exchange, queue, routingKey } });
  }

  publish(exchange: string, routingKey: string, payload: unknown, headers: Record<string, string>): Observable<unknown> {
    return this.http.post(apiUrl(this.base + '/messages'), { exchange, routingKey, payload, headers });
  }
}
