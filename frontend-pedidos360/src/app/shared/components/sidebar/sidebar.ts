import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

/**
 * Menú lateral: muestra solo las opciones del rol del usuario
 * (Administrador, Operador o Cliente) según GET /api/me.
 */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {

  readonly auth = inject(AuthService);

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
  }

  cerrarSesion(): void {
    this.auth.logout();
  }
}
