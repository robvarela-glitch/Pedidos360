import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { apiUrl } from '../config/app-config';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order.model';

/** Respuesta JSON de ms-pedidos360-orders. */
interface OrderResponse {
  id: number;
  customerId: string;
  customerName: string | null;
  status: OrderStatus;
  total: number;
  createdAt: string;
  updatedAt: string | null;
  items: { id: number; productId: string; productName: string | null; quantity: number; unitPrice: number }[];
}

/**
 * Pedidos reales (BFF -> ms-orders -> Oracle). El backend decide qué pedidos ve cada
 * usuario: un Cliente recibe solo los suyos; Operador/Admin reciben todos.
 */
@Injectable({ providedIn: 'root' })
export class OrdersService {

  private readonly http = inject(HttpClient);

  getOrders(status?: OrderStatus): Observable<Order[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<OrderResponse[]>(apiUrl('/api/orders'), { params })
      .pipe(map((orders) => orders.map(toOrder)));
  }

  getOrderById(id: number): Observable<Order> {
    return this.http.get<OrderResponse>(apiUrl('/api/orders/' + id)).pipe(map(toOrder));
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<OrderResponse>(apiUrl('/api/orders'), request).pipe(map(toOrder));
  }

  updateOrderStatus(id: number, status: OrderStatus): Observable<Order> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<OrderResponse>(apiUrl('/api/orders/' + id + '/status'), null, { params })
      .pipe(map(toOrder));
  }

  cancelOrder(id: number): Observable<Order> {
    return this.http.delete<OrderResponse>(apiUrl('/api/orders/' + id)).pipe(map(toOrder));
  }
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('es-CL', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  }).format(date);
}

function toOrder(o: OrderResponse): Order {
  return {
    id: o.id,
    customerId: o.customerId,
    customer: o.customerName || 'Cliente ' + (o.customerId ?? '').substring(0, 8),
    date: formatDate(o.createdAt),
    createdAt: o.createdAt,
    updatedAt: o.updatedAt ?? undefined,
    status: o.status,
    total: o.total ?? 0,
    items: (o.items ?? []).map((i) => ({
      id: i.id,
      productId: i.productId,
      productName: i.productName ?? 'Producto ' + i.productId,
      quantity: i.quantity,
      unitPrice: i.unitPrice
    }))
  };
}
