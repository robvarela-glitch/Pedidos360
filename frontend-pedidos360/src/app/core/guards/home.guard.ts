import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Página de inicio según el rol que el backend leyó del JWT (GET /api/me):
 *   Admin -> /dashboard   Operador -> /operacion   Cliente -> /tienda
 * Si el backend no entrega la identidad, se muestra /acceso con el motivo.
 */
export const homeRedirectGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.loadMe().pipe(
    map((me) => {
      if (!me) {
        return router.parseUrl('/acceso');
      }
      if (me.roles.includes('Admin')) {
        return router.parseUrl('/dashboard');
      }
      if (me.roles.includes('Operador')) {
        return router.parseUrl('/operacion');
      }
      if (me.roles.includes('Cliente')) {
        return router.parseUrl('/tienda');
      }
      return router.parseUrl('/acceso');
    })
  );
};
