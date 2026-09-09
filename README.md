# TelemetryHub

Plateforme **SaaS multi-tenant de supervision d'équipements industriels** (IoT temps réel) : ingestion
d'une télémétrie continue, alerting à chaud, détection d'anomalies statistique, rapports PDF/CSV et
assistant IA ancré sur une base de connaissances (RAG).

Construit comme un projet personnel « production-ready » : 6 microservices Java/Spring Boot, streaming
Kafka, TimescaleDB, frontend Angular 18, monitoring Prometheus/Grafana/Loki, déploiement Kubernetes
(Helm) + infrastructure Terraform (GCP), et pipeline CI/CD GitHub Actions complet.

> 🏭 **Démo** : console web sur `http://localhost:4200` — compte `admin@acme.com` / `Demo@2026`.
> Le service `simulator` produit un flux métrique réaliste en continu pour alimenter le dashboard.

[![CI](https://github.com/HazemMarrakchi/telemetryhub/actions/workflows/ci.yml/badge.svg)](https://github.com/HazemMarrakchi/telemetryhub/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://github.com/HazemMarrakchi/telemetryhub)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular&logoColor=white)](https://github.com/HazemMarrakchi/telemetryhub)

---

## Architecture

```
            ┌──────────────────────────────────────────────────────────────┐
            │                     Frontend Angular 18                       │
            │             (dashboard, parc, alertes, rapports, IA)          │
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

## Stack technique

| Domaine | Choix |
| --- | --- |
| Backend | Java 21, Spring Boot 3.3.5, Spring Cloud Gateway 2023.0.3, Spring Security + JWT (auth0) |
| Streaming | Apache Kafka 3.8 (KRaft), topology topics `telemetry.raw` / `telemetry.anomaly` / `telemetry.alerts` |
| Stockage | TimescaleDB (hypertables + compression) multi-schémas `tenant`, `fleet`, `metrics`, `alerts`, `reports`, `ai` |
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
| Console web | http://localhost:4200 | `admin@acme.com` / `Demo@2026` |
| Gateway (API) | http://localhost:8080 | routes `/api/v1/*` vers les services |
| Auth | 8081 · Parc 8082 · Ingestion 8083 · Alerting 8084 · Rapports 8085 | |
| AI | http://localhost:8090 | `/api/v1/ai/*`, `/api/v1/assistant/*` |
| Kafka | 9092 | topics auto-créés par `init-topics` |
| TimescaleDB | 5432 · Redis 6379 | |

**Observabilité**

```bash
docker compose -f infra/monitoring/docker-compose.monitoring.yml up -d
```

- Prometheus `http://localhost:9090` · Grafana `http://localhost:3000` (`admin` / `telemetryhub-dev` · Loki `http://localhost:3100`

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

## Arborescence

```
telemetryhub/
├─ backend/            # 6 services Spring Boot (multi-module Maven) + initdb
├─ frontend/           # Console Angular 18
├─ simulator/          # Simulateur de flux IoT (Python)
├─ ai-service/         # Anomalies Z-Score + assistant RAG (FastAPI)
├─ infra/
│  ├─ k8s/helm/telemetryhub/        # Chart Helm
│  ├─ terraform/                    # GKE, Cloud SQL, Redis
│  └─ monitoring/                   # Prometheus, Grafana, Loki/Promtail
├─ docs/               # Documentation technique
└─ .github/workflows/  # CI
```

## Feuille de route

- [ ] Authentification OIDC (Google/Okta) et SSO
- [ ] WebSocket temps réel sur le dashboard (StockBinance-style updates)
- [ ] Export direct datasource TimescaleDB depuis Grafana
- [ ] Capacity planning automatique par métrique (prédiction de pannes)