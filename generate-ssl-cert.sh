#!/bin/bash
# Script pour générer des certificats SSL auto-signés pour le développement
# Usage: ./generate-ssl-cert.sh [domaine]

DOMAIN=${1:-user-manager.example.com}
OUTPUT_DIR="./nginx/ssl"

# Créer le répertoire de sortie si nécessaire
mkdir -p "$OUTPUT_DIR"

echo "Génération d'un certificat SSL auto-signé pour $DOMAIN"
echo "Les certificats seront stockés dans $OUTPUT_DIR"

# Générer une clé privée et un certificat auto-signé
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$OUTPUT_DIR/user-manager.key" \
  -out "$OUTPUT_DIR/user-manager.crt" \
  -subj "/C=FR/ST=Pays de la Loire/L=Le Cellier/O=User Manager/CN=$DOMAIN" \
  -addext "subjectAltName = DNS:$DOMAIN,DNS:www.$DOMAIN"

if [ $? -eq 0 ]; then
    echo "Certificat SSL généré avec succès!"
    echo "Emplacement de la clé privée: $OUTPUT_DIR/user-manager.key"
    echo "Emplacement du certificat: $OUTPUT_DIR/user-manager.crt"

    # Définir les permissions appropriées
    chmod 600 "$OUTPUT_DIR/user-manager.key"
    chmod 644 "$OUTPUT_DIR/user-manager.crt"

    echo "Note: Il s'agit d'un certificat auto-signé destiné au développement uniquement."
    echo "Pour la production, utilisez un certificat émis par une autorité de certification reconnue."
else
    echo "Erreur lors de la génération du certificat SSL."
    exit 1
fi