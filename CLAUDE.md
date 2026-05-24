# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a community hospital management system (社区医院管理系统) **full-stack rewrite**. The goal is to migrate a legacy Spring Boot 2 + Vue 2 + MySQL monolith into a modern Spring Boot 3 + Next.js 15 + PostgreSQL architecture.

- `server/legacy/` — archived, read-only reference. **Never modify.**
- `server/backend-rewrite/` — the **only active backend**. All development, CI, and IDE run configs must target this directory.
- `web/` — independent Next.js 15 frontend.
- `backend-rewrite_deprecated_20260520/` (root) — historical snapshot, not used in any build or runtime.

## Development Setup

**Prerequisites:** Docker (for PostgreSQL), Java 21, Maven, Node.js / pnpm.

```bash
# 1. Start database
docker compose up -d postgres
# PostgreSQL on localhost:5432, db=hospital, user=hospital, password=hospital

# 2. Backend (port 8080)
cd server/backend-rewrite && mvn spring-boot:run

# 3. Frontend (port 3000)
cd web && npm install && npm run dev
# Set web/.env.local: NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## Commands

### Backend (`server/backend-rewrite/`)

```bash
mvn spring-boot:run          # run
mvn test                     # all tests (uses TestContainers, no local DB needed)
mvn test -Dtest=ClassName    # single test class
mvn package -DskipTests      # build JAR
```

Integration tests spin up a real PostgreSQL container via TestContainers — the local docker-compose DB is not required for tests.

### Frontend (`web/`)

```bash
npm run dev    # dev server
npm run build  # production build
npm run lint   # ESLint
```

## Architecture

### Backend Structure

Each business module under `com.hospital.<module>/` follows this layout:

```
controller/   REST endpoints
service/      Service interface + DefaultXxxService implementation
repository/   XxxRepository interface + InMemoryXxxRepository (stub) or JPA impl
domain/       Domain objects and status enums
dto/          Request/response DTOs
```

**Auth infrastructure** (`com.hospital.auth`) is fully JPA-backed. All other business modules currently use `InMemoryXxxRepository` stubs. The next rewrite step for any module is: write a Flyway migration to create the table → implement a JPA repository → replace the in-memory stub.

Cross-cutting:
- `com.hospital.config.SecurityConfig` — Spring Security + JWT filter chain
- `com.hospital.auth.security.JwtService` / `JwtAuthenticationFilter` — JWT issuance and validation
- `com.hospital.common.ApiExceptionHandler` — global `@ControllerAdvice` error handling, returns `ApiErrorResponse`
- `com.hospital.observability` — `TraceIdFilter` injects `X-Trace-Id` on every request; `ApiMetricsFilter` records timing
- `com.hospital.audit.AuditService` — used by Dictionary module to log mutations

All APIs are under `/api/v1/*`. The `application.yml` datasource points to `localhost:5432/hospital`.

### Database Migrations (Flyway)

Scripts live in `server/backend-rewrite/src/main/resources/db/migration/` as `V{n}__{description}.sql`.

**Rules:**
- Never modify a published migration script. Add a new `Vn+1__*.sql` instead.
- RBAC migrations follow a three-layer discipline tracked in `docs/migration-status.md`:
  - **L1** — base entity structure (`app_user`, `app_role`)
  - **L2** — RBAC structure (`app_permission`, `app_role_permission`)
  - **L3** — data patches / naming alignment (must run after L2)
- Current versions: V1–V45. V4 and V44 are no-op markers; V13 holds the real config-permission alignment, V45 drops the obsolete `patient_knowledge_chunk` table after the storage move to Qdrant. V37–V45 are AI module migrations (see `docs/ai-features-plan.md`).

### AI Module (`com.hospital.ai.*`)

Vision OCR + per-patient RAG + community chat, all via 火山引擎方舟 Doubao (OpenAI-compatible).

- Requires env var `HOSPITAL_AI_API_KEY` at runtime. Without it, set `hospital.ai.enabled=false` to skip AI bean wiring.
- **Patient RAG vector storage uses Qdrant** (`qdrant/qdrant`, gRPC on 6334) — see `com.hospital.ai.qdrant.*`. `docker-compose up -d qdrant` brings it up; collection `patient_knowledge` is created automatically on boot. The pgvector image is still used for Postgres but only because the extension is installed; no AI data lives in Postgres anymore.
- Single entry point for all upstream calls: `com.hospital.ai.client.AiCallTemplate.chat()` / `chatStream()`. **Always force `temperature=1`** (Volcano rejects other values). Audit + rate limit are applied via `AiAuditInterceptor` (unary) and inside `AiCallTemplate` (streaming).
- Permission codes: `ai:vision`, `ai:patient-rag`, `ai:consult`, `ai:admin`.
- Privacy: every Qdrant search goes through `PatientKnowledgeStore.search` which forces a `match(patient_id, ?)` MUST filter — there is no overload that bypasses it. Patient RAG also requires `patient_profile.ai_consent_at` (HTTP 412 otherwise).
- Detailed plan: `docs/ai-features-plan.md`. User docs: `docs/ai-features.md`.

### Frontend Structure

```
web/
├── app/               Next.js App Router pages (one directory per module)
├── components/
│   ├── ui/            Generic primitives (button, input, card, data-table, file-upload)
│   ├── layout/        auth-layout.tsx, app-shell.tsx
│   └── business/      entity-management-page.tsx + forms/
└── lib/               api.ts, api-contract.ts, auth.ts, permissions.ts, token-storage.ts
```

Most CRUD pages are thin wrappers over `EntityManagementPage` (in `components/business/`), which accepts a config object and handles list, pagination, create/edit/delete, status transitions, and permission-based rendering.

`lib/api-contract.ts` is the single source of truth for API types shared between pages and the API client. `lib/permissions.ts` handles RBAC-based UI visibility.

**File naming:** `web/components/ui/` and `web/components/layout/` use **kebab-case**. Import paths must match case exactly.

## Rewrite Status

Tracked in `docs/migration-status.md` (single source of truth — do not duplicate status elsewhere).

| Module | Legacy name | Status | Backend persistence |
|--------|-------------|--------|---------------------|
| Auth | yonghu | **Done** | JPA |
| Health check | — | **Done** | — |
| Dictionary (字典) | dictionary | **Done** | JPA |
| Medications (药品) | yaopin | Partial | In-memory stub |
| Family Doctors (家庭医生) | jiatingyisheng | Partial | In-memory stub |
| Doctors (医生) | yisheng | Partial | In-memory stub |
| Visits (就诊) | jiuzhen | Partial | In-memory stub |
| System Config (配置) | peizhi | Partial | In-memory stub |
| Medical Records (病例) | bingli | Partial | In-memory stub |
| Health Records (健康档案) | jiuankangdangan | Partial | In-memory stub |
| Qiantai (前台) | qiantai | **Not started** | — |
| Patients (用户) | yonghu | **Not started** | — |

When adding a new module: check if the corresponding legacy controller in `server/legacy/src/main/java/com/jlwl/` exists for behavior reference. Legacy uses MyBatis + MySQL; the rewrite uses JPA + PostgreSQL.

## Key Constraints

- **Never push to or run from** `backend-rewrite_deprecated_20260520/` or `server/legacy/`.
- **Never modify existing Flyway scripts.** Always add a new version.
- JWT secret in `application.yml` is a placeholder; production must use a real 32-byte minimum key.
- Database column/table renaming follows a two-phase strategy: first map old names via JPA `@Column`/`@Table` annotations, then rename in a new Flyway script — never in the same change.
- Integration tests require Docker for TestContainers. The `application-test.yml` profile is activated automatically by `@SpringBootTest` annotations in the test classes.
