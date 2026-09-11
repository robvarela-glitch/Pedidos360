import { Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { Order } from '../../core/models/order.model';
import { OrdersService } from '../../core/services/orders.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-orders',
  imports: [DecimalPipe, Sidebar],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class Orders implements OnInit {

  orders: Order[] = [];

  constructor(private ordersService: OrdersService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.ordersService.getOrders().subscribe({
      next: (data) => {
        this.orders = data;
      },
      error: (error) => {
        console.error('Error al cargar los pedidos:', error);
      }
    });
  }
}