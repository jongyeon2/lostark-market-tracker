---
phase: 03-read-api-cache
plan: 01
subsystem: api
tags: [redis, cache-aside, spring-boot, jpa, testcontainers, jackson, error-handling]

# Dependency graph
requires:
  - phase: 02-collection-pipeline
    provides: PriceCollector.persistSnapshot insert path, PriceSnapshot/TrackedItem entities, PostgresRedisContainers test base, StringRedisTemplate token bucket
provides:
  - "GET /api/items/{id}/latest — latest price served via hand-rolled Redis cache-aside (0 DB reads on a hit)"
  - "LatestPriceCache (get/put/evict + safety TTL, fail-open) over a value-serializing RedisTemplate<String, LatestPriceResponse>"
  - "Evict-on-write invalidation wired into PriceCollector.persistSnapshot (D-01)"
  - "404 half of the error contract: ApiExceptionHandler @RestControllerAdvice + ItemNotFoundException + ApiErrorResponse {timestamp,status,error,message}"
  - "GET /api/items now active-only (findByActiveTrue), API-01"
  - "Read-only finder PriceSnapshotRepository.findTopByTrackedItem_IdOrderByCollectedAtDesc"
affects: [03-02-timeline, 03-03-downsample-validation-health, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Hand-rolled cache-aside (explicit get/put/evict, no @Cacheable) mirroring the self-built token bucket"
    - "Custom @RestControllerAdvice error contract for consistent 4xx JSON bodies"
    - "Fail-open cache: Redis errors swallowed -> miss/no-op so the read path degrades latency, not availability"
    - "@MockitoSpyBean repository to assert cache-hit = 0 DB reads"

key-files:
  created:
    - src/main/java/com/lostark/tracker/cache/CacheConfig.java
    - src/main/java/com/lostark/tracker/cache/LatestPriceCache.java
    - src/main/java/com/lostark/tracker/web/dto/LatestPriceResponse.java
    - src/main/java/com/lostark/tracker/web/dto/ApiErrorResponse.java
    - src/main/java/com/lostark/tracker/web/error/ItemNotFoundException.java
    - src/main/java/com/lostark/tracker/web/error/ApiExceptionHandler.java
    - src/main/java/com/lostark/tracker/read/LatestPriceService.java
    - src/test/java/com/lostark/tracker/read/LatestPriceCacheIT.java
  modified:
    - src/main/java/com/lostark/tracker/web/ItemController.java
    - src/main/java/com/lostark/tracker/repository/PriceSnapshotRepository.java
    - src/main/java/com/lostark/tracker/collect/PriceCollector.java
    - src/test/java/com/lostark/tracker/collect/PriceCollectionIT.java
    - src/test/java/com/lostark/tracker/collect/CollectionResilienceIT.java

key-decisions:
  - "D-03: hand-rolled cache-aside via a distinct value-serializing RedisTemplate bean (same connection factory as the token bucket's StringRedisTemplate); no Spring cache abstraction"
  - "D-02: cached value is the minimal {itemId, minPrice, collectedAt} DTO, JSON-serialized"
  - "D-01: evict-on-write is the primary invalidation signal; 20-min (2-tick) safety TTL is the backstop"
  - "D-11: collectedAt serialized as UTC ISO-8601, round-trips as the same instant (off-by-9h guard)"
  - "D-10: 404 half of the error contract (missing item AND existing-item-no-snapshot both 404); 400 validation deferred to 03-03"
  - "Discretion: /api/items returns active items only, sorted by displayName"

patterns-established:
  - "Cache-aside: cache.get -> on miss validate+read DB+fill; writer evicts on successful persist"
  - "Error contract: domain exception -> @RestControllerAdvice -> ApiErrorResponse JSON"

requirements-completed: [API-01, API-02]

# Metrics
duration: ~25 min
completed: 2026-06-23
---

# Phase 3 Plan 01: Latest-price Redis cache-aside Summary

**Hand-rolled Redis cache-aside for `GET /api/items/{id}/latest` (cache hit = 0 DB reads) with evict-on-write invalidation in the Phase 2 collector, plus the 404 half of a custom `@RestControllerAdvice` error contract and active-only `/api/items`.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-06-23
- **Tasks:** 3 (all TDD/test-backed)
- **Files modified:** 13 (8 created, 5 modified)

## Accomplishments
- `LatestPriceCache` — explicit `get`/`put`/`evict` over a value-serializing `RedisTemplate<String, LatestPriceResponse>` (a distinct bean from the token bucket's `StringRedisTemplate`, same connection factory), with a 20-min safety TTL and fail-open Redis error handling (D-02/D-03).
- `LatestPriceService` cache-aside read model: hit → return with 0 DB reads; miss → 404 if item absent, read newest snapshot (404 if none), fill cache, return.
- Evict-on-write: `PriceCollector.persistSnapshot` evicts the item's latest key right after a successful save (only on a real write — idempotent skips leave the cache alone) (D-01).
- 404 error contract: `ApiExceptionHandler` (`@RestControllerAdvice`) maps `ItemNotFoundException` → 404 with the `{timestamp,status,error,message}` body (D-10); `/api/items` switched to `findByActiveTrue` (API-01).
- `LatestPriceCacheIT` proves cache-hit = 0 second DB read (`@MockitoSpyBean`, finder called `times(1)` across two reads), evict-on-write freshness, and both 404 cases; KST-midnight UTC boundary guards off-by-9h.

## Task Commits

1. **Task 1: Value-serializing Redis cache-aside primitive** - `a7dce9f` (feat)
2. **Task 2: Latest read endpoint + 404 error contract + active /api/items** - `9e24b35` (feat)
3. **Task 3: Evict-on-write hook + cache-hit/evict ITs** - `78c0696` (test)

## Files Created/Modified
- `cache/CacheConfig.java` - value-serializing RedisTemplate bean (JSON + JavaTimeModule + ParameterNamesModule)
- `cache/LatestPriceCache.java` - hand-rolled get/put/evict cache-aside primitive, fail-open, safety TTL
- `web/dto/LatestPriceResponse.java` - minimal {itemId, minPrice, collectedAt} DTO
- `web/dto/ApiErrorResponse.java` - {timestamp, status, error, message} error body
- `web/error/ItemNotFoundException.java` / `web/error/ApiExceptionHandler.java` - 404 contract
- `read/LatestPriceService.java` - cache-aside orchestration
- `web/ItemController.java` - GET /{id}/latest + active-only list()
- `repository/PriceSnapshotRepository.java` - read-only newest-snapshot finder
- `collect/PriceCollector.java` - evict-on-write hook (constructor gains LatestPriceCache)
- `test/.../LatestPriceCacheIT.java` - cache-hit/evict/404 IT
- `test/.../PriceCollectionIT.java` + `CollectionResilienceIT.java` - thread the new cache arg

## Decisions Made
None beyond the locked CONTEXT decisions (D-01/02/03/10/11) and the documented discretion calls (active-only sorted by displayName; 20-min TTL; `Jackson2JsonRedisSerializer` typed value serializer; key `item:{id}:latest`).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. Full `./gradlew test -PdockerApiVersion=1.44` is green: 33 tests, 0 failures, 0 errors.

## User Setup Required
None - no external service configuration required (Redis + Postgres already wired from Phases 1-2).

## Next Phase Readiness
- The `@RestControllerAdvice` error contract and `ItemNotFoundException` are in place for 03-02/03-03 to reuse (03-03 adds the 400 validation handlers).
- The read-only repository pattern (read methods added next to the Phase 2 insert path) is established for 03-02's `WindowQueryService` and `GameEventRepository`.
- No blockers.

---
*Phase: 03-read-api-cache*
*Completed: 2026-06-23*
