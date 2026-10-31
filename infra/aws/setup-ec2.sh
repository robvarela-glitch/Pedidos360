#!/usr/bin/env bash
# =========================================================================
# Pedidos360 - Preparar una instancia EC2 y levantar todo con docker-compose.
#
# Requisitos de la instancia (AWS Academy / Learner Lab):
#   - AMI: Amazon Linux 2023 o Ubuntu 22.04/24.04
#   - Tipo: t3.large (8 GB) recomendado. Con t3.medium (4 GB) el script crea swap de 4 GB.
#   - Disco: 30 GB
#   - Security Group (entrada): 22 (SSH, solo su IP), 80 (frontend) y 8080 (BFF / API Gateway).
#     NO abrir 1522 (Oracle), 5672/15672 (RabbitMQ) ni 8081-8086: quedan solo en 127.0.0.1.
#
# Uso (dentro de la instancia, con el proyecto ya copiado o clonado):
#   cd Pedidos360-main/infra/aws && bash setup-ec2.sh
# =========================================================================
set -euo pipefail

INFRA_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo ">> Instalando Docker y Docker Compose..."
if command -v dnf >/dev/null 2>&1; then
  sudo dnf install -y docker git
  sudo mkdir -p /usr/local/lib/docker/cli-plugins
  if ! docker compose version >/dev/null 2>&1; then
    sudo curl -SL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-$(uname -m)" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
  fi
  sudo systemctl enable --now docker
else
  sudo apt-get update -y
  sudo apt-get install -y docker.io docker-compose-v2 git || sudo apt-get install -y docker.io docker-compose-plugin git
  sudo systemctl enable --now docker
fi
sudo usermod -aG docker "$USER" || true

echo ">> Configurando swap (4 GB) si la instancia tiene poca memoria..."
if [ "$(free -m | awk '/Mem:/ {print $2}')" -lt 7000 ] && ! swapon --show | grep -q swapfile; then
  sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

cd "$INFRA_DIR"
if [ ! -f .env ]; then
  cp .env.example .env
  echo ""
  echo "!! Se creó infra/.env desde .env.example. Edítelo (AZURE_TENANT_ID, API_CLIENT_ID, claves)"
  echo "!! y vuelva a ejecutar este script:  nano $INFRA_DIR/.env"
  exit 0
fi

echo ">> Construyendo imágenes de a una (evita quedarse sin memoria)..."
export COMPOSE_PARALLEL_LIMIT=1
sudo docker compose build

echo ">> Levantando Pedidos360..."
sudo docker compose up -d
sudo docker compose ps

PUBLIC_HOST=$(curl -s --max-time 2 http://169.254.169.254/latest/meta-data/public-hostname || true)
echo ""
echo "Listo. Comprobaciones:"
echo "  curl http://localhost:8080/actuator/health"
echo "  sudo docker exec pedidos360-rabbitmq1 rabbitmqctl cluster_status"
echo "Frontend: http://${PUBLIC_HOST:-<DNS-publico-EC2>}/   BFF: http://${PUBLIC_HOST:-<DNS-publico-EC2>}:8080/"
echo "Siguiente paso: crear el API Gateway con infra/aws/create-api-gateway.sh"
