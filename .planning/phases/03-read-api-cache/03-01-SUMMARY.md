---
phase: 03-read-api-cache
plan: 01
subsystem: api
tags: [redis, cache-aside, spring-boot, jpa, testcontainers, jackson, error-handling]

# Dependency graph
requires:
  - phase: 02-collection-pipeline
    provides: PriceCollector.persistSnapshot insert 경로, PriceSnapshot/TrackedItem 엔티티, PostgresRedisContainers 테스트 베이스, StringRedisTemplate 토큰버킷
provides:
  - "GET /api/items/{id}/latest — Redis 캐시-어사이드로 최신가 서빙 (캐시 히트 시 DB 0회 조회)"
  - "LatestPriceCache (get/put/evict + 안전망 TTL, fail-open) — 값 직렬화 RedisTemplate<String, LatestPriceResponse> 기반"
  - "Evict-on-write 무효화를 PriceCollector.persistSnapshot에 연결 (D-01)"
  - "에러 계약의 404 절반: ApiExceptionHandler @RestControllerAdvice + ItemNotFoundException + ApiErrorResponse {timestamp,status,error,message}"
  - "GET /api/items를 active 전용으로 전환 (findByActiveTrue), API-01"
  - "읽기 전용 파인더 PriceSnapshotRepository.findTopByTrackedItem_IdOrderByCollectedAtDesc"
affects: [03-02-timeline, 03-03-downsample-validation-health, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "수동 cache-aside (명시적 get/put/evict, @Cacheable 미사용) — Phase 2 자체 구현 토큰버킷과 동일 철학"
    - "일관된 4xx JSON을 위한 커스텀 @RestControllerAdvice 에러 계약"
    - "Fail-open 캐시: Redis 오류를 삼켜 miss/no-op으로 처리 → 읽기 경로는 가용성이 아니라 지연만 저하"
    - "@MockitoSpyBean 리포지토리로 캐시 히트=DB 0회 단언"

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
  - "D-03: 토큰버킷의 StringRedisTemplate과 동일 커넥션 팩토리 위의 별개 값-직렬화 RedisTemplate 빈으로 수동 cache-aside 구현; Spring 캐시 추상화 미사용"
  - "D-02: 캐시 값은 최소 DTO {itemId, minPrice, collectedAt}를 JSON 직렬화"
  - "D-01: evict-on-write가 1차 무효화 신호; 20분(2틱) 안전망 TTL은 백스톱"
  - "D-11: collectedAt을 UTC ISO-8601로 직렬화, 동일 인스턴트로 라운드트립 (off-by-9h 가드)"
  - "D-10: 에러 계약의 404 절반 (없는 품목 AND 품목은 있으나 스냅샷 없음 둘 다 404); 400 검증은 03-03으로 이연"
  - "재량: /api/items는 active만 반환, displayName 정렬"

patterns-established:
  - "Cache-aside: cache.get → miss 시 검증+DB 조회+채움; 쓰기 측은 성공 persist 직후 evict"
  - "에러 계약: 도메인 예외 → @RestControllerAdvice → ApiErrorResponse JSON"

requirements-completed: [API-01, API-02]

# Metrics
duration: ~25분
completed: 2026-06-23
---

# Phase 3 Plan 01: 최신가 Redis 캐시-어사이드 요약

**`GET /api/items/{id}/latest`를 위한 수동 Redis 캐시-어사이드(캐시 히트 시 DB 0회 조회)와 Phase 2 수집기에 연결한 evict-on-write 무효화, 그리고 커스텀 `@RestControllerAdvice` 에러 계약의 404 절반 + active 전용 `/api/items`.**

## 성능

- **소요 시간:** ~25분
- **완료:** 2026-06-23
- **태스크:** 3개 (전부 TDD/테스트 기반)
- **변경 파일:** 13개 (생성 8, 수정 5)

## 주요 성과
- `LatestPriceCache` — 값-직렬화 `RedisTemplate<String, LatestPriceResponse>`(토큰버킷의 `StringRedisTemplate`과 동일 커넥션 팩토리 위의 별개 빈) 위에 명시적 `get`/`put`/`evict`, 20분 안전망 TTL, Redis 오류 fail-open (D-02/D-03).
- `LatestPriceService` 캐시-어사이드 읽기 모델: 히트 → DB 0회로 반환; miss → 품목 없으면 404, 최신 스냅샷 조회(없으면 404), 캐시 채우고 반환.
- Evict-on-write: `PriceCollector.persistSnapshot`이 성공 save 직후 해당 품목 latest 키를 evict (실제 쓰기에서만 — 멱등 스킵은 캐시 유지) (D-01).
- 404 에러 계약: `ApiExceptionHandler`(`@RestControllerAdvice`)가 `ItemNotFoundException`을 404 + `{timestamp,status,error,message}` 바디로 매핑 (D-10); `/api/items`를 `findByActiveTrue`로 전환 (API-01).
- `LatestPriceCacheIT`가 캐시 히트=2번째 읽기 DB 0회(`@MockitoSpyBean`, 두 번 읽어도 파인더 `times(1)`), evict-on-write 신선도, 404 두 경우를 증명; KST 자정 UTC 경계로 off-by-9h 가드.

## 태스크 커밋

1. **Task 1: 값-직렬화 Redis 캐시-어사이드 프리미티브** - `a7dce9f` (feat)
2. **Task 2: 최신가 엔드포인트 + 404 에러 계약 + active /api/items** - `9e24b35` (feat)
3. **Task 3: evict-on-write 훅 + 캐시 히트/evict IT** - `78c0696` (test)

## 생성/수정 파일
- `cache/CacheConfig.java` - 값-직렬화 RedisTemplate 빈 (JSON + JavaTimeModule + ParameterNamesModule)
- `cache/LatestPriceCache.java` - 수동 get/put/evict 캐시-어사이드 프리미티브, fail-open, 안전망 TTL
- `web/dto/LatestPriceResponse.java` - 최소 {itemId, minPrice, collectedAt} DTO
- `web/dto/ApiErrorResponse.java` - {timestamp, status, error, message} 에러 바디
- `web/error/ItemNotFoundException.java` / `web/error/ApiExceptionHandler.java` - 404 계약
- `read/LatestPriceService.java` - 캐시-어사이드 오케스트레이션
- `web/ItemController.java` - GET /{id}/latest + active 전용 list()
- `repository/PriceSnapshotRepository.java` - 읽기 전용 최신 스냅샷 파인더
- `collect/PriceCollector.java` - evict-on-write 훅 (생성자에 LatestPriceCache 추가)
- `test/.../LatestPriceCacheIT.java` - 캐시 히트/evict/404 IT
- `test/.../PriceCollectionIT.java` + `CollectionResilienceIT.java` - 새 캐시 인자 연결

## 결정 사항
잠긴 CONTEXT 결정(D-01/02/03/10/11)과 문서화된 재량 판단(active 전용 displayName 정렬; 20분 TTL; `Jackson2JsonRedisSerializer` 타입드 값 직렬화; 키 `item:{id}:latest`) 외에 추가 결정 없음.

## 계획 대비 이탈

없음 - 계획대로 실행됨.

## 마주친 이슈
없음. 전체 `./gradlew test -PdockerApiVersion=1.44` 그린: 33개 테스트, 실패 0, 에러 0.

## 사용자 셋업 필요
없음 - 외부 서비스 구성 불필요 (Phase 1-2에서 Redis + Postgres 이미 연결됨).

## 다음 페이즈 준비도
- `@RestControllerAdvice` 에러 계약과 `ItemNotFoundException`이 03-02/03-03 재사용 준비 완료(03-03이 400 검증 핸들러 추가).
- 읽기 전용 리포지토리 패턴(Phase 2 insert 경로 옆에 읽기 메서드만 추가)이 03-02의 `WindowQueryService`·`GameEventRepository`용으로 확립됨.
- 블로커 없음.

---
*Phase: 03-read-api-cache*
*완료: 2026-06-23*
