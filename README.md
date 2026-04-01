# ERP Gestion de Flotte - Backend Spring Boot

Ce backend est maintenant aligne sur le front actuel sans modification du front.

## Couverture fonctionnelle

- authentification JWT et roles
- dashboards par role
- vehicules, chauffeurs, clients
- recettes journalieres avec dette automatique
- maintenances detaillees avec pieces, M.O. et alertes anti-fraude
- dettes
- planning evenementiel et plannings derives
- messagerie directe entre acteurs
- notifications
- facturation PDF et envoi dans messagerie interne
- portail client mensuel detaille

## Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL

## Configuration

Les secrets ne doivent plus etre commits en dur.

Variables utilisees:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `CHAT_STORAGE_DIR`

Exemple dans [`.env.example`](C:/Users/Serigne%20Mbaye%20Sy/Downloads/gest_backend/.env.example).

## Lancer le projet

```powershell
$env:JAVA_HOME="C:\Path\To\JDK"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd spring-boot:run
```

Base URL:

- `http://localhost:8080/api`

## Endpoints clefs

### Auth

- `POST /api/auth/login`
- `GET /api/auth/me`

### Planning

- `GET /api/planning/events`
- `POST /api/planning/events`

### Maintenance

- `GET /api/maintenances`
- `GET /api/maintenances/alerts`
- `POST /api/maintenances`

### Messagerie

- `GET /api/messages/contacts`
- `GET /api/messages`
- `POST /api/messages`

### Notifications

- `GET /api/notifications`
- `PATCH /api/notifications/read-all`
- `GET /api/notifications/unread-count`

### Facturation

- `GET /api/invoices/pdf`
- `POST /api/invoices/email`
- `POST /api/invoices/message`

### Portail client

- `GET /api/clients/portal/overview`
- `GET /api/clients/portal/vehicles`
- `GET /api/clients/portal/monthly-report`

## Notes

- Le role `CLIENT` est supporte pour messagerie et notifications.
- Le chauffeur ne cree pas de recette.
- Le front attend encore une vraie execution Java locale pour valider le tout en integration.

Details supplementaires dans [BACKEND_BOOTSTRAP.md](C:/Users/Serigne%20Mbaye%20Sy/Downloads/gest_backend/BACKEND_BOOTSTRAP.md).
