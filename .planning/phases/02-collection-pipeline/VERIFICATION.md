---
phase: 02-collection-pipeline
status: passed
verified: 2026-06-22
---

# Phase 2 Verification — Collection Pipeline

**Verified:** 2026-06-22
**Method:** Goal-backward check of ROADMAP Phase 2 success criteria against the implemented codebase + a clean `./gradlew clean build` (compile + Testcontainers integration tests + package).
**Build evidence:** `./gradlew clean build` → **BUILD SUCCESSFUL**. Test suite: **29 passed / 0 failed / 0 errors / 1 skipped** (the `@Disabled` `MarketsApiSpikeTest`, no network) on real Postgres 16 + Redis 7 via Testcontainers.

## Success Criteria

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| 1 | 20분 가동 시 시작/종료 시각 + 성공/실패 카운트가 올바른 collection_run 행이 최소 2개 | ✅ PASS | `PriceCollector.collectTick()` is `@Scheduled(fixedDelayString=…600000)` → one run per 10-min tick (≥2 in 20 min). `PriceCollectionIT.tickWritesOneSnapshotPerItemAllSharingCollectedAtAndRecordsRun` asserts a `collection_run` with non-null `started_at`+`finished_at` and attempted/succeeded/failed counts; `CollectionRun.finish(...)` records the RUNNING→terminal lifecycle (D-12). |
| 2 | 레이트 예산 초과 요청을 토큰버킷이 스로틀, 재시작 후 Redis에서 토큰 수 복원 | ✅ PASS | `RedisTokenBucket` (Lua-atomic consume+lazy refill, no in-memory count). `RedisTokenBucketIT.consumesUpToCapacityThenThrottles` (capacity is a hard ceiling) + `lazilyRefillsProportionalToElapsedTime` + `restoresPartialTokenCountAfterRestart` (a fresh instance reads the persisted partial count from Redis, not reset-to-full). |
| 3 | 429는 Retry-After + 지수 백오프로 최대 3회 재시도 후 스킵 — 크래시 없이 failure_count 증가 | ✅ PASS | `RetryPolicy` (max 3, Retry-After before exponential backoff). `RetryPolicyTest` (5 cases: max-3, backoff schedule, Retry-After honored, no-retry for auth/non-retryable). `CollectionResilienceIT.rateLimitedBeyondMaxSkipsItemAndMarksRun` → no snapshot, `items_failed=1`, status FAILED, `summary_message=RATE_LIMITED`, no crash. |
| 4 | 한 품목 실패가 같은 틱의 다른 품목 price_snapshot 적재를 막지 않음 | ✅ PASS | `PriceCollectionIT.hangingItemDoesNotBlockOthersAndIsCountedFailed` (item B hangs past the per-call timeout; A+C snapshots persist, B absent, B counted failed) and `CollectionResilienceIT.oneTransientFailureYieldsPartialSuccessWithoutBlockingOthers` (A 5xx-exhausted, B persists, PARTIAL_SUCCESS). Per-call `orTimeout` + overall `allOf().get` bound the tick (D-07). |
| 5 | fixedDelay가 틱 중복을 막고, UNIQUE 제약이 중복 틱의 중복 스냅샷을 막는다 | ✅ PASS | `@Scheduled(fixedDelay)` serializes ticks (no overlap). `collected_at` = run-start UTC truncated to the minute, shared by the tick (`TickInstantNormalizationTest`, D-15). `PriceCollectionIT.reRunningSameTickWritesNoDuplicateSnapshot` (a second tick at the same collected_at adds zero rows — UNIQUE + existence guard, D-16). |

**Requirements covered:** COLL-01 (10-min fixedDelay scheduler), COLL-02 (parallel fan-out + per-call timeout + no overlap), COLL-03 (token bucket + restart restore), COLL-04 (429 Retry-After + backoff + max-3 + skip; 401/5xx distinguished), COLL-05 (failed item writes no snapshot, only run results). Decision-coverage-verify gate: **13/13 honored**.

## User-instruction compliance (this execution)
- **Flyway numbering:** exactly two migrations — `V1__init_schema.sql` (unchanged) + `V2__add_collection_run_summary.sql` (the summary_message column, user-specified filename). No duplicate version numbers.
- **avg_price / V3:** NOT collected this phase (D-06). No `V3__add_price_metrics.sql` created; **zero** `avg_price` column/field references in code (grep-verified) — documented as a DEFERRED future V3 in the V2 migration comment.
- **ddl-auto=validate:** passes on V1+V2 (`SmokeContextTest`); entity ↔ schema agree.
- **Scope guard:** no Read API cache-aside, no Admin/Event CRUD, no Event Impact, no frontend — only the collection pipeline.
- **TDD:** token bucket, retry policy, partial failure, collection_run state, duplicate-snapshot prevention, and fan-out wait logic were all driven test-first (RED test committed/run, then minimal implementation to green).

## Security (threat model)
- API key never logged, returned, or placed in any exception/marker. `collection_run.summary_message` holds only categorical markers (`AUTH_ERROR`/`RATE_LIMITED`) — proven by `CollectionResilienceIT.fatalAuthMarksRunAuthErrorIsNotRetriedAndLeaksNoKey`. No unmitigated HIGH threats.

## Out of scope (correctly deferred)
- `/api/health/collection` endpoint (OPS-01) → **Phase 3**: Phase 2 produces the `collection_run` data (incl. auth-error marker) that the health endpoint will surface.
- `avg_price` (`YDayAvgPrice`) per-tick column → future `V3__add_price_metrics.sql`; `trade_count` daily stats → Phase 5/v2.

## Verdict

**✅ PHASE 2 PASS** — all 5 success criteria met; clean build + 29 integration/unit tests green on real Postgres + Redis; rate-limit/retry/partial-failure reliability proven; Flyway numbering + scope + TDD constraints honored.

---
*Phase: 02-collection-pipeline · Verified 2026-06-22*
