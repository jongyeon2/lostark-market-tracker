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
    provides: "collection_run 행 (started/finished/counts/status/summary_message 마커)"
provides:
  - "/prices 서버 측 다운샘플: raw 점 수 > N(~500)일 때 date_trunc avg(min_price) 버킷 (API-04)"
  - "입력 검증 계약: from>to/window<=0 → 400, 없는 품목 → 404, 빈 범위 → 200 빈 배열 (API-05)"
  - "GET /api/health/collection — 최신 collection_run 스냅샷, 시크릿 노출 없음 (OPS-01)"
  - "DownsampleService + PriceBucketView 네이티브 date_trunc 프로젝션"
  - "ApiExceptionHandler를 400 핸들러(InvalidRequestException + 잘못된 파라미터)로 확장"
affects: [05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Spring Data 인터페이스 프로젝션을 통한 네이티브 PostgreSQL date_trunc 집계 (Instant 게터, 서버가 UTC로 재오프셋)"
    - "서버가 고른 버킷 단위 화이트리스트(hour/day)를 파라미터로 바인딩 — SQL 인젝션 표면 없음"
    - "검증-우선-존재검사 순서(400 다음 404)와 단일 ApiErrorResponse 헬퍼"
    - "카운트/마커만 노출하는 운영 헬스 엔드포인트 — 경로에 시크릿 필드 자체가 없음"

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
  - "D-07: avg(min_price) 버킷을 Java가 아닌 PostgreSQL date_trunc로 집계"
  - "D-08: raw 점 수 > N이면 자동 다운샘플; 응답에 downsampled + bucketWidth 메타; 클라이언트 파라미터 불필요"
  - "D-09: N≈500 (TARGET_MAX_POINTS); 버킷 단위는 span<=N시간이면 hour, 아니면 day"
  - "D-13: from>to / window<=0 → 400, 없는 품목 → 404, 유효하나 빈 범위 → 200 빈 배열"
  - "D-10: 400 핸들러가 03-01 advice를 확장; 동일 {timestamp,status,error,message} 바디"
  - "D-12: 헬스는 lastRunAt/started/counts/status/marker만 노출; key/auth 필드 없음; 런 없으면 status NO_RUNS"

patterns-established:
  - "date_trunc 네이티브 프로젝션은 Instant를 반환(Hibernate), 서비스에서 UTC로 재오프셋"
  - "이벤트는 절대 다운샘플하지 않음 — snapshots 배열만 축소 (D-04 직교성)"

requirements-completed: [API-04, API-05, OPS-01]

# Metrics
duration: ~30분
completed: 2026-06-23
---

# Phase 3 Plan 03: 다운샘플 + 검증 + 헬스 요약

**`/prices`가 큰 범위를 PostgreSQL `date_trunc` avg 버킷으로 자동 다운샘플하고, 전체 입력 검증 계약(400/404/200-빈)이 커스텀 advice를 확장하며, `GET /api/health/collection`이 시크릿 노출 없이 최신 수집 런을 드러낸다.**

## 성능

- **소요 시간:** ~30분
- **완료:** 2026-06-23
- **태스크:** 3개 (전부 테스트 기반)
- **변경 파일:** 15개 (생성 10, 수정 5)

## 주요 성과
- 서버 측 다운샘플: 네이티브 `date_trunc` 집계(버킷당 `avg(min_price)` + `count(*)`)를 `DownsampleService` 뒤에 배치; raw 점 수 > N≈500이면 자동 발동, span으로 hour/day 선택, `downsampled`/`bucketWidth` 메타 반환; events 무수정 (D-07/08/09).
- 입력 검증 계약: `from>to`/`window<=0` → 400(`InvalidRequestException`), 잘못된/누락 파라미터 → 400, 없는 품목 → 404, 유효하나 빈 범위 → 200 빈 배열 — 전부 확장된 `@RestControllerAdvice` `{timestamp,status,error,message}` 바디 경유 (D-10/D-13).
- `GET /api/health/collection`: 최신 `collection_run` 스냅샷(lastRunAt/startedAt/counts/status/marker), 비어있으면 `NO_RUNS`, key/Authorization/bearer/token 노출 없음 증명 (D-12).

## 태스크 커밋

1. **Task 1: 서버 측 다운샘플 (date_trunc + DownsampleService)** - `28d2532` (feat)
2. **Task 2: 입력 검증 계약 (400 advice 확장)** - `c340487` (feat)
3. **Task 3: 수집 헬스 엔드포인트** - `8c548d4` (feat)

## 생성/수정 파일
- `repository/PriceBucketView.java` + `PriceSnapshotRepository` 네이티브 `date_trunc` 집계
- `read/DownsampleService.java` - raw-vs-버킷 판단 + 단위 선택
- `web/dto/PricePoint.java` + 확장된 `TimelineResponse` (downsampled + bucketWidth)
- `web/PricesController.java` - 다운샘플 연결 + 범위 검증
- `web/error/InvalidRequestException.java` + `ApiExceptionHandler` 400 핸들러
- `health/CollectionHealthService.java` + `web/HealthController.java` + `web/dto/CollectionHealthResponse.java` + `CollectionRunRepository` 런 파인더
- `test/.../DownsamplePricesIT.java`, `InputValidationIT.java`, `health/CollectionHealthIT.java`

## 결정 사항
잠긴 CONTEXT 결정(D-07/08/09/10/12/13) 외 추가 결정 없음. 재량: `TARGET_MAX_POINTS=500`; 단위 규칙 `span<=500h → hour, 아니면 day`; 런 없으면 `status "NO_RUNS"` 반환.

## 계획 대비 이탈

### 자동 수정 이슈

**1. [Rule 1 - 버그] 네이티브 `date_trunc` 프로젝션 타입 불일치**
- **발견 시점:** Task 1 (DownsamplePricesIT — 큰 범위가 500 반환)
- **이슈:** Hibernate가 네이티브 `timestamptz` 컬럼을 `java.time.Instant`로 반환하는데 프로젝션 게터는 `OffsetDateTime`으로 선언 → Spring 인터페이스 프로젝션에 `Instant`→`OffsetDateTime` 컨버터가 없어 `UnsupportedOperationException`.
- **수정:** `PriceBucketView.getBucketStart()`를 `Instant`로 선언; `DownsampleService`에서 UTC로 재오프셋(`atOffset(ZoneOffset.UTC)`) — 양쪽 경계가 UTC라 정확.
- **변경 파일:** PriceBucketView.java, DownsampleService.java
- **검증:** DownsamplePricesIT 통과 (버킷 시작 = `2026-06-21T15:00:00Z`, avg=평균, sampleCount=60)
- **커밋:** `28d2532` (Task 1 커밋)

---

**총 이탈:** 1건 자동 수정 (버그 1). **영향:** 다운샘플 경로 동작에 필수; 스코프 확장 없음.

## 마주친 이슈
미해결 없음. 전체 `./gradlew test -PdockerApiVersion=1.44` 그린: 46개 테스트, 실패 0, 에러 0.

참고: 03-02의 `SnapshotPoint` DTO는 `TimelineResponse`에서 `PricePoint`로 대체되어 더 이상 참조되지 않음 — 이 계획 파일 스코프 밖이라 그대로 둠; 사소한 후속 정리 후보.

## 사용자 셋업 필요
없음 - 외부 서비스 구성 불필요.

## 다음 페이즈 준비도
- Phase 3 읽기 API 완성: latest(캐시), 타임라인(두 배열), 다운샘플, 검증, 헬스.
- `WindowQueryService`(03-02)는 Phase 5 event-impact의 잠긴 재사용 지점.
- 블로커 없음.

---
*Phase: 03-read-api-cache*
*완료: 2026-06-23*
