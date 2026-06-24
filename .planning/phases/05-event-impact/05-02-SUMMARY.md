---
phase: 05-event-impact
plan: 02
subsystem: api
tags: [spring-boot, testcontainers, event-impact, data-quality, staleness, correlation]

requires:
  - phase: 05-event-impact
    provides: "05-01 EventImpactService(배치 anchor 계산) + EventImpactItem/Response + EventImpactController + STATUS_OK/STATUS_INSUFFICIENT 상수"
provides:
  - "EventImpactService 데이터 품질 게이트: change_rate 는 양쪽 anchor 가 존재 AND 둘 다 fresh(≤30min)일 때만 산출(IMPACT-02)"
  - "두 가지 구분 가능한 insufficient_data 사유: sparse(anchor time null) vs stale(anchor time 보고, gap>30min) (D-05)"
  - "EventImpactGuardIT — sufficiency/staleness/30분 경계(포함) 회귀 고정"
affects: []

tech-stack:
  added: []
  patterns:
    - "MVP 상수(Duration.ofMinutes(30))로 staleness 임계값 보유 — @ConfigurationProperties 외부화는 v2(CFG-V2-01)"
    - "presence floor(05-01) 위에 staleness 계층을 in-place 확장 — 기존 happy-path IT zero retrofit"
    - "boundary 포함 시맨틱을 정확/정확+1초 테스트로 명시(=30min fresh, 30min+1s stale)"

key-files:
  created:
    - src/test/java/com/lostark/tracker/read/EventImpactGuardIT.java
  modified:
    - src/main/java/com/lostark/tracker/read/EventImpactService.java

key-decisions:
  - "fresh(s,E) = Duration.between(s.collectedAt, E).abs().compareTo(Duration.ofMinutes(30)) <= 0 → 30분 경계 포함(D-03)"
  - "isFreshAnchor 헬퍼가 present+priced+fresh 를 한데 묶음 → ok 분기에서 pre/post non-null 보장"
  - "stale-insufficient 에서 가격(prePrice/postPrice)은 생략, anchor TIME 만 사유로 보고(05-02 재량 default)"

patterns-established:
  - "데이터 품질 게이트: 시계열 공백/노후 데이터에서 오해 소지 있는 숫자(change_rate)를 산출하지 않고 사유와 함께 withhold"

requirements-completed: [IMPACT-02]

duration: ~15min
completed: 2026-06-24
---

# Phase 5 (event-impact) · Plan 05-02 Summary

**`EventImpactService` 에 30분 staleness + sufficiency 게이트를 in-place 로 얹어, 양쪽 anchor 가 존재하고 둘 다 fresh 일 때만 `change_rate` 를 산출하고 그 외에는 sparse(null)·stale(시간 보고)을 구분해 `insufficient_data` 로 반환.**

## Performance

- **Duration:** ~15 min
- **Completed:** 2026-06-24
- **Tasks:** 2
- **Files modified:** 2 (생성 1, 수정 1)

## Accomplishments

- `STALENESS_ALLOWANCE = Duration.ofMinutes(30)` MVP 상수 추가(D-03). `@ConfigurationProperties`/`@Value` 미사용(v2 CFG-V2-01).
- `change_rate` 산출 조건을 "양쪽 anchor present + priced + 둘 다 fresh"로 강화(D-02/D-04). 한쪽이라도 missing/stale → `insufficient_data`, change_rate 없음.
- `insufficient_data` 는 발견된 in-window anchor time 을 양쪽에 보고하고, 해당 side 에 in-window 스냅샷이 0개일 때만 null → **sparse(null) vs stale(시간 보고) 구분(D-05)**.
- 30분 경계 **포함**(`<= 30min` fresh): 정확히 30분 → ok, 30분 1초 → stale.
- `EventImpactGuardIT` 6/6 그린. 05-01 `EventImpactIT` 11/11 무수정 통과(zero retrofit). 전체 스위트 84 tests, 0 failures.

## Task Commits

1. **Task 1: staleness allowance + sufficiency/both-fresh gate + anchor-time reporting** — `18a1970` (feat)
2. **Task 2: EventImpactGuardIT (sufficiency/staleness/boundary)** — `4f29d25` (test)

## Files Created/Modified

- `src/main/java/com/lostark/tracker/read/EventImpactService.java` — `STALENESS_ALLOWANCE` 상수 + `isFreshAnchor` 헬퍼 + ok/insufficient 결정 강화
- `src/test/java/com/lostark/tracker/read/EventImpactGuardIT.java` — 6개 가드 케이스(Testcontainers)

## Decisions Made

- `fresh` 판정은 `Duration.between(anchor, E).abs() <= 30min` 으로 경계 포함(D-03).
- stale-insufficient 응답에 가격은 생략하고 anchor TIME 만 사유 신호로 보고(plan 재량 default).

## Deviations from Plan

None — 계획대로 실행. (superpowers `test-driven-development`: GuardIT 를 먼저 작성 → stale 케이스 3건이 "ok" 를 반환해 RED → staleness 게이트 구현으로 GREEN. `java-springboot` 규약 유지: 불변 상수, 순수 in-memory 계산, DTO/컨트롤러/리포지토리 무변경.)

## Issues Encountered

None.

## User Setup Required

None — 읽기 전용 변경, 마이그레이션/의존성 추가 없음.

## Next Phase Readiness

- IMPACT-01·IMPACT-02 모두 충족 → Phase 5(event-impact) 완료. ROADMAP Success Criteria 1(change_rate)·2(sparse→insufficient_data+null)·3(stale→insufficient_data+anchor time) 모두 IT 로 증명.
- 헤드라인 기능(event-impact 상관) 동작; 수집·저장·서빙 파이프라인(Phase 1-3) 위에 안전한 데이터 품질 게이트로 얹힘.

---
*Phase: 05-event-impact*
*Completed: 2026-06-24*
