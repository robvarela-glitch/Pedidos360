import { Component } from '@angular/core';
import {
  FormsModule,
  NgForm
} from '@angular/forms';

import {
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
  selector: 'app-new-order',
  imports: [
    FormsModule,
    RouterLink,
    Sidebar
  ],
  templateUrl: './new-order.html',
  styleUrl: './new-order.css'
})
export class NewOrder {

  order: Order = {
    id: 0,
    customer: '',
    date: '',
    status: 'CREADO',
    total: 0
  };


  products = [
    {
      name: 'Logitech G915',
      price: 159990
    },
    {
      name: 'Samsung Odyssey G5',
      price: 279990
    },
    {
      name: 'ASUS TUF Gaming',
      price: 899990
    },
    {
      name: 'Lenovo ThinkPad',
      price: 699990
    },
    {
      name: 'Samsung Galaxy S26',
      price: 999990
    },
    {
      name: 'HyperX Cloud III',
      price: 89990
    },
    {
      name: 'Logitech MX Master',
      price: 109990
    }
  ];


  selectedProduct = '';

  quantity = 1;

  saving = false;

  saved = false;


  constructor(
    private ordersService: OrdersService,
    private router: Router
  ) {}


  addProduct(): void {

    if (!this.selectedProduct) {
      return;
    }

    const product =
      this.products.find(
        item =>
          item.name === this.selectedProduct
      );

    if (!product) {
      return;
    }

    this.order.total +=
      product.price * this.quantity;

    this.selectedProduct = '';

    this.quantity = 1;

  }


  createOrder(
    form: NgForm
  ): void {

    if (
      form.invalid ||
      this.order.total <= 0
    ) {

      form.control.markAllAsTouched();

      return;

    }


    this.saving = true;


    const newOrder: Order = {

      ...this.order,

      id:
        Date.now(),

      date:
        new Intl.DateTimeFormat(
          'es-CL'
        ).format(
          new Date()
        ),

      status:
        'CREADO'

    };


    this.ordersService
      .createOrder(newOrder)
      .subscribe({

        next: () => {

          this.saving = false;

          this.saved = true;


          setTimeout(() => {

            this.router.navigate([
              '/orders'
            ]);

          }, 700);

        },

        error: (error) => {

          console.error(
            'Error al crear el pedido:',
            error
          );

          this.saving = false;

        }

      });

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


  cancel(): void {

    this.router.navigate([
      '/orders'
    ]);

  }

}