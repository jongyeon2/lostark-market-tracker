---
phase: 11-demo-surface-docs
plan: 04
subsystem: seed
tags: [seed, collection-run, health-card, gap-closure, idempotent, demo, honest-data]

# Dependency graph
requires:
  - phase: 11-demo-surface-docs
    provides: 11-01 시각 마감(헬스 카드 렌더), 11-03 데모 재현 문서(seed→3화면)
  - phase: 03-collection-health
    provides: CollectionRun 도메인·findTopByOrderByStartedAtDesc·CollectionHealthService 읽기 경로(무변경 재사용)
provides:
  - SyntheticDemoData.seed()가 합성 SUCCESS collection_run을 멱등 적재 — seed 데모 헬스 카드가 12/12 SUCCESS로 렌더(UAT Test 3 gap 닫음)
  - CollectionRunRepository.existsByStartedAtAndStatus 멱등 가드 — 같은 그리드 재시드 중복 방지
  - SyntheticDemoDataIT 회귀 단언 — 과거 AUTH_ERROR run 덮기 + 멱등 고정
affects: [verify-work, milestone-complete]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "seed 데이터 일관성: snapshot/event뿐 아니라 collection_run도 함께 심어 읽기 경로(헬스 카드)가 표현할 '수집 성공' 행을 공급 — 읽기 로직 0줄 변경으로 gap 해소"
    - "멱등 가드 일관: existsByStartedAtAndStatus(gridNow,SUCCESS) — snapshot=DATA-01 unique, event=count==0과 동일한 재시드 무해 계약"
    - "D-12 시크릿 미노출: seed가 쓰는 SUCCESS run의 summaryMessage를 명시적 null로 둬 실제 tick과 동일 계약"

key-files:
  created:
    - .planning/phases/11-demo-surface-docs/11-04-SUMMARY.md
  modified:
    - src/main/java/com/lostark/tracker/repository/CollectionRunRepository.java
    - src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java
    - src/main/java/com/lostark/tracker/seed/SeedDataRunner.java
    - src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java

key-decisions:
  - "근본 원인=seed 데이터 공급 누락(읽기 로직 결함 아님) → 수정은 seed 쪽에만. CollectionHealthService/Controller/DTO/HealthCard.tsx/Flyway/CollectionRun 모델 0줄 변경"
  - "startedAt=finishedAt=gridNow로 적재 — gridNow가 최신이라 findTopByOrderByStartedAtDesc()가 영속 볼륨의 과거 키리스 AUTH_ERROR run 대신 합성 SUCCESS를 반환"
  - "SeedSummary에 runs 필드 추가(권장안 채택) — 기존 IT/Runner는 snapshots()/events() 접근자만 사용하므로 안전하게 확장. SeedDataRunner 로그도 collection runs 카운트 추가"
  - "summaryMessage=null 명시(카테고리컬 마커조차 없음) → 시크릿 노출 경로 0(T-11-04-01 mitigate)"

patterns-established:
  - "영속 볼륨 잔존 시나리오 회귀 테스트: 과거 FAILED+AUTH_ERROR run 선적재 → seed() → latest가 SUCCESS로 덮이는지 단언(staleness override 고정)"

requirements-completed: [DEMO-01]

# Metrics
duration: ~12 min
completed: 2026-06-27
---

# Phase 11 Plan 04: seed SUCCESS collection_run (UAT Test 3 gap closure) Summary

**`SyntheticDemoData.seed()`가 timeline/impact와 일관된 합성 SUCCESS `collection_run`(시도=성공=활성품목수, 실패=0, 마커 없음, startedAt=gridNow)을 멱등 적재하도록 수정 — seed 데모의 수집 헬스 카드가 영속 볼륨의 과거 키리스 AUTH_ERROR run 대신 '12/12 SUCCESS'를 보이게 한다. 읽기 경로·DTO·프론트·스키마 0줄 변경, Testcontainers 회귀 테스트로 고정.**

## Performance

- **Duration:** ~12 min
- **Tasks:** 3 (repo 멱등 가드 / seed SUCCESS run 적재 / IT 회귀 단언)
- **Files modified:** 4 (CollectionRunRepository, SyntheticDemoData, SeedDataRunner, SyntheticDemoDataIT)

## Accomplishments
- **근본 원인 진단·수정:** UAT Test 3에서 seed 데모 헬스 카드가 '12/12 실패 · 인증오류'로 뜬 원인은 `seed()`가 snapshot/event만 심고 `collection_run`을 안 심어, 헬스 카드(`findTopByOrderByStartedAtDesc()`)가 표현할 '수집 성공' 행이 없었기 때문. 영속 볼륨에 남은 과거 키리스 AUTH_ERROR run(또는 NO_RUNS)이 seed 데모의 얼굴이 되던 문제를 seed 데이터 공급 보강으로 해소.
- **CollectionRunRepository 멱등 가드(Task 1):** `existsByStartedAtAndStatus(OffsetDateTime, String)` 파생 쿼리 추가. `findTopByOrderByStartedAtDesc()` 무변경.
- **seed() 합성 SUCCESS run 멱등 적재(Task 2):** 활성 품목 존재 시 `new CollectionRun(gridNow, gridNow, n, n, 0, "SUCCESS")` + `setSummaryMessage(null)`을 `existsByStartedAtAndStatus(gridNow,"SUCCESS")` 가드 하에 적재. 실제 tick과 동일 엔티티/생성자·D-12 마커 null. `SeedSummary`에 `runs` 필드 추가, `SeedDataRunner` 로그에 collection runs 카운트 반영.
- **회귀 테스트(Task 3):** `seedWritesSuccessRunThatBeatsAStalePersistedAuthErrorRun()` — 과거 FAILED+AUTH_ERROR run 선적재 후 `seed()`를 호출하면 latest run이 SUCCESS(attempted==succeeded==활성품목수, failed==0, summaryMessage==null)로 덮이고, 같은 그리드 재시드가 SUCCESS run을 중복 생성하지 않음(count 불변)을 단언.

## Verification
- **타겟 IT 그린:** `./gradlew test --tests SyntheticDemoDataIT` — 기존 2개(timeline/event-impact, snapshot 멱등) + 신규 1개 모두 통과.
- **전체 스위트 그린:** `./gradlew test` EXIT=0 — 생성자 시그니처 변경(`SyntheticDemoData`에 `CollectionRunRepository` 주입)의 광범위 영향 없음. CollectionResilienceIT/PriceCollectionIT/LatestPriceCacheIT 등 회귀 0(seed SUCCESS run은 별개 행이라 스냅샷/이벤트/캐시 단언에 무영향).
- **읽기 경로 무변경 확인:** diff는 seed/repository/test 4파일에만 — CollectionHealthService/Controller/DTO/HealthCard.tsx/Flyway/CollectionRun 모델/PriceCollector 무변경.

## Threat Model 대응
- **T-11-04-01 (Information Disclosure, mitigate):** seed가 쓰는 SUCCESS run의 `summaryMessage`를 명시적 null로 둬 카테고리컬 마커조차 없음 → /api/health/collection 시크릿 노출 경로 0. 실제 tick의 D-12/D-14 계약과 동일.
- **T-11-04-02 (Spoofing, accept):** seed 프로파일은 데모 전용(`SeedDataRunner @Profile("seed")`)이고 합성 데이터임을 frontend/README가 정직히 명시. prod-default/dev/test는 `seed()`를 호출하지 않아 실데이터 헬스 무영향.

## Task Commits

1. **Task 1: CollectionRunRepository 멱등 가드** — `6c7cf0f` (feat)
2. **Task 2: seed() 합성 SUCCESS run 멱등 적재** — `d03a9bb` (feat)
3. **Task 3: SyntheticDemoDataIT 회귀 단언** — `4c13f44` (test)

**Plan metadata:** SUMMARY + 추적 파일 커밋 (docs: complete plan)

## Files Modified
- `src/main/java/com/lostark/tracker/repository/CollectionRunRepository.java` — `existsByStartedAtAndStatus` 멱등 가드 추가
- `src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java` — `CollectionRunRepository` 주입 + `seed()` SUCCESS run 멱등 적재 + `SeedSummary.runs`
- `src/main/java/com/lostark/tracker/seed/SeedDataRunner.java` — 로그에 collection runs 카운트 반영
- `src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java` — staleness override + 멱등 회귀 단언

## Decisions Made
- **수정 위치를 seed에 국한:** 읽기 경로(헬스 카드)는 올바르게 '마지막 실행'을 표현 중이었고, 결함은 그 입력 데이터(collection_run)의 부재였음. 읽기 로직을 건드리면 실데이터 동작에 회귀 위험 → seed 데이터 공급만 보강.
- **SeedSummary runs 필드 확장(권장안):** silently 적재 대신 요약/로그에 노출해 운영 가시성 확보. 기존 소비자는 접근자만 쓰므로 record 확장이 안전.

## Deviations from Plan
None. 3개 task를 plan 그대로 실행(SeedSummary 권장안 채택). 추가 편차 없음.

## Issues Encountered
None. (LF→CRLF 경고는 Windows 워킹트리 표준 경고로 무관.)

## User Setup Required
None - 외부 서비스 설정 불필요.

## Next Phase Readiness
- **UAT Test 3 gap 닫힘:** seed 데모 헬스 카드가 seed 데이터와 일관된 '시도 12 · 성공 12 · 실패 0'(마커 없음)으로 표시 — 영속 볼륨의 과거 AUTH_ERROR run이 남아 있어도 최신 SUCCESS로 덮임.
- 권장 다음 단계: `/gsd-verify-work 11`로 UAT Test 3 재검증(클린 클론에서 seed 데모 헬스 카드 SUCCESS 확인) → `/gsd-complete-milestone`(v1.1 Frontend Demo Dashboard).

---
*Phase: 11-demo-surface-docs*
*Completed: 2026-06-27*
