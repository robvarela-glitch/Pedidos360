import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    Sidebar
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {

  totalOrders = 128;

  pendingOrders = 24;

  dispatchedOrders = 18;

  cancelledOrders = 7;

  totalSales = 4850000;

  stockProducts = 92;

  recentOrders = [

    {
      id: '#PED-00128',
      customer: 'Comercial Andes',
      date: '11 Sep 2026',
      status: 'EN_PREPARACIÓN',
      total: 459990
    },

    {
      id: '#PED-00127',
      customer: 'Distribuidora Norte',
      date: '11 Sep 2026',
      status: 'ACEPTADO',
      total: 289990
    },

    {
      id: '#PED-00126',
      customer: 'TecnoSur SpA',
      date: '10 Sep 2026',
      status: 'DESPACHADO',
      total: 679990
    },

    {
      id: '#PED-00125',
      customer: 'Importadora Central',
      date: '10 Sep 2026',
      status: 'ENTREGADO',
      total: 159990
    },

    {
      id: '#PED-00124',
      customer: 'Comercial del Pacífico',
      date: '09 Sep 2026',
      status: 'CANCELADO',
      total: 89990
    }

  ];


  orderStatusData = [

    {
      label: 'Entregados',
      value: 48,
      percentage: 38
    },

    {
      label: 'En preparación',
      value: 24,
      percentage: 19
    },

    {
      label: 'Aceptados',
      value: 21,
      percentage: 16
    },

    {
      label: 'Despachados',
      value: 18,
      percentage: 14
    },

    {
      label: 'Creados',
      value: 10,
      percentage: 8
    },

    {
      label: 'Cancelados',
      value: 7,
      percentage: 5
    }

  ];


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


  topProducts = [

    {
      name: 'Logitech G915',
      category: 'Teclados',
      sold: 42
    },

    {
      name: 'Samsung Odyssey G5',
      category: 'Monitores',
      sold: 36
    },

    {
      name: 'ASUS TUF Gaming',
      category: 'Notebooks',
      sold: 29
    },

    {
      name: 'HyperX Cloud III',
      category: 'Audio',
      sold: 24
    }

  ];


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
    status: string
  ): string {

    switch (status) {

      case 'ENTREGADO':
        return 'delivered';

      case 'EN_PREPARACIÓN':
        return 'preparing';

      case 'ACEPTADO':
        return 'accepted';

      case 'DESPACHADO':
        return 'dispatched';

      case 'CREADO':
        return 'created';

      case 'CANCELADO':
        return 'cancelled';

      default:
        return '';

    }

  }

}