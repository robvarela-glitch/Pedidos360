import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Order, OrderStatus } from '../../core/models/order.model';
import { OrdersService } from '../../core/services/orders.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-orders',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './orders.html',
  styleUrl: './orders.css'
})
export class Orders implements OnInit {

  orders: Order[] = [];

  filteredOrders: Order[] = [];

  searchText = '';

  selectedStatus: OrderStatus | '' = '';

  statuses: OrderStatus[] = [
    'CREADO',
    'ACEPTADO',
    'EN_PREPARACIÓN',
    'DESPACHADO',
    'ENTREGADO',
    'CANCELADO'
  ];


  constructor(
    private ordersService: OrdersService
  ) {}


  ngOnInit(): void {

    this.loadOrders();

  }


  loadOrders(): void {

    this.ordersService
      .getOrders()
      .subscribe({

        next: (data) => {

          this.orders = data;

          this.filteredOrders = data;

        },

        error: (error) => {

          console.error(
            'Error al cargar los pedidos:',
            error
          );

        }

      });

  }


  filterOrders(): void {

    const search =
      this.searchText
        .toLowerCase()
        .trim();


    this.filteredOrders =
      this.orders.filter((order) => {

        const matchesSearch =

          order.id
            .toString()
            .includes(search) ||

          order.customer
            .toLowerCase()
            .includes(search);


        const matchesStatus =

          this.selectedStatus === '' ||

          order.status ===
          this.selectedStatus;


        return (
          matchesSearch &&
          matchesStatus
        );

      });

  }


  onSearch(event: Event): void {

    const input =
      event.target as HTMLInputElement;

    this.searchText =
      input.value;

    this.filterOrders();

  }


  onStatusChange(event: Event): void {

    const select =
      event.target as HTMLSelectElement;

    this.selectedStatus =
      select.value as OrderStatus | '';

    this.filterOrders();

  }


  getStatusClass(
    status: OrderStatus
  ): string {

    switch (status) {

      case 'CREADO':
        return 'created';

      case 'ACEPTADO':
        return 'accepted';

      case 'EN_PREPARACIÓN':
        return 'preparing';

      case 'DESPACHADO':
        return 'dispatched';

      case 'ENTREGADO':
        return 'delivered';

      case 'CANCELADO':
        return 'cancelled';

      default:
        return '';

    }

  }


  getStatusLabel(
    status: OrderStatus
  ): string {

    switch (status) {

      case 'CREADO':
        return 'Creado';

      case 'ACEPTADO':
        return 'Aceptado';

      case 'EN_PREPARACIÓN':
        return 'En preparación';

      case 'DESPACHADO':
        return 'Despachado';

      case 'ENTREGADO':
        return 'Entregado';

      case 'CANCELADO':
        return 'Cancelado';

      default:
        return status;

    }

  }


  formatPrice(
    price: number
  ): string {

    return new Intl.NumberFormat(
      'es-CL',
      {
        style: 'currency',
        currency: 'CLP',
        maximumFractionDigits: 0
      }
    ).format(price);

  }


  getTotalOrders(): number {

    return this.orders.length;

  }


  getPendingOrders(): number {

    return this.orders.filter(
      order =>
        order.status === 'CREADO' ||
        order.status === 'ACEPTADO' ||
        order.status === 'EN_PREPARACIÓN'
    ).length;

  }


  getDispatchedOrders(): number {

    return this.orders.filter(
      order =>
        order.status === 'DESPACHADO'
    ).length;

  }


  getDeliveredOrders(): number {

    return this.orders.filter(
      order =>
        order.status === 'ENTREGADO'
    ).length;

  }


  getCancelledOrders(): number {

    return this.orders.filter(
      order =>
        order.status === 'CANCELADO'
    ).length;

  }


  getTotalSales(): number {

    return this.orders
      .filter(
        order =>
          order.status !== 'CANCELADO'
      )
      .reduce(
        (total, order) =>
          total + order.total,
        0
      );

  }

}