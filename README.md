# Backend Pedidos360 

Microservicios: `orders`, `catalog`, `notify`, `report`, `audit`.
Infraestructura: Oracle, RabbitMQ, Kafka + Zookeeper, Docker Compose, EC2.

## 1. Arquitectura y flujo de eventos

```
                 (JWT del BFF de Benjamín)
Angular (Roberto) -> BFF (Benjamín) -> ms-pedidos360-orders (8081)
                                            |         \
                                    RabbitMQ           Kafka
                                 orders.notifications   orders.events
                                       |                  /        \
                              ms-pedidos360-notify   ms-report   ms-audit
                                    (8083)             (8084)      (8085)

ms-pedidos360-catalog (8082) -> API REST de productos, independiente,
                                 también protegida con el mismo JWT.
```

Cada microservicio valida el JWT como *Resource Server* (igual que la guía
de la Semana 04, Sesión 3): revisa `issuer` (tenant de Azure AD) y
`audience` (que el token venga dirigido a esa API en particular).

## 2. Requisitos en tu computador

- **Java 17**
- **Maven 3.9+** (tu compañero ya avisó que hace falta — `mvn -v` para
  comprobar que está instalado)
- **Docker Desktop** (o Docker Engine + Docker Compose) para la
  infraestructura (Oracle, RabbitMQ, Kafka)

## 3. Levantar todo con Docker Compose (recomendado)

```bash
cd Pedidos360-main/infra
# En PowerShell: Copy-Item .env.example .env
# En bash:       cp .env.example .env
# Completa AZURE_TENANT_ID cuando Daniel lo tenga.
docker compose up --build     # levanta infra + los 5 microservicios
```

Esto construye las imágenes de cada microservicio (usando su `Dockerfile`)
y levanta:

| Servicio         | Puerto  |
|------------------|---------|
| Oracle           | 1521    |
| RabbitMQ (AMQP)  | 5672    |
| RabbitMQ (panel) | 15672   |
| Kafka            | 9092    |
| ms-orders        | 8081    |
| ms-catalog       | 8082    |
| ms-notify        | 8083    |
| ms-report        | 8084    |
| ms-audit         | 8085    |

Para apagar todo: `docker compose down` (agrega `-v` si además quieres
borrar los datos de Oracle).

## 4. Levantar SOLO la infraestructura y correr los microservicios con Maven

Útil mientras programas, para ver logs y recompilar rápido sin rearmar
imágenes Docker cada vez.

```bash
# 1) Solo infraestructura (Oracle, RabbitMQ, Kafka+Zookeeper)
cd Pedidos360-main/infra
docker compose up oracle-db rabbitmq zookeeper kafka

# 2) En otra terminal, por cada microservicio:
cd Pedidos360-main/ms-pedidos360-orders
mvn spring-boot:run

cd Pedidos360-main/ms-pedidos360-catalog
mvn spring-boot:run

cd Pedidos360-main/ms-pedidos360-notify
mvn spring-boot:run

cd Pedidos360-main/ms-pedidos360-report
mvn spring-boot:run

cd Pedidos360-main/ms-pedidos360-audit
mvn spring-boot:run
```

Cuando corres con `mvn spring-boot:run` (fuera de Docker), los
`application.properties` usan `localhost` por defecto para Oracle,
RabbitMQ y Kafka, así que basta con tener la infraestructura arriba.

## 5. Probar rápido que cada servicio responde

```bash
curl http://localhost:8081/api/orders/publico/ping
curl http://localhost:8082/api/catalog/publico/ping
curl http://localhost:8083/api/notify/publico/ping
curl http://localhost:8084/api/reports/publico/ping
curl http://localhost:8085/api/audit/publico/ping
```

Estos endpoints `/publico/*` no piden token (igual que `/api/publico` en la
guía de la Sesión 3), sirven para confirmar que el servicio levantó bien
antes de meter la autenticación en el juego.

Para probar un endpoint protegido necesitas un Access Token válido
(lo entrega Daniel/Angular vía MSAL) y mandarlo así:

```bash
curl http://localhost:8081/api/orders \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## 6. Compilar sin Docker (solo para verificar que el código compila)

```bash
cd Pedidos360-main/ms-pedidos360-orders
mvn clean package -DskipTests
```

Repetir por cada microservicio. El .jar queda en `target/`.

## 7. EC2 / infraestructura (parte de Agustín)

Para la presentación final, la forma más simple es desplegar todo el
`docker-compose.yml` dentro de una sola instancia EC2:

1. Levantar una instancia EC2 (Ubuntu 22.04, tipo `t3.medium` o superior
   — Oracle + Kafka + Zookeeper consumen bastante RAM).
2. Instalar Docker y el plugin de Docker Compose:
   ```bash
   sudo apt update && sudo apt install -y docker.io docker-compose-plugin
   sudo usermod -aG docker $USER
   ```
3. Copiar la carpeta `Pedidos360-main` al servidor (`scp` o `git clone`
   del repo).
4. Abrir en el Security Group de la instancia los puertos que necesites
   exponer hacia afuera (por ejemplo 8081-8085 para las APIs, o solo el
   puerto del API Manager/BFF si el resto queda interno).
5. `cd Pedidos360-main/infra && docker compose up --build -d`

Si más adelante quieren separar el backend en varias instancias (una
para Oracle, otra para el broker de mensajería, otra para los
microservicios), la idea es la misma pero cambiando los `DB_HOST`,
`RABBITMQ_HOST` y `KAFKA_BOOTSTRAP` de cada microservicio por la IP/DNS
interno de la instancia correspondiente en vez de los nombres de
servicio de docker-compose.

## 8. Pendiente / próximos pasos

- Cuando Daniel tenga el tenant de Azure AD listo, reemplazar
  `AZURE_TENANT_ID` y los `pedidos360.security.audience` de cada
  `application.properties` con los valores reales (o dejarlos por
  variable de entorno, como ya están).
- Revisar con Benjamín el formato exacto de los roles/claims que el BFF
  reenvía en el JWT, para ajustar `JwtGrantedAuthoritiesConverter` si
  hace falta.
- Agregar reglas de autorización por rol en cada `SecurityConfig`
  (por ahora solo exige "autenticado", no valida roles específicos).

## 9. Verificación de Kafka

Kafka usa dos listeners: `kafka:29092` dentro de Docker y `localhost:9092`
desde tu computador. Luego de iniciar el entorno, verifica que los eventos
estén llegando con:

```bash
docker compose logs -f ms-orders ms-report ms-audit
```

Al crear o actualizar un pedido, `ms-orders` publica el evento `orders.events`;
`ms-report` y `ms-audit` deben registrarlo en sus logs y en Oracle.
