# ms-pedidos360-bff

BFF (Backend For Frontend) de Pedidos360 — parte de Benjamín.

Único punto de entrada HTTP para el frontend Angular (Roberto). No tiene base de datos
propia: valida el JWT del IDaaS y reenvía la petición ya autenticada al microservicio
interno que corresponda (`orders`, `catalog`, `notify`, `report`, `audit`).

## Qué valida del JWT y dónde (`SecurityConfig`)

| Validación | Mecanismo | Si falla |
|---|---|---|
| **Firma** | `JwtDecoders.fromIssuerLocation(issuerUri)` descarga el JWKS del tenant y verifica la firma RS256 | 401 |
| **Issuer** | Incluido en `JwtValidators.createDefaultWithIssuer(issuerUri)` | 401 |
| **Expiración** | Incluida en el mismo `createDefaultWithIssuer` (`exp`/`nbf`) | 401 |
| **Audience** | Validador propio (Spring Security no lo trae por defecto), contra `pedidos360.security.audience` | 401 |
| **Roles** | Claim `roles` del token → `ROLE_xxx`, usado en `authorizeHttpRequests` | 403 si falta el rol |

Este es el mismo patrón que ya usan `orders`, `catalog`, `notify`, `report` y `audit`
(ver sus `SecurityConfig`), pero el BFF además agrega **reglas de autorización por rol**
(pendiente en el resto de los servicios, ver `PARTE-AGUSTIN.md` sección 8).

## ⚠️ Esquema de roles — a confirmar con Daniel (App roles en Entra ID)

Como todavía no está definido el formato exacto de los roles/claims (pendiente en
`PARTE-AGUSTIN.md`), asumí este esquema. Avisen si Daniel lo define distinto, para
ajustar `SecurityConfig`:

| Rol (App role en Entra ID) | Puede |
|---|---|
| `Orders.Read` | Ver pedidos (`GET /api/orders/**`) |
| `Orders.Write` | Crear/modificar/cancelar pedidos |
| `Catalog.Read` | Ver catálogo (`GET /api/catalog/**`) |
| `Catalog.Write` | Crear/editar/eliminar productos |
| `Reports.Read` | Ver reportes (`GET /api/reports/**`) |
| `Audit.Read` | Ver bitácora de auditoría (`GET /api/audit/**`) |
| `Notify.Read` | Ver notificaciones (`GET /api/notify/**`) |
| `Admin` | Acceso total a todo lo anterior |

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
- `ProxyControllerSecurityTest`: sin token → 401; con rol insuficiente → 403 (probado
  para pedidos, escritura de pedidos, y auditoría); servicio no reconocido → 404.

Manualmente, con un token real:
```bash
curl http://localhost:8080/api/orders                                   # 401, sin token
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/orders # reenvía a ms-orders
```
