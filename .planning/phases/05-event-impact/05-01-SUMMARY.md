---
phase: 05-event-impact
plan: 01
subsystem: api
tags: [spring-boot, jpa, postgres, testcontainers, event-impact, correlation, n+1]

requires:
  - phase: 03-read-api-cache
    provides: PriceSnapshotRepository 범위 파인더, WindowQueryService(4A) 경계 시맨틱, ApiExceptionHandler/InvalidRequestException/ItemNotFoundException 에러 계약, PricesController validate-then-exists 패턴
  - phase: 04-admin-events
    provides: 인증된 POST /api/admin/events 쓰기 경로, AdminAuth 테스트 헬퍼(X-Admin-Secret), /api/items/** permitAll SecurityConfig
provides:
  - "GET /api/items/{id}/event-impact?window=N — 이벤트별 anchor-delta change_rate(=post/pre-1, min_price) 읽기 엔드포인트(IMPACT-01)"
  - "EventImpactService — 단일 배치 읽기(이벤트 1회 + 스냅샷 범위 1회) 기반 in-memory anchor 계산, N+1 회피(D-09)"
  - "EventImpactResponse / EventImpactItem DTO — 요청 echo + occurred_at desc 이벤트 리스트 + status 필드"
  - "GameEventRepository.findAllByOrderByOccurredAtDesc() — 전역 이벤트 배치 소스"
affects: [05-02-event-impact-guards]

tech-stack:
  added: []
  patterns:
    - "전역 이벤트 1회 fetch + 전체 span 스냅샷 1회 fetch 후 in-memory anchor 도출 (N+1 회피, D-09/T7)"
    - "@MockitoSpyBean 으로 리포지토리 호출 횟수(times(1))를 단언해 배치 읽기를 증명"
    - "anchor 가격 필드명(prePrice/postPrice)은 평균이 아닌 '앵커 스냅샷 가격'임을 이름으로 표현(D-01)"

key-files:
  created:
    - src/main/java/com/lostark/tracker/web/dto/EventImpactResponse.java
    - src/main/java/com/lostark/tracker/web/dto/EventImpactItem.java
    - src/main/java/com/lostark/tracker/read/EventImpactService.java
    - src/main/java/com/lostark/tracker/web/EventImpactController.java
    - src/test/java/com/lostark/tracker/read/EventImpactIT.java
  modified:
    - src/main/java/com/lostark/tracker/repository/GameEventRepository.java

key-decisions:
  - "change_rate = post/pre - 1 을 BigDecimal scale 4, HALF_UP 으로 표현(D-01 재량) — 안정적 스케일 유지"
  - "window cap 168h(7일) 상수를 EventImpactController 에 보유 — >cap 은 400(D-08)"
  - "STATUS_OK/STATUS_INSUFFICIENT 를 private static final 로 EventImpactService 에 정의 — 05-02 가 동일 클래스에서 재사용"
  - "presence floor(05-01)에서도 insufficient_data 는 발견된 present-side anchor time 을 보고 → 05-02 staleness 계층이 무수정 확장 가능"

patterns-established:
  - "검증-후-존재확인(validate-then-exists): window 400 검사를 existsById 404 검사보다 먼저(PricesController/D-13 미러)"
  - "읽기 IT: @SpringBootTest(RANDOM_PORT) + @ActiveProfiles(test) + TestRestTemplate + getForEntity(String.class) raw-body 계약 단언"

requirements-completed: [IMPACT-01]

duration: ~25min
completed: 2026-06-24
---

# Phase 5 (event-impact) · Plan 05-01 Summary

**`GET /api/items/{id}/event-impact?window=N` — 게임 이벤트별 anchor 단일 스냅샷 delta `change_rate = post/pre − 1`(min_price)을 단일 배치 읽기로 계산해 occurred_at desc 로 반환하는 v1 상관 엔드포인트.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-06-24
- **Tasks:** 2
- **Files modified:** 6 (생성 5, 수정 1)

## Accomplishments

- `EventImpactService`: 전역 이벤트 1회(`findAllByOrderByOccurredAtDesc`) + 아이템 스냅샷 전체 span 1회(`findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc`) 읽기 후, 각 이벤트의 pre(=[E−Nh, E]의 마지막)·post(=[E, E+Nh]의 첫) anchor 를 in-memory 로 도출. N+1 없음(D-09).
- `EventImpactController`: `GET /api/items/{id}/event-impact?window=N` — window ≤0 / >168h → 400(`InvalidRequestException`), 누락/비정수 → 400(기존 핸들러), 없는 아이템 → 404(`ItemNotFoundException`). 검증이 존재확인보다 먼저(D-08/D-13). 새 예외 타입 0개.
- `EventImpactResponse`/`EventImpactItem` DTO + `GameEventRepository.findAllByOrderByOccurredAtDesc()`.
- `EventImpactIT` 11개 케이스 그린: happy-path change_rate, occurred_at desc, empty-events 200, missing-item 404, window 매트릭스(0/음수/>cap/누락/비정수) 400, **N+1 가드(@MockitoSpyBean times(1))**, **admin 등록→상관 E2E 슬라이스**.

## Task Commits

1. **Task 1: Response DTOs + GameEventRepository finder + EventImpactService** — `c6c7e0b` (feat)
2. **Task 2: EventImpactController + EventImpactIT (happy/contract/N+1/E2E)** — `2effd78` (feat)

## Files Created/Modified

- `src/main/java/com/lostark/tracker/read/EventImpactService.java` — 배치 anchor-delta 계산(핵심)
- `src/main/java/com/lostark/tracker/web/EventImpactController.java` — 엔드포인트 + window/404 계약
- `src/main/java/com/lostark/tracker/web/dto/EventImpactResponse.java` — 최상위 wrapper 레코드
- `src/main/java/com/lostark/tracker/web/dto/EventImpactItem.java` — 이벤트별 레코드(status + nullable anchor/impact)
- `src/main/java/com/lostark/tracker/repository/GameEventRepository.java` — `findAllByOrderByOccurredAtDesc()` 추가(`findByOccurredAtBetween` 미변경)
- `src/test/java/com/lostark/tracker/read/EventImpactIT.java` — Testcontainers IT(11 케이스)

## Decisions Made

- `change_rate` 표현은 BigDecimal scale 4 / HALF_UP (D-01 재량). 테스트는 `isEqualByComparingTo` 로 스케일 무관 비교.
- window cap = 168h 를 컨트롤러 상수로 보유(D-08). `@ConfigurationProperties` 외부화는 v2.
- happy-path IT 의 anchor 를 이벤트 ±10분에 배치 → 05-02 staleness(30분) 게이트가 무수정 통과.

## Deviations from Plan

None — 계획대로 실행. (superpowers `test-driven-development` 스킬 적용: IT 를 먼저 작성해 RED(컴파일 실패=기능 부재) 확인 후 production 코드로 GREEN. `java-springboot` 스킬의 생성자 주입·DTO·`@Service`·파생 쿼리 규약 준수.)

## Issues Encountered

None.

## User Setup Required

None — 외부 서비스 설정 불필요(읽기 전용, 마이그레이션/의존성 추가 없음).

## Next Phase Readiness

- 05-02 (Wave 2)가 동일 `EventImpactService` 에 staleness/sufficiency 게이트를 in-place 로 얹을 준비 완료: `STATUS_OK`/`STATUS_INSUFFICIENT` 상수와 `EventImpactItem` 모양 재사용, insufficient_data 가 이미 present-side anchor time 보고.
- happy-path IT 가 fresh anchor(±10분)라 05-02 도입 시 zero retrofit.

---
*Phase: 05-event-impact*
*Completed: 2026-06-24*
