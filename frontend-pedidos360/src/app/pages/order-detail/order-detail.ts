import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import {
  Order,
  OrderStatus
} from '../../core/models/order.model';

import { OrdersService } from '../../core/services/orders.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-order-detail',
  imports: [
    CommonModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css'
})
export class OrderDetail implements OnInit {

  order: Order | undefined;

  loading = true;

  statusSteps: OrderStatus[] = [
    'CREADO',
    'ACEPTADO',
    'EN_PREPARACIÓN',
    'DESPACHADO',
    'ENTREGADO'
  ];


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ordersService: OrdersService
  ) {}


  ngOnInit(): void {

    const id =
      Number(
        this.route.snapshot.paramMap.get('id')
      );

    this.loadOrder(id);

  }


  loadOrder(id: number): void {

    this.ordersService
      .getOrderById(id)
      .subscribe({

        next: (data) => {

          this.order = data;

          this.loading = false;

        },

        error: (error) => {

          console.error(
            'Error al cargar el pedido:',
            error
          );

          this.loading = false;

        }

      });

  }


  goBack(): void {

    this.router.navigate([
      '/orders'
    ]);

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


  getStepIndex(
    status: OrderStatus
  ): number {

    return this.statusSteps.indexOf(
      status
    );

  }


  isStepCompleted(
    status: OrderStatus
  ): boolean {

    if (!this.order) {
      return false;
    }

    const currentIndex =
      this.getStepIndex(
        this.order.status
      );

    const stepIndex =
      this.getStepIndex(status);

    return (
      stepIndex <= currentIndex &&
      currentIndex >= 0
    );

  }


  isCurrentStep(
    status: OrderStatus
  ): boolean {

    return (
      this.order?.status === status
    );

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

}