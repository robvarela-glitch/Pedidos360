import { ApplicationConfig, provideZonelessChangeDetection, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { routes } from './app.routes';

import {
    IPublicClientApplication,
    PublicClientApplication,
    InteractionType,
    BrowserCacheLocation
} from '@azure/msal-browser';

import {
    MsalGuard,
    MsalInterceptor,
    MSAL_INSTANCE,
    MSAL_GUARD_CONFIG,
    MSAL_INTERCEPTOR_CONFIG,
    MsalGuardConfiguration,
    MsalInterceptorConfiguration,
    MsalService,
    MsalBroadcastService
} from '@azure/msal-angular';

export function MSALInstanceFactory(): IPublicClientApplication {
    return new PublicClientApplication({
        auth: {
            // Aquí va el clientId de tu aplicación
            clientId: 'bd954135-48c5-4201-ac11-37071ec979ab', 
            // Aquí va el tenantId de tu aplicación
            authority: 'https://login.microsoftonline.com/d2199b76-e9ac-4fc3-92d6-bbeed1d825dc',
            // Aquí va la URL de redirección de tu aplicación
            redirectUri: 'http://localhost:4200',
            // Aquí va la URL de redirección después de la cerrar sesión en tu aplicación
            postLogoutRedirectUri: 'http://localhost:4200'
        },
        cache: {
            cacheLocation: BrowserCacheLocation.LocalStorage
        }
    });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
    return {
        interactionType: InteractionType.Redirect,
        authRequest: {
            scopes: ['user.read']
        }
    };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
    const protectedResourceMap = new Map<string, Array<string>>();
    protectedResourceMap.set('https://graph.microsoft.com/v1.0/me', ['user.read']);

    return {
        interactionType: InteractionType.Redirect,
        protectedResourceMap
    };
}

export const appConfig: ApplicationConfig = {
    providers: [
        provideZonelessChangeDetection(),
        provideRouter(routes),
        provideHttpClient(
            withInterceptorsFromDi()
        ),
        {
            provide: MSAL_INSTANCE,
            useFactory: MSALInstanceFactory
        },
        provideAppInitializer(async () => {
            const msalInstance = inject(MSAL_INSTANCE) as IPublicClientApplication;
            await msalInstance.initialize();
            const response = await msalInstance.handleRedirectPromise();

            if (response?.account) {
              msalInstance.setActiveAccount(response.account);
            }

            const activeAccount =
              msalInstance.getActiveAccount();

            const accounts =
              msalInstance.getAllAccounts();

            if (!activeAccount && accounts.length > 0) {
              msalInstance.setActiveAccount(accounts[0]);
            }
        }),

        {
            provide: MSAL_GUARD_CONFIG,
            useFactory: MSALGuardConfigFactory
        },
        {
            provide: MSAL_INTERCEPTOR_CONFIG,
            useFactory: MSALInterceptorConfigFactory
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: MsalInterceptor,
            multi: true
        },
        MsalService,
        MsalGuard,
        MsalBroadcastService
    ]
};
