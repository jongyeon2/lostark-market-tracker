---
phase: 02-collection-pipeline
plan: 01
subsystem: api
tags: [redis, lua, token-bucket, rate-limiter, restclient, http-error-classification, jackson, mockrestserviceserver]

# Dependency graph
requires:
  - phase: 01-foundation-task-0
    provides: Spring Boot skeleton, spring-data-redis, PostgresRedisContainers test base, LostarkSpikeClient (RestClient + key normalization), Task 0 API facts (100/min, Retry-After, leaf CategoryCode, CurrentMinPrice/Id/Name)
provides:
  - Self-built Redis token bucket (Lua-atomic consume + lazy refill, restart-restore from Redis, fail-closed)
  - RateLimiterConfig (RedisScript bean + global bucket, capacity 90 / 90-per-min)
  - LostarkApiClient (productized, key normalization, per-call 5s timeout) with 4-way HTTP error classification
  - LostarkApiException hierarchy (Auth / RateLimited[+retryAfter] / Transient / NonRetryable)
  - MarketItem / MarketItemsResponse DTOs (Id/Name/CurrentMinPrice)
affects: [02-02, 02-03, 03-read-api-cache]

# Tech tracking
tech-stack:
  added: []
  patterns: [lua-atomic-token-bucket, redis-as-single-source-of-truth, restclient-onstatus-error-classification, injected-clock-for-deterministic-time-tests]

key-files:
  created:
    - src/main/resources/redis/token_bucket.lua
    - src/main/java/com/lostark/tracker/ratelimit/{RedisTokenBucket,RateLimiterConfig}.java
    - src/main/java/com/lostark/tracker/collect/{LostarkApiClient,ApiClientConfig}.java
    - src/main/java/com/lostark/tracker/collect/dto/{MarketItem,MarketItemsResponse}.java
    - src/main/java/com/lostark/tracker/collect/error/{LostarkApiException,AuthApiException,RateLimitedApiException,TransientApiException,NonRetryableApiException}.java
    - src/test/java/com/lostark/tracker/ratelimit/RedisTokenBucketIT.java
    - src/test/java/com/lostark/tracker/collect/LostarkApiClientTest.java
  modified:
    - src/main/resources/application.yml

key-decisions:
  - "Token state stored in Redis with tostring() — Redis converts Lua numbers passed to redis.call into integers, which would truncate fractional token accrual; tostring preserves the float"
  - "RedisTokenBucket holds NO in-memory count — Redis is the single source of truth, so restart-restore is automatic and provable (Success Criterion 2)"
  - "tryAcquire fails CLOSED on Redis error — never grant a token when Redis is unreachable, so the app self-throttles rather than flooding the 100/min API"
  - "Error classification via RestClient .onStatus ordered 401/403 -> 429 -> 5xx -> other-4xx; ResourceAccessException (timeout) mapped to Transient"
  - "TrackedItem.category will hold the leaf CategoryCode (numeric string) so the collector can call POST /markets/items (CategoryCode+ItemName) per item (D-05) — the seeder in 02-02 seeds numeric leaf codes, not label text"
  - "Injected LongSupplier clock makes lazy-refill assertions deterministic without real waiting"

patterns-established:
  - "Self-built Lua-atomic Redis primitive driven by RedisScript<Long> + StringRedisTemplate (no Bucket4j)"
  - "Typed exception hierarchy expresses retry policy at the call site; the retry orchestration consumes it in 02-03"
  - "Pure-mock client tests via MockRestServiceServer.bindTo(RestClient.Builder) — no Docker needed for classification tests"

requirements-completed: [COLL-03, COLL-04]

# Metrics
duration: ~25min
completed: 2026-06-22
---

# Phase 02 / Plan 01: Rate-Limited API Client + Redis Token Bucket Summary

**Self-built Lua-atomic Redis token bucket (restart-restorable, fail-closed) plus a productized LostarkApiClient that classifies every HTTP outcome into a distinct typed exception (Auth/RateLimited+RetryAfter/Transient/NonRetryable).**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-06-22
- **Tasks:** 2
- **Files created:** 13 (1 modified)

## Accomplishments
- `token_bucket.lua` + `RedisTokenBucket` — atomic consume+lazy-refill in one round trip; token count lives only in Redis so it restores after restart (Success Criterion 2, COLL-03); fails closed on Redis error
- `RateLimiterConfig` — global bucket (one API key), capacity 90 / refill 90-per-min with head-room under the real 100/min
- `LostarkApiClient` — key normalization (bearer prefix + all-whitespace strip, the Task-0 401 gotcha), per-call 5s timeout (D-07), and 4-way error classification (COLL-04 classification half)
- `LostarkApiException` hierarchy + `MarketItem`/`MarketItemsResponse` DTOs (Id/Name/CurrentMinPrice only — avg_price/trade_count NOT collected, D-06)
- `RedisTokenBucketIT` (Testcontainers Redis) proves consume/throttle, lazy refill over an injected clock, and restart-restore of a partial count; `LostarkApiClientTest` (MockRestServiceServer) proves all 5 classification branches + parse

## Task Commits

1. **Task 1: Self-built Redis token bucket (Lua-atomic, lazy refill, restart-restore)** — `0dc6126` (feat, TDD)
2. **Task 2: Productized LostarkApiClient + HTTP error classification** — `addb902` (feat, TDD)

## Files Created/Modified
- `redis/token_bucket.lua` — atomic consume + lazy refill; tostring() guards fractional tokens
- `ratelimit/RedisTokenBucket.java` — no in-memory count; tryAcquire() executes the Lua script; fail-closed
- `ratelimit/RateLimiterConfig.java` — RedisScript<Long> bean + global bucket params
- `collect/LostarkApiClient.java` — POST /markets/items, key normalization, onStatus classification, timeout->Transient
- `collect/ApiClientConfig.java` — RestClient.Builder with 5s connect/read timeout; client bean
- `collect/dto/{MarketItem,MarketItemsResponse}.java` — list response mapping (ignore-unknown)
- `collect/error/*.java` — 4-way typed exception hierarchy
- `application.yml` — added `lostark.api.base-url` (default portal host) + `lostark.api.key` (env)

## Decisions Made
- **Redis-Lua integer-truncation landmine:** Lua numbers passed to `redis.call` are coerced to integers, which would drop fractional token accrual. Tokens are stored via `tostring()` so the float survives across calls — a real correctness fix, not cosmetic.
- **Fail-closed limiter:** on any Redis exception `tryAcquire` returns false. Self-throttling beats bypassing the limiter and hammering a 100/min API.
- **`category` = leaf CategoryCode (clarification for 02-02):** `/markets/items` needs a numeric leaf CategoryCode + ItemName. `TrackedItem.category` will carry the numeric leaf code (string); `display_name` is the ItemName; the response is matched by `Id` == external_item_id (D-05). The 02-02 seeder must seed numeric leaf codes.
- **Spike client left untouched** (`@Profile("spike")`); the real client is a separate class.

## Deviations from Plan

### Auto-fixed Issues

**1. [Missing critical — context boot] Added `lostark.api.base-url`/`key` to application.yml**
- **Found during:** Task 2 — `lostarkApiClient` is a non-profiled bean created in every context, so `${lostark.api.base-url}` had to resolve or `@SpringBootTest` contexts (incl. SmokeContextTest) would fail to start.
- **Fix:** Added `lostark.api.base-url` (default to the Task-0 portal host) + `lostark.api.key` (env, default empty) to `application.yml`.
- **Verification:** `SmokeContextTest` boots green with the new beans present.
- **Committed in:** `addb902`

---
**Total deviations:** 1 (necessary for context startup). No scope creep.

## Issues Encountered
None blocking. Confirmed `MockRestServiceServer.bindTo(RestClient.Builder)` works on Spring Framework 6.2 (Boot 3.4.1), so client classification is tested without Docker.

## User Setup Required
None for tests. For a real run against the live API, set `LOSTARK_API_KEY` (env). Base URL defaults to the portal host.

## Next Phase Readiness
- 02-02 (scheduler + fan-out) can now inject `LostarkApiClient` + `RedisTokenBucket`. The collector must seed `TrackedItem.category` with numeric leaf CategoryCodes and resolve items by `Id`.
- The retry layer (02-03) consumes the typed exceptions — `RateLimitedApiException.getRetryAfterSeconds()` is ready for Retry-After-first backoff.

---
*Phase: 02-collection-pipeline*
*Completed: 2026-06-22*
