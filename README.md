# TelemetryHub

Plateforme **SaaS multi-tenant de supervision d'équipements industriels** (IoT temps réel) : ingestion
d'une télémétrie continue, alerting à chaud, détection d'anomalies statistique, rapports PDF/CSV et
assistant IA ancré sur une base de connaissances (RAG).

Construit comme un projet personnel « production-ready » : 7 microservices Java/Spring Boot, streaming
Kafka, TimescaleDB, frontend Angular 18, monitoring Prometheus/Grafana/Loki, déploiement Kubernetes
(Helm) + infrastructure Terraform (GCP), et pipeline CI/CD GitHub Actions complet.

> 🏭 **Démo** : console web sur `http://localhost:4200` — compte `admin@acme.com` / `Demo@2026!`.
> Le service `simulator` produit un flux métrique réaliste en continu pour alimenter le dashboard.

[![CI](https://github.com/HazemMarrakchi/telemetryhub/actions/workflows/ci.yml/badge.svg)](https://github.com/HazemMarrakchi/telemetryhub/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://github.com/HazemMarrakchi/telemetryhub)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular&logoColor=white)](https://github.com/HazemMarrakchi/telemetryhub)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## Sommaire

- [Architecture](#architecture)
- [Maintenance (CMMS)](#maintenance-cmms)
- [Stack technique](#stack-technique)
- [Démarrage rapide (Docker Compose)](#démarrage-rapide-docker-compose)
- [API](#api)
- [Développement local](#développement-local)
- [Monitoring](#monitoring)
- [Déploiement Kubernetes (Helm)](#déploiement-kubernetes-helm)
- [Infrastructure Cloud (Terraform / GCP)](#infrastructure-cloud-terraform--gcp)
- [Sécurité](#sécurité)
- [Tests & CI](#tests--ci)
- [Documentation](#documentation)
- [Arborescence](#arborescence)
- [Feuille de route](#feuille-de-route)
- [Licence](#licence)

## Architecture

```
            ┌──────────────────────────────────────────────────────────────┐
            │                     Frontend Angular 18                       │
            │     (dashboard, parc, alertes, maintenance, rapports, IA)     │
            └───────────────────────────────┬──────────────────────────────┘
                                            │ HTTPS /api
                       ┌────────────────────▼────────────────────┐
                       │          gateway-service (8080)          │
                       │  JWT validate + rate-limit (Redis)       │
                       └─────┬────────┬────────┬───────────┬─────┘
                             │        │        │           │
             ┌───────────────▼──┐  ┌──▼─────────▼──┐  ┌─────▼──────────────┐
             │ auth-service 8081│  │ fleet 8082     │  │ reports 8085       │
             │ tenants + JWT    │  │ parc           │  │ PDF / CSV / TTL    │
             └──────────────────┘  └───────────────┘  └────────────────────┘
             ┌───────────────▼──┐                                     ┌─────▼──────────────┐
             │ maintenance 8086 │ ──── Kafka telemetry.alerts ───────▶ │ alerting (8084)     │
             │ CMMS (ordres de  │ ←─────────────────────────────────── │ seuils + rules       │
             │ travaux, arrêts) │                                     └────────────────────┘
             └──────────────────┘
  simulator ──▶ Kafka ──▶ ingestion (8083) ──▶ TimescaleDB ──▶ ingestion
  (Python)     telemetry.raw     └──▶ alerting (8084) ──▶ telemetry.alerts
                          ai-service (8090)  Z-Score + RAG (pgvector)
```

**Flux temps réel**
1. `simulator` (kafka-python) émet des lectures d'équipements sur `telemetry.raw`.
2. `ingestion-service` consomme, valide le schéma et insère dans la hypertable TimescaleDB
   `metrics.telemetry_readings` (chunk 1 jour, compression après 7 jours).
3. `alerting-service` évalue les règles de seuil par équipement (cooldown anti-déclenchement
   répété) et publie `telemetry.alerts`.
4. `ai-service` applique une **détection Z-Score** sur la fenêtre glissante, stocke les anomalies
   (schéma `ai`), et sert un **assistant RAG** sur la base de connaissances (`ai.documents`,
   recherche cosinus `pgvector`).
5. `maintenance-service` (CMMS) consomme `telemetry.alerts` : une alerte `CRITICAL/FATAL` ouvre un
   **ordre de travail** et un **temps d'arrêt** ; au passage à `RESOLVED` l'arrêt se referme
   automatiquement (boucle fermée alerting → maintenance).

## Maintenance (CMMS)

Module de maintenance préventive et corrective intégré au flux d'alertes :

- **Ordres de travail** : création manuelle ou automatique depuis une alerte critique (`source=ALERT`),
  cycle *Assigner → Démarrer → Terminer / Annuler*, types d'intervention `PREVENTIVE | CORRECTIVE | INSPECTION`,
  priorité, échéance, notes de clôture, pièces et coût estimé.
- **Temps d'arrêt & OEE** : ouverture/fermeture automatique depuis les alertes, disponibilité machine,
  simplicité globale `GET /v1/downtime/oee` (disponibilité 24 h, arrêts fusionnés, taux de clôture).
- **Maintenance préventive planifiée** : plannings récurrents (`GET/POST/DELETE /v1/work-orders/schedules`)
  — le service génère automatiquement un ordre de travail (`source=SCHEDULE`) à chaque échéance
  (`next_run_at`), avance la prochaine génération de `interval_days`, et n'ouvre qu'un seul ordre à la
  fois par planning (dé-duplication).
- **Historique des interventions** : `GET /v1/work-orders/history` (ordres clôturés, paginé).
- **Tendances de coûts** : `GET /v1/work-orders/costs` — agrégation mensuelle (coût estimé) sur les
  ordres `COMPLETED`/`CANCELLED`, alimente le graphe 12 mois de la page Maintenance.
- **Page Maintenance** (front) : onglets *Ordres de travail*, *Historique & tendances* (graphe SVG des
  coûts + liste des clôtures), *Maintenance préventive* (création/suppression de plannings, échéances) —
  KPI temps réel, cards OEE, assignation aux techniciens, transitions et notes de clôture.

## Stack technique

| Domaine | Choix |
| --- | --- |
| Backend | Java 21, Spring Boot 3.3.5, Spring Cloud Gateway 2023.0.3, Spring Security + JWT (auth0) |
| Streaming | Apache Kafka 3.8 (KRaft), topology topics `telemetry.raw` / `telemetry.anomaly` / `telemetry.alerts` |
| Stockage | TimescaleDB (hypertables + compression) multi-schémas `tenant`, `fleet`, `metrics`, `alerts`, `reports`, `maintenance`, `ai` |
| Cache / rate-limit | Redis 7 |
| IA | FastAPI, détection Z-Score, embeddings hashing 128‑d, RAG pgvector (réponses sourcées) |
| Frontend | Angular 18, NGRX, RxJS, (lazy-loading par fiche) |
| Langages simulateur | Python (kafka-python), profils métriques synthétiques réalistes |
| Tests | JUnit 5 + Spring Boot Test (JWT signés), Angular Karma/Jasmine, pytest (2 services Python), ruff |
| Infra | Docker Compose, Helm chart K8s, Terraform (GCP: GKE, Cloud SQL TimescaleDB, Memorystore) |
| Observabilité | Prometheus + Grafana (dashboards provisionnés) + Loki/Promtail (logs) |
| CI/CD | GitHub Actions : backend (Maven/Jacoco), frontend (lint/test/build), Python (ruff/pytest), compose, helm, terraform fmt |

## Démarrage rapide (Docker Compose)

Prérequis : Docker ≥ 24, Docker Compose ≥ 2.20.

```bash
docker compose up -d --build
```

| Service | URL | Notes |
| --- | --- | --- |
| Console web | http://localhost:4200 | `admin@acme.com` / `Demo@2026!` |
| Gateway (API) | http://localhost:8080 | routes `/api/v1/*` vers les services |
| Auth | 8081 · Parc 8082 · Ingestion 8083 · Alerting 8084 · Rapports 8085 · Maintenance (CMMS) 8086 | |
| AI | http://localhost:8090 | `/api/v1/ai/*`, `/api/v1/assistant/*` |
| Kafka | 9092 | topics auto-créés par `init-topics` |
| TimescaleDB | 5432 · Redis 6379 | |

**Observabilité**

```bash
docker compose -f infra/monitoring/docker-compose.monitoring.yml up -d
```

- Prometheus `http://localhost:9090` · Grafana `http://localhost:3000` (`admin` / `telemetryhub-dev`) · Loki `http://localhost:3100`

## API

Tous les appels passent par le gateway (`http://localhost:8080`) avec l'en-tête
`Authorization: Bearer <accessToken>` ; l'isolation multi-tenant est appliquée automatiquement
(headers `X-Tenant-Id` / `X-User-Id` / `X-User-Role` injectés par la gateway).

| Service | Endpoints principaux |
| --- | --- |
| Auth (8081) | `POST /api/v1/auth/register-tenant` · `/login` · `/refresh` · `/logout` · `/revoke-all` — `GET /api/v1/auth/users` — `POST /api/v1/auth/invite` |
| Fleet (8082) | CRUD `/api/v1/models`, `/api/v1/sites`, `/api/v1/equipments` — `PATCH /equipments/{id}/status` · `/heartbeat` — `GET /api/v1/maintenance/due` |
| Ingestion (8083) | `POST /api/v1/ingestion/telemetry` — `GET /api/v1/telemetry/raw` · `/aggregate` · `/latest` |
| Alerting (8084) | `GET /api/v1/alerts` (paginé) — `POST /api/v1/alerts/{id}/acknowledge` · `/resolve` — CRUD `/api/v1/rules` (+ `PATCH /{id}/enabled`) |
| Reports (8085) | `POST /api/v1/reports` (génération async PDF/CSV) — `GET /api/v1/reports` — `GET /api/v1/reports/{id}` (téléchargement) |
| Maintenance (8086) | CRUD `/api/v1/work-orders` + `/assign` `/start` `/complete` `/cancel` — `GET /work-orders/kpi` · `/history` · `/costs` — `/work-orders/schedules` — `GET /downtime/oee` · `/downtime/equipments/{id}/availability` |
| AI (8090) | `GET /api/v1/ai/health` — `POST /api/v1/ai/detect` — `GET /api/v1/ai/anomalies` — `POST /api/v1/assistant/chat` — `GET /api/v1/assistant/knowledge/stats` |

> Parcours détaillé (connexion, pages, exemples `curl` prêts à l'emploi) : voir le
> [guide d'utilisation](GUIDE-UTILISATION.md).

## Développement local

Prérequis : **JDK 21** (Temurin), **Maven 3.9+**, **Node 20** + npm, **Python 3.11+**, Docker.

```bash
# 1. Infrastructure seule (TimescaleDB, Kafka + topics, Redis)
docker compose up -d postgres kafka init-topics redis

# 2. Backend — un terminal par service (profil par défaut : localhost)
mvn -f backend/pom.xml spring-boot:run -pl auth-service
mvn -f backend/pom.xml spring-boot:run -pl gateway-service

# 3. Frontend (http://localhost:4200)
cd frontend && npm install && npm start

# 4. Services Python
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --port 8090
cd simulator && pip install -r requirements.txt && python -m app.main
```

## Monitoring

Chaque service Spring Boot expose `/actuator/prometheus` (micrometer) — dashboard Grafana
« TelemetryHub — Vue d'ensemble »: débit HTTP, erreurs 5xx, latence P95, débit par service.
Logs consolidés via Loki/Promtail (auto-découverte des conteneurs Docker).

## Déploiement Kubernetes (Helm)

```bash
helm lint infra/k8s/helm/telemetryhub
helm template telemetryhub infra/k8s/helm/telemetryhub   # preview
helm upgrade --install telemetryhub infra/k8s/helm/telemetryhub --namespace telemetryhub --create-namespace
```

## Infrastructure Cloud (Terraform / GCP)

- Clusters **GKE** (pool applicatif autoscalé, plan de contrôle privé, Workload Identity).
- **Cloud SQL** PostgreSQL 15 + TimescaleDB (HA régionale en production) et **Memorystore** Redis.

```bash
cd infra/terraform
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

## Sécurité

- Authentification **JWT** (HS256) signé par `auth-service` ; refresh tokens.
- `gateway-service` : validation du JWT, injection des headers `X-Tenant-Id`, `X-User-Id`,
  `X-User-Role`, **rate limiting** global et par route (Redis).
- Multi-tenant : isolation par `tenant_id` sur toutes les requêtes (TenantContext) et par schéma DB.
- Actuator restreint : seul `/actuator/health`, `/actuator/info`, `/actuator/prometheus` exposés.
- Secrets : jamais commités — injectés par variable d'environnement / secret K8s.

## Tests & CI

```bash
# Backend (build complet + tests + couverture)
mvn -f backend/pom.xml verify

# Frontend
cd frontend && npm ci && npx ng lint && npx ng test --watch=false --browsers=ChromeHeadless && npx ng build

# Services Python
cd simulator && ruff check . && python -m pytest
cd ai-service && ruff check . && python -m pytest
```

Le pipeline `.github/workflows/ci.yml` rejoue l'ensemble sur chaque push (`main`) et PR.
Un workflow complémentaire (`lock-linux.yml`) régénère le `package-lock.json` du frontend sur Linux.

## Documentation

- **[Guide d'utilisation](GUIDE-UTILISATION.md)** — démarrage, connexion, parcours des 5 pages de la
  console, exemples `curl` et endpoints détaillés.
- **[Architecture technique](docs/ARCHITECTURE.md)** — vues de conteneurs (C4), diagrammes Mermaid,
  flux Kafka et schémas de données.

## Arborescence

```
telemetryhub/
├─ backend/              # 7 services Spring Boot (multi-module Maven) + initdb
├─ frontend/             # Console Angular 18
├─ simulator/            # Simulateur de flux IoT (Python)
├─ ai-service/           # Anomalies Z-Score + assistant RAG (FastAPI)
├─ infra/
│  ├─ k8s/helm/telemetryhub/        # Chart Helm
│  ├─ terraform/                    # GKE, Cloud SQL, Redis
│  └─ monitoring/                   # Prometheus, Grafana, Loki/Promtail
├─ docs/                 # Documentation technique (ARCHITECTURE.md + diagrammes)
├─ GUIDE-UTILISATION.md  # Guide d'utilisation pas à pas
└─ .github/workflows/    # CI
```

## Feuille de route

- [ ] Authentification OIDC (Google/Okta) et SSO
- [ ] WebSocket temps réel sur le dashboard (StockBinance-style updates)
- [ ] Export direct datasource TimescaleDB depuis Grafana
- [ ] Capacity planning automatique par métrique (prédiction de pannes)
- [x] CMMS : historique des interventions, maintenance préventive planifiée et tendances de coûts

## Licence

Distribué sous licence [MIT](LICENSE) — © 2026 Hazem Marrakchi.
