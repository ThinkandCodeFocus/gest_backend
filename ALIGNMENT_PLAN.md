# Plan d'Alignement Backend-Frontend
**Date**: 29 Mars 2026  
**Status**: En cours de planification  
**Scope**: Refonte majeure pour alignement complet

---

## 1. Analyse des Écarts (État Actuel vs Attendu)

### ❌ Rôles & Permissions (CRITIQUE)
| Problème | État Actuel | Attendu | Impact |
|----------|-----------|---------|---------|
| Rôle CLIENT | ❌ Absent | ✅ Ajouter | Portail client impossible |
| DRIVER crée recettes | ❌ Autorisé | ❌ Interdit | Faille de sécurité métier |
| RESPONSABLE accès settings | ❌ Possible | ❌ Interdit (ADMIN only) | Accès non-supervisé aux règles |

### ❌ Messagerie (ARCHITECTURE MAJEURE)
| Aspect | État Actuel | Attendu | Effort |
|--------|-----------|---------|--------|
| Modèle | Channels | Direct 1-to-1 (user↔user) | 🔴 Refonte |
| Endpoints | `/chat/channels`, `/chat/messages` | `/messages?contactId=...` | 🔴 Refonte API |
| Cas d'usage | Équipe → general chat | Chauffeur ↔ Ops, Client ↔ Ops | 🔴 Refonte logique |
| Attachments | Uploads possibles | Uploads + lien PDF factures | 🟡 Extension |

### ❌ Facturation (CHANGEMENT MÉTIER)
| Aspect | État Actuel | Attendu | Action |
|--------|-----------|---------|--------|
| Envoi facture | Email uniquement | **Messagerie interne (principal)** + Email opt | Ajouter endpoint `/invoices/{id}/send-to-messages` |
| Flux | Direct client | Via DRIVER (liens) + OPERATIONS | Requête métier → Dashboard |

### ❌ Dashboards (UNIFICATION)
| État Actuel | Attendu | Refonte |
|-----------|---------|---------|
| `GET /dashboard` (tous) | Séparé par rôle | 🟡 Ajouter variantes role-spécifiques |
| | `GET /dashboard/direction` | 📊 KPIs financiers, alertes |
| | `GET /dashboard/operations` | 📊 Productivité, maintenance |
| | `GET /dashboard/assistant` | 📊 Assignations, tâches |

### ❌ Planning (UNIFICATION)
| État Actuel | Attendu | Effort |
|-----------|---------|---------|
| Séparé: `driver_schedules` + `maintenance_schedules` | Unifié: `planning_events` (type: driver/maintenance) | 🟡 Table pivot + endpoints |
| Format: Dates séparées | Format: `{date, slot, type, title, owner, priority}` | 🟡 Restructuration DTO |

### ❌ Maintenance (EXTENSION MÉTIER)
| État Actuel | Attendu | Effort |
|-----------|---------|---------|
| `MaintenanceRecord` simple | Pièces multiples + main d'œuvre + prestataire | 🔴 Refonte table + logique |
| Pas de détection fraude | Endpoint `/check-fraud` (comparaison prix) | 🟡 Logique métier |
| Pas de lien PDF | Lien vers PDF justificatif | 🟡 Champ URL |

### ❌ Settings Système (MODULE ABSENT)
| Attendu | Statut | Effort |
|--------|--------|--------|
| `GET /settings/system` | ❌ N'existe pas | 🔴 Créer |
| `GET /settings/revenue-rules` | ❌ N'existe pas | 🔴 Créer |
| `GET /settings/vehicle-types` | ❌ N'existe pas | 🔴 Créer |
| `GET /settings/day-statuses` | ❌ N'existe pas | 🔴 Créer |

### ❌ Portail Client (MODULE ABSENT)
| Endpoint | Statut | Effort |
|----------|--------|--------|
| `GET /clients/portal/overview?month=YYYY-MM` | ❌ N'existe pas | 🔴 Créer |
| `GET /clients/portal/vehicles` | ❌ N'existe pas | 🔴 Créer |
| `GET /clients/portal/monthly-report?vehicleId=...` | ❌ N'existe pas | 🔴 Créer |

### ⚠️ Vehicles (ENRICHISSEMENT)
| Ajout | Statut | Effort |
|--------|--------|--------|
| Filtres: `search, type, status, client, page, pageSize, sort` | 🟡 Partiels | 🟡 Compléter |
| Champs: `emailClient`, `numeroTelephoneClient`, `amortissement`, `statutGlobal` | 🟡 Partiels | 🟡 Ajouter |
| Contrainte: `matricule` unique | ❌ Non validé | 🟡 Ajouter |

### ⚠️ Revenues (REFONTE MÉTIER)
| Problème | État Actuel | Solution |
|----------|-----------|----------|
| DRIVER crée | ❌ Oui | ❌ Non (seul OPERATIONS/ADMIN) |
| Endpoints | Minimalistes | Ajouter: `/preview`, `/validate`, `/reject` |
| Calculs auto | ❌ Incomplet | ✅ part_societe, part_chauffeur, dette_generee |

### ⚠️ Permissions Générales
| Aspect | État Actuel | Attendu |
|--------|-----------|---------|
| Filtres pagination | 🟡 Partiels | ✅ TOUS endpoints (page, pageSize, sort, search, filtres métier) |
| Validation données | 🟡 Basique | ✅ Stricte + normalisée |
| Agrégations perf | 🟡 Dashboard faible | ✅ Optimisées (no N+1) |

---

## 2. Plan d'Exécution par Phase

### Phase 1: Réforme Rôles & Permissions (P0 - CRITIQUE)
**Dépendances**: Aucune  
**Durée estimée**: 2-3h  
**Livraables**:
- [ ] Ajouter enum rôle `CLIENT`
- [ ] Corriger `@PreAuthorize` sur revenues: DRIVER → OPERATIONS_MANAGER/ADMIN uniquement
- [ ] Corriger `@PreAuthorize` sur settings: ADMIN only (retirer RESPONSABLE)
- [ ] Tests: vérifier rejections par rôle insuffisant

### Phase 2: Refonte Messagerie (P0 - BLOQUANTE)
**Dépendances**: Phase 1  
**Durée estimée**: 4-5h  
**Livraables**:
- [ ] Créer `Message` entity (1-to-1): sender, recipient, content, attachments
- [ ] Créer `MessageThread` entity: participants, lastMessage, unreadCount
- [ ] Supprimer `ChatChannel` et `ChatMessage` (ou laisser legacy)
- [ ] Endpoints:
  - `GET /messages/contacts` → liste destinataires actifs
  - `GET /messages?contactId=...` → historique avec contact
  - `POST /messages` → créer + broadcast SSE
  - `PATCH /messages/{id}/read` → marquer lu
  - `GET /messages/unread-count` → aggrégation
- [ ] MessageRealtimeService (SSE)
- [ ] Tests: threading, unread state, delivery

### Phase 3: Module Settings (P1 - HAUTE)
**Dépendances**: Phase 1  
**Durée estimée**: 3-4h  
**Livraables**:
- [ ] Créer entités:
  - `SystemSetting` (key-value)
  - `RevenueRule` (calcul parts, règles)
  - `VehicleType` (enum metadata)
  - `DayStatus` (enum metadata)
- [ ] Repository + Service pour chaque
- [ ] Endpoints: GET/PUT pour chaque setting
- [ ] Tests: RBAC (ADMIN only), validation règles
- [ ] Seed données initiales dans DataInitializer

### Phase 4: Portail Client (P1 - HAUTE)
**Dépendances**: Phase 1, 3  
**Durée estimée**: 4-5h  
**Livraables**:
- [ ] Rôle CLIENT → vérrouillé à sa company
- [ ] Créer ClientPortalService
- [ ] Endpoints:
  - `GET /clients/portal/overview?month=YYYY-MM` → revenus, dettes, véhicules actifs
  - `GET /clients/portal/vehicles` → liste avec KPIs
  - `GET /clients/portal/monthly-report?vehicleId=...` → rapport détaillé (PDF ou JSON)
- [ ] Tests: isolation tenant (CLIENT ne voit que sa data)

### Phase 5: Dashboards Séparés par Rôle (P0 - HAUTE)
**Dépendances**: Phase 1, 3  
**Durée estimée**: 3-4h  
**Livraables**:
- [ ] `GET /dashboard/direction` → profit, ROI, alertes critiques
- [ ] `GET /dashboard/operations` → productivité, maintenances urgentes, assignations
- [ ] `GET /dashboard/assistant` → tâches day, assignations
- [ ] `GET /dashboard/alerts` → alertes unifiées
- [ ] `GET /dashboard/maintenance-alerts` → maintenance urgentes
- [ ] `GET /dashboard/kpis` → KPIs principaux (réutilisables)
- [ ] Tests: données par rôle

### Phase 6: Extension Modèles (P1 - HAUTE)
**Dépendances**: Phase 3  
**Durée estimée**: 5-6h  

#### 6a. Vehicle
- [ ] Ajouter champs: `emailClient`, `numeroTelephoneClient`, `amortissement`, `statutGlobal`
- [ ] Valider unique `matricule`
- [ ] Enrichir GET /vehicles:
  - `search` (matricule, immatriculation, client)
  - `type` (filter)
  - `status` (filter)
  - `client` (filter)
  - `page`, `pageSize`, `sort`

#### 6b. Maintenance → Pièces Multi
- [ ] Remodeler: `MaintenanceRecord` + `MaintenancePart` (1-many)
- [ ] Champs MaintenancePart: `description`, `prix`, `quantité`
- [ ] Champs MaintenanceRecord: `maintenanceType`, `dureeMainOeuvre`, `prestataireId` (link), `urlPdf`, `dateDebut`, `dateFin`
- [ ] Endpoint: `POST /maintenances/check-fraud` → comparer prix vs historique

#### 6c. Revenue → Validation Stricte
- [ ] Interdire DRIVER
- [ ] Ajouter endpoints: `/preview`, `/{id}/validate`, `/{id}/reject`
- [ ] Calculs: `partSociete`, `partChauffeur`, `detteGeneree` (auto)
- [ ] Statuts normalisés: DRAFT, SUBMITTED, VALIDATED, REJECTED

#### 6d. Tests Nouveaux Modèles
- [ ] MultiPart maintenance
- [ ] Revenue auto-calc
- [ ] Filters + pagination

### Phase 7: Planning Unifié (P2 - NORMAL)
**Dépendances**: Phase 1, 3  
**Durée estimée**: 3-4h  
**Livraables**:
- [ ] Créer `PlanningEvent` (remplace driver_schedules + maintenance_schedules)
  - Champs: `date`, `slot` (matin/après-midi/nuit), `type` (DRIVER/MAINTENANCE), `title`, `ownerId`, `priority`
- [ ] Endpoints: `GET /planning/events?date=...&type=...&ownerId=...`
- [ ] Migrer données existantes
- [ ] Tests: date ranges, type filters

### Phase 8: Filtres, Pagination, Validation (P1 - TRANSVERSE)
**Dépendances**: Toutes phases précédentes  
**Durée estimée**: 4-5h  
**Actions**:
- [ ] Standardiser paginiation sur TOUS endpoints list
  - PageRequest wrapper: `{page: 0, pageSize: 10, sort: "createdAt:desc"}`
  - Response: `{data: [...], total: N, page: 0, pageSize: 10}`
- [ ] Ajouter validation @NotNull, @NotBlank, @Min, @Max partout
- [ ] Implémenter handlers d'erreur (ValidationException → 400)
- [ ] Tests: validation, limits

### Phase 9: Tests Unitaires & Intégration (P2 - TRANSVERSE)
**Dépendances**: Toutes phases  
**Durée estimée**: 6-8h  
**Actions**:
- [ ] Tests pour chaque service nouveau
- [ ] Tests RBAC (rôles incorrects → 403)
- [ ] Tests tenant isolation (CLIENT ne croise pas autre company)
- [ ] Tests performance (dashboards, agrégations)

---

## 3. Estimations & Priorités

| Phase | Priorité | Durée | Dépendances |
|-------|----------|-------|-------------|
| 1. Rôles | 🔴 P0 | 2-3h | Aucune |
| 2. Messagerie | 🔴 P0 | 4-5h | Phase 1 |
| 3. Settings | 🟠 P1 | 3-4h | Phase 1 |
| 4. Portail Client | 🟠 P1 | 4-5h | Phase 1, 3 |
| 5. Dashboards | 🟠 P1 | 3-4h | Phase 1, 3 |
| 6. Extensions Modèles | 🟠 P1 | 5-6h | Phase 3 |
| 7. Planning Unifié | 🟡 P2 | 3-4h | Phase 1, 3 |
| 8. Filtres/Validation | 🟠 P1 | 4-5h | Phases 1-7 |
| 9. Tests | 🟡 P2 | 6-8h | Toutes |
| **TOTAL** | — | **34-44h** | — |

---

## 4. Dépendances Critiques

```
Phase 1 (Rôles) ────────────┬─── Phase 2 (Messagerie)
                             ├─── Phase 3 (Settings)
                             ├─── Phase 4 (Portail Client)
                             ├─── Phase 5 (Dashboards)
                             ├─── Phase 6 (Extensions)
                             └─── Phase 7 (Planning)
                                    ↓
                             Phase 8 (Filtres/Validation)
                                    ↓
                             Phase 9 (Tests)
```

---

## 5. Actions Immédiat (Next Steps)

1. ✅ **Confirmation utilisateur**: Accepter ce plan?
2. ⏳ **Lancer Phase 1**: Ajouter rôle CLIENT + fixer permissions critiques
3. ⏳ **Déployer Phase 1**: Build + test smoke
4. ⏳ **Itérer phases 2-9** selon priorité

---

## 6. Notes Techniques

- **Backward compatibility**: Modules Legacy (ChatChannel) restent mais depreciated
- **Migration data**: Appliquer au startup (Flyway ou Liquibase)
- **Performance**: Benchmarker dashboards (N+1 queries)
- **Testing**: Couvrir 80%+ des nouvelles branches

