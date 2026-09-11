import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Order } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrdersService {

  private apiUrl = '/api/orders';

  private mockOrders: Order[] = [
    {
      id: 1,
      customer: 'Empresa ABC',
      date: '11/09/2026',
      status: 'ACEPTADO',
      total: 125000
    },
    {
      id: 2,
      customer: 'Comercial XYZ',
      date: '11/09/2026',
      status: 'EN_PREPARACIÓN',
      total: 89500
    },
    {
      id: 3,
      customer: 'Distribuidora Sur',
      date: '10/09/2026',
      status: 'DESPACHADO',
      total: 210000
    },
    {
      id: 4,
      customer: 'Comercial Norte',
      date: '09/09/2026',
      status: 'ENTREGADO',
      total: 74990
    },
    {
      id: 5,
      customer: 'Empresa Central',
      date: '09/09/2026',
      status: 'CANCELADO',
      total: 45000
    }
  ];


  constructor(
    private http: HttpClient
  ) {}


  getOrders(): Observable<Order[]> {

    return of(this.mockOrders);

  }


  getOrderById(
    id: number
  ): Observable<Order | undefined> {

    const order =
      this.mockOrders.find(
        item => item.id === id
      );

    return of(order);

  }


  createOrder(
    order: Order
  ): Observable<Order> {

    this.mockOrders.push(order);

    return of(order);

  }


  updateOrderStatus(
    id: number,
    status: string
  ): Observable<Order | undefined> {

    const order =
      this.mockOrders.find(
        item => item.id === id
      );

    if (order) {

      order.status =
        status as Order['status'];

    }

    return of(order);

  }

}