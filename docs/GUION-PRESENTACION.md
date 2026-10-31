# Guion de presentación — Evaluación 4 (grupal, cada integrante fundamenta su parte)

Preparar antes: tres pestañas del navegador (consola AWS, portal de Entra ID, frontend en la URL del API Gateway), una terminal SSH a la EC2 y una PowerShell con `$h` del Admin y `$hPedro` cargados (docs/PRUEBAS.md, paso 3). **No mostrar tokens completos.**

| # | Elemento de la pauta | Quién (sugerido) | Qué mostrar | Resultado esperado / evidencia |
|---|---|---|---|---|
| 1 | Instancia de API Manager en la nube | Agustín | AWS → API Gateway → `pedidos360-api` → Stages `$default` (URL) | API activa con su Invoke URL |
| 2 | Configuración del API Manager hacia el backend | Agustín | *Routes* (lista con métodos explícitos), *Integrations* (`http://<EC2>:8080` y `:80`, mapping `overwrite:path`), *CORS* | Rutas `GET /api/orders`, `PATCH /api/orders/{id}/status`, `POST /api/rabbitadmin/queues`, etc. |
| 3 | Frontend consume los endpoints por el API Manager | Roberto | Abrir `https://<api-id>.execute-api...`; DevTools → Network: llamadas a `https://<api-id>.../api/orders` con `Authorization: Bearer ...` (no expandir el token) | Pedidos y catálogo cargan desde Oracle |
| 4 | El API Manager valida JWT | Benjamín | Terminal: `curl -i $API/api/orders` → **401**; con `Bearer abc.def.ghi` → **401**; con token real → **200**. API Gateway → *Authorization* → autorizador `entra-id-jwt` (issuer, audience) | 401 / 401 / 200 |
| 5 | Tenant en IDaaS y usuarios registrados | Daniel | Entra ID → Overview (Tenant ID) → Users (Ana, Pedro, Olga, admin) → App registration (Expose an API, **App roles**) → Enterprise app → Users and groups | Usuarios con su rol asignado |
| 6 | Login OAuth 2.0 / OIDC y JWT válido | Daniel / Roberto | Cerrar sesión → iniciar sesión con Microsoft. DevTools → Network → petición a `/oauth2/v2.0/token` (`grant_type=authorization_code`, `code_verifier` = PKCE). Abrir `GET /api/me` en el frontend/Network | `/api/me` muestra `scope: access_as_user`, `audience`, `roles` |
| 7 | Backend y frontend desplegados, activos e integrados | Agustín | SSH a EC2: `sudo docker compose ps` (todos healthy). Crear un pedido como Cliente en el frontend y verlo como Operador; cambiar estado | Flujo completo en la nube |
| 8 | Dos nodos RabbitMQ en clúster | Agustín | `sudo docker exec pedidos360-rabbitmq1 rabbitmqctl cluster_status` (Running Nodes: 2) y/o página **Mensajería** (Admin) → "Nodos del clúster (2)" | 2 nodos `running` |
| 9 | Tres colas con sus DLQ funcionando | Benjamín | Página **Mensajería** → colas `pedidos360.notify.orders`, `.report.orders`, `.audit.events` + sus `.dlq`. Publicar prueba **fatal** y **transient** → Actualizar → DLQ con mensajes; logs `[RETRY]` y `[DLQ]` | DLQ con 1+ mensajes y logs de reintentos |
| 10 | Exchanges direct y topic configurados | Benjamín | `rabbitmqctl list_exchanges name type` o Mensajería → `pedidos360.orders.direct (direct)`, `pedidos360.events.topic (topic)`, bindings `notify.order`, `order.*`, `#` | Explicar: direct = notificación punto a punto; topic = reportes solo `order.*`, auditoría todo `#` (incluye `product.*`) |
| 11 | docker-compose en la nube levantando RabbitMQ | Agustín | SSH: `cd ~/Pedidos360-main/infra && sudo docker compose ps` y `sudo docker compose logs --tail 20 rabbitmq1 rabbitmq2` | Contenedores `pedidos360-rabbitmq1/2` healthy |

## Demostración de roles (si hay tiempo; refuerza la retroalimentación anterior)
1. **Ana (Cliente)**: catálogo → nuevo pedido → ve solo "Mis pedidos"; puede cancelar mientras está CREADO.
2. **Pedro (Cliente)** en otra ventana privada: no ve el pedido de Ana; abrir `/orders/<id de Ana>` → mensaje "pertenece a otro cliente (403)".
3. **Olga (Operador)**: ve todos los pedidos → "Pasar a En preparación" → la notificación aparece en el historial del pedido (ms-notify vía RabbitMQ).
4. **Admin**: Reportes (ms-report), Auditoría (ms-audit, incluye cambios de catálogo `product.*`), Mensajería (ms-rabbitadmin).

## Defensa técnica (preguntas probables)
- **¿Por qué el JWT se valida dos veces?** API Gateway rechaza tokens inválidos en el borde; el BFF y cada microservicio vuelven a validar firma, issuer, audience y scope (defensa en profundidad) y aplican rol y propiedad.
- **¿Cómo se identifica al usuario?** Claim `oid` del token validado; el `customerId` del JSON se ignora.
- **¿Cómo se manejan los fallos de mensajes?** ACK manual (`ManualAckHandler`): éxito → ACK; error no recuperable → NACK sin requeue → DLX → DLQ; error recuperable → reintento (republicación en la misma cola con `x-retry-count`) hasta 3 intentos → DLQ. Todo envío a DLQ queda en el log con `[DLQ]`.
- **¿Dónde están los nombres de colas?** `application.properties` (prefijo `pedidos360.rabbit`) + `RabbitTopologyProperties`; mismos nombres en todos los servicios.
- **¿Qué hace el administrador?** `ms-pedidos360-rabbitadmin`: API REST (`/api/rabbitadmin/queues|exchanges|bindings|nodes|messages`), lógica en `RabbitAdminService`, DTO con validación, recursos base protegidos y acceso solo para Admin.
- **¿Dónde está la base de datos?** Oracle Free en la instancia EC2 (contenedor con volumen persistente), accesible solo desde la red interna de Docker.

## Plan B (si algo de la nube falla en vivo)
- Mostrar el mismo flujo en local (`http://localhost`) con docker-compose y los tests automáticos (`mvn test`).
- Si la cuenta AWS no permite API Gateway: mostrar el mensaje de error de permisos, el script `infra/aws/create-api-gateway.sh` y el BFF como API Manager de respaldo (rutas explícitas + JWT + CORS en `SecurityConfig`).
