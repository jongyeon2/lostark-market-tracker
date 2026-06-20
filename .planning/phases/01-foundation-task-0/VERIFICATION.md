# Phase 1 Verification — Foundation + Task 0

**Verified:** 2026-06-20
**Method:** Goal-backward check of ROADMAP Phase 1 success criteria against the implemented codebase + a clean `./gradlew clean build` (compile + Testcontainers integration tests + package).
**Build evidence:** `./gradlew clean build` → **BUILD SUCCESSFUL** (SmokeContextTest + SchemaRoundTripIT pass on real Postgres 16 + Redis 7; MarketsApiSpikeTest `@Disabled`, no network).

## Success Criteria

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| 1 | `docker-compose up` → Postgres + Redis up, app connects, DDL creates the 4 tables | ✅ PASS | `docker-compose.yml` (postgres:16 + redis:7, healthchecks); `V1__init_schema.sql` creates tracked_item / price_snapshot / game_event / collection_run; `SmokeContextTest` boots the full context against Testcontainers with Flyway applied under `ddl-auto=validate` |
| 2 | price_snapshot `UNIQUE(tracked_item_id, collected_at)` enforced at DB level | ✅ PASS | V1 `CONSTRAINT uq_price_snapshot_item_time UNIQUE (tracked_item_id, collected_at)`; `SchemaRoundTripIT.duplicateSnapshotViolatesUniqueConstraint` asserts `DataIntegrityViolationException` on duplicate insert |
| 3 | All timestamp columns `TIMESTAMPTZ`, stored/read as UTC instants | ✅ PASS | V1 has 8 `TIMESTAMPTZ` columns; entities map them to `OffsetDateTime` with `hibernate.jdbc.time_zone=UTC`; `SchemaRoundTripIT.timestamptzRoundTripsAsUtcInstant` round-trips a KST-midnight-boundary instant equal in UTC |
| 4 | Task 0 spike confirms avg_price/trade_count availability + documents matching rule | ✅ PASS | `TASK0-FINDINGS.md` (field-availability matrix; D-06: avg_price/trade_count provided DAILY; D-05: external_item_id = API `Id` + display_name; rate limit 100/min; exit-gate PASS); ratified at the blocking human checkpoint; design doc LOCKED |
| 5 | tracked_item insert/lookup by external_item_id + display_name, unambiguous | ✅ PASS | `ItemController` POST/GET `/api/items`; `TrackedItemRepository.findByExternalItemId`; `SchemaRoundTripIT.itemInsertedAndReadBackViaHttp` (real HTTP round-trip echoes external_item_id + display_name) |

**Requirements covered:** DATA-01 (UNIQUE idempotency), DATA-02 (TIMESTAMPTZ UTC), DATA-03 (matching rule + item identity), DATA-04 (collection_run), DIST-01 (docker-compose local reproduction).

## Bonus / hardening delivered
- Shared `PostgresRedisContainers` Testcontainers base — the persistence/test contract reused by Phases 2–5.
- Docker Engine 29.x compatibility resolved (`api.version=1.44` pin) — portable to Linux CI.
- JWT key hardening in the spike client (strips bearer prefix + paste-injected whitespace).

## Carry-forward to Phase 2
- `V2__add_price_metrics.sql` to add `avg_price` (from `YDayAvgPrice`).
- Collect `CurrentMinPrice` → `price_snapshot.min_price` per tick; honor 100/min via token bucket using `x-ratelimit-*` + `Retry-After`.
- `trade_count` remains out of the per-tick model (daily-only) — optional v2 daily-stats table.

## Verdict

**✅ PHASE 1 PASS** — all 5 success criteria met; build + integration tests green; data model locked on measured reality.

---
*Phase: 01-foundation-task-0 · Verified 2026-06-20*
