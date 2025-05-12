#!/bin/bash

# Créer le répertoire des logs s'il n'existe pas
mkdir -p logs

# Vérifier si le réseau existe déjà
NETWORK_EXISTS=$(docker network ls | grep user-manager-network | wc -l)
if [ "$NETWORK_EXISTS" -eq "0" ]; then
    echo "Creating user-manager-network..."
    docker network create user-manager-network
fi

# Démarrer la stack de monitoring
echo "Starting monitoring stack..."
# Notez le changement de chemin ici
cd monitoring && docker-compose up -d

echo "Monitoring stack started!"
echo "Prometheus UI: http://localhost:9090"
echo "Grafana UI: http://localhost:3000 (admin/admin)"
echo "Loki API: http://localhost:3100"

echo "La stack de monitoring est prête!"