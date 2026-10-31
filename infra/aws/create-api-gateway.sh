#!/usr/bin/env bash
# =========================================================================
# Pedidos360 - API Manager: AWS API Gateway (HTTP API)
#
# Crea:
#   - Un HTTP API "pedidos360-api" con CORS configurado.
#   - Un autorizador JWT de Microsoft Entra ID (valida firma, issuer, audience y expiración).
#   - Rutas EXPLÍCITAS (método + ruta) hacia el BFF en EC2, protegidas con el autorizador
#     y el scope access_as_user. Rutas públicas: solo health/ping y el frontend.
#   - Integración del frontend (GET / y GET /{proxy+}) para servirlo por HTTPS desde el mismo dominio.
#
# Requisitos: AWS CLI v2 con credenciales (Learner Lab: "AWS Details" -> credenciales, o
# ejecutarlo dentro de una EC2 con LabInstanceProfile). Región por defecto us-east-1.
#
# Uso:
#   export EC2_HOST=ec2-xx-xx-xx-xx.compute-1.amazonaws.com
#   export AZURE_TENANT_ID=<tenant>
#   export API_CLIENT_ID=<client-id>
#   export TOKEN_VERSION=1     # 1 o 2: ver "tokenVersion" en GET /api/me (local) antes de ejecutar
#   bash create-api-gateway.sh
# =========================================================================
set -euo pipefail

: "${EC2_HOST:?Defina EC2_HOST (DNS o IP pública de la instancia EC2)}"
: "${AZURE_TENANT_ID:?Defina AZURE_TENANT_ID}"
: "${API_CLIENT_ID:?Defina API_CLIENT_ID}"
TOKEN_VERSION="${TOKEN_VERSION:-1}"
REGION="${AWS_REGION:-us-east-1}"
API_NAME="${API_NAME:-pedidos360-api}"
BFF_PORT="${BFF_PORT:-8080}"
EXTRA_ORIGINS="${EXTRA_ORIGINS:-http://localhost:4200}"

if [ "$TOKEN_VERSION" = "2" ]; then
  ISSUER="https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0"
else
  ISSUER="https://sts.windows.net/${AZURE_TENANT_ID}/"
fi

aws_() { aws --region "$REGION" "$@"; }

echo ">> Creando HTTP API ${API_NAME}..."
API_ID=$(aws_ apigatewayv2 create-api \
  --name "$API_NAME" \
  --protocol-type HTTP \
  --description "Pedidos360 - API Manager (Entra ID JWT)" \
  --query ApiId --output text)
API_URL="https://${API_ID}.execute-api.${REGION}.amazonaws.com"

echo ">> Configurando CORS (orígenes: ${API_URL}, ${EXTRA_ORIGINS})..."
ORIGINS_JSON=$(printf '"%s",' "$API_URL" ${EXTRA_ORIGINS//,/ } | sed 's/,$//')
aws_ apigatewayv2 update-api --api-id "$API_ID" --cors-configuration \
  "{\"AllowOrigins\":[${ORIGINS_JSON}],\"AllowMethods\":[\"GET\",\"POST\",\"PUT\",\"PATCH\",\"DELETE\",\"OPTIONS\"],\"AllowHeaders\":[\"authorization\",\"content-type\",\"accept\"],\"ExposeHeaders\":[\"www-authenticate\"],\"MaxAge\":3600}" >/dev/null

echo ">> Creando autorizador JWT (issuer ${ISSUER})..."
AUTH_ID=$(aws_ apigatewayv2 create-authorizer \
  --api-id "$API_ID" \
  --name entra-id-jwt \
  --authorizer-type JWT \
  --identity-source '$request.header.Authorization' \
  --jwt-configuration "{\"Audience\":[\"api://${API_CLIENT_ID}\",\"${API_CLIENT_ID}\"],\"Issuer\":\"${ISSUER}\"}" \
  --query AuthorizerId --output text)

echo ">> Creando integraciones (BFF y frontend en EC2)..."
BFF_INTEGRATION=$(aws_ apigatewayv2 create-integration \
  --api-id "$API_ID" \
  --integration-type HTTP_PROXY \
  --integration-method ANY \
  --integration-uri "http://${EC2_HOST}:${BFF_PORT}" \
  --payload-format-version 1.0 \
  --request-parameters '{"overwrite:path":"$request.path"}' \
  --query IntegrationId --output text)

FRONTEND_INTEGRATION=$(aws_ apigatewayv2 create-integration \
  --api-id "$API_ID" \
  --integration-type HTTP_PROXY \
  --integration-method GET \
  --integration-uri "http://${EC2_HOST}:80" \
  --payload-format-version 1.0 \
  --request-parameters '{"overwrite:path":"$request.path"}' \
  --query IntegrationId --output text)

protected_route() {
  aws_ apigatewayv2 create-route --api-id "$API_ID" --route-key "$1" \
    --target "integrations/${BFF_INTEGRATION}" \
    --authorization-type JWT --authorizer-id "$AUTH_ID" \
    --authorization-scopes access_as_user >/dev/null
  echo "   [JWT]     $1"
}
public_route() {
  aws_ apigatewayv2 create-route --api-id "$API_ID" --route-key "$1" \
    --target "integrations/$2" >/dev/null
  echo "   [público] $1"
}

echo ">> Creando rutas..."
# Identidad
protected_route "GET /api/me"
# Catálogo
protected_route "GET /api/catalog/products"
protected_route "GET /api/catalog/products/{id}"
protected_route "POST /api/catalog/products"
protected_route "PUT /api/catalog/products/{id}"
protected_route "DELETE /api/catalog/products/{id}"
# Pedidos
protected_route "GET /api/orders"
protected_route "POST /api/orders"
protected_route "GET /api/orders/{id}"
protected_route "PATCH /api/orders/{id}/status"
protected_route "DELETE /api/orders/{id}"
# Notificaciones
protected_route "GET /api/notify/notifications"
protected_route "GET /api/notify/notifications/pedido/{orderId}"
# Reportes y auditoría
protected_route "GET /api/reports/summary"
protected_route "GET /api/reports/entries"
protected_route "GET /api/audit/logs"
protected_route "GET /api/audit/logs/pedido/{orderId}"
# Administración RabbitMQ
protected_route "GET /api/rabbitadmin/queues"
protected_route "GET /api/rabbitadmin/queues/{name}"
protected_route "POST /api/rabbitadmin/queues"
protected_route "DELETE /api/rabbitadmin/queues/{name}"
protected_route "DELETE /api/rabbitadmin/queues/{name}/messages"
protected_route "GET /api/rabbitadmin/exchanges"
protected_route "POST /api/rabbitadmin/exchanges"
protected_route "DELETE /api/rabbitadmin/exchanges/{name}"
protected_route "GET /api/rabbitadmin/bindings"
protected_route "POST /api/rabbitadmin/bindings"
protected_route "DELETE /api/rabbitadmin/bindings"
protected_route "GET /api/rabbitadmin/nodes"
protected_route "POST /api/rabbitadmin/messages"
# Cualquier otra ruta /api también exige JWT en el API Gateway (el BFF la rechaza con 403)
protected_route "ANY /api/{proxy+}"
# Públicos por diseño
public_route "GET /api/{service}/publico/ping" "$BFF_INTEGRATION"
public_route "GET /actuator/health" "$BFF_INTEGRATION"
public_route "GET /" "$FRONTEND_INTEGRATION"
public_route "GET /{proxy+}" "$FRONTEND_INTEGRATION"

echo ">> Publicando stage \$default (auto-deploy)..."
aws_ apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null

cat <<MSG

API Gateway creado.
  API ID : ${API_ID}
  URL    : ${API_URL}

Siguientes pasos:
  1) En Entra ID (App Registration > Authentication > Single-page application) agregar
     el Redirect URI:  ${API_URL}
  2) En infra/.env de la EC2 (el BFF debe aceptar el origen del API Gateway):
       CORS_ALLOWED_ORIGINS=${API_URL},http://localhost:4200,http://localhost
     y aplicar:  sudo docker compose up -d ms-bff
     (FRONTEND_API_BASE_URL puede quedar vacío: el frontend se abre desde ${API_URL}
      y llama a ${API_URL}/api/... a través del API Gateway)
  3) Probar:
       curl -i ${API_URL}/api/orders                 # 401 (sin token, lo rechaza el API Gateway)
       curl -i ${API_URL}/api/orders/publico/ping    # 200 (público)
       Abrir ${API_URL}/ en el navegador e iniciar sesión.
MSG
