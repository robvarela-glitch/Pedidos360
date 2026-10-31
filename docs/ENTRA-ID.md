# Microsoft Entra ID — configuración de Pedidos360

App Registration existente: **Pedidos360** · Client ID `bd954135-48c5-4201-ac11-37071ec979ab` · Tenant `d2199b76-e9ac-4fc3-92d6-bbeed1d825dc`.
La misma App Registration es el cliente SPA (Angular) **y** la API (scope `access_as_user`).

## 1. Expose an API (ya existe, solo verificar)
- Application ID URI: `api://bd954135-48c5-4201-ac11-37071ec979ab`
- Scope: `access_as_user` (Admins and users, Enabled).
- En **API permissions** debe aparecer `Pedidos360 / access_as_user` (Delegated) con consentimiento concedido.

## 2. App roles (NUEVO — necesario para Admin / Operador / Cliente)
App registrations → Pedidos360 → **App roles** → *Create app role* (tres veces):

| Display name | Allowed member types | Value | Description |
|---|---|---|---|
| Administrador | Users/Groups | `Admin` | Administra catálogo, reportes, auditoría y RabbitMQ |
| Operador | Users/Groups | `Operador` | Gestiona y cambia el estado de los pedidos |
| Cliente | Users/Groups | `Cliente` | Crea y consulta sus propios pedidos |

> El **Value** debe ser exactamente `Admin`, `Operador` y `Cliente` (mayúsculas incluidas): el backend los lee del claim `roles`.

## 3. Usuarios y asignación de roles
1. **Users** → *New user* → crear usuarios de prueba, por ejemplo:
   - `ana@<dominio>` (Cliente) y `pedro@<dominio>` (Cliente) → para probar que Ana no ve pedidos de Pedro.
   - `olga@<dominio>` (Operador).
   - `admin@<dominio>` (Administrador).
2. **Enterprise applications** → Pedidos360 → **Users and groups** → *Add user/group* → elegir usuario y rol.
3. Los usuarios deben **cerrar sesión e iniciar nuevamente** para que el token traiga el claim `roles`.

Si un usuario no tiene rol asignado, el backend le aplica `DEFAULT_ROLE` (por defecto `Cliente`). Para exigir rol explícito, dejar `DEFAULT_ROLE=` vacío en `infra/.env`.

## 4. Authentication → Single-page application (Redirect URIs)
Agregar (tipo **SPA**, no "Web"):
- `http://localhost:4200` (desarrollo con `ng serve`)
- `http://localhost` (docker-compose local con nginx)
- `https://<api-id>.execute-api.us-east-1.amazonaws.com` (frontend publicado por el API Gateway; lo entrega `create-api-gateway.sh`)

Entra ID solo acepta `http://` para `localhost`; en la nube se usa la URL **https** del API Gateway.

## 5. Versión del access token
El backend acepta tokens v1 y v2 del tenant. El **API Gateway** necesita saber cuál emite la app:

1. Iniciar sesión en el frontend local y abrir en otra pestaña la respuesta de `GET http://localhost/api/me` (o verla en DevTools → Network).
2. Revisar `tokenVersion` e `issuer`:
   - `"1.0"` → issuer `https://sts.windows.net/<tenant>/` → usar `TOKEN_VERSION=1` en el script del API Gateway.
   - `"2.0"` → issuer `https://login.microsoftonline.com/<tenant>/v2.0` → `TOKEN_VERSION=2`.

(Opcional) Para forzar v2: Manifest → `"requestedAccessTokenVersion": 2` (en el editor nuevo, dentro de `"api"`). Después de cambiarlo, cerrar sesión y volver a entrar.

## 6. Qué mostrar en la presentación
- Overview del tenant (Tenant ID, dominio) y la lista de **Users**.
- App registration: Expose an API (scope), App roles, Redirect URIs.
- Enterprise application → Users and groups (usuario ↔ rol).
- Respuesta de `/api/me` (sin el token): `roles`, `scope`, `audience`, `issuer`.

**Nunca** publicar el JWT completo en capturas, informes o el repositorio.
