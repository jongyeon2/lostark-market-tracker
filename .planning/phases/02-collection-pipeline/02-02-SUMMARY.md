---
phase: 02-collection-pipeline
plan: 02
subsystem: api
tags: [spring-scheduling, async, completablefuture, threadpooltaskexecutor, jpa, idempotency, testcontainers, mockitobean]

# Dependency graph
requires:
  - phase: 02-01
    provides: LostarkApiClient (typed errors), RedisTokenBucket, MarketItem/MarketItemsResponse DTOs
  - phase: 01-foundation-task-0
    provides: TrackedItem/PriceSnapshot/CollectionRun entities + repos, UNIQUE(tracked_item_id, collected_at), PostgresRedisContainers
provides:
  - "@Scheduled fixedDelay collection tick (no overlap) with parallel @Async fan-out + allOf().join under per-call + overall timeouts (D-07)"
  - Tick-normalized collected_at (run-start UTC truncated to minute) shared by all snapshots in a tick (D-15)
  - Idempotent snapshot persistence via UNIQUE + existence guard (D-16)
  - collection_run RUNNING->terminal lifecycle with count-based SUCCESS/PARTIAL_SUCCESS/FAILED (D-12)
  - WatchlistSeeder (idempotent, dev/seed profile) + ItemFetchService (@Async) + ItemFetchResult
affects: [02-03, 03-read-api-cache]

# Tech tracking
tech-stack:
  added: []
  patterns: [scheduled-fixeddelay-tick, async-fanout-with-orTimeout+allOf, persist-after-join-success-only, injected-clock-for-deterministic-collected_at, derived-exists-idempotency-guard]

key-files:
  created:
    - src/main/java/com/lostark/tracker/collect/{CollectionConfig,WatchlistSeeder,PriceCollector,ItemFetchService,ItemFetchResult}.java
    - src/test/java/com/lostark/tracker/collect/{TickInstantNormalizationTest,PriceCollectionIT}.java
  modified:
    - src/main/java/com/lostark/tracker/domain/CollectionRun.java
    - src/main/java/com/lostark/tracker/repository/{TrackedItemRepository,PriceSnapshotRepository}.java
    - src/main/resources/application-test.yml

key-decisions:
  - "Persist AFTER the join, in the collector thread, for SUCCESS results only — deliberate deviation from D-06 (async-task-persists) to eliminate a late-write bug where a task finishing after the timeout would resurrect a snapshot the run already counted as failed"
  - "Per-call bound via CompletableFuture.orTimeout(perCall) AND overall allOf().get(overall) backstop (D-07)"
  - "collected_at = run-start instant in UTC truncated to the minute (D-15); injected Clock makes it deterministic in tests"
  - "collection_run lifecycle as two writes: RUNNING at start, finish() mutator sets finishedAt+counts+status (D-12)"
  - "WatchlistSeeder gated to dev/seed profiles per user instruction (inactive in test); idempotent upsert by external_item_id"
  - "TrackedItem.category carries the numeric leaf CategoryCode so the per-item ItemName call works (D-05)"

patterns-established:
  - "Async fan-out: fetch returns a DTO future, collector persists successes after allOf().join — keeps a slow item from blocking or corrupting others"
  - "Idempotency proven via existsByTrackedItem_IdAndCollectedAt + UNIQUE catch (skip, not fail)"
  - "Scheduled bean kept from auto-firing in ITs via collection.initial-delay-ms; tests drive collectTick() with a pinned clock"

requirements-completed: [COLL-01, COLL-02]

# Metrics
duration: ~30min
completed: 2026-06-22
---

# Phase 02 / Plan 02: Scheduled Collector + Parallel Fan-out Summary

**A @Scheduled fixedDelay tick that fans out one @Async price fetch per active item, joins them under per-call + overall timeouts, writes one idempotent snapshot per success sharing a tick-normalized collected_at, and records a collection_run with count-based status.**

## Performance

- **Duration:** ~30 min
- **Completed:** 2026-06-22
- **Tasks:** 2
- **Files created:** 7 (5 modified)
- **Tests:** full suite 19 passed / 0 failed / 1 skipped (the @Disabled spike)

## Accomplishments
- `PriceCollector.collectTick()` — `@Scheduled(fixedDelay)` (no overlapping ticks); fan-out via `@Async` + `allOf().get(overall)`, each future bounded by `orTimeout(perCall)` (D-07); a hanging item cannot stall the tick
- Tick-normalized `collected_at` (run-start UTC truncated to minute) shared by every snapshot in the tick (D-15); UNIQUE + existence guard make re-runs idempotent (D-16, Success Criterion 5)
- `collection_run` RUNNING→terminal lifecycle with count-based SUCCESS/PARTIAL_SUCCESS/FAILED (D-12, Success Criterion 1)
- `ItemFetchService` (@Async) resolves each item by `Id == externalItemId` (D-05) and returns a DTO; `WatchlistSeeder` seeds the curated list idempotently (dev/seed profile)
- `PriceCollectionIT` proves shared collected_at, idempotent re-run, hanging-item isolation (Success Criterion 4), and the recorded run; `TickInstantNormalizationTest` proves the minute-truncation/UTC rule

## Task Commits

1. **Task 1: scheduling/async/executor config + idempotent watchlist seeder** — `5fb514e` (feat)
2. **Task 2: PriceCollector fan-out + timeouts + idempotent persist + collection_run** — `f49313a` (feat, TDD)

## Files Created/Modified
- `collect/PriceCollector.java` — the scheduled tick; fan-out, timeouts, persist-after-join, run lifecycle
- `collect/ItemFetchService.java` — @Async per-item fetch (HTTP only, returns ItemFetchResult)
- `collect/ItemFetchResult.java` — success/failed result carrying minPrice/fetchedAt or a categorical reason
- `collect/CollectionConfig.java` — @EnableScheduling/@EnableAsync + bounded executor + UTC Clock bean
- `collect/WatchlistSeeder.java` — idempotent dev/seed-profile seeder
- `domain/CollectionRun.java` — added `finish(...)` lifecycle mutator
- `repository/TrackedItemRepository.java` — `findByActiveTrue()`; `PriceSnapshotRepository.java` — `existsByTrackedItem_IdAndCollectedAt(...)`
- `application-test.yml` — `collection.initial-delay-ms` to stop scheduler auto-fire in ITs

## Decisions Made
- **Persist-after-join (deviation from D-06):** the `@Async` fetch performs HTTP only and returns a DTO; the collector persists SUCCESS results after the join. This honors the user's "only successful items persist" rule and removes a late-write correctness bug (a task finishing after the overall timeout cannot resurrect a snapshot already counted failed). Entity IDs still cross the async boundary; no lazy-init issue arises because the async path touches no JPA.
- **Two-layer timeout (D-07):** per-call `orTimeout` bounds each item; overall `allOf().get` is the backstop — both below the 10-min tick interval.
- **Deterministic collected_at:** an injected `Clock` lets the IT pin `collected_at`, making the idempotent-re-run assertion deterministic across minute boundaries.

## Deviations from Plan

### Auto-fixed Issues

**1. [Necessary] Modified CollectionRun + TrackedItemRepository (not in plan's files_modified)**
- **Found during:** Task 2 — the run lifecycle needs a terminal mutator and the collector needs active-item lookup.
- **Fix:** Added `CollectionRun.finish(...)` and `TrackedItemRepository.findByActiveTrue()`; both follow existing patterns.
- **Verification:** PriceCollectionIT asserts the finalized run counts/status and that only active items are polled.
- **Committed in:** `f49313a`

**2. [Design improvement] Persist-after-join instead of async-task-persist (D-06 literal)**
- **Found during:** Task 2 — async-task-persist would let a timed-out task write a late snapshot.
- **Fix:** Moved persistence to the collector thread, SUCCESS-only, after the join (documented above).
- **Verification:** hanging-item IT proves item B writes no snapshot while A/C do.
- **Committed in:** `f49313a`

---
**Total deviations:** 2 (1 necessary, 1 correctness-improving). No scope creep.

## Issues Encountered
- Lazy `PriceSnapshot.trackedItem` would throw if navigated outside a tx in the IT — switched the hanging-item assertion to `existsByTrackedItem_IdAndCollectedAt(...)` (no lazy navigation).

## User Setup Required
None for tests. A real run uses the `dev` (or `seed`) profile so the watchlist seeds; set `LOSTARK_API_KEY`.

## Next Phase Readiness
- 02-03 wraps `ItemFetchService` with the bounded retry (429 Retry-After + 5xx) and the fatal-auth (401/403) convergence, refines the `collection_run` status vocabulary, and adds the `summary_message` marker column via Flyway **V2** (`V2__add_collection_run_summary.sql` per the user's migration-numbering rule).
- The collector already counts failures and finalizes PARTIAL_SUCCESS/FAILED; 02-03 layers retry + markers on top without changing the fan-out contract.

---
*Phase: 02-collection-pipeline*
*Completed: 2026-06-22*
