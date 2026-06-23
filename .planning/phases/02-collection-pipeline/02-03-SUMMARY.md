---
phase: 02-collection-pipeline
plan: 03
subsystem: api
tags: [retry, exponential-backoff, retry-after, fatal-auth, partial-failure, flyway, ddl-validate, testcontainers]

# Dependency graph
requires:
  - phase: 02-02
    provides: PriceCollector 팬아웃 + collection_run 라이프사이클, ItemFetchService(@Async), ItemFetchResult
  - phase: 02-01
    provides: 타입드 LostarkApiException 계층(Auth/RateLimited[+retryAfter]/Transient/NonRetryable)
provides:
  - 수동 바운드 RetryPolicy (max 3, 지수 백오프, Retry-After 준수; @Retryable AOP 없음)
  - Fatal-auth(401/403) 수렴 — 공유 플래그가 신규 호출 중단 + run을 AUTH_ERROR로 마킹(D-08)
  - 카테고리적 collection_run.summary_message 마커(AUTH_ERROR/RATE_LIMITED)와 함께 부분 실패 격리
  - Flyway V2(collection_run.summary_message) + CollectionRun.summaryMessage 필드(ddl-validate)
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
  - "수동 RetryPolicy(@Retryable AOP 없음)로 정책을 가시화 + 단위 테스트 가능하게, 자체 구현 토큰버킷과 일관"
  - "429는 지수 백오프 전에 Retry-After 준수; 5xx/타임아웃은 지수 백오프; 401/403 + other-4xx는 재시도 안 함"
  - "Fatal auth는 틱당 공유 AtomicBoolean 사용: 첫 401이 신규 outbound 호출 중단 + AUTH_ERROR 마커 설정(D-08); in-flight 품목은 실패로 수렴"
  - "summary_message는 카테고리적 마커만 보유(AUTH_ERROR > RATE_LIMITED 우선); API 키는 어디에도 안 나타남"
  - "Flyway V2 파일명 V2__add_collection_run_summary.sql(사용자 마이그레이션 번호 규칙); avg_price는 DEFERRED 미래 V3(미생성, 미참조)"

patterns-established:
  - "바운드 재시도를 주입 가능 Supplier 래퍼 + 주입 가능 Sleeper로 — 결정적 타이밍 테스트"
  - "Run 레벨 신호는 카테고리적 마커만 — 시크릿 프리, Phase 3 /health 준비됨"

requirements-completed: [COLL-04, COLL-05]

# Metrics
duration: ~30분
completed: 2026-06-22
---

# Phase 02 / Plan 03: 재시도 + 부분 실패 + Run 마커 요약

**팬아웃에 연결된 수동 바운드 재시도(max-3, Retry-After 우선 백오프), 신규 호출을 중단하고 run을 AUTH_ERROR로 마킹하는 fatal-auth 수렴, PARTIAL_SUCCESS로의 품목별 실패 격리, 그리고 Flyway V2 summary_message 컬럼 — 모든 마커는 시크릿 프리.**

## 성능

- **소요 시간:** ~30분
- **완료:** 2026-06-22
- **태스크:** 2개
- **생성 파일:** 4개 (4개 수정)
- **테스트:** 깔끔한 전체 스위트 29 통과 / 0 실패 / 1 스킵(@Disabled 스파이크)

## 주요 성과
- `RetryPolicy` — 명시적 max-3 바운드 재시도; 429는 지수 백오프 전 Retry-After 준수, 5xx/타임아웃 지수 백오프, 401/403 + other-4xx 재시도 안 함(D-09/D-10/D-11)
- Fatal auth(401/403): 틱당 공유 `AtomicBoolean`이 신규 outbound 호출 중단 + run을 `AUTH_ERROR` 마킹; 키 누출 없음(D-08)
- 품목별 실패 격리 → `PARTIAL_SUCCESS`/`FAILED`; 실패 품목은 스냅샷 없이 카운트만(COLL-05, Success Criterion 4)
- 429는 최대 3회 재시도 후 품목 스킵 `failed++` + `RATE_LIMITED` 마커 — 크래시 없음(Success Criterion 3)
- Flyway **V2**(`V2__add_collection_run_summary.sql`)가 nullable `summary_message` 추가; `CollectionRun.summaryMessage`가 `ddl-auto=validate`에서 미러링
- `RetryPolicyTest`(단위, 페이크 sleeper) + `CollectionResilienceIT`(Testcontainers)가 전체 정책 증명

## 태스크 커밋

1. **Task 1: Flyway V2 summary_message + 엔티티 필드(validate)** — `dbcb5e4` (feat)
2. **Task 2: 바운드 재시도 + fatal-auth + 부분 실패 마커** — `a62a0f5` (feat, TDD)

## 생성/수정 파일
- `collect/RetryPolicy.java` — 수동 바운드 재시도 + 주입 가능 Sleeper
- `collect/ItemFetchService.java` — 호출을 RetryPolicy로 감싸고 카테고리적 사유로 분류; 401/403에 fatalAuth 트립
- `collect/PriceCollector.java` — 공유 fatalAuth 플래그; AUTH_ERROR/RATE_LIMITED를 summary_message로 집계
- `collect/CollectionConfig.java` — RetryPolicy 빈(실제 sleeper, max 3, 200ms 베이스)
- `domain/CollectionRun.java` — summaryMessage 필드 + getter/setter
- `db/migration/V2__add_collection_run_summary.sql` — nullable summary_message; 이연된 V3 avg_price 문서화
- `test/.../RetryPolicyTest.java`, `test/.../CollectionResilienceIT.java`

## 결정 사항
- **Flyway 번호(사용자 규칙):** V2 = summary_message(`V2__add_collection_run_summary.sql`); avg_price/price-metrics는 DEFERRED 미래 `V3__add_price_metrics.sql` — 이번 페이즈 미생성, 코드 어디서도 `avg_price` 컬럼 미참조(grep로 검증). `ddl-auto=validate`가 V1+V2에서 통과.
- **수동 재시도, AOP 없음:** 정책을 명시적·단위 테스트 가능하게; Retry-After가 백오프보다 우선.
- **마커 안전:** `summary_message`는 `AUTH_ERROR`/`RATE_LIMITED`로만 설정; 키/Authorization 헤더가 절대 도달 안 함(D-08) — fatal-auth IT가 정확한 마커를 단언해 증명.

## 계획 대비 이탈
실질적 이탈 없음 — 계획대로 실행됨. `CollectionConfig`(RetryPolicy 빈)와 `PriceCollector`(마커 집계)가 계획 명시 파일 외에 함께 수정됐는데, 이는 재시도/마커 배선에 내재적이며 모두 확립된 패턴을 따름.

## 마주친 이슈
없음. 실제 RetryPolicy 빈의 작은 백오프(200ms 베이스)가 실제 재시도를 돌리면서 회복탄력성 IT를 빠르게 유지; 단위 테스트는 기록형 sleeper로 정확·무대기 타이밍 단언.

## 사용자 셋업 필요
테스트에는 없음. 실제 실행은 `LOSTARK_API_KEY` 필요; dev/seed 프로파일이 워치리스트 시드.

## 다음 페이즈 준비도
- 수집 파이프라인 완성: 레이트리밋 클라이언트 + 토큰버킷(02-01), 스케줄 팬아웃 + 멱등 영속 + collection_run(02-02), 재시도 + 부분 실패 + 마커(02-03).
- Phase 3(Read API + Cache)가 `price_snapshot`(시계열)과 `collection_run`(`/api/health/collection`용 `summary_message` 마커 포함, OPS-01)을 소비. Phase 2는 의도적으로 그 데이터 생산에서 멈춤 — 헬스 엔드포인트는 Phase 3.

---
*Phase: 02-collection-pipeline*
*완료: 2026-06-22*
