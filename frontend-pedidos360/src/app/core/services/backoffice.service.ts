import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { apiUrl } from '../config/app-config';

export interface ReportSummary {
  pedidosCreados: number;
  pedidosCancelados: number;
  cambiosDeEstado: number;
  totalVendido: number;
  totalCancelado: number;
  totalNeto: number;
}

export interface AuditLogEntry {
  id: number;
  sourceService: string;
  entityId: string;
  eventType: string;
  routingKey: string | null;
  actor: string | null;
  payload: string;
  recordedAt: string;
}

export interface NotificationEntry {
  id: number;
  orderId: number;
  customerId: string;
  eventType: string;
  message: string;
  sentAt: string;
}

/** Reportes (Admin), auditoría (Admin) y notificaciones (filtradas por dueño en el backend). */
@Injectable({ providedIn: 'root' })
export class BackofficeService {

  private readonly http = inject(HttpClient);

  getReportSummary(): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(apiUrl('/api/reports/summary'));
  }

  getAuditLogs(): Observable<AuditLogEntry[]> {
    return this.http.get<AuditLogEntry[]>(apiUrl('/api/audit/logs'));
  }

  getNotifications(): Observable<NotificationEntry[]> {
    return this.http.get<NotificationEntry[]>(apiUrl('/api/notify/notifications'));
  }

  getOrderNotifications(orderId: number): Observable<NotificationEntry[]> {
    return this.http.get<NotificationEntry[]>(apiUrl('/api/notify/notifications/pedido/' + orderId));
  }
}
