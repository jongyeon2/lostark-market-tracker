---
phase: 03-read-api-cache
verified: 2026-06-23T16:05:00Z
status: passed
score: 5/5 성공 기준 검증됨
---

# Phase 3: Read API + Cache 검증 보고서

**페이즈 목표:** 캐시된 최신가, 타임라인(스냅샷+이벤트), 큰 범위 다운샘플, 헬스 엔드포인트를 제공한다.
**검증 시각:** 2026-06-23T16:05:00Z
**상태:** passed
**검증 방식:** 목표 역산(goal-backward) — ROADMAP Phase 3 목표 + 5개 성공 기준. 오케스트레이터가 인라인으로 수행(이 환경은 백그라운드 서브에이전트가 권한 거부됨).

## 목표 달성

### 관측 가능한 진실 (ROADMAP 성공 기준)

| # | 진실 | 상태 | 증거 |
|---|------|------|------|
| 1 | `GET /api/items`가 id·external_item_id·display_name JSON 배열을 반환한다 | ✓ 검증됨 | `ItemController.list()` → `findByActiveTrue()` (ItemController.java:50) → `TrackedItemResponse{id, externalItemId, displayName, category, active}`. 테스트 `LatestPriceCacheIT.listReturnsActiveItemsOnlyWithPublicFields`(active 전용·공개 필드) + `SchemaRoundTripIT` 라운드트립. |
| 2 | `GET /api/items/{id}/latest`가 Redis에서 최신가를 서빙하고, 새 스냅샷 쓰기 후 캐시가 갱신된다 | ✓ 검증됨 | `LatestPriceService.latest` 캐시-어사이드(cache.get→채움, LatestPriceService.java:42/57); `PriceCollector.persistSnapshot` evict-on-write(PriceCollector.java:165). 테스트 `cacheHitServesSecondReadWithZeroDbReads`(두 번 읽어도 파인더 `times(1)`) + `evictOnWriteMakesNextReadReflectTheNewSnapshot`. |
| 3 | `GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 game_event를 함께 반환한다 | ✓ 검증됨 | `PricesController` → `WindowQueryService.fetchWindow`가 스냅샷 윈도우 파인더 + `GameEventRepository.findByOccurredAtBetween` 조합(WindowQueryService.java:38-41). 두 배열 `{snapshots, events}`. 테스트 `TimelinePricesIT` + `WindowQueryServiceIT`(양끝 포함 경계, 오름차순). |
| 4 | 30일 범위가 서버 측에서 제한된 점 수로 다운샘플된다 | ✓ 검증됨 | `DownsampleService`(N=500) → 네이티브 `date_trunc` avg 버킷(PriceSnapshotRepository.java:39); span으로 hour/day(DownsampleService.java:67). 테스트 `DownsamplePricesIT.largeRangeAutoDownsamplesToHourlyAverageBuckets`(>N→제한된 버킷, avg=평균, sampleCount) + `smallRangeReturnsRawPointsNotDownsampled`. |
| 5 | from>to→400, 없는 품목→404, 빈 범위→200 빈 배열; `/api/health/collection`이 last_run_at + 카운트를 반환한다 | ✓ 검증됨 | `PricesController` 검증(to<=from→`InvalidRequestException`, PricesController.java:57) + `ApiExceptionHandler` 400/404; `HealthController` GET /api/health/collection → `findTopByOrderByStartedAtDesc`. 테스트 `InputValidationIT`(400/404/200-빈/잘못된 날짜) + `CollectionHealthIT`(필드 + 시크릿 없음). |

**점수:** 5/5 진실 검증됨

### 필수 산출물

| 산출물 | 상태 | 상세 |
|--------|------|------|
| `cache/CacheConfig.java` | ✓ 존재 + 실질적 | 값-직렬화 `RedisTemplate<String, LatestPriceResponse>`, 별개 빈, JSON + JavaTimeModule |
| `cache/LatestPriceCache.java` | ✓ 존재 + 실질적 | get/put/evict + 20분 TTL, fail-open, Spring 캐시 추상화 미사용(grep 클린) |
| `read/LatestPriceService.java` | ✓ 존재 + 실질적 | 캐시-어사이드 오케스트레이션, 없는 품목/스냅샷 없음 404 |
| `read/WindowQueryService.java` | ✓ 존재 + 실질적 | 4A 공유 윈도우 쿼리, 두 쿼리, `WindowResult` |
| `read/DownsampleService.java` | ✓ 존재 + 실질적 | N≈500 임계, hour/day 단위, date_trunc 프로젝션 매핑 |
| `health/CollectionHealthService.java` | ✓ 존재 + 실질적 | 최신 런 매핑; 경로에서 key/auth 읽지 않음 |
| `web/PricesController.java` | ✓ 존재 + 실질적 | /prices: 검증 → 404 가드 → fetchWindow → 다운샘플 |
| `web/HealthController.java` | ✓ 존재 + 실질적 | GET /api/health/collection |
| `web/error/ApiExceptionHandler.java` | ✓ 존재 + 실질적 | @RestControllerAdvice: 404 + 400 핸들러, 공유 바디 |
| `web/error/{ItemNotFoundException,InvalidRequestException}.java` | ✓ 존재 | 404 / 400 도메인 예외 |

**산출물:** 10/10 검증됨

### 핵심 연결 검증

| From | To | 상태 | 상세 |
|------|----|------|------|
| ItemController `/latest` | LatestPriceService.latest | ✓ 연결됨 | ItemController.java:58 |
| LatestPriceService | LatestPriceCache get/put | ✓ 연결됨 | LatestPriceService.java:42, 57 |
| PriceCollector.persistSnapshot | LatestPriceCache.evict | ✓ 연결됨 | PriceCollector.java:165 (성공 save 직후에만) |
| PricesController | WindowQueryService.fetchWindow | ✓ 연결됨 | 두 파인더 조합(WindowQueryService.java:40-41) |
| PricesController | DownsampleService | ✓ 연결됨 | window.snapshots() → 응답 조립 전 다운샘플 |
| ApiExceptionHandler | InvalidRequestException | ✓ 연결됨 | 400 핸들러(ApiExceptionHandler.java:33) |
| CollectionHealthService | CollectionRunRepository.findTopByOrderByStartedAtDesc | ✓ 연결됨 | CollectionHealthService.java:24 |

**연결:** 7/7 검증됨

## 요구사항 커버리지

계획 frontmatter의 모든 요구사항 ID가 회계됨(03-01 `[API-01, API-02]`, 03-02 `[API-03]`, 03-03 `[API-04, API-05, OPS-01]`) — 합집합이 페이즈 요구사항 집합과 일치.

| 요구사항 | 상태 | 증거 |
|----------|------|------|
| API-01: 워치리스트 조회 | ✓ 충족 | GET /api/items active 전용 (SC1) |
| API-02: 최신가 캐시 서빙 | ✓ 충족 | /latest 캐시-어사이드 + evict (SC2) |
| API-03: 타임라인 | ✓ 충족 | /prices 두 배열 (SC3) |
| API-04: 다운샘플 | ✓ 충족 | 서버 측 date_trunc 버킷 (SC4) |
| API-05: 입력 검증 | ✓ 충족 | 400/404/200-빈 계약 (SC5) |
| OPS-01: 수집 헬스 | ✓ 충족 | /api/health/collection (SC5) |

**커버리지:** 6/6 요구사항 충족 (REQUIREMENTS.md에 전부 complete 표시)

## 발견된 안티패턴

| 파일 | 패턴 | 심각도 | 영향 |
|------|------|--------|------|
| `web/dto/SnapshotPoint.java` | TimelineResponse에서 `PricePoint`로 대체, 현재 미참조 | ℹ️ 정보 | 03-02의 잔여 DTO; 사소한 후속 정리, 기능 영향 없음 |

**안티패턴:** 1건 발견 (블로커 0, 경고 0, 정보 1). 읽기 경로에 스텁·TODO·플레이스홀더 반환 없음. `@Cacheable`/`@CacheEvict` grep 클린(수동 cache-aside, D-03).

## CONTEXT 결정 준수

D-01 evict-on-write ✓ · D-02 최소 DTO ✓ · D-03 수동(Spring 캐시 미사용) ✓ · D-04 두 배열 + 이벤트 비다운샘플 ✓ · D-05 양끝 포함 containment ✓ · D-06 WindowQueryService(4A, Phase 5 재사용) ✓ · D-07 date_trunc avg를 DB에서 ✓ · D-08 자동 다운샘플 + 메타 ✓ · D-09 N≈500 hour/day ✓ · D-10 커스텀 advice ✓ · D-11 UTC, IT의 off-by-9h 가드 ✓ · D-12 헬스 시크릿 없음 ✓ · D-13 검증 계약 ✓.

## 사람 검증 필요

없음 — 다섯 성공 기준 모두 Testcontainers 통합 테스트로 프로그램적으로 검증됨.

## 갭 요약

**갭 없음.** 페이즈 목표 달성.

검증 중 발견·해소한 갭 1건: SC1(active 전용 `/api/items`)이 구현은 되었으나 테스트 단언이 없었음 → `LatestPriceCacheIT.listReturnsActiveItemsOnlyWithPublicFields` 추가(커밋 `4c6a71d`), 통과.

## 검증 메타데이터

**must-haves 출처:** ROADMAP.md Phase 3 목표 + 5개 성공 기준; 플랜별 PLAN frontmatter `must_haves`
**자동 검사:** `./gradlew test -PdockerApiVersion=1.44` → BUILD SUCCESSFUL — 47개 테스트, 실패 0, 에러 0, 스킵 1(`MarketsApiSpikeTest`, `@Disabled` Task 0 수동 스파이크). Phase 1/2 IT 포함(페이즈 간 회귀 없음).
**사람 검사 필요:** 0
**이탈:** 1건 (Rule 1 — 네이티브 `date_trunc` 프로젝션을 `Instant`로 타이핑 후 UTC 재오프셋; 03-03-SUMMARY에 문서화)

---
*검증: 2026-06-23T16:05:00Z*
*검증자: Claude (오케스트레이터, 인라인)*
