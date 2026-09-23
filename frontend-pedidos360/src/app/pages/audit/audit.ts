import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-audit',
  imports: [
    CommonModule,
    Sidebar
  ],
  templateUrl: './audit.html',
  styleUrl: './audit.css'
})
export class Audit {

  searchText = '';

  selectedUser = '';

  selectedAction = '';

  users = [
    'Administrador',
    'Operador'
  ];

  actions = [
    'Creación de pedido',
    'Actualización de pedido',
    'Creación de producto',
    'Actualización de stock',
    'Cambio de estado'
  ];


  auditRecords = [

    {
      id: 1,
      user: 'Administrador',
      action: 'Creación de pedido',
      module: 'Pedidos',
      description: 'Se creó el pedido #001',
      date: '11/09/2026 10:32'
    },

    {
      id: 2,
      user: 'Operador',
      action: 'Actualización de pedido',
      module: 'Pedidos',
      description: 'El pedido #002 pasó a En preparación',
      date: '11/09/2026 10:18'
    },

    {
      id: 3,
      user: 'Administrador',
      action: 'Creación de producto',
      module: 'Catálogo',
      description: 'Se agregó el producto Notebook Lenovo',
      date: '11/09/2026 09:45'
    },

    {
      id: 4,
      user: 'Operador',
      action: 'Actualización de stock',
      module: 'Catálogo',
      description: 'Se actualizó el stock del producto SKU-003',
      date: '11/09/2026 09:20'
    },

    {
      id: 5,
      user: 'Administrador',
      action: 'Cambio de estado',
      module: 'Pedidos',
      description: 'El pedido #003 pasó a Despachado',
      date: '10/09/2026 18:42'
    }

  ];


  filteredRecords = this.auditRecords;


  onSearch(event: Event): void {

    const input =
      event.target as HTMLInputElement;

    this.searchText =
      input.value;

    this.filterRecords();

  }


  onUserChange(event: Event): void {

    const select =
      event.target as HTMLSelectElement;

    this.selectedUser =
      select.value;

    this.filterRecords();

  }


  onActionChange(event: Event): void {

    const select =
      event.target as HTMLSelectElement;

    this.selectedAction =
      select.value;

    this.filterRecords();

  }


  filterRecords(): void {

    const search =
      this.searchText
        .toLowerCase()
        .trim();


    this.filteredRecords =
      this.auditRecords.filter(
        record => {

          const matchesSearch =

            record.user
              .toLowerCase()
              .includes(search) ||

            record.action
              .toLowerCase()
              .includes(search) ||

            record.module
              .toLowerCase()
              .includes(search) ||

            record.description
              .toLowerCase()
              .includes(search);


          const matchesUser =
            this.selectedUser === '' ||
            record.user === this.selectedUser;


          const matchesAction =
            this.selectedAction === '' ||
            record.action === this.selectedAction;


          return (
            matchesSearch &&
            matchesUser &&
            matchesAction
          );

        }
      );

  }


  getActionClass(
    action: string
  ): string {

    if (
      action.includes('Creación')
    ) {
      return 'create';
    }


    if (
      action.includes('Actualización')
    ) {
      return 'update';
    }


    if (
      action.includes('Cambio')
    ) {
      return 'change';
    }


    return '';

  }


  getModuleClass(
    module: string
  ): string {

    if (module === 'Pedidos') {
      return 'orders';
    }


    if (module === 'Catálogo') {
      return 'catalog';
    }


    return '';

  }


  getTotalRecords(): number {

    return this.auditRecords.length;

  }


  getAdminActions(): number {

    return this.auditRecords.filter(
      record =>
        record.user === 'Administrador'
    ).length;

  }


  getOperatorActions(): number {

    return this.auditRecords.filter(
      record =>
        record.user === 'Operador'
    ).length;

  }


  getTodayRecords(): number {

    return this.auditRecords.filter(
      record =>
        record.date.startsWith(
          '11/09/2026'
        )
    ).length;

  }

}