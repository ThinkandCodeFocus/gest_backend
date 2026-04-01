# Backend Bootstrap

## Variables d'environnement

Le backend lit maintenant ses valeurs sensibles via variables d'environnement et peut charger un fichier `.env` a la racine du projet grace a `spring.config.import`.

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

Variables ajoutees pour le branchement local front/back:

- `SERVER_PORT`
- `APP_CORS_ALLOWED_ORIGINS`

Un exemple complet est fourni dans [.env.example](C:/Users/Serigne%20Mbaye%20Sy/Downloads/gest_backend/.env.example).

## Endpoints importants pour le front actuel

### Auth

- `POST /api/auth/login`
- `GET /api/auth/me`

### Dashboard

- `GET /api/dashboard`
- `GET /api/dashboard/direction`
- `GET /api/dashboard/operations`
- `GET /api/dashboard/assistant`

### Vehicules / RH

- `GET /api/vehicles`
- `GET /api/drivers`
- `GET /api/clients`

### Recettes

- `GET /api/revenues`
- `POST /api/revenues`
- `PUT /api/revenues/{revenueId}`

### Maintenance

- `GET /api/maintenances`
- `GET /api/maintenances/alerts`
- `POST /api/maintenances`
- `PUT /api/maintenances/{maintenanceId}`

### Dettes

- `GET /api/debts`

### Planning

- `GET /api/planning/events`
- `POST /api/planning/events`
- `PUT /api/planning/events/{id}`
- `DELETE /api/planning/events/{id}`
- `GET /api/planning/driver-schedules`
- `GET /api/planning/maintenance-schedules`

### Messagerie

- `GET /api/messages/contacts`
- `GET /api/messages`
- `POST /api/messages`
- `PATCH /api/messages/{messageId}/read`
- `GET /api/messages/unread-count`

### Notifications

- `GET /api/notifications`
- `PATCH /api/notifications/{notificationId}/read`
- `PATCH /api/notifications/read-all`
- `GET /api/notifications/unread-count`
- `GET /api/notifications/stream`

### Facturation

- `GET /api/invoices/pdf`
- `POST /api/invoices/email`
- `POST /api/invoices/message`

### Portail client

- `GET /api/clients/portal/overview`
- `GET /api/clients/portal/vehicles`
- `GET /api/clients/portal/monthly-report`

## Points backend alignes sur le front

- Le chauffeur ne cree pas de recette.
- La messagerie est orientee contact direct.
- L'envoi principal de facture peut passer par la messagerie interne.
- Le portail client renvoie un tableau mensuel detaille par jour.
- La maintenance supporte plusieurs pieces, M.O., prestataire et alerte anti-fraude.

## Lancement local exact

1. Copier `.env.example` en `.env`.
2. Verifier que MySQL tourne et que la base `transport_backend` existe.
3. Demarrer le backend:

```powershell
cmd /c .\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Le backend sera disponible sur `http://localhost:8080/api`.

## Comptes demo

- `direction@orbitfleet.app` / `demo1234`
- `responsable@orbitfleet.app` / `demo1234`
- `assistant@orbitfleet.app` / `demo1234`
- `chauffeur@orbitfleet.app` / `demo1234`
