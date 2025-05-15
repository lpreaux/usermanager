# Roadmap fonctionnelle

## 1. Sécurité et authentification

### Phase 1 : Implémentation de base (2 semaines)
- Intégration complète de Spring Security
- Authentification par JWT
- Gestion des sessions utilisateur
- Protection CSRF
- Expiration et rotation des tokens

### Phase 2 : Fonctionnalités avancées (3 semaines)
- Multi-facteur (SMS/email/TOTP)
- Sign-in with Google/GitHub/etc.
- Password strength requirements
- Rate limiting des tentatives de connexion
- Audit des connexions

## 2. Gestion des rôles et permissions

### Phase 1 : Modèle de base (2 semaines)
- Ajout d'entités Role et Permission
- Relations User-Role et Role-Permission
- API de gestion des rôles/permissions
- Tests unitaires et d'intégration

### Phase 2 : RBAC complet (3 semaines)
- Implémentation d'un système RBAC (Role-Based Access Control)
- Vérification des permissions au niveau des endpoints
- Interface d'administration des rôles
- Documentation de l'API

## 3. Fonctionnalités métier

### Phase 1 : Gestion des profils (2 semaines)
- Photos de profil (upload/redimensionnement)
- Préférences utilisateur
- Historique d'activité
- Vérification d'email

### Phase 2 : Fonctionnalités sociales (3 semaines)
- Gestion des relations entre utilisateurs
- Système de notification
- Messages entre utilisateurs
- Paramètres de confidentialité

## 4. Performance et scalabilité

### Phase 1 : Optimisations (2 semaines)
- Mise en cache des requêtes fréquentes (Redis)
- Pagination et recherche avancée
- Optimisation des requêtes SQL
- Compression des réponses

### Phase 2 : Scalabilité horizontale (3 semaines)
- Sessions distribuées
- Déploiement sur Kubernetes
- Séparation lecture/écriture
- Tests de charge