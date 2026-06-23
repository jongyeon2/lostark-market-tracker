---
phase: 02-collection-pipeline
plan: 01
subsystem: api
tags: [redis, lua, token-bucket, rate-limiter, restclient, http-error-classification, jackson, mockrestserviceserver]

# Dependency graph
requires:
  - phase: 01-foundation-task-0
    provides: Spring Boot 스켈레톤, spring-data-redis, PostgresRedisContainers 테스트 베이스, LostarkSpikeClient (RestClient + 키 정규화), Task 0 API 사실(100/min, Retry-After, 리프 CategoryCode, CurrentMinPrice/Id/Name)
provides:
  - 자체 구현 Redis 토큰버킷 (Lua 원자적 consume + lazy refill, Redis에서 재시작 복원, fail-closed)
  - RateLimiterConfig (RedisScript 빈 + 글로벌 버킷, 용량 90 / 90-per-min)
  - LostarkApiClient (제품화, 키 정규화, 호출당 5s 타임아웃) + 4방향 HTTP 에러 분류
  - LostarkApiException 계층 (Auth / RateLimited[+retryAfter] / Transient / NonRetryable)
  - MarketItem / MarketItemsResponse DTO (Id/Name/CurrentMinPrice)
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
  - "토큰 상태를 Redis에 tostring()으로 저장 — Redis는 redis.call에 넘긴 Lua number를 정수로 변환해 소수점 토큰 누적을 절삭하므로, tostring으로 float 보존"
  - "RedisTokenBucket은 메모리 카운트 없음 — Redis가 단일 진실 공급원이라 재시작 복원이 자동·증명 가능(Success Criterion 2)"
  - "tryAcquire는 Redis 오류 시 fail CLOSED — Redis 도달 불가 시 토큰을 절대 부여 안 함, 앱이 100/min API를 폭주시키는 대신 스스로 스로틀"
  - "에러 분류는 RestClient .onStatus 순서 401/403 -> 429 -> 5xx -> other-4xx; ResourceAccessException(타임아웃)은 Transient로 매핑"
  - "TrackedItem.category는 리프 CategoryCode(숫자 문자열)를 담아 수집기가 품목별 POST /markets/items(CategoryCode+ItemName) 호출 가능(D-05) — 02-02 시더는 라벨 텍스트가 아닌 숫자 리프 코드를 시드"
  - "주입된 LongSupplier 시계로 실제 대기 없이 lazy-refill 단언을 결정적으로"

patterns-established:
  - "RedisScript<Long> + StringRedisTemplate로 구동하는 자체 구현 Lua 원자적 Redis 프리미티브(Bucket4j 없음)"
  - "타입드 예외 계층이 호출부에서 재시도 정책을 표현; 재시도 오케스트레이션은 02-03이 소비"
  - "MockRestServiceServer.bindTo(RestClient.Builder) 순수 목 클라이언트 테스트 — 분류 테스트에 Docker 불필요"

requirements-completed: [COLL-03, COLL-04]

# Metrics
duration: ~25분
completed: 2026-06-22
---

# Phase 02 / Plan 01: 레이트리밋 API 클라이언트 + Redis 토큰버킷 요약

**자체 구현 Lua 원자적 Redis 토큰버킷(재시작 복원 가능, fail-closed)과, 모든 HTTP 결과를 구분된 타입드 예외(Auth/RateLimited+RetryAfter/Transient/NonRetryable)로 분류하는 제품화된 LostarkApiClient.**

## 성능

- **소요 시간:** ~25분
- **완료:** 2026-06-22
- **태스크:** 2개
- **생성 파일:** 13개 (1개 수정)

## 주요 성과
- `token_bucket.lua` + `RedisTokenBucket` — 한 번의 왕복으로 원자적 consume+lazy-refill; 토큰 카운트가 Redis에만 존재해 재시작 후 복원(Success Criterion 2, COLL-03); Redis 오류 시 fail closed
- `RateLimiterConfig` — 글로벌 버킷(API 키 1개), 용량 90 / 리필 90-per-min, 실제 100/min 대비 헤드룸
- `LostarkApiClient` — 키 정규화(bearer 접두 + 모든 공백 제거, Task-0 401 함정), 호출당 5s 타임아웃(D-07), 4방향 에러 분류(COLL-04 분류 절반)
- `LostarkApiException` 계층 + `MarketItem`/`MarketItemsResponse` DTO(Id/Name/CurrentMinPrice만 — avg_price/trade_count 미수집, D-06)
- `RedisTokenBucketIT`(Testcontainers Redis)가 consume/throttle, 주입 시계 기반 lazy refill, 부분 카운트 재시작 복원 증명; `LostarkApiClientTest`(MockRestServiceServer)가 5개 분류 분기 + 파싱 증명

## 태스크 커밋

1. **Task 1: 자체 구현 Redis 토큰버킷(Lua 원자적, lazy refill, 재시작 복원)** — `0dc6126` (feat, TDD)
2. **Task 2: 제품화 LostarkApiClient + HTTP 에러 분류** — `addb902` (feat, TDD)

## 생성/수정 파일
- `redis/token_bucket.lua` — 원자적 consume + lazy refill; tostring()이 소수 토큰 보호
- `ratelimit/RedisTokenBucket.java` — 메모리 카운트 없음; tryAcquire()가 Lua 스크립트 실행; fail-closed
- `ratelimit/RateLimiterConfig.java` — RedisScript<Long> 빈 + 글로벌 버킷 파라미터
- `collect/LostarkApiClient.java` — POST /markets/items, 키 정규화, onStatus 분류, 타임아웃->Transient
- `collect/ApiClientConfig.java` — 5s connect/read 타임아웃 RestClient.Builder; 클라이언트 빈
- `collect/dto/{MarketItem,MarketItemsResponse}.java` — 목록 응답 매핑(미지 필드 무시)
- `collect/error/*.java` — 4방향 타입드 예외 계층
- `application.yml` — `lostark.api.base-url`(기본 포털 호스트) + `lostark.api.key`(env) 추가

## 결정 사항
- **Redis-Lua 정수 절삭 지뢰:** `redis.call`에 넘긴 Lua number는 정수로 강제되어 소수 토큰 누적이 사라짐. 토큰을 `tostring()`으로 저장해 호출 간 float 생존 — 외형이 아닌 실제 정확성 수정.
- **Fail-closed 리미터:** Redis 예외 시 `tryAcquire`가 false 반환. 리미터 우회로 100/min API를 두드리는 것보다 자기 스로틀이 낫다.
- **`category` = 리프 CategoryCode(02-02용 명확화):** `/markets/items`는 숫자 리프 CategoryCode + ItemName 필요. `TrackedItem.category`가 숫자 리프 코드(문자열) 보유; `display_name`이 ItemName; 응답은 `Id` == external_item_id로 매칭(D-05). 02-02 시더는 숫자 리프 코드를 시드해야 함.
- **스파이크 클라이언트 무수정**(`@Profile("spike")`); 실제 클라이언트는 별도 클래스.

## 계획 대비 이탈

### 자동 수정 이슈

**1. [필수 누락 — 컨텍스트 부팅] application.yml에 `lostark.api.base-url`/`key` 추가**
- **발견 시점:** Task 2 — `lostarkApiClient`는 모든 컨텍스트에서 생성되는 비프로파일 빈이라 `${lostark.api.base-url}`가 해석돼야 `@SpringBootTest` 컨텍스트(SmokeContextTest 포함)가 시작 가능.
- **수정:** `lostark.api.base-url`(Task-0 포털 호스트 기본) + `lostark.api.key`(env, 기본 빈 값)를 `application.yml`에 추가.
- **검증:** `SmokeContextTest`가 새 빈과 함께 그린 부팅.
- **커밋:** `addb902`

---
**총 이탈:** 1건(컨텍스트 시작에 필요). 스코프 확장 없음.

## 마주친 이슈
블로킹 없음. `MockRestServiceServer.bindTo(RestClient.Builder)`가 Spring Framework 6.2(Boot 3.4.1)에서 동작 확인 → Docker 없이 클라이언트 분류 테스트.

## 사용자 셋업 필요
테스트에는 없음. 라이브 API 실제 실행 시 `LOSTARK_API_KEY`(env) 설정. Base URL은 포털 호스트 기본.

## 다음 페이즈 준비도
- 02-02(스케줄러 + 팬아웃)가 이제 `LostarkApiClient` + `RedisTokenBucket` 주입 가능. 수집기는 `TrackedItem.category`에 숫자 리프 CategoryCode를 시드하고 `Id`로 품목 해석해야 함.
- 재시도 레이어(02-03)는 타입드 예외 소비 — `RateLimitedApiException.getRetryAfterSeconds()`가 Retry-After 우선 백오프 준비됨.

---
*Phase: 02-collection-pipeline*
*완료: 2026-06-22*
