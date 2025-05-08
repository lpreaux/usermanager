# User Manager

[![CircleCI](https://dl.circleci.com/status-badge/img/circleci/9S3XviWTB3m5oEDfR2c5gT/SgeB5oGNZF8Np1AhpjjwR6/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/circleci/9S3XviWTB3m5oEDfR2c5gT/SgeB5oGNZF8Np1AhpjjwR6/tree/main)
[![codecov](https://codecov.io/gitlab/lpreaux/usermanager/graph/badge.svg?token=7D5GDS8H1G)](https://codecov.io/gitlab/lpreaux/usermanager)

Application de gestion d'utilisateurs développée avec une architecture hexagonale en Spring Boot.

## Architecture

Ce projet suit les principes de l'architecture hexagonale (Ports & Adapters) :

- **Domain** : Logique métier pure (Entities, Value Objects)
- **Application** : Cas d'utilisation et orchestration
- **Infrastructure** : Adaptateurs (Web, Persistence)

## Technologies

- Java 23
- Spring Boot 3.4
- Spring Data JPA
- MariaDB
- Flyway
- Docker
- JUnit 5
- Mockito

## Couverture de code

La couverture de code est automatiquement calculée et suivie via Codecov.
Voir le [rapport détaillé](https://codecov.io/gh/YOUR_USERNAME/user-manager).

## Démarrage rapide

```bash
# Démarrer l'environnement
./dev-env.sh

# Exécuter les tests avec couverture
./mvnw clean verify

# Voir le rapport de couverture local
open target/site/jacoco/index.html
```

## CI/CD

Le projet utilise CircleCI pour l'intégration continue avec :
- Exécution automatique des tests
- Analyse de la couverture de code
- Upload vers Codecov
- Vérification des seuils de qualité