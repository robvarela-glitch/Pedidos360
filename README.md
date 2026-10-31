# Pedidos360

Sistema de gestión de pedidos Cloud Native — Desarrollo Cloud Native I (Duoc UC).

**Integrantes:** Roberto Varela · Daniel More · Benjamín Madariaga · Agustín Moya

| Componente | Tecnología | Puerto |
|---|---|---|
| `frontend-pedidos360` | Angular 22 + MSAL (Microsoft Entra ID) servido con nginx | 80 (4200 en `ng serve`) |
| `ms-pedidos360-bff` | Spring Boot · único punto de entrada, valida JWT y autoriza por rol | 8080 |
| `ms-pedidos360-orders` | Spring Boot · pedidos (Oracle) · productor RabbitMQ | 8081 |
| `ms-pedidos360-catalog` | Spring Boot · catálogo (Oracle) · productor RabbitMQ | 8082 |
| `ms-pedidos360-notify` | Spring Boot · consumidor RabbitMQ (direct) · notificaciones | 8083 |
| `ms-pedidos360-report` | Spring Boot · consumidor RabbitMQ (topic `order.*`) · reportes | 8084 |
| `ms-pedidos360-audit` | Spring Boot · consumidor RabbitMQ (topic `#`) · auditoría | 8085 |
| `ms-pedidos360-rabbitadmin` | Spring Boot · **administrador de RabbitMQ** (colas, exchanges, bindings) | 8086 |
| Prometheus | Recolecta métricas de los 7 microservicios y de los 2 nodos RabbitMQ | 9090 (solo local) |
| Grafana | Dashboard **Pedidos360 — Observabilidad** | 3000 (solo local) |
| `infra` | Docker Compose: Oracle Free, **clúster RabbitMQ de 2 nodos**, Prometheus, Grafana, Kafka opcional | — |

## 1. Arquitectura

Diagrama del despliegue objetivo en AWS. En local, el navegador entra por http://localhost (nginx) en lugar del API Gateway.

```
Navegador ──HTTPS──> AWS API Gateway (API Manager: autorizador JWT de Entra ID + rutas explícitas + CORS)
                         │
                         ├── GET /, /{proxy+} ──> frontend (nginx, EC2:80)
                         └── /api/... ─────────> ms-bff (EC2:8080) ── valida JWT y rol ──┐
                                                                                         │ reenvía el mismo JWT
             ┌──────────────────┬──────────────────┬───────────────┬──────────────┬───────┘
          ms-orders         ms-catalog         ms-notify      ms-report      ms-audit      ms-rabbitadmin
          (Oracle)          (Oracle)           (Oracle)       (Oracle)       (Oracle)      (AMQP + API mgmt)
              │                  │
              │ publica          │ publica
              ▼                  ▼
  pedidos360.orders.direct (direct) ──[notify.order]──> pedidos360.notify.orders ──> ms-notify
  pedidos360.events.topic  (topic)  ──[order.*]──────> pedidos360.report.orders ──> ms-report
                                    ──[#]────────────> pedidos360.audit.events  ──> ms-audit
  pedidos360.dlx           (direct) ──[<cola>.dlq]───> pedidos360.notify.orders.dlq / .report.orders.dlq / .audit.events.dlq

  RabbitMQ: clúster de 2 nodos (rabbit@rabbitmq1 + rabbit@rabbitmq2)

  Observabilidad: Prometheus ──lee /actuator/prometheus (7 servicios) y :15692 (2 nodos RabbitMQ)──> Grafana
```

### Seguridad (Microsoft Entra ID)
- Login OAuth 2.0 / OpenID Connect con **Authorization Code + PKCE** (MSAL Browser).
- El frontend pide el scope `api://<client-id>/access_as_user`; MSAL agrega `Authorization: Bearer` solo a las llamadas al backend.
- **Todos** los servicios validan firma, `exp`, `iss` (tenant), `aud` (API de Pedidos360) y `scp=access_as_user` (`JwtSecuritySupport`).
- Roles = App roles de Entra ID en el claim `roles`: **Admin**, **Operador**, **Cliente** (sin rol asignado ⇒ `DEFAULT_ROLE`, por defecto `Cliente`).
- Identidad del usuario = claim `oid`. Los pedidos se asocian a esa identidad; un Cliente solo ve y cancela **sus** pedidos (403 si intenta uno ajeno).
- El BFF autoriza con una lista explícita de método + ruta (`denyAll` para el resto) y configura CORS con orígenes explícitos.

| Rol | Puede |
|---|---|
| Cliente | Ver catálogo, crear pedidos, ver y cancelar (en CREADO) **sus** pedidos, ver sus notificaciones |
| Operador | Todo lo anterior sobre **todos** los pedidos + cambiar estado (CREADO → EN_PREPARACION → DESPACHADO → ENTREGADO, o CANCELADO) |
| Admin | Todo lo anterior + administrar catálogo, reportes, auditoría y RabbitMQ (`/api/rabbitadmin`) |

## 2. Levantar todo en local (Windows / PowerShell)

Requisitos: Docker Desktop (≥ 8 GB de RAM asignados), Java 17 y Maven 3.9 (para pruebas), Node 22 (para `ng serve`).

```powershell
cd Pedidos360-main\infra
Copy-Item .env.example .env        # completar AZURE_TENANT_ID y API_CLIENT_ID
# Si antes se levantó la versión anterior (proyecto "infra"), bajarla primero (NO borra volúmenes):
docker compose -p infra down
docker compose up --build -d
docker compose ps                  # todos "healthy" (Oracle tarda 1-2 min la primera vez)
```

Abrir **http://localhost** (frontend en nginx, que reenvía `/api` al BFF) e iniciar sesión con Microsoft.
Para desarrollo con recarga: `cd frontend-pedidos360; npm ci; npm start` y abrir http://localhost:4200 (usa `public/config.json`).

Puertos solo locales (127.0.0.1): Oracle 1522, RabbitMQ 5672 / panel 15672 (nodo 1) y 15673 (nodo 2), microservicios 8081-8086, Prometheus 9090, Grafana 3000.

| Qué | URL |
|---|---|
| Aplicación | http://localhost |
| Grafana | http://localhost:3000 (usuario y clave de `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`) |
| Prometheus | http://localhost:9090 → *Status → Targets* |
| Panel RabbitMQ | http://localhost:15672 |
| Salud del BFF | http://localhost:8080/actuator/health |

> Si después de reconstruir contenedores la aplicación muestra **502 en `/api/me`**, nginx quedó apuntando a la IP anterior del BFF: `docker compose restart frontend`.

## 3. Pruebas

- Pruebas automáticas (por servicio): `mvn test` → ver [docs/PRUEBAS.md](docs/PRUEBAS.md).
- Pruebas de extremo a extremo con token real, DLQ, clúster y persistencia: [docs/PRUEBAS.md](docs/PRUEBAS.md).

## 4. Observabilidad

- Cada microservicio expone métricas Micrometer en `/actuator/prometheus`, accesible **solo desde la red interna de Docker**.
- Métrica propia `pedidos360_rabbit_messages_total{queue, result}` en `ManualAckHandler`: mensajes terminados en `ack`, `retry` o `dlq`.
- Grafana muestra servicios activos, nodos RabbitMQ, mensajes en DLQ, pedidos creados, accesos 401/403, peticiones, latencia, memoria y ACK / RETRY / DLQ por cola.
- Detalle y demostración: [docs/OBSERVABILIDAD.md](docs/OBSERVABILIDAD.md).

## 5. Documentación

| Documento | Contenido |
|---|---|
| [docs/ENTRA-ID.md](docs/ENTRA-ID.md) | App roles, usuarios, redirect URIs y versión del token |
| [docs/DESPLIEGUE-AWS.md](docs/DESPLIEGUE-AWS.md) | EC2 + docker-compose + API Gateway con autorizador JWT |
| [docs/PRUEBAS.md](docs/PRUEBAS.md) | Comandos PowerShell, resultados esperados y evidencias |
| [docs/GUION-PRESENTACION.md](docs/GUION-PRESENTACION.md) | Guion de la Evaluación 4 punto por punto |
| [docs/MATRIZ-CUMPLIMIENTO.md](docs/MATRIZ-CUMPLIMIENTO.md) | Rúbrica de las Evaluaciones 3 y 4 → archivo/clase que la cumple |
| [docs/OBSERVABILIDAD.md](docs/OBSERVABILIDAD.md) | Prometheus + Grafana: qué se mide, cómo comprobarlo y qué mostrar |

## 6. Variables principales (`infra/.env`)

| Variable | Uso |
|---|---|
| `AZURE_TENANT_ID` | Tenant de Entra ID (obligatoria) |
| `API_CLIENT_ID` | Client ID de la App Registration (también define la audience aceptada) |
| `DEFAULT_ROLE` | Rol para usuarios sin App role (`Cliente`; vacío = exigir rol) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos por el BFF (agregar la URL del API Gateway en la nube) |
| `ORACLE_PASSWORD`, `ORACLE_APP_PASSWORD`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_ERLANG_COOKIE` | Credenciales internas (cambiar en la nube) |
| `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | Acceso a Grafana |
| `KAFKA_ENABLED` | `true` + `docker compose --profile kafka up` para el canal Kafka opcional |

`infra/.env` está en `.gitignore`: no subirlo a GitHub.
