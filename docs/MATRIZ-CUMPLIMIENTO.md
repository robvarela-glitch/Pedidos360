# Matriz de cumplimiento — Evaluaciones 3 y 4

Estados: **Implementado** (en código, con test cuando aplica) · **Requiere ejecución** (hay que levantarlo/demostrarlo) · **Requiere acción externa** (Entra ID / AWS).

## Rúbrica (idéntica en Evaluación 3 y Evaluación 4)

| # | Indicador | Peso | Dónde se cumple | Cómo se demuestra |
|---|---|---|---|---|
| R1 | Nombres de colas, exchanges y bindings centralizados | 12 % | `application.properties` (prefijo `pedidos360.rabbit.*`, mismo bloque en los 5 servicios con RabbitMQ) + `config/RabbitTopologyProperties.java` (`@ConfigurationProperties`). Listeners usan `${pedidos360.rabbit.queues.*}`; publishers usan `RabbitTopologyProperties` | Mostrar el bloque de properties y que no hay strings de colas en servicios de negocio |
| R2 | Beans Queue/Exchange/Binding por caso de uso | 13 % | Notificar: `notify/config/RabbitConfig` (direct + cola + DLQ + bindings). Reportes: `report/config/RabbitConfig` (topic `order.*`). Auditoría: `audit/config/RabbitConfig` (topic `#`). Productores: `orders/config/RabbitConfig`, `catalog/config/RabbitConfig` | `rabbitmqctl list_bindings` |
| R3 | Negocio separado de la configuración de mensajería | 10 % | `OrderService` / `ProductService` solo llaman a `OrderEventPublisher.publish(...)` / `ProductEventPublisher.publish(...)`; configuración en paquetes `config` | Revisión de código |
| R4 | Consumidores `@RabbitListener` agrupados por dominio | 15 % | `notify/listener/OrderNotificationListener`, `report/listener/OrderReportListener`, `audit/listener/DomainEventAuditListener` (un consumidor por dominio y por cola) | Panel RabbitMQ: 1 consumidor por cola |
| R5 | ACK explícito y manejo de errores (reintentos, NACK, DLQ) | 20 % | `messaging/ManualAckHandler` (acknowledge-mode=manual): ACK / NACK sin requeue → DLX → DLQ / reintento con `x-retry-count` (máx. 3) / log `[ACK]`, `[RETRY]`, `[DLQ]`. `NonRecoverableMessageException` para errores no recuperables. Test `ManualAckHandlerTest` | PRUEBAS.md §7 |
| R6 | Microservicio administrador con endpoints REST | 13 % | `ms-pedidos360-rabbitadmin` → `RabbitAdminController`: `GET/POST /queues`, `GET/DELETE /queues/{name}`, `DELETE /queues/{name}/messages`, `GET/POST /exchanges`, `DELETE /exchanges/{name}`, `GET/POST/DELETE /bindings`, `GET /nodes`, `POST /messages` | PRUEBAS.md §8 / página Mensajería |
| R7 | Lógica encapsulada en un servicio (RabbitAdminService) | 10 % | `service/RabbitAdminService` (AmqpAdmin/RabbitTemplate) + `service/RabbitManagementClient` (listados). El controlador no conoce detalles de RabbitMQ | `RabbitAdminServiceTest` |
| R8 | Validación de entrada | 7 % | DTO `CreateQueueRequest`, `CreateExchangeRequest`, `BindingRequest`, `PublishMessageRequest` con `@NotBlank`, `@Pattern` (sin vacíos, sin `amq.`), enum `ExchangeKind`, rangos de TTL/longitud, routing keys válidas; `GlobalExceptionHandler` con mensajes claros (400/404/409/503) | `RequestValidationTest`, `RabbitAdminControllerTest`, PRUEBAS.md §8 |

## Instrucciones — Evaluación 3 (encargo de código)

| Requisito | Estado | Evidencia |
|---|---|---|
| Backend compila, buenas prácticas, pruebas básicas | Implementado (verificar `mvn test`) | Tests en bff, orders, notify, rabbitadmin |
| Frontend completo, modular, sin errores, vistas funcionales | Implementado (verificar `npm run build`) | Servicios HTTP reales (`core/services`), vistas por rol, sin datos simulados |
| Integración con base de datos cloud (entidades, repositorios, conexión) | Implementado · Requiere ejecución en EC2 | Entidades JPA `PED360_*`, repositorios Spring Data, `spring.datasource.*`; Oracle en la instancia EC2 |
| Filtros que validan el JWT del IDaaS | Implementado | `JwtSecuritySupport` + `SecurityConfig` en los 7 servicios |
| Frontend con login IDaaS y JWT en las llamadas | Implementado | `app.config.ts` (MSAL, scope `access_as_user`, interceptor hacia `/api/*`) |
| `.gitignore` por tecnología | Implementado | `.gitignore` raíz, frontend, cada microservicio e `infra` (`.env` excluido) |
| Productores y consumidores RabbitMQ | Implementado | Productores: orders, catalog. Consumidores: notify, report, audit |
| Mensajes no entregados → DLQ y logs | Implementado | `ManualAckHandler` + 3 DLQ + logs `[DLQ]`; publisher returns en orders |
| Administrador de RabbitMQ crea/elimina colas, exchanges y bindings | Implementado | `ms-pedidos360-rabbitadmin` |

## Elementos a mostrar — Evaluación 4

| Elemento | Estado | Dónde |
|---|---|---|
| Instancia de API Manager en la nube | Requiere acción externa (AWS) | `infra/aws/create-api-gateway.sh`, DESPLIEGUE-AWS.md |
| Configuración del API Manager hacia el backend | Requiere acción externa | Rutas explícitas + integraciones + CORS (mismo script) |
| Frontend consume endpoints vía API Manager | Requiere ejecución | Frontend servido por el API Gateway (mismo origen) |
| API Manager valida JWT | Requiere acción externa | Autorizador JWT `entra-id-jwt` + `authorization-scopes access_as_user` |
| Tenant IDaaS y usuarios | Requiere acción externa (Entra ID) | ENTRA-ID.md (App roles + usuarios) |
| Login OAuth 2.0 / OIDC con JWT válido | Implementado | MSAL (Authorization Code + PKCE), `GET /api/me` |
| Backend y frontend desplegados e integrados | Requiere ejecución en EC2 | `infra/aws/setup-ec2.sh` |
| Dos nodos RabbitMQ en clúster | Implementado · Requiere ejecución | `infra/docker-compose.yml` (rabbitmq1/rabbitmq2) + `infra/rabbitmq/rabbitmq.conf` |
| Tres colas con DLQ funcionando | Implementado · Requiere ejecución | notify/report/audit + `.dlq` |
| Exchanges direct y topic | Implementado | `pedidos360.orders.direct`, `pedidos360.events.topic` (+ `pedidos360.dlx`) |
| docker-compose en la nube levantando RabbitMQ | Requiere ejecución en EC2 | `sudo docker compose ps` |

## Retroalimentación de la evaluación anterior

| Observación | Solución |
|---|---|
| Dashboard único que simulaba todos los perfiles | Roles Admin / Operador / Cliente (App roles), menú y rutas por rol, autorización en BFF y microservicios (`@PreAuthorize`) |
| Backend no identificaba al usuario | `oid` del JWT (`CurrentUser`), pedidos y notificaciones filtrados por dueño; 403 para pedidos ajenos |
| Datos estáticos / sin BD real | Frontend sin mocks; catálogo inicial cargado en Oracle (`CatalogSeeder`); todo persiste en Oracle |
| Despliegue local | Scripts EC2 + API Gateway; puertos internos cerrados |
| Proxy genérico, sin métodos explícitos, CORS | API Gateway con ~30 rutas método+ruta y CORS; BFF con lista explícita y `denyAll`, CORS por orígenes |
| Catálogo accesible sin token | Solo `/actuator/health` y `/api/*/publico/ping` son públicos |
