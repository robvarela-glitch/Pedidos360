import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Se muestra cuando el backend no entrega la identidad (GET /api/me falla)
 * o el usuario no tiene ninguno de los roles de Pedidos360.
 */
@Component({
  selector: 'app-access',
  imports: [RouterLink],
  template: `
    <main class="access">
      <section class="panel">
        <h1>No pudimos abrir tu espacio de trabajo</h1>
        @if (auth.error()) {
          <p class="reason">{{ auth.error() }}</p>
        } @else if (auth.user() && auth.user()!.roles.length === 0) {
          <p class="reason">Tu usuario no tiene un rol de Pedidos360 asignado en Microsoft Entra ID.</p>
        } @else {
          <p class="reason">Estamos verificando tu sesión con el backend.</p>
        }
        <p class="help">
          Si el problema continúa, pide a la persona que administra Entra ID que te asigne el rol
          Cliente, Operador o Administrador en la aplicación Pedidos360.
        </p>
        <div class="actions">
          <a routerLink="/inicio" class="primary">Reintentar</a>
          <button type="button" (click)="auth.logout()">Cerrar sesión</button>
        </div>
      </section>
    </main>
  `,
  styles: [`
    .access { min-height: 100vh; display: grid; place-items: center; background: #f4f6f8; padding: 24px; font-family: system-ui, sans-serif; }
    .panel { max-width: 520px; background: #fff; border: 1px solid #dde3ea; border-radius: 16px; padding: 32px; color: #0e1726; }
    h1 { margin: 0 0 12px; font-size: 22px; }
    .reason { margin: 0 0 12px; padding: 12px 14px; border-radius: 10px; background: #fef3f2; color: #912018; line-height: 1.45; }
    .help { color: #475467; line-height: 1.5; margin: 0 0 20px; }
    .actions { display: flex; gap: 10px; }
    .actions a, .actions button { border-radius: 10px; padding: 10px 16px; font: inherit; font-weight: 600; cursor: pointer; text-decoration: none; }
    .primary { background: #2563eb; color: #fff; border: 0; }
    button { background: #fff; color: #0e1726; border: 1px solid #dde3ea; }
    a:focus-visible, button:focus-visible { outline: 3px solid #93c5fd; outline-offset: 2px; }
  `]
})
export class Access implements OnInit {
  readonly auth = inject(AuthService);

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
  }
}
