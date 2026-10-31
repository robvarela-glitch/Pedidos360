# ms-pedidos360-bff

BFF (Backend For Frontend) de Pedidos360 — parte de Benjamín.

Único punto de entrada HTTP para el frontend Angular (Roberto). No tiene base de datos
propia: valida el JWT del IDaaS y reenvía la petición ya autenticada al microservicio
interno que corresponda (`orders`, `catalog`, `notify`, `report`, `audit`).

## Qué valida del JWT y dónde (`SecurityConfig`)

| Validación | Mecanismo | Si falla |
|---|---|---|
| **Firma** | `NimbusJwtDecoder.withJwkSetUri(...)` con las claves del tenant (`JwtSecuritySupport`) | 401 |
| **Issuer** | Debe ser el tenant (formato v1 `sts.windows.net` o v2 `login.microsoftonline.com/.../v2.0`) | 401 |
| **Expiración** | `JwtTimestampValidator` (`exp`/`nbf`) | 401 |
| **Audience** | Validador propio contra `pedidos360.security.audiences` (`api://<client-id>` y `<client-id>`) | 401 |
| **Scope** | El claim `scp` debe incluir `access_as_user` | 401 |
| **Roles** | Claim `roles` del token → `ROLE_xxx`, usado en `authorizeHttpRequests` | 403 si falta el rol |

Este es el mismo patrón que ya usan `orders`, `catalog`, `notify`, `report` y `audit`
(ver sus `SecurityConfig`), pero el BFF además agrega **reglas de autorización por rol**
(pendiente en el resto de los servicios, ver `PARTE-AGUSTIN.md` sección 8).

## Roles (App roles de Entra ID, claim `roles`)

| Rol | Rutas permitidas en el BFF |
|---|---|
| `Cliente` | `GET /api/me`, `GET /api/catalog/products[/{id}]`, `GET/POST /api/orders`, `GET/DELETE /api/orders/{id}`, `GET /api/notify/notifications/**` |
| `Operador` | Lo del Cliente + `PATCH /api/orders/{id}/status` |
| `Admin` | Todo lo anterior + `POST/PUT/DELETE /api/catalog/products`, `GET /api/reports/**`, `GET /api/audit/**`, `GET/POST/DELETE /api/rabbitadmin/**` |

Cualquier otra combinación de método y ruta se rechaza (`denyAll`). Sin rol asignado en Entra ID se aplica `DEFAULT_ROLE` (por defecto `Cliente`).
La propiedad del recurso (un Cliente solo ve sus pedidos) la aplica cada microservicio con el claim `oid`.

## Rutas y a qué microservicio reenvían

El `ProxyController` es genérico: cualquier ruta `/api/{service}/**` se reenvía —
mismo método, path, query params y body — hacia la URL base configurada para ese
`service`, agregando el mismo header `Authorization`.

| Prefijo | Microservicio destino | Variable de entorno |
|---|---|---|
| `/api/orders/**` | ms-pedidos360-orders (8081) | `ORDERS_SERVICE_URL` |
| `/api/catalog/**` | ms-pedidos360-catalog (8082) | `CATALOG_SERVICE_URL` |
| `/api/notify/**` | ms-pedidos360-notify (8083) | `NOTIFY_SERVICE_URL` |
| `/api/reports/**` | ms-pedidos360-report (8084) | `REPORT_SERVICE_URL` |
| `/api/audit/**` | ms-pedidos360-audit (8085) | `AUDIT_SERVICE_URL` |
| `/api/rabbitadmin/**` | ms-pedidos360-rabbitadmin (8086) | `RABBITADMIN_SERVICE_URL` |

## Ejecutar

**Con Docker Compose** (ya agregado a `infra/docker-compose.yml`):
```bash
cd infra
docker compose up --build
```
El BFF queda en `http://localhost:8080`.

**Con Maven, en local** (con los 5 microservicios ya corriendo en sus puertos):
```bash
export AZURE_TENANT_ID=<tu-tenant-id>
mvn spring-boot:run
```

## Probar

```bash
mvn test
```
- `BffApplicationTests`: el contexto de Spring levanta (seguridad + enrutamiento).
- `ProxyControllerSecurityTest`: sin token → 401; rol insuficiente → 403; método o servicio no
  declarado → 403; CORS permitido / rechazado; `/api/me` devuelve la identidad del token.

Manualmente, con un token real:
```bash
curl http://localhost:8080/api/orders                                   # 401, sin token
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/orders # reenvía a ms-orders
```
