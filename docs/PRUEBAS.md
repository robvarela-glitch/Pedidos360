# Pruebas de Pedidos360 (Windows PowerShell)

Todas las rutas son relativas a la carpeta `Pedidos360-main`. Guardar como evidencia: captura de la consola **sin mostrar el token**.

## 0. Pruebas automáticas (sin Docker)

```powershell
$servicios = "bff","orders","catalog","notify","report","audit","rabbitadmin"
foreach ($s in $servicios) {
  Write-Host "== ms-pedidos360-$s" -ForegroundColor Cyan
  Push-Location "ms-pedidos360-$s"
  mvn -q -B test
  if ($LASTEXITCODE -ne 0) { Write-Host "FALLÓ ms-pedidos360-$s" -ForegroundColor Red }
  Pop-Location
}
cd frontend-pedidos360; npm ci; npm run build; cd ..
```

| Test | Qué demuestra |
|---|---|
| `ProxyControllerSecurityTest` (BFF) | 401 sin token, 403 por rol (Cliente no cambia estados, Operador no ve reportes, Cliente no administra RabbitMQ), método no declarado rechazado, CORS permitido / rechazado, `/api/me` |
| `OrderServiceTest` (orders) | Identidad desde el token, precio desde catálogo, Ana no ve ni cancela pedidos de Pedro, transiciones de estado |
| `ManualAckHandlerTest` (notify) | ACK, error no recuperable → DLQ, recuperable → reintento, reintentos agotados → DLQ |
| `RabbitAdminServiceTest`, `RabbitAdminControllerTest`, `RequestValidationTest` (rabbitadmin) | Cola con DLQ, exchange topic, binding, recursos protegidos, 401/403, validaciones (nombres vacíos, `amq.`, routing keys inválidas) |

## 1. Levantar y verificar contenedores

```powershell
cd infra
docker compose up --build -d
docker compose ps
curl.exe -s http://localhost:8080/actuator/health
foreach ($p in 8081..8086) { curl.exe -s "http://localhost:$p/actuator/health"; "" }
```
Esperado: `{"status":"UP"}` en todos. Si un servicio no queda `healthy`: `docker logs --tail 80 pedidos360-ms-<nombre>`.

## 2. Clúster RabbitMQ y topología

```powershell
docker exec pedidos360-rabbitmq1 rabbitmqctl cluster_status
docker exec pedidos360-rabbitmq1 rabbitmqctl list_exchanges name type | Select-String pedidos360
docker exec pedidos360-rabbitmq1 rabbitmqctl list_queues name messages consumers
docker exec pedidos360-rabbitmq1 rabbitmqctl list_bindings source_name routing_key destination_name | Select-String pedidos360
```
Esperado:
- `Running Nodes`: `rabbit@rabbitmq1` y `rabbit@rabbitmq2`.
- Exchanges: `pedidos360.orders.direct direct`, `pedidos360.events.topic topic`, `pedidos360.dlx direct`.
- Colas: `pedidos360.notify.orders`, `pedidos360.report.orders`, `pedidos360.audit.events` (1 consumidor cada una) y sus tres `.dlq`.
- Panel web: http://localhost:15672 (usuario/clave de `RABBITMQ_USER` / `RABBITMQ_PASSWORD`, por defecto `pedidos360`) → pestaña *Overview* muestra los 2 nodos.

## 3. Obtener un access token (sin mostrarlo)
1. Abrir http://localhost e iniciar sesión.
2. DevTools (F12) → Network → seleccionar la petición `me` → Headers → copiar el valor de `Authorization` **sin** la palabra `Bearer`.
3. En PowerShell (no se muestra en pantalla):
```powershell
$token = Read-Host "Pegue el access token" -AsSecureString
$t = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($token))
$h = @{ Authorization = "Bearer $t" }
$api = "http://localhost:8080"     # en la nube: URL del API Gateway

# Devuelve solo el código HTTP (funciona en PowerShell 5.1 y 7)
function Status($method, $url, $headers = @{}, $body = $null) {
  try {
    (Invoke-WebRequest $url -Method $method -Headers $headers -ContentType "application/json" -Body $body -UseBasicParsing).StatusCode
  } catch {
    [int]$_.Exception.Response.StatusCode
  }
}
```

## 4. JWT: rechazo y aceptación

```powershell
"Sin token:      " + (Status GET "$api/api/orders")
"Token inválido: " + (Status GET "$api/api/orders" @{ Authorization = "Bearer abc.def.ghi" })
Invoke-RestMethod "$api/api/me" -Headers $h | Format-List userId,name,roles,scope,audience,issuer,tokenVersion
```
Esperado: `401`, `401`, y `/api/me` con `scope = access_as_user`, `audience` = la API de Pedidos360 y los roles del usuario.
El motivo del rechazo aparece en el header `WWW-Authenticate` (`curl.exe -i ...`).

## 5. Pedido de extremo a extremo (API → Oracle → RabbitMQ → consumidores)

```powershell
$productos = Invoke-RestMethod "$api/api/catalog/products" -Headers $h
$productos | Select-Object id, name, price, stock
$body = @{ items = @(@{ productId = $productos[0].id; quantity = 2 }) } | ConvertTo-Json -Depth 5
$pedido = Invoke-RestMethod "$api/api/orders" -Method Post -Headers $h -ContentType "application/json" -Body $body
$pedido | Select-Object id, customerId, customerName, status, total
Start-Sleep -Seconds 3
docker logs --since 1m pedidos360-ms-orders  | Select-String "Evento ORDER_CREATED"
docker logs --since 1m pedidos360-ms-notify  | Select-String "ACK|Notificación"
docker logs --since 1m pedidos360-ms-report  | Select-String "ACK|Reporte"
docker logs --since 1m pedidos360-ms-audit   | Select-String "ACK|Auditoría"
Invoke-RestMethod "$api/api/notify/notifications/pedido/$($pedido.id)" -Headers $h
```
Esperado: `customerId` = su `oid` (aunque el JSON enviara otro), `total` calculado con el precio del catálogo, eventos publicados por el exchange direct y el topic, y una notificación guardada.

Persistencia en Oracle:
```powershell
docker exec -it pedidos360-oracle sqlplus -s pedidos360/pedidos360@FREEPDB1
```
```sql
SELECT ID, CUSTOMER_NAME, STATUS, TOTAL FROM PED360_ORDERS ORDER BY ID;
SELECT ORDER_ID, EVENT_TYPE, MESSAGE FROM PED360_NOTIFICATIONS ORDER BY ID;
SELECT ORDER_ID, EVENT_TYPE, TOTAL FROM PED360_ORDER_REPORTS ORDER BY ID;
SELECT ENTITY_ID, EVENT_TYPE, ROUTING_KEY FROM PED360_AUDIT_LOG ORDER BY ID;
EXIT;
```
Datos tras reinicio: `docker compose restart ms-orders` y repetir `Invoke-RestMethod "$api/api/orders" -Headers $h` → los pedidos siguen.

## 6. Roles y propiedad (Ana vs Pedro)
Con el token de **Ana** (Cliente) crear un pedido (paso 5) y anotar su id en `$idAna`. Luego iniciar sesión como **Pedro** (Cliente), obtener su token igual que en el paso 3 y guardarlo en `$hPedro`:
```powershell
$idAna = <id del pedido de Ana>
$hPedro = @{ Authorization = "Bearer <token de Pedro>" }   # no mostrarlo en pantalla
"Pedro ve pedido de Ana:      " + (Status GET    "$api/api/orders/$idAna" $hPedro)
"Pedro cancela pedido de Ana: " + (Status DELETE "$api/api/orders/$idAna" $hPedro)
"Cliente cambia estado:       " + (Status PATCH  "$api/api/orders/$idAna/status?status=DESPACHADO" $hPedro)
"Cliente ve reportes:         " + (Status GET    "$api/api/reports/summary" $hPedro)
```
Esperado: `403`, `403`, `403`, `403`. `GET /api/orders` de Pedro no incluye pedidos de Ana.
Con el token de **Operador**: `GET /api/orders` muestra todos y `PATCH .../status?status=EN_PREPARACION` responde `200`; `status=ENTREGADO` desde CREADO responde `409` (transición inválida).

## 7. Reintentos y DLQ (prueba controlada, token de Admin)

```powershell
# a) Error NO recuperable -> DLQ inmediata
$m = @{ exchange="pedidos360.orders.direct"; routingKey="notify.order"
        payload=@{ orderId=999999; customerId="demo"; eventType="ORDER_CREATED"; status="CREADO"; total=1 }
        headers=@{ "x-simulate-failure"="fatal" } } | ConvertTo-Json -Depth 5
Invoke-RestMethod "$api/api/rabbitadmin/messages" -Method Post -Headers $h -ContentType "application/json" -Body $m
# b) Error recuperable -> 3 intentos -> DLQ (llega a report y audit por el topic)
$m2 = @{ exchange="pedidos360.events.topic"; routingKey="order.created"
         payload=@{ orderId=999998; customerId="demo"; eventType="ORDER_CREATED"; status="CREADO"; total=1 }
         headers=@{ "x-simulate-failure"="transient" } } | ConvertTo-Json -Depth 5
Invoke-RestMethod "$api/api/rabbitadmin/messages" -Method Post -Headers $h -ContentType "application/json" -Body $m2
Start-Sleep -Seconds 8
docker logs --since 1m pedidos360-ms-notify | Select-String "DLQ|RETRY"
docker logs --since 1m pedidos360-ms-report | Select-String "DLQ|RETRY"
docker exec pedidos360-rabbitmq1 rabbitmqctl list_queues name messages | Select-String dlq
```
Esperado: `[DLQ] ... error NO recuperable` en notify; `[RETRY] ... intento 1/3`, `2/3` y `[DLQ] ... reintentos agotados (3/3)` en report y audit; las tres `.dlq` con mensajes.
Un JSON sin `orderId`/`eventType` (sin header) también va directo a la DLQ como error no recuperable.

## 8. Microservicio administrador de RabbitMQ (token de Admin)

```powershell
$j = "application/json"
Invoke-RestMethod "$api/api/rabbitadmin/nodes" -Headers $h
Invoke-RestMethod "$api/api/rabbitadmin/exchanges" -Method Post -Headers $h -ContentType $j -Body '{"name":"pruebas.topic","type":"TOPIC"}'
Invoke-RestMethod "$api/api/rabbitadmin/queues" -Method Post -Headers $h -ContentType $j -Body '{"name":"pruebas.demo","withDeadLetterQueue":true}'
Invoke-RestMethod "$api/api/rabbitadmin/bindings" -Method Post -Headers $h -ContentType $j -Body '{"exchange":"pruebas.topic","queue":"pruebas.demo","routingKey":"demo.#"}'
Invoke-RestMethod "$api/api/rabbitadmin/queues/pruebas.demo" -Headers $h
# Validaciones (esperado 400)
"Cola sin nombre:      " + (Status POST "$api/api/rabbitadmin/queues" $h '{"name":""}')
"Exchange amq.*:       " + (Status POST "$api/api/rabbitadmin/exchanges" $h '{"name":"amq.x","type":"TOPIC"}')
"Tipo inválido:        " + (Status POST "$api/api/rabbitadmin/exchanges" $h '{"name":"x.y","type":"OTRO"}')
"Routing key inválida: " + (Status POST "$api/api/rabbitadmin/bindings" $h '{"exchange":"pruebas.topic","queue":"pruebas.demo","routingKey":"a..b"}')
# Recurso protegido (esperado 409)
"Borrar cola base:     " + (Status DELETE "$api/api/rabbitadmin/queues/pedidos360.notify.orders" $h)
# Limpieza
Invoke-RestMethod "$api/api/rabbitadmin/bindings" -Method Delete -Headers $h -ContentType $j -Body '{"exchange":"pruebas.topic","queue":"pruebas.demo","routingKey":"demo.#"}'
Invoke-RestMethod "$api/api/rabbitadmin/queues/pruebas.demo" -Method Delete -Headers $h
Invoke-RestMethod "$api/api/rabbitadmin/exchanges/pruebas.topic" -Method Delete -Headers $h
```
Lo mismo puede hacerse desde la página **Mensajería** del frontend (rol Admin).

## 9. Tolerancia del clúster (opcional)
```powershell
docker stop pedidos360-rabbitmq2
docker exec pedidos360-rabbitmq1 rabbitmqctl cluster_status   # rabbitmq2 aparece como no disponible
docker start pedidos360-rabbitmq2
```

## Errores frecuentes
| Síntoma | Causa probable |
|---|---|
| 401 con `invalid_token ... audience` | El token no es para la API (falta el scope `access_as_user` en el login) o `API_CLIENT_ID` incorrecto |
| 401 con `... issuer` | `AZURE_TENANT_ID` incorrecto |
| 403 en todo | Usuario sin rol y `DEFAULT_ROLE` vacío |
| 403 `Invalid CORS request` | Origen del frontend no está en `CORS_ALLOWED_ORIGINS` |
| 503 al crear pedido | ms-catalog no responde |
| `Network unreachable` hacia login.microsoftonline.com | Falta `-Djava.net.preferIPv4Stack=true` (ya incluido en `JAVA_TOOL_OPTIONS` del compose) |
