import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-reports',
  imports: [
    CommonModule,
    Sidebar
  ],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports {

  totalOrders = 128;

  pendingOrders = 24;

  dispatchedOrders = 18;

  cancelledOrders = 7;

  totalSales = 4850000;

  averageOrder = 37891;

  deliveryRate = 76;

  averagePreparationTime = 42;


  /* ================================
     VENTAS MENSUALES
  ================================= */

  salesData = [
    {
      month: 'Abr',
      value: 2800000
    },
    {
      month: 'May',
      value: 3450000
    },
    {
      month: 'Jun',
      value: 3100000
    },
    {
      month: 'Jul',
      value: 4200000
    },
    {
      month: 'Ago',
      value: 3980000
    },
    {
      month: 'Sep',
      value: 4850000
    }
  ];


  /* ================================
     ESTADOS DE PEDIDOS
  ================================= */

  orderStatuses = [

    {
      name: 'Creado',
      value: 18,
      percentage: 14,
      className: 'created'
    },

    {
      name: 'Aceptado',
      value: 32,
      percentage: 25,
      className: 'accepted'
    },

    {
      name: 'En preparación',
      value: 24,
      percentage: 19,
      className: 'preparing'
    },

    {
      name: 'Despachado',
      value: 18,
      percentage: 14,
      className: 'dispatched'
    },

    {
      name: 'Entregado',
      value: 29,
      percentage: 23,
      className: 'delivered'
    },

    {
      name: 'Cancelado',
      value: 7,
      percentage: 5,
      className: 'cancelled'
    }

  ];


  /* ================================
     PRODUCTOS MÁS VENDIDOS
  ================================= */

  topProducts = [

    {
      name: 'Notebook Lenovo',
      category: 'Notebooks',
      sales: 35,
      revenue: 20999650
    },

    {
      name: 'Smartphone Samsung',
      category: 'Smartphones',
      sales: 28,
      revenue: 11199720
    },

    {
      name: 'Mouse Logitech',
      category: 'Mouse',
      sales: 42,
      revenue: 839580
    },

    {
      name: 'Teclado Mecánico',
      category: 'Teclados',
      sales: 23,
      revenue: 919770
    }

  ];


  /* ================================
     INDICADORES OPERATIVOS
  ================================= */

  operationalIndicators = [

    {
      label: 'Pedidos entregados',
      value: 29,
      description: 'pedidos completados',
      className: 'green'
    },

    {
      label: 'Pedidos pendientes',
      value: 24,
      description: 'requieren atención',
      className: 'orange'
    },

    {
      label: 'En despacho',
      value: 18,
      description: 'en distribución',
      className: 'purple'
    },

    {
      label: 'Cancelaciones',
      value: 7,
      description: 'pedidos cancelados',
      className: 'red'
    }

  ];


  /* ================================
     FUNCIONES
  ================================= */

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


  getBarHeight(
    value: number
  ): number {

    const max =
      Math.max(
        ...this.salesData.map(
          item => item.value
        )
      );

    return (
      value / max
    ) * 100;

  }


  getStatusClass(
    className: string
  ): string {

    return className;

  }


  getTotalStatusOrders(): number {

    return this.orderStatuses.reduce(
      (total, status) =>
        total + status.value,
      0
    );

  }


  getProductRank(
    index: number
  ): string {

    return `0${index + 1}`;

  }


  getRevenuePercentage(
    revenue: number
  ): number {

    const max =
      Math.max(
        ...this.topProducts.map(
          product => product.revenue
        )
      );

    return (
      revenue / max
    ) * 100;

  }

}