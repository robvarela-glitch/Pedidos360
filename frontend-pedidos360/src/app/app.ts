import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {

  private readonly msalService = inject(MsalService);

  constructor() {
    this.msalService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result?.account) {
          this.msalService.instance.setActiveAccount(result.account);
        }
      },
      error: (error) => {
        console.error('Error durante el login de Microsoft:', error);
      }
    });
  }
}