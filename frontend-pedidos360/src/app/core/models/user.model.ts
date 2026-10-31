export type AppRole = 'Admin' | 'Operador' | 'Cliente';

/** Respuesta de GET /api/me: identidad que el backend obtuvo del JWT validado. */
export interface CurrentUser {
  userId: string;
  name: string | null;
  username: string | null;
  roles: AppRole[];
  rolesInToken: string[];
  scope: string | null;
  issuer: string | null;
  audience: string[] | null;
  tokenVersion: string | null;
  expiresAt: string | null;
}
