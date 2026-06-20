---
phase: 01-foundation-task-0
plan: 02
subsystem: database
tags: [flyway, jpa, postgres, hibernate-validate, timestamptz, testcontainers, rest]

# Dependency graph
requires:
  - phase: 01-01
    provides: Spring Boot skeleton, ddl-auto=validate config, PostgresRedisContainers test base, docker-compose
provides:
  - Flyway V1 schema for the 4 locked tables (tracked_item, price_snapshot, game_event, collection_run)
  - JPA entities + EventType enum mirroring the schema (validated on boot)
  - TrackedItem/PriceSnapshot/CollectionRun repositories
  - POST/GET /api/items HTTP round-trip through live Postgres
  - SchemaRoundTripIT proving UNIQUE idempotency, TIMESTAMPTZ UTC, item identity, collection_run
affects: [02-collection-pipeline, 03-read-api-cache, 04-admin-events, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns: [flyway-versioned-ddl, jpa-validate-mirrors-flyway, OffsetDateTime<->TIMESTAMPTZ-UTC, dto-bound-controller]

key-files:
  created:
    - src/main/resources/db/migration/V1__init_schema.sql
    - src/main/java/com/lostark/tracker/domain/{TrackedItem,PriceSnapshot,GameEvent,CollectionRun,EventType}.java
    - src/main/java/com/lostark/tracker/repository/{TrackedItem,PriceSnapshot,CollectionRun}Repository.java
    - src/main/java/com/lostark/tracker/web/ItemController.java
    - src/main/java/com/lostark/tracker/web/dto/{TrackedItemRequest,TrackedItemResponse}.java
    - src/test/java/com/lostark/tracker/SchemaRoundTripIT.java
  modified: []

key-decisions:
  - "TIMESTAMPTZ columns mapped to java.time.OffsetDateTime; hibernate.jdbc.time_zone=UTC for deterministic round-trip"
  - "price_snapshot.tracked_item_id mapped as @ManyToOne(LAZY); UNIQUE declared on @Table for validate agreement"
  - "Added CollectionRunRepository (beyond the plan's file list) — needed to prove DATA-04"
  - "JSON contract is camelCase (Spring/Jackson default) for this skeleton round-trip"

patterns-established:
  - "Every schema change is a new Flyway V{n} file; entities mirror it and JPA only validates (D-02)"
  - "Controllers bind to validated DTOs, never to entities (no mass-assignment)"
  - "Integration tests extend PostgresRedisContainers; instants compared via toInstant() to assert UTC equality"

requirements-completed: [DATA-01, DATA-02, DATA-03, DATA-04]

# Metrics
duration: ~12min
completed: 2026-06-20
---

# Phase 01 / Plan 02: Locked Data Model Summary

**Flyway-owned 4-table schema with JPA entities validated on boot, plus a real HTTP /api/items round-trip and a Testcontainers IT proving UNIQUE idempotency and TIMESTAMPTZ UTC behavior.**

## Performance

- **Duration:** ~12 min
- **Completed:** 2026-06-20
- **Tasks:** 2
- **Files created:** 12

## Accomplishments
- `V1__init_schema.sql` locks the 4 tables with PostgreSQL types only, 8 TIMESTAMPTZ columns, and `UNIQUE(tracked_item_id, collected_at)` (which doubles as the range-query index)
- JPA entities + `EventType` enum mirror the schema; app boots clean under `ddl-auto=validate`
- `POST/GET /api/items` round-trips a tracked_item through live Postgres
- `SchemaRoundTripIT` proves DATA-01 (UNIQUE violation), DATA-02 (UTC round-trip at a KST boundary), DATA-03 (HTTP item identity), DATA-04 (collection_run counts)
- `avg_price` / `trade_count` intentionally absent (D-06) — gated by the 01-03 Task 0 spike

## Task Commits

1. **Task 1: Flyway V1 + JPA entities + repositories (validate)** — `9303a30` (feat)
2. **Task 2: /api/items round-trip + SchemaRoundTripIT** — `efbf069` (feat)

## Files Created/Modified
- `src/main/resources/db/migration/V1__init_schema.sql` — 4-table DDL, UNIQUE, TIMESTAMPTZ
- `domain/{TrackedItem,PriceSnapshot,GameEvent,CollectionRun,EventType}.java` — entities + enum
- `repository/{TrackedItem,PriceSnapshot,CollectionRun}Repository.java` — Spring Data JPA
- `web/ItemController.java` + `web/dto/{TrackedItemRequest,TrackedItemResponse}.java` — HTTP slice
- `test/.../SchemaRoundTripIT.java` — DATA-01..04 integration proof

## Decisions Made
- **OffsetDateTime + `hibernate.jdbc.time_zone=UTC`** (set in 01-01) for deterministic TIMESTAMPTZ round-trips; tests compare `toInstant()`.
- **@ManyToOne(LAZY)** for `price_snapshot.tracked_item_id`; `@Table(uniqueConstraints=...)` mirrors the DB UNIQUE so `validate` agrees.
- **camelCase JSON** for the skeleton round-trip (a snake_case external contract, if wanted, is a Phase 3 read-API concern).

## Deviations from Plan

### Auto-fixed Issues

**1. [Missing critical] Added CollectionRunRepository**
- **Found during:** Task 2 — DATA-04 requires persisting/reading a `collection_run`, but the plan's file list named only TrackedItem/PriceSnapshot repositories.
- **Fix:** Added `CollectionRunRepository extends JpaRepository<CollectionRun, Long>` following the established repository pattern.
- **Verification:** `collectionRunRecordsStartFinishAndCounts` test passes.
- **Committed in:** `9303a30`

---
**Total deviations:** 1 (necessary to satisfy DATA-04). No scope creep.

## Issues Encountered
None — the 01-01 `api.version=1.44` fix carried over, so all Testcontainers runs connected cleanly.

## User Setup Required
None.

## Next Phase Readiness
- The shared persistence module is locked and validated; ready for **01-03** (Task 0 API spike) to confirm `avg_price`/`trade_count` availability and the item-matching rule against the real API, then ratify the model lock.
- `ddl-auto=validate` means any future entity/schema drift fails fast on boot — the discipline signal the project wants to show.

---
*Phase: 01-foundation-task-0*
*Completed: 2026-06-20*
