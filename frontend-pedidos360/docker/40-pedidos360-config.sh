#!/bin/sh
# Genera /config.json con las variables de entorno del contenedor (sin recompilar Angular).
set -e
envsubst '${API_BASE_URL} ${AZURE_TENANT_ID} ${AZURE_CLIENT_ID} ${API_SCOPE}' \
  < /usr/share/nginx/config.json.template > /usr/share/nginx/html/config.json
echo "Pedidos360: config.json generado (apiBaseUrl='${API_BASE_URL}')"
