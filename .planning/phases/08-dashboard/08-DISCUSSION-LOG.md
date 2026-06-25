# Phase 8: Dashboard - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-25
**Phase:** 8-dashboard
**Areas discussed:** 목록+최신가 통합 방식, N+1 latest 패칭 전략, status 의미 등급 + summaryMessage, 카드별 빈/대기 상태

---

## 목록 + 최신가 통합 방식 (DASH-03 + DASH-04 레이아웃)

| Option | Description | Selected |
|--------|-------------|----------|
| 품목당 통합 카드 그리드 | 품목 하나 = 카드 하나에 displayName·category·minPrice·collectedAt(KST). health 카드는 상단 별도 | ✓ |
| 목록 / 최신가 섹션 분리 | 활성 품목 목록을 위, 최신가 요약을 아래 별도 섹션 | |
| 단일 테이블(행=품목) | 행=품목, 열=이름·카테고리·minPrice·수집시각의 밀도 높은 표 | |

**User's choice:** 품목당 통합 카드 그리드
**Notes:** "health(살아있음) → 무엇을 추적 → 지금 얼마"의 정보 위계가 ROADMAP 사용자 확인 포인트에 부합. → CONTEXT D-01/D-02.

---

## N+1 latest 패칭 전략 (DASH-04)

| Option | Description | Selected |
|--------|-------------|----------|
| 카드별 개별 useLatestPrice(id) | 각 카드가 자기 훅 + 자기 AsyncBoundary. React Query 자동 병렬·개별 캐시·개별 재시도 | ✓ |
| useQueries로 한 곳에서 배치 | 부모가 N개 쿼리를 묶어 집계 상태, 카드는 prop으로 값 받음 | |
| 목록 로드 후 일괄 프리페치 | useItems 성공 후 각 id prefetch, 첫 페인트는 느림 | |

**User's choice:** 카드별 개별 useLatestPrice(id)
**Notes:** 배치 엔드포인트 없음 → 품목당 1요청 불가피. 카드 독립성 우선, 한 품목 실패가 전체를 안 가림. → CONTEXT D-03/D-04. 배치 API는 Deferred.

---

## status 의미 등급 + summaryMessage (DASH-01 + DASH-02)

| Option | Description | Selected |
|--------|-------------|----------|
| 4등급 + summaryMessage 진단 마커 | SUCCESS=정상/PARTIAL_SUCCESS=경고/FAILED=위험/NO_RUNS=대기. summaryMessage는 진단 보조 텍스트 | ✓ |
| 3등급 (FAILED를 경고에 합침) | 정상/주의/대기 — ROADMAP 원안에 가깝게 단순화 | |
| 정상/비정상 2등급 | SUCCESS만 정상, 나머지 비정상 | |

**User's choice:** 4등급 + summaryMessage 진단 마커
**Notes:** 실측 코드 4값(PriceCollector.java)을 그대로 등급화. ROADMAP이 빠뜨린 FAILED를 별도 '위험'으로 구분 — honest-data 에토스. 색/배지 시각은 08-UI-SPEC 소관. → CONTEXT D-05/D-06.

---

## 카드별 빈/대기 상태 (완료조건 #5)

| Option | Description | Selected |
|--------|-------------|----------|
| 카드별 독립 처리 | 위젯마다 자체 AsyncBoundary/EmptyState. 한 품목 실패가 나머지·health 안 가림 | ✓ |
| 화면 단위 처리 | 하나라도 실패/빈이면 화면 전체를 빈/에러 상태로 | |
| 혼합 | health/품목 영역 독립, 품목들은 목록 단위로 빈/에러 | |

**User's choice:** 카드별 독립 처리
**Notes:** NO_RUNS=health 카드 대기 카피, 품목 latest 404=그 카드만 데이터 없음, 품목 0개=그리드 EmptyState. D-03(카드별 훅)·Phase 7 D-08과 결합. → CONTEXT D-07/D-08.

---

## Claude's Discretion

- 그리드 컬럼 수·반응형 브레이크포인트, 카드 내부 필드 순서, health 카운트 표현 방식 (08-UI-SPEC/planner)
- 절대 KST(기본) vs 상대시간("3분 전") 표시
- `isEmpty` 술어 세부, 품목 latest 404를 EmptyState vs ErrorState로 볼지(최초 수집 전은 빈 상태 방향)

## Deferred Ideas

- 배치 latest 엔드포인트(`/api/items/latest?ids=`) — 백엔드 변경, v2
- 실시간 자동 갱신(폴링/SSE) — FE-V2-02
- 상대시간 표시 — 재량 보류
- 품목 카드 → 타임라인/임팩트 딥링크 — Phase 9~10 selector 도입 후
