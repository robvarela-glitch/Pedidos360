/**
 * Configuración en tiempo de ejecución (archivo /config.json).
 *
 * Se carga ANTES de iniciar Angular (ver main.ts), así la misma compilación sirve
 * para local y para la nube: en Docker, el contenedor genera config.json con las
 * variables de entorno (API_BASE_URL, AZURE_TENANT_ID, AZURE_CLIENT_ID, API_SCOPE).
 */
export interface AppConfig {
  /** URL base del backend (BFF o API Gateway). Vacío = mismo origen que el frontend. */
  apiBaseUrl: string;
  tenantId: string;
  clientId: string;
  /** Scope delegado de la API de Pedidos360: api://<client-id>/access_as_user */
  apiScope: string;
}

let current: AppConfig | null = null;

export async function loadAppConfig(): Promise<AppConfig> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('No se pudo cargar /config.json (HTTP ' + response.status + ')');
  }
  const config = (await response.json()) as AppConfig;
  const base = (config.apiBaseUrl || window.location.origin).replace(/\/+$/, '');
  current = { ...config, apiBaseUrl: base };
  return current;
}

export function appSettings(): AppConfig {
  if (!current) {
    throw new Error('La configuración aún no se ha cargado');
  }
  return current;
}

/** Construye la URL absoluta de un endpoint del backend: apiUrl('/api/orders'). */
export function apiUrl(path: string): string {
  return appSettings().apiBaseUrl + path;
}
