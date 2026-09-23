import { Component, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { RedirectRequest } from '@azure/msal-browser';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {

  private readonly msalService = inject(MsalService);

  login(): void {

    console.log('LOGIN PEDIDOS360 EJECUTADO');

    const loginRequest: RedirectRequest = {
      scopes: ['User.Read']
    };

    console.log('Intentando abrir Microsoft...');

    this.msalService.loginRedirect(loginRequest);
  }
}