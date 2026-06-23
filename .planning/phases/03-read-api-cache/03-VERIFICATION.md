---
phase: 03-read-api-cache
verified: 2026-06-23T16:05:00Z
status: passed
score: 5/5 success criteria verified
---

# Phase 3: Read API + Cache Verification Report

**Phase Goal:** 캐시된 최신가, 타임라인(스냅샷+이벤트), 큰 범위 다운샘플, 헬스 엔드포인트를 제공한다.
**Verified:** 2026-06-23T16:05:00Z
**Status:** passed
**Verification approach:** Goal-backward (ROADMAP Phase 3 goal + 5 success criteria), executed inline by the orchestrator (background subagents are permission-denied in this environment).

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `GET /api/items`가 id·external_item_id·display_name JSON 배열을 반환한다 | ✓ VERIFIED | `ItemController.list()` → `findByActiveTrue()` (ItemController.java:50) → `TrackedItemResponse{id, externalItemId, displayName, category, active}`. Test `LatestPriceCacheIT.listReturnsActiveItemsOnlyWithPublicFields` (active-only, public fields) + `SchemaRoundTripIT` round-trip. |
| 2 | `GET /api/items/{id}/latest`가 Redis에서 최신가를 서빙하고, 새 스냅샷 쓰기 후 캐시가 갱신된다 | ✓ VERIFIED | `LatestPriceService.latest` cache-aside (cache.get→fill, LatestPriceService.java:42/57); `PriceCollector.persistSnapshot` evict-on-write (PriceCollector.java:165). Tests `cacheHitServesSecondReadWithZeroDbReads` (finder `times(1)` across two reads) + `evictOnWriteMakesNextReadReflectTheNewSnapshot`. |
| 3 | `GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 game_event를 함께 반환한다 | ✓ VERIFIED | `PricesController` → `WindowQueryService.fetchWindow` composes snapshot window finder + `GameEventRepository.findByOccurredAtBetween` (WindowQueryService.java:38-41). Two arrays `{snapshots, events}`. Tests `TimelinePricesIT` + `WindowQueryServiceIT` (inclusive boundaries, asc order). |
| 4 | 30일 범위가 서버 측에서 제한된 점 수로 다운샘플된다 | ✓ VERIFIED | `DownsampleService` (N=500) → native `date_trunc` avg buckets (PriceSnapshotRepository.java:39); hour/day by span (DownsampleService.java:67). Test `DownsamplePricesIT.largeRangeAutoDownsamplesToHourlyAverageBuckets` (>N→bounded buckets, avg=mean, sampleCount) + `smallRangeReturnsRawPointsNotDownsampled`. |
| 5 | from>to→400, 없는 품목→404, 빈 범위→200 빈 배열; `/api/health/collection`이 last_run_at + 카운트를 반환한다 | ✓ VERIFIED | `PricesController` validation (to<=from→`InvalidRequestException`, PricesController.java:57) + `ApiExceptionHandler` 400/404; `HealthController` GET /api/health/collection → `findTopByOrderByStartedAtDesc`. Tests `InputValidationIT` (400/404/200-empty/malformed) + `CollectionHealthIT` (fields + no-secret). |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| `cache/CacheConfig.java` | ✓ EXISTS + SUBSTANTIVE | Value-serializing `RedisTemplate<String, LatestPriceResponse>`, distinct bean, JSON + JavaTimeModule |
| `cache/LatestPriceCache.java` | ✓ EXISTS + SUBSTANTIVE | get/put/evict + 20-min TTL, fail-open, no Spring cache abstraction (grep clean) |
| `read/LatestPriceService.java` | ✓ EXISTS + SUBSTANTIVE | Cache-aside orchestration, 404 for missing/no-snapshot |
| `read/WindowQueryService.java` | ✓ EXISTS + SUBSTANTIVE | 4A shared window query, two queries, `WindowResult` |
| `read/DownsampleService.java` | ✓ EXISTS + SUBSTANTIVE | N≈500 threshold, hour/day unit, date_trunc projection mapping |
| `health/CollectionHealthService.java` | ✓ EXISTS + SUBSTANTIVE | Maps latest run; no key/auth read on path |
| `web/PricesController.java` | ✓ EXISTS + SUBSTANTIVE | /prices: validate → 404 guard → fetchWindow → downsample |
| `web/HealthController.java` | ✓ EXISTS + SUBSTANTIVE | GET /api/health/collection |
| `web/error/ApiExceptionHandler.java` | ✓ EXISTS + SUBSTANTIVE | @RestControllerAdvice: 404 + 400 handlers, shared body |
| `web/error/{ItemNotFoundException,InvalidRequestException}.java` | ✓ EXISTS | 404 / 400 domain exceptions |

**Artifacts:** 10/10 verified

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| ItemController `/latest` | LatestPriceService.latest | ✓ WIRED | ItemController.java:58 |
| LatestPriceService | LatestPriceCache get/put | ✓ WIRED | LatestPriceService.java:42, 57 |
| PriceCollector.persistSnapshot | LatestPriceCache.evict | ✓ WIRED | PriceCollector.java:165 (after successful save only) |
| PricesController | WindowQueryService.fetchWindow | ✓ WIRED | composes both finders (WindowQueryService.java:40-41) |
| PricesController | DownsampleService | ✓ WIRED | window.snapshots() → downsample before assembly |
| ApiExceptionHandler | InvalidRequestException | ✓ WIRED | 400 handler (ApiExceptionHandler.java:33) |
| CollectionHealthService | CollectionRunRepository.findTopByOrderByStartedAtDesc | ✓ WIRED | CollectionHealthService.java:24 |

**Wiring:** 7/7 connections verified

## Requirements Coverage

Every requirement ID in the plan frontmatter is accounted for (03-01 `[API-01, API-02]`, 03-02 `[API-03]`, 03-03 `[API-04, API-05, OPS-01]`) — union equals the phase requirement set.

| Requirement | Status | Evidence |
|-------------|--------|----------|
| API-01: 워치리스트 조회 | ✓ SATISFIED | GET /api/items active-only (SC1) |
| API-02: 최신가 캐시 서빙 | ✓ SATISFIED | /latest cache-aside + evict (SC2) |
| API-03: 타임라인 | ✓ SATISFIED | /prices two arrays (SC3) |
| API-04: 다운샘플 | ✓ SATISFIED | server-side date_trunc buckets (SC4) |
| API-05: 입력 검증 | ✓ SATISFIED | 400/404/200-empty contract (SC5) |
| OPS-01: 수집 헬스 | ✓ SATISFIED | /api/health/collection (SC5) |

**Coverage:** 6/6 requirements satisfied (all marked complete in REQUIREMENTS.md)

## Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `web/dto/SnapshotPoint.java` | Superseded by `PricePoint` in TimelineResponse; now unreferenced | ℹ️ Info | Dead DTO from 03-02; trivial follow-up cleanup, no functional impact |

**Anti-patterns:** 1 found (0 blockers, 0 warnings, 1 info). No stubs, TODOs, or placeholder returns on the read paths. `@Cacheable`/`@CacheEvict` grep is clean (hand-rolled cache-aside, D-03).

## CONTEXT Decision Compliance

D-01 evict-on-write ✓ · D-02 minimal DTO ✓ · D-03 hand-rolled (no Spring cache) ✓ · D-04 two arrays + events-not-downsampled ✓ · D-05 inclusive containment ✓ · D-06 WindowQueryService (4A, Phase 5 reuse) ✓ · D-07 date_trunc avg in DB ✓ · D-08 auto-downsample + meta ✓ · D-09 N≈500 hour/day ✓ · D-10 custom advice ✓ · D-11 UTC, off-by-9h guards in ITs ✓ · D-12 health no-secret ✓ · D-13 validation contract ✓.

## Human Verification Required

None — all five success criteria are verified programmatically by Testcontainers integration tests.

## Gaps Summary

**No gaps found.** Phase goal achieved.

One verification gap was found and CLOSED during verification: SC1 (active-only `/api/items`) was implemented but lacked a test assertion; `LatestPriceCacheIT.listReturnsActiveItemsOnlyWithPublicFields` was added (commit `4c6a71d`) and passes.

## Verification Metadata

**Must-haves source:** ROADMAP.md Phase 3 goal + 5 success criteria; PLAN frontmatter `must_haves` per plan
**Automated checks:** `./gradlew test -PdockerApiVersion=1.44` → BUILD SUCCESSFUL — 47 tests, 0 failures, 0 errors, 1 skipped (`MarketsApiSpikeTest`, `@Disabled` Task 0 manual spike). Includes Phase 1/2 ITs (no cross-phase regression).
**Human checks required:** 0
**Deviations:** 1 (Rule 1 — native `date_trunc` projection typed as `Instant`, re-offset to UTC; documented in 03-03-SUMMARY)

---
*Verified: 2026-06-23T16:05:00Z*
*Verifier: Claude (orchestrator, inline)*
