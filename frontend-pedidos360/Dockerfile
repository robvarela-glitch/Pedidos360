# --- Etapa 1: compilar Angular ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# --- Etapa 2: servir con nginx ---
FROM nginx:1.27-alpine
COPY docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker/config.json.template /usr/share/nginx/config.json.template
COPY docker/40-pedidos360-config.sh /docker-entrypoint.d/40-pedidos360-config.sh
RUN chmod +x /docker-entrypoint.d/40-pedidos360-config.sh
COPY --from=build /app/dist/frontend-pedidos360/browser /usr/share/nginx/html

# Valores por defecto (docker-compose los reemplaza)
ENV API_BASE_URL="" \
    BFF_UPSTREAM="http://ms-bff:8080" \
    AZURE_TENANT_ID="" \
    AZURE_CLIENT_ID="" \
    API_SCOPE=""
EXPOSE 80
