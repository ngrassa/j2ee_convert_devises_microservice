#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   INSTALL_PREREQS=1 ./setup.sh         # installe dépendances (Ubuntu), puis build + run
#   ./setup.sh                           # build+run complet
#   SERVICES="frontend" ./setup.sh       # rebuild + restart seulement ces services
#   SERVICES="frontend converter-service" ./setup.sh

install_prereqs() {
  if ! command -v apt-get >/dev/null 2>&1; then
    echo "Installation auto seulement pour Ubuntu/Debian (apt-get)."
    return
  fi

  echo "Mise à jour des paquets..."
  sudo apt-get update -y

  echo "Installation de Docker, plugin Compose, Java 21 et Maven..."
  sudo apt-get install -y docker.io docker-compose-plugin openjdk-21-jdk maven

  echo "Activation de Docker..."
  sudo systemctl enable docker
  sudo systemctl start docker

  if id -nG "$USER" | grep -qw docker; then
    echo "Utilisateur déjà dans le groupe docker."
  else
    echo "Ajout de $USER au groupe docker (reconnexion requise)..."
    sudo usermod -aG docker "$USER"
  fi
}

if [[ "${INSTALL_PREREQS:-0}" == "1" ]]; then
  install_prereqs
fi

TARGETS="${SERVICES:-}"

if [[ -n "$TARGETS" ]]; then
  echo "Partial refresh for services: $TARGETS"
  docker compose rm -sf $TARGETS || true
  echo "Building targets..."
  docker compose build --no-cache --pull $TARGETS
  echo "Starting targets..."
  docker compose up -d $TARGETS
else
  echo "Stopping and removing previous containers..."
  docker compose down --remove-orphans || true
  echo "Building all services from scratch..."
  docker compose build --no-cache --pull
  echo "Starting stack..."
  docker compose up -d
fi

docker compose ps

echo "Accès :"
echo "- Frontend : http://localhost:9080"
echo "- Converter API : http://localhost:9082/api/convert?amount=100&to=EUR"
echo "- Rates API : http://localhost:9081/api/rates"
