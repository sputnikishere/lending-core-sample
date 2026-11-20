# Lending platform — technical architecture

A modular loan origination and servicing platform: loan issuance and servicing, scoring and credit-bureau reporting, collections, reporting, and export to the accounting system. Multi-tenant, event-driven, deployed on Kubernetes.

**Stack:** Java 21 · Spring Boot 3.5 · React 19 · PostgreSQL · Kafka (KRaft) · Redis · MinIO · Flowable (BPMN) · Helm

## Entry points

| Client | Service | Auth |
|---|---|---|
| Employee (browser) | `fe-lk-empl` — back-office SPA (React 19, TypeScript, Vite, Ant Design) → `be-lk-empl` — back-office API | JWT + roles |
| External origination system (partner/merchant front end) | `be-external` — public API for loan origination, CORS, callback notifications | `X_API_KEY` |
| Accounting system | `be-accounting-export` — multi-tenant export to the accounting ledger, one DB pool per tenant | tenant token |

## Domain services (shared `core` module: entities, QueryDSL, facades, `main` DB migrations)

- **be-calculations** — interest accrual and recalculations per loan, schedulers; per-loan locking so a loan is never recalculated concurrently.
- **be-ops** — service and batch operations, product configuration.
- **be-reports** — reports and statistics, snapshots in Redis.
- **be-collection** — collections: work queue, collector assignment, interaction journal.
- **be-migration** — moves products and loans from the legacy system (read-only access to the legacy DB).

## Platform services (each with its own DB)

- **be-files** — files and metadata, objects in MinIO (`files_db`).
- **be-scoring** — scoring pipelines and DMN decision tables, credit-bureau requests (`scoring_db`).
- **be-workflow** — BPMN orchestration on Flowable (`workflow_db`).
- **be-signature-gateway** — qualified e-signature and encryption gateway, proxy to the credit bureaus.

## Integration and data

- **Sync:** REST via Feign clients, service-to-service auth with `X_API_KEY` (shared `web-common` module, used by 11 services).
- **Async:** Kafka (KRaft). `exchange.*` topics between services, `inner.*` topics inside a service; producers: back office, external API, scoring, workflow.
- **Data:** 4 PostgreSQL instances + MinIO + Redis.
  - `main` — core domain DB, 38 JPA entities, Liquibase changelog owned by `core` only; used by 8 services. Areas: client, application & offers, loans & products, payments & schedule, accruals, reference data & access, credit-bureau events, collections, migration, reports, audit & events, comments.
  - `files_db`, `scoring_db`, `workflow_db` — owned by their services (Flowable creates its own schema).
- **External systems:** credit bureaus (credit histories, event reporting), e-signature provider, legacy core (read-only, migrated through `be-migration`).

## Security and access

- JWT + roles for the employee back office.
- `X_API_KEY` for service-to-service and partner calls (11 services).
- Tenant tokens for the accounting export.
- Qualified e-signature and encryption through the signature gateway.
- No service reads another service's tables — only via API or Kafka.

## Shared modules (Maven)

`core`, `kafka`, `redis`, `web-common`, credit-bureau API and upload contracts, `*-contract` + `*-client` per service.

## Build, delivery, operations

- **Build:** Maven multi-module, Java 21; images on `eclipse-temurin:21-jre`.
- **Local:** Docker Compose — 4 × PostgreSQL, Kafka KRaft, Redis, MinIO.
- **Deploy:** Helm chart + `values-<env>.yaml`, Kubernetes / k3s environments.
- **CI/CD:** k3d + Gitea + Drone, local registry, push to master.
- **Observability:** Loki · Prometheus · Grafana.
