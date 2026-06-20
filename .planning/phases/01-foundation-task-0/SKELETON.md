# Walking Skeleton — 로스트아크 거래소 시세 수집·분석 파이프라인 (Lostark Price Tracker)

**Phase:** 1
**Generated:** 2026-06-20

## Capability Proven End-to-End

A caller can `docker-compose up` (Postgres + Redis), start the Spring Boot app (which connects to both and applies the Flyway schema under `ddl-auto=validate`), then insert and read back a `tracked_item` and its `price_snapshot` through the live PostgreSQL — proving `TIMESTAMPTZ` round-trips as a UTC instant and the `price_snapshot UNIQUE(tracked_item_id, collected_at)` constraint is enforced at the DB level.

This is backend-only (no frontend — the UI gate did not fire). "One real UI interaction" is mapped to one real HTTP/repository round-trip that writes to and reads back from the live DB.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build & language | Gradle (Groovy DSL) + Java 21 (LTS) | D-01. Modern Spring portfolio standard; Java 21 LTS. |
| Framework | Spring Boot 3.4.x | D-01. Requires RestClient (Spring 6.1+), Flyway, Spring Data JPA, Actuator, Testcontainers BOM. |
| Schema ownership | Flyway versioned SQL migrations own the schema; JPA `spring.jpa.hibernate.ddl-auto=validate` | D-02. Schema is hand-managed with discipline; JPA only validates entity↔schema agreement, never creates/alters. |
| Persistence | Spring Data JPA repositories over PostgreSQL | Shared persistence module reused by Phases 2/3/4/5. |
| HTTP client | `RestClient` (Spring 6.1+, synchronous) | D-03. Collection runs on an `@Async` pool; a synchronous client fits more simply. WebClient/RestTemplate excluded. |
| DB engine | PostgreSQL (NOT MySQL) | CLAUDE.md constraint. No MySQL-only SQL/types. All time columns `TIMESTAMPTZ` (UTC). |
| Dev DB environment | docker-compose `postgres` container | No local install; dev DB is the compose container. |
| Test DB environment | Testcontainers PostgreSQL (+ Testcontainers Redis) | Local and CI use the identical mechanism; unit + integration verified on the same engine. |
| Cache / rate-limit backend | Redis (via docker-compose dev / Testcontainers test) | Phase 2 token bucket + Phase 3 cache-aside build on this. Phase 1 only proves connectivity. |
| Secrets handling | Lostark API JWT key sourced from env var only; never committed; DB/Redis credentials parameterized in compose, not hardcoded in source | Security threat model (ASVS L1). Task 0 spike key gated to `spike` profile, `@Disabled` in CI. |

## Stack Touched in Phase 1

- [ ] Project scaffold — Gradle Groovy DSL, Java 21, Spring Boot 3.4.x; build + lint + test runner (JUnit 5)
- [ ] Routing — Spring Boot Actuator `/actuator/health` (real route proving the app is up and connected)
- [ ] Database — Flyway applies 4 tables; one real write AND one real read-back through the JPA persistence layer on live PostgreSQL
- [ ] Cache infra — Redis container up; app connects (health reflects Redis liveness)
- [ ] Local full-stack run — documented `docker-compose up` command that brings up Postgres + Redis, app starts under `ddl-auto=validate`, Flyway applies the 4 tables

## Shared Persistence Module Contract (reused by later phases)

The package/module layout established here is a contract for Phases 2–5:

- **Package root:** `com.lostark.tracker`
- **Domain/persistence package:** `com.lostark.tracker.domain` (entities) + `com.lostark.tracker.repository` (Spring Data JPA repositories)
- **Entities (locked names/columns):**
  - `tracked_item` — `id`, `external_item_id`, `display_name`, `category`, `active`, `created_at` (TIMESTAMPTZ)
  - `price_snapshot` — `id`, `tracked_item_id` (FK), `collected_at` (TIMESTAMPTZ, tick-normalized), `min_price`, `fetched_at` (TIMESTAMPTZ); `UNIQUE(tracked_item_id, collected_at)`. `avg_price`/`trade_count` are **conditional on Task 0** (D-06) — added only if the live API provides them.
  - `game_event` — `id`, `event_type` (LOA_ON/MAJOR_UPDATE/SEASON_END/BALANCE_PATCH), `title`, `occurred_at` (TIMESTAMPTZ single instant), `description`, `created_at`, `updated_at` (TIMESTAMPTZ)
  - `collection_run` — `id`, `started_at`, `finished_at` (TIMESTAMPTZ), `items_attempted`, `items_succeeded`, `items_failed`, `status`
- **Schema rule:** every schema change is a new Flyway `V{n}__*.sql` file. Entities must match; JPA only validates.
- **Time rule:** all instants stored/compared as UTC; `TIMESTAMPTZ` columns; KST conversion happens only at the response/display layer (later phases).

## Out of Scope (Deferred to Later Slices)

These are explicitly NOT in the skeleton — this list prevents later phases from re-litigating Phase 1's minimalism:

- Scheduled collection, `@Async` fan-out, token bucket, 429/401 handling → Phase 2
- `LostarkApiClient` production wiring (Phase 1 only does the Task 0 read-only spike) → Phase 2
- Redis cache-aside / latest cache / invalidation → Phase 3
- Timeline / downsample / range queries / health-collection endpoint → Phase 3
- Admin CRUD + Spring Security secret gate → Phase 4
- event-impact metrics → Phase 5
- CI (GitHub Actions), seed/demo data, README demo surface → Phase 6
- `avg_price` / `trade_count` as core model columns → only after Task 0 confirms (D-06)
- `source` column (auctions), period-type events (start/end), retention/rollup/partitioning → v2

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2 (Collection Pipeline): reliable 10-min collector — RestClient JWT calls, Redis token bucket, parallel fan-out + timeouts, 429/401 handling, `collection_run` recording.
- Phase 3 (Read API + Cache): `/api/items`, latest (Redis cache-aside), timeline (snapshots + overlapping events), large-range downsample, `/api/health/collection`.
- Phase 4 (Admin + Events): event/item CRUD behind a shared-secret Spring Security gate on `/api/admin/**`.
- Phase 5 (Event Impact, gate-conditional): per-event ±N-hour change-rate with sufficiency + staleness guards.
- Phase 6 (Distribution + Docs): GitHub Actions CI (Testcontainers), isolated seed profile, README demo surface.
