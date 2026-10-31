import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuditLogEntry, BackofficeService } from '../../core/services/backoffice.service';
import { describeHttpError } from '../../core/services/auth.service';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

interface AuditRecord {
  id: number;
  user: string;
  action: string;
  module: string;
  description: string;
  date: string;
  routingKey: string;
}

/**
 * Bitácora de auditoría (solo Administrador): eventos de dominio que ms-audit recibe
 * por la cola pedidos360.audit.events (exchange topic, patrón #) y guarda en Oracle.
 */
@Component({
  selector: 'app-audit',
  imports: [
    CommonModule,
    Sidebar
  ],
  templateUrl: './audit.html',
  styleUrl: './audit.css'
})
export class Audit implements OnInit {

  private readonly backoffice = inject(BackofficeService);
  private readonly cdr = inject(ChangeDetectorRef);

  searchText = '';
  selectedUser = '';
  selectedAction = '';
  users: string[] = [];
  actions: string[] = [];
  auditRecords: AuditRecord[] = [];
  filteredRecords: AuditRecord[] = [];
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.backoffice.getAuditLogs().subscribe({
      next: (logs) => {
        this.auditRecords = logs.map(toRecord);
        this.users = [...new Set(this.auditRecords.map((r) => r.user))];
        this.actions = [...new Set(this.auditRecords.map((r) => r.action))];
        this.filterRecords();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = describeHttpError(err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onSearch(event: Event): void {
    this.searchText = (event.target as HTMLInputElement).value;
    this.filterRecords();
  }

  onUserChange(event: Event): void {
    this.selectedUser = (event.target as HTMLSelectElement).value;
    this.filterRecords();
  }

  onActionChange(event: Event): void {
    this.selectedAction = (event.target as HTMLSelectElement).value;
    this.filterRecords();
  }

  filterRecords(): void {
    const search = this.searchText.toLowerCase().trim();
    this.filteredRecords = this.auditRecords.filter((record) => {
      const matchesSearch =
        record.user.toLowerCase().includes(search) ||
        record.action.toLowerCase().includes(search) ||
        record.module.toLowerCase().includes(search) ||
        record.description.toLowerCase().includes(search);
      const matchesUser = this.selectedUser === '' || record.user === this.selectedUser;
      const matchesAction = this.selectedAction === '' || record.action === this.selectedAction;
      return matchesSearch && matchesUser && matchesAction;
    });
  }

  getActionClass(action: string): string {
    if (action.includes('Creación')) {
      return 'create';
    }
    if (action.includes('Actualización') || action.includes('Desactivación')) {
      return 'update';
    }
    if (action.includes('Cambio') || action.includes('Cancelación')) {
      return 'change';
    }
    return '';
  }

  getModuleClass(module: string): string {
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

  /** Eventos del módulo Pedidos (order.*). */
  getAdminActions(): number {
    return this.auditRecords.filter((r) => r.module === 'Pedidos').length;
  }

  /** Eventos del módulo Catálogo (product.*). */
  getOperatorActions(): number {
    return this.auditRecords.filter((r) => r.module === 'Catálogo').length;
  }

  getTodayRecords(): number {
    const today = formatAuditDate(new Date().toISOString()).split(' ')[0];
    return this.auditRecords.filter((r) => r.date.startsWith(today)).length;
  }
}

const ACTIONS: Record<string, string> = {
  ORDER_CREATED: 'Creación de pedido',
  ORDER_STATUS_UPDATED: 'Cambio de estado',
  ORDER_CANCELLED: 'Cancelación de pedido',
  PRODUCT_CREATED: 'Creación de producto',
  PRODUCT_UPDATED: 'Actualización de producto',
  PRODUCT_DEACTIVATED: 'Desactivación de producto'
};

function toRecord(log: AuditLogEntry): AuditRecord {
  const isProduct = (log.routingKey ?? '').startsWith('product.') || log.eventType.startsWith('PRODUCT');
  let detail = '';
  try {
    const payload = JSON.parse(log.payload);
    detail = isProduct
      ? (payload.name ? payload.name + ' (SKU ' + payload.sku + ')' : '')
      : (payload.status ? 'estado ' + payload.status : '');
  } catch {
    detail = '';
  }
  return {
    id: log.id,
    user: log.actor || log.sourceService,
    action: ACTIONS[log.eventType] ?? log.eventType,
    module: isProduct ? 'Catálogo' : 'Pedidos',
    description: (isProduct ? 'Producto #' : 'Pedido #') + log.entityId
      + (detail ? ' · ' + detail : '') + ' · routing key ' + (log.routingKey ?? '-'),
    date: formatAuditDate(log.recordedAt),
    routingKey: log.routingKey ?? ''
  };
}

/** dd/MM/yyyy HH:mm (el template separa fecha y hora por el espacio). */
function formatAuditDate(value: string): string {
  const d = new Date(value);
  if (isNaN(d.getTime())) {
    return value;
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}
