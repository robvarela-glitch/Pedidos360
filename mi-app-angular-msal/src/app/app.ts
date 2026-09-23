import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AuthenticationResult, EventMessage, EventType } from '@azure/msal-browser';
import { filter } from 'rxjs/internal/operators/filter';

@Component({
imports: [RouterOutlet, RouterLink],
selector: 'app-root',
styleUrl: './app.css',
templateUrl: './app.html',
})
export class App {
    protected readonly title = signal('mi-app-angular-msal');

    private msalService = inject(MsalService);
    private msalBroadcastService = inject(MsalBroadcastService);

    isLoggedIn = false;

    ngOnInit(): void {
        // Escuchar cuando finalice la inicialización (INITIALIZE_END) o el login
        this.msalBroadcastService.msalSubject$
        .pipe(
            filter((msg: EventMessage) =>
                msg.eventType === EventType.INITIALIZE_END ||
                msg.eventType === EventType.LOGIN_SUCCESS
            )
        )
        .subscribe((result: EventMessage) => {
            if (result.eventType === EventType.LOGIN_SUCCESS) {
                const payload = result.payload as AuthenticationResult;
                this.msalService.instance.setActiveAccount(payload.account);
            }
            this.checkLoginStatus();
        });

        // Intento inicial protegido por try/catch por si ya está listo
        this.checkLoginStatus();
    }

    checkLoginStatus(): void {
        try {
            const activeAccount = this.msalService.instance.getActiveAccount();
            if (!activeAccount && this.msalService.instance.getAllAccounts().length > 0) {
                this.msalService.instance.setActiveAccount(this.msalService.instance.getAllAccounts()[0]);
            }
        this.isLoggedIn = !!this.msalService.instance.getActiveAccount();
        } catch {
            // Ignora la excepción si la consulta ocurre antes de que la inicialización concluya
            this.isLoggedIn = false;
        }
    }

    login(): void {
        this.msalService.loginRedirect();
    }

    logout(): void {
        this.msalService.logoutRedirect();
    }
}