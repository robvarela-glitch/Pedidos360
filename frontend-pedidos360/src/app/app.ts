import {
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';

import {
  Router,
  RouterLink,
  RouterOutlet
} from '@angular/router';

import {
  MsalBroadcastService,
  MsalService
} from '@azure/msal-angular';

import {
  AuthenticationResult,
  EventMessage,
  EventType,
  AccountInfo
} from '@azure/msal-browser';

import { filter } from 'rxjs/operators';


@Component({
  selector: 'app-root',

  imports: [
    RouterOutlet,
    RouterLink
  ],

  styleUrl: './app.css',

  templateUrl: './app.html'
})
export class App implements OnInit {

  protected readonly title =
    signal('Pedidos360');


  private readonly msalService =
    inject(MsalService);

  private readonly msalBroadcastService =
    inject(MsalBroadcastService);

  private readonly router =
    inject(Router);


  isLoggedIn = false;



  ngOnInit(): void {

    console.log('Pedidos360 iniciado');


    this.msalBroadcastService.msalSubject$
      .pipe(

        filter(
          (message: EventMessage) =>
            message.eventType === EventType.LOGIN_SUCCESS
        )

      )
      .subscribe((message: EventMessage) => {

        console.log(
          'LOGIN DE MICROSOFT COMPLETADO'
        );


        const payload =
          message.payload as AuthenticationResult;


        if (payload.account) {

          this.msalService.instance
            .setActiveAccount(
              payload.account
            );

        }


        this.checkLoginStatus();


        this.goToDashboard();

      });


    this.checkLoginStatus();


    setTimeout(() => {

      if (this.isLoggedIn) {

        this.goToDashboard();

      }

    }, 100);
  }


  checkLoginStatus(): void {

    try {

      let activeAccount =
        this.msalService.instance
          .getActiveAccount();


      const accounts =
        this.msalService.instance
          .getAllAccounts();


      if (!activeAccount && accounts.length > 0) {

        activeAccount =
          accounts[0];

        this.msalService.instance
          .setActiveAccount(
            activeAccount
          );
      }


      this.isLoggedIn =
        !!activeAccount;


      console.log(
        'Usuario autenticado:',
        this.isLoggedIn
      );


      if (activeAccount) {

        console.log(
          'Cuenta:',
          activeAccount.username
        );

        console.log(
          'Nombre:',
          activeAccount.name
        );

      }

    } catch (error) {

      console.error(
        'Error comprobando autenticación:',
        error
      );

      this.isLoggedIn = false;
    }
  }

  goToDashboard(): void {

    const account =
      this.msalService.instance
        .getActiveAccount();


    if (!account) {

      console.log(
        'No existe una cuenta autenticada'
      );

      return;
    }


    console.log(
      'Redirigiendo al Dashboard...'
    );


    this.router.navigate(
      ['/dashboard'],
      {
        replaceUrl: true
      }
    );
  }

  login(): void {

    console.log(
      'Iniciando sesión con Microsoft...'
    );

    this.msalService.loginRedirect({

      scopes: [
        'User.Read'
      ]

    });
  }

  logout(): void {

    console.log(
      'Cerrando sesión...'
    );

    this.msalService.logoutRedirect();
  }
}