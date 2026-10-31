import { ApplicationConfig, provideZonelessChangeDetection, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { routes } from './app.routes';
import { appSettings } from './core/config/app-config';

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

/**
 * Login con Microsoft Entra ID (OAuth 2.0 / OpenID Connect).
 * MSAL Browser usa Authorization Code Flow con PKCE para aplicaciones SPA.
 */
export function MSALInstanceFactory(): IPublicClientApplication {
    const config = appSettings();
    return new PublicClientApplication({
        auth: {
            clientId: config.clientId,
            authority: 'https://login.microsoftonline.com/' + config.tenantId,
            // La URL actual del frontend (localhost o API Gateway) debe estar registrada
            // como Redirect URI de tipo "Single-page application" en Entra ID.
            redirectUri: window.location.origin,
            postLogoutRedirectUri: window.location.origin
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
            scopes: ['openid', 'profile', appSettings().apiScope]
        }
    };
}

/**
 * El interceptor agrega "Authorization: Bearer <access token>" SOLO a las llamadas
 * hacia el backend de Pedidos360 (token con audience de la API y scope access_as_user)
 * y hacia Microsoft Graph (perfil del usuario).
 */
export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
    const config = appSettings();
    const protectedResourceMap = new Map<string, Array<string>>();
    protectedResourceMap.set('https://graph.microsoft.com/v1.0/me', ['user.read']);
    protectedResourceMap.set(config.apiBaseUrl + '/api/*', [config.apiScope]);

    return {
        interactionType: InteractionType.Redirect,
        protectedResourceMap
    };
}

export function createAppConfig(): ApplicationConfig {
    return {
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

                const accounts = msalInstance.getAllAccounts();
                if (!msalInstance.getActiveAccount() && accounts.length > 0) {
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
}
