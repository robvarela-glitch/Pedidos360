# Despliegue en AWS (EC2 + docker-compose + API Gateway)

```
Internet ──HTTPS──> API Gateway (HTTP API "pedidos360-api")
                      ├─ autorizador JWT (Entra ID): issuer + audience + scope access_as_user
                      ├─ /api/... rutas explícitas ──> EC2:8080 (BFF)
                      └─ / y /{proxy+} (público) ───> EC2:80 (frontend nginx)
EC2 (docker-compose): Oracle · RabbitMQ x2 (clúster) · 7 microservicios · frontend
```

Oracle, RabbitMQ y los microservicios internos quedan publicados solo en `127.0.0.1` de la instancia: no son accesibles desde Internet.

## 1. Crear la instancia EC2 (AWS Academy / Learner Lab)
1. EC2 → Launch instance:
   - AMI **Amazon Linux 2023** (o Ubuntu 22.04/24.04).
   - Tipo **t3.large** (8 GB). Con t3.medium funciona más lento (el script agrega swap).
   - Key pair: `vockey` (o uno propio). Disco 30 GB.
   - IAM instance profile: **LabInstanceProfile** (permite usar la AWS CLI desde la instancia).
2. Security Group — reglas de entrada:

| Puerto | Origen | Motivo |
|---|---|---|
| 22 | Mi IP | SSH |
| 80 | 0.0.0.0/0 | Frontend (lo consume el API Gateway) |
| 8080 | 0.0.0.0/0 | BFF (lo consume el API Gateway; igual valida JWT) |

**No** abrir 1522, 5672, 15672, 15673 ni 8081-8086.

## 2. Copiar el proyecto y levantarlo
```powershell
# Desde Windows (PowerShell), en la carpeta que contiene Pedidos360-main:
scp -i labsuser.pem -r Pedidos360-main ec2-user@<DNS-publico-EC2>:~/
ssh -i labsuser.pem ec2-user@<DNS-publico-EC2>
```
(En Ubuntu el usuario es `ubuntu`. También se puede `git clone` del repositorio.)

En la instancia:
```bash
cd ~/Pedidos360-main/infra/aws
bash setup-ec2.sh          # instala Docker y crea infra/.env la primera vez
nano ../.env               # AZURE_TENANT_ID, API_CLIENT_ID y claves propias
bash setup-ec2.sh          # construye y levanta todo (10-20 min la primera vez)
curl http://localhost:8080/actuator/health
sudo docker exec pedidos360-rabbitmq1 rabbitmqctl cluster_status
```

## 3. Crear el API Gateway (API Manager)
Desde la misma instancia (usa LabInstanceProfile) o desde un PC con las credenciales del Learner Lab:
```bash
cd ~/Pedidos360-main/infra/aws
export EC2_HOST=$(curl -s http://169.254.169.254/latest/meta-data/public-hostname)
export AZURE_TENANT_ID=<tenant>
export API_CLIENT_ID=<client-id>
export TOKEN_VERSION=1        # ver docs/ENTRA-ID.md, sección 5
bash create-api-gateway.sh
```
> Si la instancia usa IMDSv2 y el `curl` anterior no devuelve nada, copie el DNS público desde la consola de EC2.

El script crea: HTTP API, CORS, autorizador JWT `entra-id-jwt`, integraciones al BFF y al frontend, ~30 rutas explícitas con JWT + scope `access_as_user`, rutas públicas (ping, health, frontend) y el stage `$default`. Al final imprime la URL `https://<api-id>.execute-api.us-east-1.amazonaws.com`.

Después:
1. Entra ID → Authentication → SPA → agregar la URL del API Gateway como Redirect URI.
2. En la EC2: `nano ~/Pedidos360-main/infra/.env` →
   `CORS_ALLOWED_ORIGINS=https://<api-id>.execute-api.us-east-1.amazonaws.com,http://localhost:4200,http://localhost`
   y `cd ~/Pedidos360-main/infra && sudo docker compose up -d ms-bff`.
3. Abrir `https://<api-id>.execute-api.us-east-1.amazonaws.com/` e iniciar sesión.

### Si el Learner Lab no permite crear el API Gateway por CLI
Hacerlo en la consola (API Gateway → Create API → **HTTP API**) con los mismos datos:
- Integración HTTP `http://<EC2>:8080` y otra `http://<EC2>:80`, ambas con *Parameter mapping* → `overwrite:path` = `$request.path`.
- Authorizer **JWT**: Identity source `$request.header.Authorization`, Issuer (según la versión del token) y Audience `api://<client-id>` y `<client-id>`.
- Rutas: copiar la lista de `create-api-gateway.sh` (cada una con el authorizer y el scope `access_as_user`).
- CORS: orígenes `https://<api-id>...` y `http://localhost:4200`; métodos GET, POST, PUT, PATCH, DELETE, OPTIONS; headers `authorization, content-type`.
Si la cuenta no tiene permiso para API Gateway, registrar el mensaje exacto de error (captura) e informarlo al docente: el BFF sigue validando JWT y rutas explícitas como respaldo.

## 4. Verificación en la nube
```bash
API=https://<api-id>.execute-api.us-east-1.amazonaws.com
curl -i $API/api/orders/publico/ping             # 200 (público)
curl -i $API/api/orders                          # 401 (el autorizador rechaza: sin token)
curl -i -H "Authorization: Bearer abc.def.ghi" $API/api/orders   # 401 (token inválido)
```
Con un token real ver [PRUEBAS.md](PRUEBAS.md).

## 5. Apagar / costos
- `sudo docker compose stop` detiene los contenedores sin borrar datos.
- No usar `docker compose down -v` (borra la base de datos Oracle).
- Detener la instancia EC2 al terminar la sesión del Learner Lab (la IP/DNS pública cambia al reiniciarla: actualizar `EC2_HOST` en las integraciones del API Gateway).
