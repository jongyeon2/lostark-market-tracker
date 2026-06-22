---
phase: 02-collection-pipeline
plan: 03
subsystem: api
tags: [retry, exponential-backoff, retry-after, fatal-auth, partial-failure, flyway, ddl-validate, testcontainers]

# Dependency graph
requires:
  - phase: 02-02
    provides: PriceCollector fan-out + collection_run lifecycle, ItemFetchService (@Async), ItemFetchResult
  - phase: 02-01
    provides: typed LostarkApiException hierarchy (Auth/RateLimited[+retryAfter]/Transient/NonRetryable)
provides:
  - Hand-rolled bounded RetryPolicy (max 3, exponential backoff, Retry-After honored; no @Retryable AOP)
  - Fatal-auth (401/403) convergence — shared flag stops new calls + marks run AUTH_ERROR (D-08)
  - Partial-failure isolation with categorical collection_run.summary_message markers (AUTH_ERROR/RATE_LIMITED)
  - Flyway V2 (collection_run.summary_message) + CollectionRun.summaryMessage field (ddl-validate)
affects: [03-read-api-cache]

# Tech tracking
tech-stack:
  added: []
  patterns: [hand-rolled-bounded-retry, retry-after-before-backoff, shared-fatalauth-flag, categorical-run-markers-no-secrets, forward-flyway-v2]

key-files:
  created:
    - src/main/java/com/lostark/tracker/collect/RetryPolicy.java
    - src/main/resources/db/migration/V2__add_collection_run_summary.sql
    - src/test/java/com/lostark/tracker/collect/{RetryPolicyTest,CollectionResilienceIT}.java
  modified:
    - src/main/java/com/lostark/tracker/collect/{ItemFetchService,PriceCollector,CollectionConfig}.java
    - src/main/java/com/lostark/tracker/domain/CollectionRun.java

key-decisions:
  - "Hand-rolled RetryPolicy (no @Retryable AOP) so the policy is visible + unit-testable, consistent with the self-built token bucket"
  - "Retry-After honored before exponential backoff for 429; 5xx/timeout exponential backoff; 401/403 + other-4xx never retried"
  - "Fatal auth uses a per-tick shared AtomicBoolean: first 401 stops new outbound calls and sets the AUTH_ERROR marker (D-08); in-flight items converge to failure"
  - "summary_message carries ONLY categorical markers (AUTH_ERROR > RATE_LIMITED priority); the API key never appears anywhere"
  - "Flyway V2 filename V2__add_collection_run_summary.sql per the user's migration-numbering rule; avg_price stays a DEFERRED future V3 (not created, not referenced)"

patterns-established:
  - "Bounded retry as an injectable Supplier wrapper with an injectable Sleeper for deterministic timing tests"
  - "Run-level signals are categorical markers only — secret-free, ready for Phase 3 /health"

requirements-completed: [COLL-04, COLL-05]

# Metrics
duration: ~30min
completed: 2026-06-22
---

# Phase 02 / Plan 03: Retry + Partial-Failure + Run Markers Summary

**Hand-rolled bounded retry (max-3, Retry-After-first backoff) wired into the fan-out, fatal-auth convergence that stops new calls and marks the run AUTH_ERROR, per-item failure isolation to PARTIAL_SUCCESS, and a Flyway V2 summary_message column — all markers secret-free.**

## Performance

- **Duration:** ~30 min
- **Completed:** 2026-06-22
- **Tasks:** 2
- **Files created:** 4 (4 modified)
- **Tests:** clean full suite 29 passed / 0 failed / 1 skipped (the @Disabled spike)

## Accomplishments
- `RetryPolicy` — explicit max-3 bounded retry; 429 honors Retry-After before exponential backoff, 5xx/timeout exponential backoff, 401/403 + other-4xx never retried (D-09/D-10/D-11)
- Fatal auth (401/403): a per-tick shared `AtomicBoolean` stops NEW outbound calls and marks the run `AUTH_ERROR`; the key never leaks (D-08)
- Per-item failure isolation → `PARTIAL_SUCCESS`/`FAILED`; failed items write no snapshot, only counts (COLL-05, Success Criterion 4)
- 429 retried up to 3 then the item is skipped with `failed++` and a `RATE_LIMITED` marker — no crash (Success Criterion 3)
- Flyway **V2** (`V2__add_collection_run_summary.sql`) adds nullable `summary_message`; `CollectionRun.summaryMessage` mirrors it under `ddl-auto=validate`
- `RetryPolicyTest` (unit, fake sleeper) + `CollectionResilienceIT` (Testcontainers) prove the whole policy

## Task Commits

1. **Task 1: Flyway V2 summary_message + entity field (validate)** — `dbcb5e4` (feat)
2. **Task 2: bounded retry + fatal-auth + partial-failure markers** — `a62a0f5` (feat, TDD)

## Files Created/Modified
- `collect/RetryPolicy.java` — hand-rolled bounded retry + injectable Sleeper
- `collect/ItemFetchService.java` — wraps the call in RetryPolicy; classifies into categorical reasons; trips fatalAuth on 401/403
- `collect/PriceCollector.java` — shared fatalAuth flag; aggregates AUTH_ERROR/RATE_LIMITED into summary_message
- `collect/CollectionConfig.java` — RetryPolicy bean (real sleeper, max 3, 200ms base)
- `domain/CollectionRun.java` — summaryMessage field + getter/setter
- `db/migration/V2__add_collection_run_summary.sql` — nullable summary_message; documents deferred V3 avg_price
- `test/.../RetryPolicyTest.java`, `test/.../CollectionResilienceIT.java`

## Decisions Made
- **Flyway numbering (user rule):** V2 = summary_message (`V2__add_collection_run_summary.sql`); avg_price/price-metrics is a DEFERRED future `V3__add_price_metrics.sql` — NOT created this phase, and no `avg_price` column is referenced anywhere in code (verified by grep). `ddl-auto=validate` passes on V1+V2.
- **Hand-rolled retry, no AOP:** keeps the policy explicit and unit-testable; Retry-After takes precedence over backoff.
- **Marker safety:** `summary_message` is set only to `AUTH_ERROR`/`RATE_LIMITED`; the key/Authorization header never reaches it (D-08) — proven by the fatal-auth IT asserting the exact marker.

## Deviations from Plan
None of substance — plan executed as written. `CollectionConfig` (RetryPolicy bean) and `PriceCollector` (marker aggregation) were touched in addition to the plan's named files, which is inherent to wiring the retry/markers; all follow established patterns.

## Issues Encountered
None. The real RetryPolicy bean's small backoff (200ms base) keeps the resilience IT fast while exercising real retries; the unit test uses a recording sleeper for exact, wait-free timing assertions.

## User Setup Required
None for tests. A real run needs `LOSTARK_API_KEY`; the dev/seed profile seeds the watchlist.

## Next Phase Readiness
- Collection pipeline is complete: rate-limited client + token bucket (02-01), scheduled fan-out + idempotent persistence + collection_run (02-02), retry + partial-failure + markers (02-03).
- Phase 3 (Read API + Cache) consumes `price_snapshot` (time series) and `collection_run` (incl. the `summary_message` marker for `/api/health/collection`, OPS-01). Phase 2 deliberately stops at producing that data — the health endpoint is Phase 3.

---
*Phase: 02-collection-pipeline*
*Completed: 2026-06-22*
