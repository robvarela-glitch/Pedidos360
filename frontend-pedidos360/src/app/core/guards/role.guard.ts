import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AppRole } from '../models/user.model';
import { AuthService } from '../services/auth.service';

/**
 * Restringe una ruta de Angular a ciertos roles (se usa junto a MsalGuard).
 * Si el rol no corresponde, envía al usuario a su página de inicio (/inicio).
 * Es solo para la experiencia de usuario: el backend vuelve a validar cada petición.
 */
export function roleGuard(roles: AppRole[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.hasAnyRole(roles).pipe(
      map((allowed) => (allowed ? true : router.parseUrl('/inicio')))
    );
  };
}
