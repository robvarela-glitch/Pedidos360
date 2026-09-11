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
  filteredOrders: Order[] = [];

  searchText = '';
  selectedStatus = '';

  constructor(private ordersService: OrdersService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.ordersService.getOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.filteredOrders = data;
      },
      error: (error) => {
        console.error('Error al cargar los pedidos:', error);
      }
    });
  }

  filterOrders(): void {
    const search = this.searchText.toLowerCase().trim();

    this.filteredOrders = this.orders.filter((order) => {

      const matchesSearch =
        order.id.toString().includes(search) ||
        order.customer.toLowerCase().includes(search);

      const matchesStatus =
        this.selectedStatus === '' ||
        order.status === this.selectedStatus;

      return matchesSearch && matchesStatus;
    });
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.searchText = input.value;

    this.filterOrders();
  }

  onStatusChange(event: Event): void {
    const select = event.target as HTMLSelectElement;

    this.selectedStatus = select.value;

    this.filterOrders();
  }
}