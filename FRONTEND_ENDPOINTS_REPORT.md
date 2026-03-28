# Rapport API - Integration Frontend

## Authentification
- Type d'auth: JWT Bearer
- Endpoint de connexion: `POST /auth/login`
- Header a envoyer apres connexion:
  - `Authorization: Bearer <token>`


## Resume des endpoints
Chemin de base pour toutes les routes: `/api`

### Auth
- `POST /auth/login`
  - Public
  - Body: `AuthRequest`

### Dashboard
- `GET /dashboard`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Vehicules
- `GET /vehicles`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /vehicles`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `VehicleRequest`
- `PUT /vehicles/{vehicleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `VehicleRequest`
- `DELETE /vehicles/{vehicleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Chauffeurs
- `GET /drivers`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /drivers`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverRequest`
- `PUT /drivers/{driverId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverRequest`
- `DELETE /drivers/{driverId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Clients
- `GET /clients`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /clients`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `ClientRequest`
- `PUT /clients/{clientId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `ClientRequest`
- `DELETE /clients/{clientId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Recettes journalieres
- `GET /revenues`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /revenues`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Body: `DailyRevenueRequest`
- `PUT /revenues/{revenueId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `DailyRevenueRequest`
- `DELETE /revenues/{revenueId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Dettes
- `GET /debts`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /debts`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `DebtCreateRequest`
- `PATCH /debts/{debtId}/payments`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `DebtPaymentRequest`
- `PATCH /debts/{debtId}/cancel`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Maintenance
- `GET /maintenances`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /maintenances`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `MaintenanceRequest`
- `PUT /maintenances/{maintenanceId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `MaintenanceRequest`
- `DELETE /maintenances/{maintenanceId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Factures
- `GET /invoices/pdf?clientId=<uuid>&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Reponse: PDF binaire
- `POST /invoices/email`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `InvoiceEmailRequest`

### Notifications
- `GET /notifications?unreadOnly=false`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
- `POST /notifications`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `NotificationCreateRequest`
- `PATCH /notifications/{notificationId}/read`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
- `GET /notifications/stream`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Endpoint SSE (flux temps reel)

### Chat
- `GET /chat/channels?includeArchived=false`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
- `POST /chat/channels`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Body: `ChatChannelCreateRequest`
- `PATCH /chat/channels/{channelId}/archive`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
- `GET /chat/channels/{channelId}/messages`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
- `POST /chat/channels/{channelId}/messages`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Body: `ChatMessageCreateRequest`
- `POST /chat/channels/{channelId}/attachments` (multipart)
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Parties: `file` + `content` optionnel
- `GET /chat/messages/{messageId}/attachment`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Reponse: fichier binaire
- `DELETE /chat/messages/{messageId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `GET /chat/stream`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`, `DRIVER`
  - Endpoint SSE (flux temps reel)

### Planning
- `GET /planning/driver-schedules?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /planning/driver-schedules`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverScheduleRequest`
- `PUT /planning/driver-schedules/{scheduleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverScheduleRequest`
- `DELETE /planning/driver-schedules/{scheduleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

- `GET /planning/maintenance-schedules?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /planning/maintenance-schedules`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `MaintenanceScheduleRequest`
- `PUT /planning/maintenance-schedules/{scheduleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `MaintenanceScheduleRequest`
- `DELETE /planning/maintenance-schedules/{scheduleId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### RH
- `GET /hr/absences?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /hr/absences`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverAbsenceRequest`
- `PUT /hr/absences/{absenceId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `DriverAbsenceRequest`
- `DELETE /hr/absences/{absenceId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Finance
- `GET /finance/entries?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `POST /finance/entries`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `FinancialEntryRequest`
- `PUT /finance/entries/{entryId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
  - Body: `FinancialEntryRequest`
- `DELETE /finance/entries/{entryId}`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`
- `GET /finance/summary?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`

### Audit
- `GET /audit/logs?from=2026-03-01T00:00:00Z&to=2026-03-31T23:59:59Z`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`

### Reporting
- `GET /reports/overview?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
- `GET /reports/overview.csv?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
  - Roles: `ADMIN`, `OPERATIONS_MANAGER`, `ASSISTANT`
  - Reponse: fichier CSV

---

## Notes Frontend
- Le scope tenant est derive de l'utilisateur/entreprise authentifie cote backend (ne pas envoyer `companyId` dans les payloads des modules coeur).
- Toutes les routes securisees exigent un token Bearer obtenu via `/auth/login`.
- Les parametres de date utilisent le format ISO (`YYYY-MM-DD`) et l'audit utilise un datetime ISO (`Instant`).
- Endpoints SSE:
  - `/notifications/stream`
  - `/chat/stream`
