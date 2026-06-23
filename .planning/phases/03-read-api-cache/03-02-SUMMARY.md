---
phase: 03-read-api-cache
plan: 02
subsystem: api
tags: [spring-boot, jpa, timeline, window-query, game-event, testcontainers]

# Dependency graph
requires:
  - phase: 03-read-api-cache
    provides: "03-01 ItemNotFoundException + ApiExceptionHandler 404 contract; read-only-finder pattern beside the Phase 2 insert path"
  - phase: 02-collection-pipeline
    provides: "PriceSnapshot entity + GameEvent entity (game_event table)"
provides:
  - "GET /api/items/{id}/prices?from=&to= — two-array timeline {snapshots, events} (D-04)"
  - "WindowQueryService.fetchWindow (4A shared window query) reused by Phase 5 event-impact (D-06)"
  - "GameEventRepository.findByOccurredAtBetween (inclusive occurred_at containment, D-05)"
  - "PriceSnapshotRepository.findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc (read-only window finder)"
  - "TimelineResponse / SnapshotPoint / EventPoint DTOs"
affects: [03-03-downsample-validation-health, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "4A shared window query extracted as a pure data-access service (two queries, no N+1) for cross-phase reuse"
    - "Timeline as two orthogonal arrays so downsampling can shrink snapshots without touching events"
    - "Range/window reads hit the DB directly (not cached) — only latest is cached"

key-files:
  created:
    - src/main/java/com/lostark/tracker/repository/GameEventRepository.java
    - src/main/java/com/lostark/tracker/read/WindowQueryService.java
    - src/main/java/com/lostark/tracker/web/dto/TimelineResponse.java
    - src/main/java/com/lostark/tracker/web/dto/SnapshotPoint.java
    - src/main/java/com/lostark/tracker/web/dto/EventPoint.java
    - src/main/java/com/lostark/tracker/web/PricesController.java
    - src/test/java/com/lostark/tracker/read/WindowQueryServiceIT.java
    - src/test/java/com/lostark/tracker/read/TimelinePricesIT.java
  modified:
    - src/main/java/com/lostark/tracker/repository/PriceSnapshotRepository.java

key-decisions:
  - "D-04: timeline returns two independent arrays {snapshots, events}"
  - "D-05: event overlap is inclusive containment from <= occurred_at <= to (Spring Data Between)"
  - "D-06: 4A window query extracted as WindowQueryService for Phase 5 reuse (two queries, no N+1)"
  - "D-11: from/to parsed as UTC OffsetDateTime, responses UTC ISO-8601, no KST conversion"
  - "D-13 (empty half): valid window with no data -> 200 with empty arrays; missing item -> 404"

patterns-established:
  - "Shared window query service consumed by timeline now and event-impact later"
  - "Separate PricesController so /prices file ownership is disjoint from 03-01's ItemController"

requirements-completed: [API-03]

# Metrics
duration: ~15 min
completed: 2026-06-23
---

# Phase 3 Plan 02: Timeline read + shared window query Summary

**`GET /api/items/{id}/prices?from=&to=` returns the window's snapshots and overlapping events as two UTC arrays, backed by a reusable 4A `WindowQueryService` with inclusive boundary semantics tested for Phase 5 reuse.**

## Performance

- **Duration:** ~15 min
- **Completed:** 2026-06-23
- **Tasks:** 2 (both test-backed)
- **Files modified:** 9 (8 created, 1 modified)

## Accomplishments
- `WindowQueryService.fetchWindow` (4A) — composes the snapshot window finder + `GameEventRepository.findByOccurredAtBetween` into a `WindowResult{snapshots, events}` with exactly two queries; pure data access (no existence check, no mapping) so Phase 5 reuses identical inclusive UTC boundary semantics (D-06).
- `GameEventRepository` (new) over the existing `game_event` table; read-only window finder added to `PriceSnapshotRepository` beside the Phase 2 insert-path methods (untouched).
- `PricesController` `GET /{id}/prices` — parses `from`/`to` as UTC `OffsetDateTime` (no KST shift, D-11), 404s a missing item via the shared 03-01 advice, maps to `TimelineResponse{snapshots, events}` (D-04).
- ITs prove inclusive overlap (on-`from`/on-`to` included, just-outside excluded — D-05), ascending snapshots (out-of-order inserts), KST-midnight UTC instant round-trip (off-by-9h guard), 404 missing item, and 200 + empty arrays for an empty window (D-13 empty half).

## Task Commits

1. **Task 1: 4A shared window query (repos + WindowQueryService)** - `1e8ceae` (feat)
2. **Task 2: /prices endpoint + two-array DTOs** - `a71e433` (feat)

## Files Created/Modified
- `repository/GameEventRepository.java` - new; `findByOccurredAtBetween` (inclusive)
- `read/WindowQueryService.java` - 4A shared window query, `WindowResult` record
- `web/dto/TimelineResponse.java` / `SnapshotPoint.java` / `EventPoint.java` - two-array DTOs
- `web/PricesController.java` - `GET /{id}/prices`
- `test/.../WindowQueryServiceIT.java` - boundary/ordering proof
- `test/.../TimelinePricesIT.java` - endpoint proof (two arrays, 404, empty window)
- `repository/PriceSnapshotRepository.java` - read-only window finder added

## Decisions Made
None beyond the locked CONTEXT decisions (D-04/05/06/11/13). Discretion: `WindowResult` as a nested record on the service; `PricesController` kept separate from `ItemController` per the plan.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. Full `./gradlew test -PdockerApiVersion=1.44` is green: 37 tests, 0 failures, 0 errors.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 03-03 extends THIS endpoint: it adds the 400 range-validation handlers to `ApiExceptionHandler` and the server-side downsample branch on `/prices` (shrinking `snapshots` only, events untouched — the two-array orthogonality is in place).
- `WindowQueryService` is the locked reuse point for Phase 5 event-impact.
- No blockers.

---
*Phase: 03-read-api-cache*
*Completed: 2026-06-23*
