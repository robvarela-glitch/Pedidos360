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

    alert('1 - BOTON FUNCIONA');

    console.log('1 - BOTON FUNCIONA');

    const loginRequest: RedirectRequest = {
      scopes: ['User.Read']
    };

    alert('2 - EJECUTANDO LOGIN REDIRECT');

    console.log('2 - EJECUTANDO LOGIN REDIRECT');

    this.msalService.loginRedirect(loginRequest).subscribe({
      next: () => {
        console.log('3 - LOGIN REDIRECT EJECUTADO');
      },
      error: (error) => {
        console.error('4 - ERROR LOGIN REDIRECT:', error);
        alert('4 - ERROR LOGIN REDIRECT: ' + error);
      }
    });
  }
}