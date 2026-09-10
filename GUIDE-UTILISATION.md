# Guide d'utilisation – TelemetryHub

Supervision industrielle temps réel **multi-tenant** : ingestion de télémétrie (Kafka), stockage (TimescaleDB), alertes, rapports CSV/PDF et assistant IA.

---

## 1. Démarrage rapide

```bash
docker compose up -d --build
```

- Pile : 13 conteneurs `th-*` (gateway, auth, fleet, ingestion, alerting, reports, ai-service, simulator, kafka, postgres, redis, prometheus, frontend).
- Vérifier : `docker compose ps` → tout `Up`/`healthy`.
- Accès :
  - Application : **http://localhost:4200**
  - API (gateway) : **http://localhost:8080** (racine `/api/v1/**`)
  - Health gateway : `http://localhost:8080/actuator/health` → `{"status":"UP"}`

> Le frontend appelle directement le gateway sur `http://localhost:8080/api` (CORS autorisé pour `http://localhost:4200`).

---

## 2. Connexion

Ouvrir **http://localhost:4200** puis « Se connecter ».

| Rôle        | Email            | Mot de passe   |
|-------------|------------------|----------------|
| Admin tenant| admin@acme.com   | Demo@2026!     |
| Opérateur   | oper2@acme.com   | Oper@2026!     |

> Le mot de passe est **`Demo@2026!`** (avec le `!`, 10 caractères minimum exigé). L'aide à l'écran a été corrigée en conséquence.

La connexion renvoie `accessToken` + `refreshToken` ; le token est stocké en `localStorage` (`th_access_token`) et envoyé en `Authorization: Bearer …` sur tous les appels API.

---

## 3. Les 5 pages de l'application

Une fois connecté, la barre latérale donne accès à :

### 3.1 Tableau de bord
- Graphique en direct des valeurs de télémétrie (agrégées par métrique).
- Tableau des **dernières mesures** (`/v1/telemetry/latest`) : métrique, valeur, horodatage.

### 3.2 Parc & équipements
Gestion du référentiel : **modèles → sites → équipements**.
- **Ajouter un équipement** (formulaire) : nom, N° série, modèle (liste déroulante), site (liste déroulante).
  - Le `serialNumber` doit être **unique** au niveau du tenant (sinon erreur → utiliser un n° série distinct).
- **Créer un modèle et un site** : via API (pas encore de formulaire dédié) :

```bash
# Modèle
curl -X POST http://localhost:8080/api/v1/models -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Compresseur SAS-01","manufacturer":"ACME Industries","description":"Compresseur à vis","category":"COMPRESSOR"}'

# Site
curl -X POST http://localhost:8080/api/v1/sites -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Usine de Lyon","address":"12 rue des Forges","city":"Lyon","country":"France","latitude":45.75,"longitude":4.85}'
```

- **Changer un statut** (API) : `ACTIVE` | `MAINTENANCE` | `OFFLINE` | `DECOMMISSIONED` :

```bash
curl -X PATCH http://localhost:8080/api/v1/equipments/<EQUIPMENT_ID>/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"status":"MAINTENANCE"}'
```

- **Maintenance** : liste des équipements à maintenance (`Fleet` → gateway) :

```bash
curl http://localhost:8080/api/v1/maintenance/due -H "Authorization: Bearer $TOKEN"
```

### 3.3 Alertes
**Liste des événements** : statut `OPEN` | `ACKNOWLEDGED` | `RESOLVED`, filtre par statut/sévérité, acquitter / résoudre (boutons ou API).

**Règles** (formulaire « Nom de la règle, métrique, opérateur, seuil, sévérité ») :
- Opérateurs : `GT` | `GTE` | `LT` | `LTE` | `EQ` | `NE`
- Sévérités : `INFO` | `WARNING` | `CRITICAL` | `FATAL`

```bash
# Créer une règle (ex. temp > 80 °C)
curl -X POST http://localhost:8080/api/v1/rules -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Température trop élevée","description":"Seuil chauffe compresseur","equipmentId":"<EQUIPMENT_ID>","metric":"temperature","operator":"GT","threshold":80,"unit":"°C","severity":"CRITICAL","cooldownMinutes":5}'

# Activer / désactiver
curl -X PATCH http://localhost:8080/api/v1/rules/<RULE_ID>/enabled \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"enabled":true}'

# Liste des alertes
curl "http://localhost:8080/api/v1/alerts?status=OPEN" -H "Authorization: Bearer $TOKEN"

# Acquitter / résoudre
curl -X POST http://localhost:8080/api/v1/alerts/<ALERT_ID>/acknowledge -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8080/api/v1/alerts/<ALERT_ID>/resolve -H "Authorization: Bearer $TOKEN"
```

> Les règles sont évaluées en continu sur le flux Kafka (`telemetry.raw`) : créez une règle, puis envoyez des mesures qui la déclenchent → une alerte `OPEN` apparaît.

### 3.4 Rapports
- Formulaire : **type** (`CSV` ou `PDF`), **métrique** (ex. `temperature`, `vibration`), **période** (de / à).
- POST `/v1/reports` → 202, génération asynchrone ; statut `GENERATING → READY`.
- La liste (30 derniers) affiche titre, type, statut, taille, date. **Télécharger** = GET `/v1/reports/{id}` (le fichier est servi avec `Content-Disposition`).

```bash
curl -X POST http://localhost:8080/api/v1/reports -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"kind":"CSV","metric":"temperature","from":"2026-09-01T00:00:00Z","to":"2026-09-10T23:59:59Z"}'

# Téléchargement : utiliser -o pour sauver le fichier
curl -o rapport.csv http://localhost:8080/api/v1/reports/<REPORT_ID> -H "Authorization: Bearer $TOKEN"
```

### 3.5 Assistant IA
- Champ libre : poser une question sur la **base de connaissances** du tenant (RAG).
- `POST /api/v1/assistant/chat` `{"question":"…", "tenantId":"<votre tenantId>"}` → réponse + documents sources.

```bash
# État du service IA (vérifie la base de connaissances)
curl "http://localhost:8080/api/v1/ai/health" -H "Authorization: Bearer $TOKEN"
# stats de la KB
curl "http://localhost:8080/api/v1/assistant/knowledge/stats" -H "Authorization: Bearer $TOKEN"
# anomalies détectées par le moteur IA (Z-score)
curl "http://localhost:8080/api/v1/ai/anomalies?tenantId=<TENANT_ID>&limit=100" -H "Authorization: Bearer $TOKEN"
```

### 3.6 Maintenance (CMMS)
Module de maintenance préventive et corrective : une **alerte CRITIQUE/FATALE** crée automatiquement un **ordre de travail** (`source=ALERT`) et ouvre un **temps d'arrêt** ; quand l'alerte passe `RESOLVED`, le temps d'arrêt se referme. Vous pouvez aussi créer des ordres manuellement.

- Page **Maintenance** : KPI (nouveaux, en cours, en retard, clôturés, minutes d'arrêt du jour), formulaire de création, liste filtrable (statut), assignation à un technicien, transitions *Assigner → Démarrer → Terminer / Annuler*.
- Créer un ordre : `POST /v1/work-orders` `{"equipmentId","title","description","priority":"LOW|MEDIUM|HIGH|CRITICAL"}`.
- Transitions : `POST /v1/work-orders/{id}/assign` (body `assignedToUserId`), `POST .../start`, `POST .../complete`, `POST .../cancel`.
- KPI : `GET /v1/work-orders/kpi` → `{created, assigned, inProgress, open, overdue, completedToday, completed, downtimeTodayMinutes}`.
- Disponibilité machine (24 h) : `GET /v1/downtime/equipments/{equipmentId}/availability` → `{uptimeMinutes, downtimeMinutes, availabilityPercent, downtimeCount}`.

```bash
# Créer un ordre de travail manuel
curl -X POST http://localhost:8080/api/v1/work-orders -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"equipmentId":"<EQUIPMENT_ID>","title":"Remplacement roulement","priority":"HIGH"}'
```

> Un ordre automatique apparaît peu après une alerte `CRITICAL` : son titre est « Ordre automatique : <nom de la règle> », la disponibilité de la machine passe sous 100 % pendant l'arrêt.

---

## 4. Envoyer des données (ingestion)

Deux sources : le **simulateur** (déjà actif avec docker compose, file de mesures dans Kafka/TimescaleDB) et l'**API d'ingestion** :

```bash
curl -X POST http://localhost:8080/api/v1/ingestion/telemetry \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{
    "equipmentId": "<EQUIPMENT_ID>",
    "tenantId": "<TENANT_ID>",
    "metric": "temperature",
    "value": 87.5,
    "unit": "°C",
    "recordedAt": "2026-09-10T12:00:00Z",
    "source": "API",
    "deviceFingerprint": "sensor-01"
  }'
```

→ Réponse **202** (`Événement accepté pour ingestion`) ; la mesure transite par Kafka, est persistée, évaluée par les règles d'alerte et analysée par le moteur IA (anomalies Z-score).

**Consultation :**
```bash
# Dernières mesures
curl "http://localhost:8080/api/v1/telemetry/latest" -H "Authorization: Bearer $TOKEN"
# Données brutes sur une plage (from/to ISO, equipmentId optionnel)
curl "http://localhost:8080/api/v1/telemetry/raw?from=2026-09-10T00:00:00Z&to=2026-09-10T23:59:59Z" -H "Authorization: Bearer $TOKEN"
# Agrégat (bucket = 1h, 5m, 1d…)
curl "http://localhost:8080/api/v1/telemetry/aggregate?bucket=1h&from=2026-09-10T00:00:00Z&to=2026-09-10T23:59:59Z" -H "Authorization: Bearer $TOKEN"
```

---

## 5. Gérer les membres (invitations, rôles)

Rôles disponibles : `SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `VIEWER`.

```bash
# Inviter un collaborateur
curl -X POST http://localhost:8080/api/v1/auth/invite -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"tech@acme.com","fullName":"Technicien","role":"OPERATOR","validityHours":72}'
# → 201 { id, token, tenantId, email, role, status: PENDING, invitedBy, expiresAt }

# Accepter l'invitation (avec le token retourné)
curl -X POST http://localhost:8080/api/v1/auth/invitation/<TOKEN>/accept \
  -H "Content-Type: application/json" \
  -d '{"password":"SonMotDePasse2026!","deviceName":"PC-atelier"}'
# → 200, jeton créé ; puis login normal avec l'email invité

# Révoguer une invitation
curl -X POST http://localhost:8080/api/v1/auth/invitation/<ID>/revoke -H "Authorization: Bearer $TOKEN"

# Révoquer toutes les sessions du tenant
curl -X POST http://localhost:8080/api/v1/auth/revoke-all -H "Authorization: Bearer $TOKEN"

# Rafraîchir le token / déconnexion
curl -X POST http://localhost:8080/api/v1/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
curl -X POST http://localhost:8080/api/v1/auth/logout -H "Authorization: Bearer $TOKEN"
```

> Une invitation déjà **acceptée** ne peut pas être réutilisée ; un email déjà présent dans le tenant est rejeté → utiliser un email neuf pour tester.

---

## 6. Créer son propre tenant (API)

L'inscription UI n'existe pas encore, mais l'API est disponible :

```bash
curl -X POST http://localhost:8080/api/v1/auth/register-tenant \
  -H "Content-Type: application/json" \
  -d '{"tenantName":"Ma Compagnie","slug":"ma-compagnie","adminEmail":"admin@ma-compagnie.fr","adminFullName":"Administrateur","password":"MonMotDePasse2026!"}'
# → 201, compte TENANT_ADMIN créé ; login avec admin@ma-compagnie.fr / MonMotDePasse2026!
```

Contraintes : slug 3→80 car., email valide, mot de passe **10→100 caractères**.

---

## 7. Référence API rapide

Tout passe par le gateway `http://localhost:8080`, préfixe `/api`, `Authorization: Bearer <accessToken>` (sauf health/login/register/refresh/invitation-accept).

| Méthode | Route                          | Description |
|---------|--------------------------------|-------------|
| POST | `/v1/auth/register-tenant`     | Créer un tenant + admin |
| POST | `/v1/auth/login`               | Login (`{email,password,deviceName?}`) |
| POST | `/v1/auth/refresh`             | Rafraîchir le JWT |
| POST | `/v1/auth/logout`              | Déconnexion |
| POST | `/v1/auth/revoke-all`          | Révoquer les sessions du tenant |
| POST | `/v1/auth/invite`              | Inviter un membre |
| POST | `/v1/auth/invitation/{token}/accept` | Accepter l'invitation |
| POST | `/v1/auth/invitation/{id}/revoke`    | Révoguer l'invitation |
| GET/POST | `/v1/models`               | Modèles d'équipement |
| GET/POST | `/v1/sites`                | Sites |
| GET/POST/PATCH | `/v1/equipments`      | Équipements (+ `/status`) |
| GET | `/v1/maintenance/due`          | Équipements à maintenir |
| POST | `/v1/ingestion/telemetry`     | Ingest télémétrie (202) |
| GET | `/v1/telemetry/raw` `aggregate` `latest` | Lecture télémétrie |
| GET/POST | `/v1/rules`              | Règles d'alerte (+ `PATCH /{id}/enabled`, `DELETE /{id}`) |
| GET | `/v1/alerts` (+ `/acknowledge`, `/resolve`) | Événements d'alerte |
| POST | `/v1/reports` (GET liste, GET `/{id}` fichier) | Rapports CSV/PDF |
| GET/POST | `/v1/ai/health` `/v1/ai/detect` `/v1/ai/anomalies` | Moteur IA |
| POST | `/v1/assistant/chat` ; GET `/v1/assistant/knowledge/stats` | Assistant RAG |

---

## 8. Dépannage rapide

- **401 « Identifiants invalides »** → vérifier le mot de passe (`Demo@2026!` avec `!`), ou refaire un login après un `revoke-all`.
- **CORS bloqué dans la console** → le CORS est centralisé au gateway ; redémarrer `th-gateway-service` si besoin (`docker compose restart gateway-service`).
- **Erreur `serialNumber` dupliqué** → choisir un n° série unique.
- **Erreur réseau / 404 sur `:8080`** → Docker Desktop peut être arrêté : relancer `Docker Desktop.exe`, puis `docker compose up -d` (les conteneurs repartent seuls grâce à `restart: unless-stopped`).
- **Base de connaissances IA vide** → vérifier `GET /v1/ai/health` (`documents` = nombre de docs) et `GET /v1/assistant/knowledge/stats`.