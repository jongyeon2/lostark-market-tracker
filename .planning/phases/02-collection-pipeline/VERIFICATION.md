---
phase: 02-collection-pipeline
status: passed
verified: 2026-06-22
---

# Phase 2 검증 — Collection Pipeline

**검증 시각:** 2026-06-22
**방식:** 구현된 코드베이스 대비 ROADMAP Phase 2 성공 기준의 목표 역산 검사 + 클린 `./gradlew clean build`(컴파일 + Testcontainers 통합 테스트 + 패키지).
**빌드 증거:** `./gradlew clean build` → **BUILD SUCCESSFUL**. 테스트 스위트: **29 통과 / 0 실패 / 0 에러 / 1 스킵**(`@Disabled` `MarketsApiSpikeTest`, 네트워크 없음), 실제 Postgres 16 + Redis 7 (Testcontainers).

## 성공 기준

| # | 기준 | 판정 | 증거 |
|---|------|------|------|
| 1 | 20분 가동 시 시작/종료 시각 + 성공/실패 카운트가 올바른 collection_run 행이 최소 2개 | ✅ PASS | `PriceCollector.collectTick()`이 `@Scheduled(fixedDelayString=…600000)` → 10분 틱당 run 1개(20분에 ≥2). `PriceCollectionIT.tickWritesOneSnapshotPerItemAllSharingCollectedAtAndRecordsRun`이 non-null `started_at`+`finished_at` 및 attempted/succeeded/failed 카운트를 가진 `collection_run` 단언; `CollectionRun.finish(...)`가 RUNNING→terminal 라이프사이클 기록(D-12). |
| 2 | 레이트 예산 초과 요청을 토큰버킷이 스로틀, 재시작 후 Redis에서 토큰 수 복원 | ✅ PASS | `RedisTokenBucket`(Lua 원자적 consume+lazy refill, 메모리 카운트 없음). `RedisTokenBucketIT.consumesUpToCapacityThenThrottles`(용량이 하드 천장) + `lazilyRefillsProportionalToElapsedTime` + `restoresPartialTokenCountAfterRestart`(새 인스턴스가 Redis에 영속된 부분 카운트를 읽음, 풀로 리셋 안 함). |
| 3 | 429는 Retry-After + 지수 백오프로 최대 3회 재시도 후 스킵 — 크래시 없이 failure_count 증가 | ✅ PASS | `RetryPolicy`(max 3, 지수 백오프 전 Retry-After). `RetryPolicyTest`(5케이스: max-3, 백오프 스케줄, Retry-After 준수, auth/non-retryable은 재시도 안 함). `CollectionResilienceIT.rateLimitedBeyondMaxSkipsItemAndMarksRun` → 스냅샷 없음, `items_failed=1`, status FAILED, `summary_message=RATE_LIMITED`, 크래시 없음. |
| 4 | 한 품목 실패가 같은 틱의 다른 품목 price_snapshot 적재를 막지 않음 | ✅ PASS | `PriceCollectionIT.hangingItemDoesNotBlockOthersAndIsCountedFailed`(품목 B가 호출당 타임아웃을 넘겨 멈춤; A+C 스냅샷은 영속, B는 부재, B는 실패로 카운트), `CollectionResilienceIT.oneTransientFailureYieldsPartialSuccessWithoutBlockingOthers`(A는 5xx 소진, B는 영속, PARTIAL_SUCCESS). 호출당 `orTimeout` + 전체 `allOf().get`이 틱을 바운드(D-07). |
| 5 | fixedDelay가 틱 중복을 막고, UNIQUE 제약이 중복 틱의 중복 스냅샷을 막는다 | ✅ PASS | `@Scheduled(fixedDelay)`가 틱을 직렬화(중복 없음). `collected_at` = run-start UTC 분단위 절삭, 틱이 공유(`TickInstantNormalizationTest`, D-15). `PriceCollectionIT.reRunningSameTickWritesNoDuplicateSnapshot`(같은 collected_at의 2번째 틱이 0행 추가 — UNIQUE + 존재 가드, D-16). |

**커버된 요구사항:** COLL-01(10분 fixedDelay 스케줄러), COLL-02(병렬 팬아웃 + 호출당 타임아웃 + 중복 없음), COLL-03(토큰버킷 + 재시작 복원), COLL-04(429 Retry-After + 백오프 + max-3 + 스킵; 401/5xx 구분), COLL-05(실패 품목은 스냅샷 없이 run 결과만). Decision-coverage-verify 게이트: **13/13 준수**.

## 사용자 지시 준수(이번 실행)
- **Flyway 번호:** 정확히 두 마이그레이션 — `V1__init_schema.sql`(불변) + `V2__add_collection_run_summary.sql`(summary_message 컬럼, 사용자 지정 파일명). 중복 버전 번호 없음.
- **avg_price / V3:** 이번 페이즈 미수집(D-06). `V3__add_price_metrics.sql` 미생성; 코드 내 `avg_price` 컬럼/필드 참조 **0건**(grep 검증) — V2 마이그레이션 주석에 DEFERRED 미래 V3로 문서화.
- **ddl-auto=validate:** V1+V2에서 통과(`SmokeContextTest`); 엔티티 ↔ 스키마 일치.
- **스코프 가드:** Read API 캐시-어사이드 없음, Admin/Event CRUD 없음, Event Impact 없음, 프론트엔드 없음 — 수집 파이프라인만.
- **TDD:** 토큰버킷, 재시도 정책, 부분 실패, collection_run 상태, 중복 스냅샷 방지, 팬아웃 대기 로직 전부 테스트 우선 구동(RED 테스트 커밋/실행 후 최소 구현으로 그린).

## 보안(위협 모델)
- API 키는 로그·반환·어떤 예외/마커에도 절대 들어가지 않음. `collection_run.summary_message`는 카테고리적 마커(`AUTH_ERROR`/`RATE_LIMITED`)만 보유 — `CollectionResilienceIT.fatalAuthMarksRunAuthErrorIsNotRetriedAndLeaksNoKey`로 증명. 미완화 HIGH 위협 없음.

## 스코프 밖(올바르게 이연)
- `/api/health/collection` 엔드포인트(OPS-01) → **Phase 3**: Phase 2는 헬스 엔드포인트가 드러낼 `collection_run` 데이터(auth-error 마커 포함)를 생산.
- `avg_price`(`YDayAvgPrice`) per-tick 컬럼 → 미래 `V3__add_price_metrics.sql`; `trade_count` 일단위 stats → Phase 5/v2.

## 판정

**✅ PHASE 2 PASS** — 5개 성공 기준 전부 충족; 클린 빌드 + 29개 통합/단위 테스트가 실제 Postgres + Redis에서 그린; 레이트리밋/재시도/부분 실패 신뢰성 증명; Flyway 번호 + 스코프 + TDD 제약 준수.

---
*Phase: 02-collection-pipeline · 검증 2026-06-22*
