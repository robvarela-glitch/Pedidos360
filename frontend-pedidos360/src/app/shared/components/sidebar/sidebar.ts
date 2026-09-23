import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar {

  constructor(private msalService: MsalService) {}

  cerrarSesion(): void {

    // Eliminar token guardado manualmente
    localStorage.removeItem('access_token');

    // Cerrar sesión de Microsoft
    this.msalService.logoutRedirect({
      postLogoutRedirectUri: window.location.origin + '/login'
    });

  }

}