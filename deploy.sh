#!/bin/bash
# Script de déploiement pour User Manager avec approche feature flag
# Usage: ./deploy.sh [dev|prod]

# Configuration par défaut
ENV=${1:-dev}  # Si aucun argument n'est fourni, utiliser 'dev'

# Validation de l'environnement
if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
    echo "Environnement non reconnu: $ENV"
    echo "Usage: $0 [dev|prod]"
    exit 1
fi

echo "==================================================="
echo "Déploiement de User Manager en environnement: $ENV"
echo "==================================================="

# Configuration spécifique à l'environnement
case $ENV in
    dev)
        ENV_FILE=".env.dev"
        # En dev, on utilise docker-compose.yml et docker-compose.override.yml (implicitement)
        COMPOSE_CMD="docker compose"
        # Variables pour le développement
        export NETWORK_NAME="user-manager-network-dev"
        export VOLUME_PREFIX="dev-"
        ;;
    prod)
        ENV_FILE=".env.prod"
        # En prod, on utilise explicitement docker-compose.yml et docker-compose.prod.yml
        COMPOSE_CMD="docker compose -f docker-compose.yml -f docker-compose.prod.yml"
        # Variables pour la production
        export NETWORK_NAME="user-manager-network-prod"
        export VOLUME_PREFIX="prod-"
        ;;
esac

# Vérification des fichiers nécessaires
if [ ! -f "$ENV_FILE" ]; then
    echo "Erreur: $ENV_FILE n'existe pas"
    exit 1
fi

# Copier le fichier d'environnement approprié vers .env
echo "Utilisation du fichier d'environnement: $ENV_FILE"
cp "$ENV_FILE" .env

# Vérifier si les variables contiennent des caractères $ qui pourraient causer des problèmes
if grep -q '\$' .env; then
    echo "Attention: Le fichier .env contient des caractères $ qui peuvent causer des problèmes."
    echo "Veuillez remplacer ces caractères par d'autres caractères spéciaux ou les échapper."
    echo "Lignes concernées:"
    grep '\$' .env

    # Option pour continuer malgré l'avertissement
    read -p "Voulez-vous continuer quand même? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Déploiement annulé."
        exit 1
    fi
fi

# Chargement des variables d'environnement
echo "Chargement des variables d'environnement"
set -a
source .env
set +a

# Export de la variable d'environnement pour Spring Boot
export SPRING_PROFILES_ACTIVE=$ENV

# En production, vérifier que les répertoires de logs et SSL existent
if [ "$ENV" = "prod" ]; then
    # Création des répertoires nécessaires
    echo "Création des répertoires nécessaires..."
    mkdir -p ./nginx/logs ./nginx/ssl ./backups/prod ./logs

    # Vérifier l'existence des certificats SSL
    if [ ! -f "./nginx/ssl/user-manager.crt" ] || [ ! -f "./nginx/ssl/user-manager.key" ]; then
        echo "Attention: Les certificats SSL ne sont pas présents dans './nginx/ssl/'"

        # Option pour générer des certificats auto-signés
        read -p "Voulez-vous générer des certificats auto-signés pour le test? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            if [ -f "./generate-ssl-cert.sh" ]; then
                echo "Génération de certificats auto-signés..."
                ./generate-ssl-cert.sh
            else
                echo "Le script de génération de certificats n'a pas été trouvé."
                echo "Veuillez créer manuellement les certificats SSL avant de continuer."
                exit 1
            fi
        else
            echo "Veuillez fournir des certificats SSL avant de continuer."
            exit 1
        fi
    fi

    # Vérifier l'existence du fichier .htpasswd pour l'authentification NGINX
    if [ ! -f "./nginx/conf/.htpasswd" ]; then
        echo "Attention: Le fichier d'authentification .htpasswd n'est pas présent."

        # Option pour générer le fichier .htpasswd
        read -p "Voulez-vous générer un fichier .htpasswd? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            if [ -f "./generate-htpasswd.sh" ]; then
                echo "Génération du fichier .htpasswd..."
                ./generate-htpasswd.sh admin
            else
                echo "Le script de génération de .htpasswd n'a pas été trouvé."
                echo "Veuillez créer manuellement le fichier .htpasswd avant de continuer."
                exit 1
            fi
        fi
    fi

    # Démarrer le monitoring en production
    if [ -f "./start-monitoring.sh" ]; then
        echo "Démarrage de la stack de monitoring..."
        ./start-monitoring.sh
    else
        echo "Attention: Le script de monitoring n'a pas été trouvé."
    fi
fi

# Arrêt des conteneurs existants (si besoin)
echo "Arrêt des conteneurs existants..."
$COMPOSE_CMD down

# Build et démarrage des conteneurs
echo "Construction et démarrage des conteneurs..."
$COMPOSE_CMD up -d --build

# Vérification du déploiement
echo "Vérification du déploiement..."
sleep 10  # Attente plus longue pour laisser le temps aux conteneurs de démarrer

# Vérification de l'état de l'application
if [ "$ENV" = "dev" ]; then
    # En dev, un seul conteneur app
    APP_CONTAINER=$($COMPOSE_CMD ps -q app)

    if [ -z "$APP_CONTAINER" ]; then
        echo "Erreur: Le conteneur de l'application n'a pas démarré"
        echo "Affichage des logs Docker Compose:"
        $COMPOSE_CMD logs
        exit 1
    fi

    APP_STATUS=$(docker inspect --format='{{.State.Status}}' $APP_CONTAINER)
    if [ "$APP_STATUS" != "running" ]; then
        echo "Erreur: Le conteneur de l'application n'est pas en cours d'exécution"
        docker logs $APP_CONTAINER
        exit 1
    fi

    # Affichage des logs pour vérification
    echo "Affichage des derniers logs de l'application:"
    docker logs --tail 10 $APP_CONTAINER
else
    # En prod, il peut y avoir plusieurs conteneurs (replicas)
    APP_CONTAINERS=$($COMPOSE_CMD ps -q app)

    if [ -z "$APP_CONTAINERS" ]; then
        echo "Erreur: Aucun conteneur d'application n'a démarré"
        echo "Affichage des logs Docker Compose:"
        $COMPOSE_CMD logs
        exit 1
    fi

    echo "Vérification des conteneurs d'application (replicas):"
    for container in $APP_CONTAINERS; do
        APP_STATUS=$(docker inspect --format='{{.State.Status}}' $container)
        echo " - Conteneur $container: $APP_STATUS"

        if [ "$APP_STATUS" != "running" ]; then
            echo "Erreur: Un conteneur d'application n'est pas en cours d'exécution"
            docker logs $container
            exit 1
        fi
    done

    # Affichage des logs pour la vérification
    echo "Affichage des derniers logs d'un des conteneurs d'application:"
    FIRST_CONTAINER=$(echo $APP_CONTAINERS | cut -d ' ' -f1)
    docker logs --tail 10 $FIRST_CONTAINER
fi

echo "==================================================="
echo "Déploiement de l'environnement $ENV terminé avec succès!"
echo "==================================================="

# Affichage des informations d'accès
case $ENV in
    dev)
        echo "Application accessible à:"
        echo "  - API: http://localhost:8080"
        echo "  - Swagger UI: http://localhost:8080/swagger-ui/index.html"
        echo "  - Actuator: http://localhost:8080/actuator"
        echo "  - Adminer: http://localhost:8081"
        echo ""
        echo "Debugging:"
        echo "  - Port de débogage Java: 8000"
        echo "  - LiveReload: 35729"
        ;;
    prod)
        echo "Application accessible à:"
        echo "  - API: https://user-manager.example.com"
        echo "  - Administration: https://user-manager.example.com/management (accès restreint)"
        echo ""
        echo "Monitoring:"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Grafana: http://localhost:3000 (admin/admin)"
        echo "  - Loki: http://localhost:3100"
        ;;
esac

echo "==================================================="
echo "Pour arrêter l'application:"
echo "  $COMPOSE_CMD down"
echo ""
echo "Pour voir les logs en temps réel:"
echo "  $COMPOSE_CMD logs -f"
echo "==================================================="

# En développement, afficher les informations sur les feature flags
if [ "$ENV" = "dev" ]; then
    echo "Feature Flags activés en développement:"
    echo "  - new-user-interface: ${FEATURE_NEW_USER_INTERFACE:-true}"
    echo "  - enhanced-search: ${FEATURE_ENHANCED_SEARCH:-true}"
    echo "  - beta-features: ${FEATURE_BETA_FEATURES:-true}"
    echo ""
    echo "Pour désactiver un feature flag en développement, modifiez .env.dev"
fi