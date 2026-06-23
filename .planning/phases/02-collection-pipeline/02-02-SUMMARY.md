---
phase: 02-collection-pipeline
plan: 02
subsystem: api
tags: [spring-scheduling, async, completablefuture, threadpooltaskexecutor, jpa, idempotency, testcontainers, mockitobean]

# Dependency graph
requires:
  - phase: 02-01
    provides: LostarkApiClient(타입드 에러), RedisTokenBucket, MarketItem/MarketItemsResponse DTO
  - phase: 01-foundation-task-0
    provides: TrackedItem/PriceSnapshot/CollectionRun 엔티티 + 리포지토리, UNIQUE(tracked_item_id, collected_at), PostgresRedisContainers
provides:
  - "@Scheduled fixedDelay 수집 틱(중복 없음) + 병렬 @Async 팬아웃 + 호출당/전체 타임아웃 하의 allOf().join (D-07)"
  - 틱 정규화 collected_at(run-start UTC 분단위 절삭)을 틱의 모든 스냅샷이 공유(D-15)
  - UNIQUE + 존재 가드로 멱등 스냅샷 영속(D-16)
  - collection_run RUNNING->terminal 라이프사이클, 카운트 기반 SUCCESS/PARTIAL_SUCCESS/FAILED(D-12)
  - WatchlistSeeder(멱등, dev/seed 프로파일) + ItemFetchService(@Async) + ItemFetchResult
affects: [02-03, 03-read-api-cache]

# Tech tracking
tech-stack:
  added: []
  patterns: [scheduled-fixeddelay-tick, async-fanout-with-orTimeout+allOf, persist-after-join-success-only, injected-clock-for-deterministic-collected_at, derived-exists-idempotency-guard]

key-files:
  created:
    - src/main/java/com/lostark/tracker/collect/{CollectionConfig,WatchlistSeeder,PriceCollector,ItemFetchService,ItemFetchResult}.java
    - src/test/java/com/lostark/tracker/collect/{TickInstantNormalizationTest,PriceCollectionIT}.java
  modified:
    - src/main/java/com/lostark/tracker/domain/CollectionRun.java
    - src/main/java/com/lostark/tracker/repository/{TrackedItemRepository,PriceSnapshotRepository}.java
    - src/main/resources/application-test.yml

key-decisions:
  - "join 이후 수집기 스레드에서 SUCCESS 결과만 영속 — D-06(async-task-persists)에서 의도적 이탈: 타임아웃 후 끝난 태스크가 이미 실패로 센 스냅샷을 되살리는 늦은-쓰기 버그 제거"
  - "호출당 바운드는 CompletableFuture.orTimeout(perCall) AND 전체 allOf().get(overall) 백스톱(D-07)"
  - "collected_at = run-start 인스턴트 UTC 분단위 절삭(D-15); 주입 Clock으로 테스트에서 결정적"
  - "collection_run 라이프사이클은 두 쓰기: 시작 시 RUNNING, finish() 뮤테이터가 finishedAt+counts+status 설정(D-12)"
  - "WatchlistSeeder는 사용자 지시대로 dev/seed 프로파일 게이트(test에서 비활성); external_item_id 기준 멱등 upsert"
  - "TrackedItem.category가 숫자 리프 CategoryCode를 담아 품목별 ItemName 호출 동작(D-05)"

patterns-established:
  - "Async 팬아웃: fetch가 DTO future 반환, 수집기가 allOf().join 후 성공만 영속 — 느린 품목이 타인을 블록/오염 못함"
  - "멱등성은 existsByTrackedItem_IdAndCollectedAt + UNIQUE catch(실패 아닌 스킵)로 증명"
  - "Scheduled 빈이 IT에서 자동 발화하지 않게 collection.initial-delay-ms로; 테스트는 핀 시계로 collectTick() 구동"

requirements-completed: [COLL-01, COLL-02]

# Metrics
duration: ~30분
completed: 2026-06-22
---

# Phase 02 / Plan 02: 스케줄 수집기 + 병렬 팬아웃 요약

**활성 품목마다 @Async 가격 fetch를 팬아웃하고 호출당/전체 타임아웃 하에 join하며, 성공당 틱 정규화 collected_at을 공유하는 멱등 스냅샷 하나를 쓰고, 카운트 기반 status로 collection_run을 기록하는 @Scheduled fixedDelay 틱.**

## 성능

- **소요 시간:** ~30분
- **완료:** 2026-06-22
- **태스크:** 2개
- **생성 파일:** 7개 (5개 수정)
- **테스트:** 전체 스위트 19 통과 / 0 실패 / 1 스킵(@Disabled 스파이크)

## 주요 성과
- `PriceCollector.collectTick()` — `@Scheduled(fixedDelay)`(틱 중복 없음); `@Async` + `allOf().get(overall)` 팬아웃, 각 future는 `orTimeout(perCall)`로 바운드(D-07); 멈춘 품목이 틱을 정지시키지 못함
- 틱 정규화 `collected_at`(run-start UTC 분단위 절삭)을 틱의 모든 스냅샷이 공유(D-15); UNIQUE + 존재 가드로 재실행 멱등(D-16, Success Criterion 5)
- `collection_run` RUNNING→terminal 라이프사이클, 카운트 기반 SUCCESS/PARTIAL_SUCCESS/FAILED(D-12, Success Criterion 1)
- `ItemFetchService`(@Async)가 각 품목을 `Id == externalItemId`로 해석(D-05)하고 DTO 반환; `WatchlistSeeder`가 큐레이션 목록을 멱등 시드(dev/seed 프로파일)
- `PriceCollectionIT`가 공유 collected_at, 멱등 재실행, 멈춘 품목 격리(Success Criterion 4), 기록된 run 증명; `TickInstantNormalizationTest`가 분단위 절삭/UTC 규칙 증명

## 태스크 커밋

1. **Task 1: 스케줄링/async/executor 구성 + 멱등 워치리스트 시더** — `5fb514e` (feat)
2. **Task 2: PriceCollector 팬아웃 + 타임아웃 + 멱등 영속 + collection_run** — `f49313a` (feat, TDD)

## 생성/수정 파일
- `collect/PriceCollector.java` — 스케줄 틱; 팬아웃, 타임아웃, join 후 영속, run 라이프사이클
- `collect/ItemFetchService.java` — @Async 품목별 fetch(HTTP만, ItemFetchResult 반환)
- `collect/ItemFetchResult.java` — minPrice/fetchedAt 또는 카테고리적 사유를 담은 성공/실패 결과
- `collect/CollectionConfig.java` — @EnableScheduling/@EnableAsync + 바운드 executor + UTC Clock 빈
- `collect/WatchlistSeeder.java` — 멱등 dev/seed 프로파일 시더
- `domain/CollectionRun.java` — `finish(...)` 라이프사이클 뮤테이터 추가
- `repository/TrackedItemRepository.java` — `findByActiveTrue()`; `PriceSnapshotRepository.java` — `existsByTrackedItem_IdAndCollectedAt(...)`
- `application-test.yml` — IT에서 스케줄러 자동 발화 막는 `collection.initial-delay-ms`

## 결정 사항
- **Persist-after-join(D-06에서 이탈):** `@Async` fetch는 HTTP만 수행하고 DTO 반환; 수집기가 join 후 SUCCESS 결과 영속. 사용자의 "성공 품목만 영속" 규칙을 지키고, 늦은-쓰기 정확성 버그(전체 타임아웃 후 끝난 태스크가 이미 실패로 센 스냅샷을 되살림)를 제거. 엔티티 ID는 여전히 async 경계를 넘지만, async 경로가 JPA를 안 건드려 lazy-init 문제 없음.
- **2계층 타임아웃(D-07):** 호출당 `orTimeout`이 각 품목 바운드; 전체 `allOf().get`이 백스톱 — 둘 다 10분 틱 간격 미만.
- **결정적 collected_at:** 주입 `Clock`으로 IT가 `collected_at`을 핀해 분 경계를 넘나드는 멱등 재실행 단언을 결정적으로.

## 계획 대비 이탈

### 자동 수정 이슈

**1. [필요] CollectionRun + TrackedItemRepository 수정(계획 files_modified에 없음)**
- **발견 시점:** Task 2 — run 라이프사이클에 terminal 뮤테이터가 필요하고 수집기는 활성 품목 조회가 필요.
- **수정:** `CollectionRun.finish(...)`와 `TrackedItemRepository.findByActiveTrue()` 추가; 둘 다 기존 패턴 따름.
- **검증:** PriceCollectionIT가 최종 run 카운트/status와 활성 품목만 폴링됨을 단언.
- **커밋:** `f49313a`

**2. [설계 개선] async-task-persist(D-06 문자 그대로) 대신 persist-after-join**
- **발견 시점:** Task 2 — async-task-persist는 타임아웃된 태스크가 늦은 스냅샷을 쓰게 함.
- **수정:** 영속을 수집기 스레드로, SUCCESS 전용, join 후로 이동(상기 문서화).
- **검증:** 멈춘 품목 IT가 품목 B는 스냅샷을 안 쓰고 A/C는 씀을 증명.
- **커밋:** `f49313a`

---
**총 이탈:** 2건(1 필요, 1 정확성 개선). 스코프 확장 없음.

## 마주친 이슈
- Lazy `PriceSnapshot.trackedItem`은 IT에서 tx 밖 탐색 시 예외 → 멈춘 품목 단언을 `existsByTrackedItem_IdAndCollectedAt(...)`(lazy 탐색 없음)로 전환.

## 사용자 셋업 필요
테스트에는 없음. 실제 실행은 `dev`(또는 `seed`) 프로파일로 워치리스트 시드; `LOSTARK_API_KEY` 설정.

## 다음 페이즈 준비도
- 02-03이 `ItemFetchService`를 바운드 재시도(429 Retry-After + 5xx)와 fatal-auth(401/403) 수렴으로 감싸고, `collection_run` status 어휘를 정제하며, Flyway **V2**(`V2__add_collection_run_summary.sql`, 사용자 마이그레이션 번호 규칙)로 `summary_message` 마커 컬럼 추가.
- 수집기는 이미 실패를 세고 PARTIAL_SUCCESS/FAILED를 마감; 02-03은 팬아웃 계약 변경 없이 재시도 + 마커를 위에 얹음.

---
*Phase: 02-collection-pipeline*
*완료: 2026-06-22*
