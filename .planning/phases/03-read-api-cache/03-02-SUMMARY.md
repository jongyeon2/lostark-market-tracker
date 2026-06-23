---
phase: 03-read-api-cache
plan: 02
subsystem: api
tags: [spring-boot, jpa, timeline, window-query, game-event, testcontainers]

# Dependency graph
requires:
  - phase: 03-read-api-cache
    provides: "03-01 ItemNotFoundException + ApiExceptionHandler 404 계약; Phase 2 insert 경로 옆 읽기 전용 파인더 패턴"
  - phase: 02-collection-pipeline
    provides: "PriceSnapshot 엔티티 + GameEvent 엔티티 (game_event 테이블)"
provides:
  - "GET /api/items/{id}/prices?from=&to= — 두 배열 타임라인 {snapshots, events} (D-04)"
  - "WindowQueryService.fetchWindow (4A 공유 윈도우 쿼리) — Phase 5 event-impact 재사용 (D-06)"
  - "GameEventRepository.findByOccurredAtBetween (occurred_at 양끝 포함 containment, D-05)"
  - "PriceSnapshotRepository.findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc (읽기 전용 윈도우 파인더)"
  - "TimelineResponse / SnapshotPoint / EventPoint DTO"
affects: [03-03-downsample-validation-health, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "4A 공유 윈도우 쿼리를 순수 데이터 액세스 서비스(두 쿼리, N+1 없음)로 추출해 페이즈 간 재사용"
    - "타임라인을 직교하는 두 배열로 — 다운샘플이 events를 건드리지 않고 snapshots만 축소 가능"
    - "범위/윈도우 읽기는 DB 직조회(캐시 안 함) — latest만 캐시"

key-files:
  created:
    - src/main/java/com/lostark/tracker/repository/GameEventRepository.java
    - src/main/java/com/lostark/tracker/read/WindowQueryService.java
    - src/main/java/com/lostark/tracker/web/dto/TimelineResponse.java
    - src/main/java/com/lostark/tracker/web/dto/SnapshotPoint.java
    - src/main/java/com/lostark/tracker/web/dto/EventPoint.java
    - src/main/java/com/lostark/tracker/web/PricesController.java
    - src/test/java/com/lostark/tracker/read/WindowQueryServiceIT.java
    - src/test/java/com/lostark/tracker/read/TimelinePricesIT.java
  modified:
    - src/main/java/com/lostark/tracker/repository/PriceSnapshotRepository.java

key-decisions:
  - "D-04: 타임라인은 독립된 두 배열 {snapshots, events} 반환"
  - "D-05: 이벤트 겹침은 양끝 포함 containment from <= occurred_at <= to (Spring Data Between)"
  - "D-06: 4A 윈도우 쿼리를 WindowQueryService로 추출해 Phase 5 재사용 (두 쿼리, N+1 없음)"
  - "D-11: from/to를 UTC OffsetDateTime으로 파싱, 응답은 UTC ISO-8601, KST 변환 없음"
  - "D-13 (빈 절반): 유효하나 데이터 없는 범위 → 200 빈 배열; 없는 품목 → 404"

patterns-established:
  - "타임라인은 지금, event-impact는 나중에 소비하는 공유 윈도우 쿼리 서비스"
  - "/prices 파일 소유권을 03-01의 ItemController와 분리하기 위한 별도 PricesController"

requirements-completed: [API-03]

# Metrics
duration: ~15분
completed: 2026-06-23
---

# Phase 3 Plan 02: 타임라인 읽기 + 공유 윈도우 쿼리 요약

**`GET /api/items/{id}/prices?from=&to=`가 윈도우 내 스냅샷과 겹치는 이벤트를 UTC 두 배열로 반환하며, Phase 5가 재사용할 재사용 가능한 4A `WindowQueryService`가 양끝 포함 경계 시맨틱과 함께 테스트됨.**

## 성능

- **소요 시간:** ~15분
- **완료:** 2026-06-23
- **태스크:** 2개 (전부 테스트 기반)
- **변경 파일:** 9개 (생성 8, 수정 1)

## 주요 성과
- `WindowQueryService.fetchWindow` (4A) — 스냅샷 윈도우 파인더 + `GameEventRepository.findByOccurredAtBetween`을 정확히 두 쿼리로 묶어 `WindowResult{snapshots, events}` 반환; 순수 데이터 액세스(존재 검사·매핑 없음)라 Phase 5가 동일한 양끝 포함 UTC 경계 시맨틱을 재사용 (D-06).
- `GameEventRepository`(신규)로 기존 `game_event` 테이블 조회; 읽기 전용 윈도우 파인더를 `PriceSnapshotRepository`의 Phase 2 insert 경로 메서드 옆에 추가(기존 메서드 무수정).
- `PricesController` `GET /{id}/prices` — `from`/`to`를 UTC `OffsetDateTime`으로 파싱(KST 변환 없음, D-11), 없는 품목은 공유 03-01 advice로 404, `TimelineResponse{snapshots, events}`로 매핑 (D-04).
- IT가 양끝 포함 겹침(on-`from`/on-`to` 포함, 바로 바깥 제외 — D-05), 스냅샷 오름차순(순서 섞어 insert), KST 자정 UTC 인스턴트 라운드트립(off-by-9h 가드), 없는 품목 404, 빈 윈도우 200 + 빈 배열(D-13 빈 절반) 증명.

## 태스크 커밋

1. **Task 1: 4A 공유 윈도우 쿼리 (리포지토리 + WindowQueryService)** - `1e8ceae` (feat)
2. **Task 2: /prices 엔드포인트 + 두 배열 DTO** - `a71e433` (feat)

## 생성/수정 파일
- `repository/GameEventRepository.java` - 신규; `findByOccurredAtBetween` (양끝 포함)
- `read/WindowQueryService.java` - 4A 공유 윈도우 쿼리, `WindowResult` 레코드
- `web/dto/TimelineResponse.java` / `SnapshotPoint.java` / `EventPoint.java` - 두 배열 DTO
- `web/PricesController.java` - `GET /{id}/prices`
- `test/.../WindowQueryServiceIT.java` - 경계/정렬 증명
- `test/.../TimelinePricesIT.java` - 엔드포인트 증명 (두 배열, 404, 빈 윈도우)
- `repository/PriceSnapshotRepository.java` - 읽기 전용 윈도우 파인더 추가

## 결정 사항
잠긴 CONTEXT 결정(D-04/05/06/11/13) 외 추가 결정 없음. 재량: `WindowResult`를 서비스 내 중첩 레코드로; `PricesController`를 계획대로 `ItemController`와 분리.

## 계획 대비 이탈

없음 - 계획대로 실행됨.

## 마주친 이슈
없음. 전체 `./gradlew test -PdockerApiVersion=1.44` 그린: 37개 테스트, 실패 0, 에러 0.

## 사용자 셋업 필요
없음 - 외부 서비스 구성 불필요.

## 다음 페이즈 준비도
- 03-03이 바로 이 엔드포인트를 확장: `ApiExceptionHandler`에 400 범위 검증 핸들러 추가 + `/prices`에 서버 측 다운샘플 분기(스냅샷만 축소, events 무수정 — 두 배열 직교성 이미 확보).
- `WindowQueryService`는 Phase 5 event-impact의 잠긴 재사용 지점.
- 블로커 없음.

---
*Phase: 03-read-api-cache*
*완료: 2026-06-23*
