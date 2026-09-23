import {
  Component,
  inject,
  OnInit
} from '@angular/core';

import {
  Router
} from '@angular/router';

import {
  MsalService
} from '@azure/msal-angular';

import {
  RedirectRequest
} from '@azure/msal-browser';


@Component({
  selector: 'app-login',

  standalone: true,

  imports: [],

  templateUrl: './login.html',

  styleUrl: './login.css'
})
export class Login implements OnInit {


  private readonly msalService =
    inject(MsalService);


  private readonly router =
    inject(Router);


  // =====================================================
  // INICIO
  // =====================================================

  ngOnInit(): void {

    console.log(
      'Página de Login cargada'
    );


    // Comprobar si Microsoft ya tiene
    // una cuenta autenticada.
    const activeAccount =
      this.msalService.instance
        .getActiveAccount();


    const accounts =
      this.msalService.instance
        .getAllAccounts();


    // Si no existe una cuenta activa pero
    // hay una cuenta almacenada, utilizarla.
    if (!activeAccount && accounts.length > 0) {

      this.msalService.instance
        .setActiveAccount(
          accounts[0]
        );
    }


    const account =
      this.msalService.instance
        .getActiveAccount();


    // Si ya está autenticado,
    // enviarlo directamente al dashboard.
    if (account) {

      console.log(
        'Sesión existente detectada'
      );

      console.log(
        'Usuario:',
        account.username
      );


      this.router.navigate(
        ['/dashboard'],
        {
          replaceUrl: true
        }
      );
    }
  }


  // =====================================================
  // LOGIN CON MICROSOFT
  // =====================================================

  login(): void {

    console.log(
      'LOGIN PEDIDOS360 EJECUTADO'
    );


    const loginRequest:
      RedirectRequest = {

        scopes: [
          'User.Read'
        ]
      };


    console.log(
      'Intentando abrir Microsoft...'
    );


    this.msalService.loginRedirect(
      loginRequest
    );
  }
}