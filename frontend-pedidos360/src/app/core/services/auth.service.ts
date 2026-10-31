import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { Observable, catchError, map, of, shareReplay, tap } from 'rxjs';

import { apiUrl } from '../config/app-config';
import { AppRole, CurrentUser } from '../models/user.model';

/**
 * Identidad y roles del usuario. La fuente de verdad es el BACKEND (GET /api/me),
 * que valida el JWT y aplica las reglas de roles; el frontend solo adapta la vista.
 * (Ocultar opciones en Angular no reemplaza la autorización del backend.)
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);

  readonly user = signal<CurrentUser | null>(null);
  readonly error = signal<string | null>(null);

  readonly isAdmin = computed(() => this.hasRole('Admin'));
  readonly isOperador = computed(() => this.hasRole('Operador'));
  readonly isCliente = computed(() => this.hasRole('Cliente'));
  /** Admin u Operador: gestionan todos los pedidos. */
  readonly canManageOrders = computed(() => this.isAdmin() || this.isOperador());
  readonly mainRole = computed<AppRole | null>(() =>
    this.isAdmin() ? 'Admin' : this.isOperador() ? 'Operador' : this.isCliente() ? 'Cliente' : null);
  readonly displayName = computed(() =>
    this.user()?.name ?? this.msal.instance.getActiveAccount()?.name ?? 'Usuario');

  private me$: Observable<CurrentUser | null> | null = null;

  /** Carga (una vez) la identidad desde el backend. */
  loadMe(): Observable<CurrentUser | null> {
    if (!this.me$) {
      this.me$ = this.http.get<CurrentUser>(apiUrl('/api/me')).pipe(
        tap((me) => {
          this.user.set(me);
          this.error.set(null);
        }),
        catchError((err) => {
          this.error.set(describeHttpError(err));
          this.me$ = null;
          return of(null);
        }),
        shareReplay(1)
      );
    }
    return this.me$;
  }

  hasRole(role: AppRole): boolean {
    return this.user()?.roles?.includes(role) ?? false;
  }

  hasAnyRole(roles: AppRole[]): Observable<boolean> {
    return this.loadMe().pipe(map((me) => !!me && roles.some((r) => me.roles.includes(r))));
  }

  roleLabel(): string {
    switch (this.mainRole()) {
      case 'Admin': return 'Administrador';
      case 'Operador': return 'Operador';
      case 'Cliente': return 'Cliente';
      default: return 'Sin rol';
    }
  }

  logout(): void {
    this.user.set(null);
    this.me$ = null;
    try {
      localStorage.removeItem('pedidos360.cart');
    } catch {
      // sin almacenamiento local
    }
    this.msal.logoutRedirect({ postLogoutRedirectUri: window.location.origin + '/login' });
  }
}

/**
 * Mensaje legible para errores HTTP del backend (401, 403, 409, 422, 503...).
 * Incluye el método y la ruta que fallaron para poder diagnosticar (sin datos sensibles).
 */
export function describeHttpError(err: any): string {
  const status = err?.status;
  const backendMessage = err?.error?.message;
  const where = requestLabel(err?.url);
  if (status === 0) {
    return 'No hay conexión con el backend' + where + '. Revise que el BFF / API Gateway esté activo.';
  }
  if (status === 401) {
    return 'El backend rechazó la sesión (401)' + where + '. Cierre sesión e inicie nuevamente.';
  }
  if (status === 403) {
    return (backendMessage ?? 'No tiene permisos para esta operación') + ' (403' + where + ').';
  }
  if (backendMessage) {
    const details: string[] = err?.error?.details ?? [];
    return details.length ? backendMessage + ': ' + details.join('; ') : backendMessage;
  }
  return 'Error inesperado (' + (status ?? 'sin código') + where + ').';
}

function requestLabel(url: string | null | undefined): string {
  if (!url) {
    return '';
  }
  try {
    const parsed = new URL(url, window.location.origin);
    return ' en ' + parsed.pathname;
  } catch {
    return '';
  }
}
