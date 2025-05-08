# Plan d'amélioration de la couverture de code

## État actuel

| Package/Classe | Couverture actuelle | Objectif | Priorité |
|----------------|---------------------|----------|----------|
| DTOs/Commands | 0-40% | 60% | Basse |
| Value Objects | 66-79% | 90% | Haute |
| Domain Model | 66% | 85% | Haute |
| Services | 54% | 80% | Haute |
| Controllers | 60-64% | 75% | Moyenne |
| Exception Handler | 35% | 60% | Moyenne |

## Actions immédiates (Sprint 1)

1. **Corriger l'exécution des tests unitaires dans CircleCI** ✅
2. **Ajouter les tests manquants pour les Value Objects**
    - [x] PasswordTest
    - [x] PhoneNumberTest
    - [ ] Compléter les tests existants

3. **Relâcher temporairement les seuils de couverture** ✅

## Actions à court terme (Sprint 2)

1. **Améliorer la couverture des services**
    - [ ] Tests pour tous les cas d'erreur dans UserService
    - [ ] Tests pour les cas limites

2. **Tester les contrôleurs**
    - [ ] Tests pour tous les endpoints
    - [ ] Tests des cas d'erreur HTTP

## Actions à moyen terme (Sprint 3-4)

1. **GlobalExceptionHandler**
    - [ ] Tests pour chaque type d'exception
    - [ ] Tests des formats de réponse

2. **DTOs et Commands**
    - [ ] Tests de validation
    - [ ] Tests de sérialisation/désérialisation

## Stratégie par couche (Architecture hexagonale)

### 1. Domaine (Priorité : HAUTE)
- Objectif : 90% de couverture
- Focus : Logique métier pure
- Approche : Tests unitaires exhaustifs

### 2. Application (Priorité : HAUTE)
- Objectif : 80% de couverture
- Focus : Orchestration et cas d'usage
- Approche : Tests unitaires avec mocks

### 3. Infrastructure (Priorité : MOYENNE)
- Objectif : 70% de couverture
- Focus : Intégration et adaptateurs
- Approche : Tests d'intégration

## Métriques de suivi

```yaml
# Évolution des seuils (à ajuster progressivement)
Sprint 1: Package 50%, Class 40%
Sprint 2: Package 60%, Class 50%
Sprint 3: Package 70%, Class 60%
Sprint 4: Package 80%, Class 70%
```

## Outils de monitoring

1. **CircleCI** : Exécution automatique
2. **Codecov** : Historique et tendances
3. **SonarCloud** : Analyse qualité

## Bonnes pratiques

1. **Écrire les tests AVANT de coder** (TDD)
2. **Un test par comportement**, pas par méthode
3. **Tester les cas limites** et les erreurs
4. **Maintenir les tests** lors des refactoring

## Exclusions justifiées

Certains éléments peuvent être exclus de la couverture :
- Classes de configuration Spring
- DTOs simples sans logique
- Exceptions personnalisées
- Main application class

## Automatisation

```bash
# Script pour vérifier la couverture localement
#!/bin/bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```