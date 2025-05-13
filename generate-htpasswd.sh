#!/bin/bash
# Script pour générer le fichier .htpasswd pour l'authentification NGINX
# Usage: ./generate-htpasswd.sh <username>

if [ $# -ne 1 ]; then
    echo "Usage: $0 <username>"
    exit 1
fi

USERNAME=$1
OUTPUT_FILE=".htpasswd"

# Vérifier que le package htpasswd est installé
if ! command -v htpasswd &> /dev/null; then
    echo "htpasswd n'est pas installé. Installation en cours..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y apache2-utils
    elif command -v yum &> /dev/null; then
        sudo yum install -y httpd-tools
    elif command -v apk &> /dev/null; then
        apk add --no-cache apache2-utils
    else
        echo "Impossible d'installer htpasswd. Veuillez l'installer manuellement."
        exit 1
    fi
fi

# Générer un mot de passe aléatoire sécurisé
PASSWORD=$(openssl rand -base64 12)

# Créer ou mettre à jour le fichier .htpasswd
htpasswd -bc "$OUTPUT_FILE" "$USERNAME" "$PASSWORD"

if [ $? -eq 0 ]; then
    echo "Fichier .htpasswd créé avec succès."
    echo "Nom d'utilisateur: $USERNAME"
    echo "Mot de passe: $PASSWORD"
    echo ""
    echo "N'oubliez pas de déplacer ce fichier vers ./nginx/conf/.htpasswd avant le déploiement."

    # Créer le répertoire si nécessaire
    mkdir -p "./nginx/conf"

    # Copier le fichier
    cp "$OUTPUT_FILE" "./nginx/conf/.htpasswd"
    echo "Fichier copié dans ./nginx/conf/.htpasswd"
else
    echo "Erreur lors de la création du fichier .htpasswd"
    exit 1
fi