import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners
} from '@angular/core';

import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi
} from '@angular/common/http';

import { provideRouter } from '@angular/router';

import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType
} from '@azure/msal-browser';

import {
  MsalGuard,
  MsalInterceptor,
  MsalService,
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG
} from '@azure/msal-angular';

import { routes } from './app.routes';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: 'bd954135-48c5-4201-ac11-37071ec979ab',
      authority: 'https://login.microsoftonline.com/d2199b76-e9ac-4fc3-92d6-bbeed1d825dc',
      redirectUri: 'http://localhost:4200',
      postLogoutRedirectUri: 'http://localhost:4200'
    },
    cache: {
      cacheLocation: 'localStorage',
      storeAuthStateInCookie: false
    }
  });
}

export function MSALGuardConfigFactory() {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: ['User.Read']
    }
  };
}

export function MSALInterceptorConfigFactory() {

  const protectedResourceMap = new Map<string, Array<string>>();

  /*
   * Por ahora dejamos User.Read para la autenticación
   * con Microsoft.
   *
   * La URL del BFF se agregará cuando tengamos
   * confirmado el scope personalizado de Pedidos360.
   */

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}

export const appConfig: ApplicationConfig = {
  providers: [

    provideBrowserGlobalErrorListeners(),

    provideRouter(routes),

    provideHttpClient(
      withInterceptorsFromDi()
    ),

    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory
    },

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

    MsalGuard
  ]
};