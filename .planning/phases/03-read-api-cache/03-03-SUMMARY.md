---
phase: 03-read-api-cache
plan: 03
subsystem: api
tags: [spring-boot, jpa, postgresql, date-trunc, downsample, validation, health, testcontainers]

# Dependency graph
requires:
  - phase: 03-read-api-cache
    provides: "03-02 PricesController + TimelineResponse + WindowQueryService; 03-01 ApiExceptionHandler + ItemNotFoundException"
  - phase: 02-collection-pipeline
    provides: "collection_run rows (started/finished/counts/status/summary_message marker)"
provides:
  - "Server-side downsampling on /prices: date_trunc avg(min_price) buckets when raw count > N (~500) (API-04)"
  - "Input-validation contract: from>to/window<=0 -> 400, missing item -> 404, empty range -> 200 empty (API-05)"
  - "GET /api/health/collection — latest collection_run snapshot, no secret leak (OPS-01)"
  - "DownsampleService + PriceBucketView native date_trunc projection"
  - "ApiExceptionHandler extended with 400 handlers (InvalidRequestException + bad-param)"
affects: [05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Native PostgreSQL date_trunc aggregate via Spring Data interface projection (Instant getter, server re-offsets to UTC)"
    - "Server-chosen bucket-unit whitelist (hour/day) bound as a parameter — no SQL injection surface"
    - "Validation-before-existence ordering (400 then 404) and a single ApiErrorResponse helper"
    - "Ops health endpoint exposing counts/markers only — no secret field exists on the path"

key-files:
  created:
    - src/main/java/com/lostark/tracker/repository/PriceBucketView.java
    - src/main/java/com/lostark/tracker/read/DownsampleService.java
    - src/main/java/com/lostark/tracker/web/dto/PricePoint.java
    - src/main/java/com/lostark/tracker/web/error/InvalidRequestException.java
    - src/main/java/com/lostark/tracker/health/CollectionHealthService.java
    - src/main/java/com/lostark/tracker/web/HealthController.java
    - src/main/java/com/lostark/tracker/web/dto/CollectionHealthResponse.java
    - src/test/java/com/lostark/tracker/read/DownsamplePricesIT.java
    - src/test/java/com/lostark/tracker/read/InputValidationIT.java
    - src/test/java/com/lostark/tracker/health/CollectionHealthIT.java
  modified:
    - src/main/java/com/lostark/tracker/repository/PriceSnapshotRepository.java
    - src/main/java/com/lostark/tracker/web/dto/TimelineResponse.java
    - src/main/java/com/lostark/tracker/web/PricesController.java
    - src/main/java/com/lostark/tracker/web/error/ApiExceptionHandler.java
    - src/main/java/com/lostark/tracker/repository/CollectionRunRepository.java

key-decisions:
  - "D-07: avg(min_price) buckets aggregated in PostgreSQL via date_trunc (not Java)"
  - "D-08: auto-downsample when raw count > N; response carries downsampled + bucketWidth meta; client passes no param"
  - "D-09: N≈500 (TARGET_MAX_POINTS); bucket unit hour while span<=N hours, else day"
  - "D-13: from>to / window<=0 -> 400, missing item -> 404, valid-but-empty -> 200 empty"
  - "D-10: 400 handlers extend the 03-01 advice; same {timestamp,status,error,message} body"
  - "D-12: health surfaces lastRunAt/started/counts/status/marker only; no key/auth field; no-runs -> status NO_RUNS"

patterns-established:
  - "date_trunc native projection returns Instant (Hibernate), re-offset to UTC in the service"
  - "Events never downsampled — only the snapshots array shrinks (D-04 orthogonality)"

requirements-completed: [API-04, API-05, OPS-01]

# Metrics
duration: ~30 min
completed: 2026-06-23
---

# Phase 3 Plan 03: Downsampling + validation + health Summary

**`/prices` auto-downsamples large ranges to PostgreSQL `date_trunc` avg buckets, the full input-validation contract (400/404/200-empty) extends the custom advice, and `GET /api/health/collection` surfaces the latest collection run without leaking a secret.**

## Performance

- **Duration:** ~30 min
- **Completed:** 2026-06-23
- **Tasks:** 3 (all test-backed)
- **Files modified:** 15 (10 created, 5 modified)

## Accomplishments
- Server-side downsampling: a native `date_trunc` aggregate (`avg(min_price)` + `count(*)` per bucket) behind `DownsampleService`; auto-triggers when raw count > N≈500, picks hour/day by span, returns `downsampled`/`bucketWidth` meta; events untouched (D-07/08/09).
- Input-validation contract: `from>to`/`window<=0` → 400 (`InvalidRequestException`), malformed/missing param → 400, missing item → 404, valid-but-empty → 200 empty — all via the extended `@RestControllerAdvice` `{timestamp,status,error,message}` body (D-10/D-13).
- `GET /api/health/collection`: latest `collection_run` snapshot (lastRunAt/startedAt/counts/status/marker), `NO_RUNS` when empty, and proven to leak no key/Authorization/bearer/token (D-12).

## Task Commits

1. **Task 1: Server-side downsample (date_trunc + DownsampleService)** - `28d2532` (feat)
2. **Task 2: Input-validation contract (400 advice extension)** - `c340487` (feat)
3. **Task 3: Collection health endpoint** - `8c548d4` (feat)

## Files Created/Modified
- `repository/PriceBucketView.java` + `PriceSnapshotRepository` native `date_trunc` aggregate
- `read/DownsampleService.java` - raw-vs-bucket decision + unit choice
- `web/dto/PricePoint.java` + extended `TimelineResponse` (downsampled + bucketWidth)
- `web/PricesController.java` - downsample wiring + range validation
- `web/error/InvalidRequestException.java` + `ApiExceptionHandler` 400 handlers
- `health/CollectionHealthService.java` + `web/HealthController.java` + `web/dto/CollectionHealthResponse.java` + `CollectionRunRepository` run finder
- `test/.../DownsamplePricesIT.java`, `InputValidationIT.java`, `health/CollectionHealthIT.java`

## Decisions Made
None beyond the locked CONTEXT decisions (D-07/08/09/10/12/13). Discretion: `TARGET_MAX_POINTS=500`; unit rule `span<=500h -> hour else day`; no-runs returns `status "NO_RUNS"`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Native `date_trunc` projection type mismatch**
- **Found during:** Task 1 (DownsamplePricesIT — large range returned 500)
- **Issue:** Hibernate returns the native `timestamptz` column as `java.time.Instant`; the projection getter declared `OffsetDateTime`, and Spring's interface projection had no `Instant`→`OffsetDateTime` converter (`UnsupportedOperationException`).
- **Fix:** `PriceBucketView.getBucketStart()` typed as `Instant`; `DownsampleService` re-offsets it to UTC (`atOffset(ZoneOffset.UTC)`) — exact, since both bounds are UTC.
- **Files modified:** PriceBucketView.java, DownsampleService.java
- **Verification:** DownsamplePricesIT passes (bucket start = `2026-06-21T15:00:00Z`, avg=mean, sampleCount=60)
- **Committed in:** `28d2532` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug). **Impact:** Necessary for the downsample path to function; no scope creep.

## Issues Encountered
None unresolved. Full `./gradlew test -PdockerApiVersion=1.44` is green: 46 tests, 0 failures, 0 errors.

Note: 03-02's `SnapshotPoint` DTO is now superseded by `PricePoint` in `TimelineResponse` and is no longer referenced — left in place (out of this plan's file scope); a trivial follow-up cleanup candidate.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 3 read API is complete: latest (cached), timeline (two arrays), downsampling, validation, and health.
- `WindowQueryService` (03-02) remains the locked reuse point for Phase 5 event-impact.
- No blockers.

---
*Phase: 03-read-api-cache*
*Completed: 2026-06-23*
