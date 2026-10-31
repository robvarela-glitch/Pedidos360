# Observabilidad: Prometheus + Grafana

## Qué se agregó

| Pieza | Dónde | Qué hace |
|---|---|---|
| `micrometer-registry-prometheus` | `pom.xml` de los 7 microservicios | Expone las métricas de Spring Boot en `/actuator/prometheus` |
| `management.*` | `application.properties` de los 7 servicios | Habilita el endpoint, etiqueta cada métrica con `application` y activa histogramas de latencia |
| Regla `/actuator/prometheus` | `SecurityConfig` de los 7 servicios | Solo acepta peticiones desde la red interna de Docker (`172.16.0.0/12`, `192.168.0.0/16`) o `127.0.0.1`. Desde Internet: rechazado |
| Métrica propia `pedidos360_rabbit_messages_total` | `ManualAckHandler` (notify, report, audit) | Cuenta cada mensaje por cola y resultado: `ack`, `retry`, `dlq` |
| Prometheus | `infra/prometheus/prometheus.yml` | Lee cada 15 s los 7 microservicios y los 2 nodos RabbitMQ (plugin `rabbitmq_prometheus`, puerto 15692) |
| Grafana | `infra/grafana/...` | Fuente de datos y dashboard **Pedidos360 — Observabilidad** creados automáticamente |
| Servicios `prometheus` y `grafana` | `infra/docker-compose.yml` | Publicados solo en `127.0.0.1` (9090 y 3000) |

## Cómo levantarlo (PowerShell, desde `infra`)

```powershell
cd ..\ms-pedidos360-notify; mvn -q -B test; cd ..\infra
docker compose up -d --build
docker compose ps prometheus grafana
```

## Cómo comprobarlo

1. **Métricas de un servicio**: `curl.exe -s http://localhost:8081/actuator/prometheus | Select-String "http_server_requests_seconds_count" | Select-Object -First 3`
2. **Plugin de RabbitMQ**: `docker exec pedidos360-prometheus wget -qO- http://rabbitmq1:15692/metrics/per-object | Select-String "rabbitmq_queue_messages_ready" | Select-Object -First 3`
3. **Prometheus**: http://localhost:9090 → *Status → Targets*: 7 destinos `pedidos360` y 2 `rabbitmq` en estado **UP**.
4. **Grafana**: http://localhost:3000 (usuario y clave de `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` en `infra/.env`) → dashboard **Pedidos360 — Observabilidad**.

## Demostración sugerida

| Acción | Panel donde se ve |
|---|---|
| Comprar en la tienda como Cliente | *Pedidos creados (24 h)*, *Peticiones por segundo*, *Mensajes con ACK* |
| Llamar `/api/orders` sin token (`curl.exe -i http://localhost:8080/api/orders`) | *Accesos rechazados 401/403*, *Respuestas del BFF por código HTTP* |
| Publicar prueba **fatal** y **transient** en Mensajería | *Resultado del procesamiento: ACK / RETRY / DLQ*, *Mensajes en DLQ* |
| Detener un servicio (`docker compose stop ms-report`) y volver a iniciarlo | *Microservicios activos* baja a 6 y vuelve a 7 |

## En EC2

Prometheus y Grafana no se exponen a Internet. Para verlos desde el PC:

```powershell
ssh -i clave.pem -L 3000:localhost:3000 -L 9090:localhost:9090 ec2-user@<IP-EC2>
```

Luego abrir http://localhost:3000 en el navegador local.
