---
phase: 08-dashboard
plan: 02
subsystem: ui
tags: [react, react-query, shadcn, tailwind, health, async-boundary]

# Dependency graph
requires:
  - phase: 08-dashboard (08-01)
    provides: "StatusBadge({status}) / SummaryMarker({summaryMessage}) 표현 프리미티브 + shadcn badge"
  - phase: 07-frontend-foundation
    provides: "useCollectionHealth() 훅, <AsyncBoundary status/onRetry>, formatKst(off-by-9h 가드), shadcn Card/CardHeader/CardTitle/CardContent, collectionHealthSchema"
provides:
  - "HealthCard — /api/health/collection을 대시보드 최상단 전폭 proof-of-life 카드로 표현. 자체 <AsyncBoundary>로 로딩/에러 격리(D-07), 성공 시 '수집 헬스' + StatusBadge + 시도/성공/실패 카운트(tabular-nums, 실패>0 text-down) + 마지막 실행 formatKst + SummaryMarker, NO_RUNS는 대기 카피(에러 아님) (DASH-01/02)"
affects: [08-03 DashboardPage]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "위젯 자체 AsyncBoundary 격리(D-07): Card 바깥, AsyncBoundary 안 — 한 위젯 실패가 화면 전체를 비우지 않음"
    - "NO_RUNS는 success-path 분기(빈 상태 아님): health는 단일 객체라 AsyncBoundary.isEmpty 미사용, NO_RUNS는 실데이터 대기 카피로 처리"
    - "08-01 프리미티브 합성: 매핑을 재유도하지 않고 StatusBadge/SummaryMarker에 위임"

key-files:
  created:
    - frontend/src/features/dashboard/HealthCard.tsx
  modified: []

key-decisions:
  - "Card를 바깥, AsyncBoundary를 안에 배치 — 로딩/에러도 카드 프레임 안에서 렌더되어 위젯 단위 격리가 시각적으로도 성립(D-07/D-08)"
  - "NO_RUNS 대기 카피 타이포를 기존 EmptyState 컨벤션(Heading 20/600 + muted Body 16/400)에 맞춤 — 4사이즈/2웨이트 시스템 준수 + '대기' 톤"
  - "data 타입 내로잉은 `{data && <HealthContent health={data} />}` 가드로 — success 분기에서만 children 렌더되므로 안전"

patterns-established:
  - "feature/dashboard 데이터 카드: 단일 훅 → 자체 AsyncBoundary → 성공 경로 서브컴포넌트(health: typed prop)"

requirements-completed: [DASH-01, DASH-02]

# Metrics
duration: 5 min
completed: 2026-06-25
---

# Phase 08 Plan 02: Collection Health Card Summary

**`/api/health/collection`을 대시보드 최상단 전폭 proof-of-life 카드로 끌어올린 `HealthCard` — 자체 AsyncBoundary로 로딩/에러를 격리하고, 성공 시 4등급 StatusBadge + 시도/성공/실패 카운트(tabular-nums) + KST 마지막 실행 + 진단 마커를 렌더하며, NO_RUNS는 깨지지 않는 대기 카드로 표현.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-06-25T12:38:58Z
- **Completed:** 2026-06-25T12:43:47Z
- **Tasks:** 1
- **Files modified:** 1 (생성)

## Accomplishments
- `HealthCard`가 `useCollectionHealth()`를 자체 `<AsyncBoundary status onRetry>`로 감싸 health 실패가 아래 품목 그리드를 비우지 않도록 격리(D-07/D-08)
- 성공 분기(DASH-01/02): '수집 헬스' 제목 + 우측 `StatusBadge`, `시도 N · 성공 N · 실패 N` 카운트 행(tabular-nums, 실패>0이면 `text-down`), '마지막 실행' = `formatKst(lastRunAt)` 또는 '—', `SummaryMarker`(null이면 미표시)
- NO_RUNS 분기(완료조건 #5): '수집 대기' 배지 + '아직 수집 실행 기록이 없어요' 대기 카피 — ErrorState 아님, 깨지지 않음
- 새 훅·폴링·쓰기 표면 없음, `npm run build` 무오류, Java `src/` 무변경 — 08-03이 전폭으로 배치할 수 있도록 export

## Task Commits

1. **Task 1: HealthCard — useCollectionHealth via own AsyncBoundary** - `a49ebda` (feat)

**Plan metadata:** (이 SUMMARY 커밋)

## Files Created/Modified
- `frontend/src/features/dashboard/HealthCard.tsx` - Collection Health 카드: 단일 훅 + 자체 AsyncBoundary + 성공/NO_RUNS 분기, 08-01 프리미티브 합성

## Decisions Made
- **Card 바깥 / AsyncBoundary 안:** 로딩 스켈레톤·에러 상태도 카드 프레임 내부에서 렌더 → 위젯 단위 격리가 시각적으로도 성립.
- **NO_RUNS = success 분기:** health는 단일 객체이므로 AsyncBoundary의 `isEmpty`를 쓰지 않고, NO_RUNS를 성공 경로 안의 대기 카피 분기로 처리(빈 리스트가 아니라 '실데이터: 아직 실행 없음').
- **타이포 일관성:** 대기 카피를 기존 `EmptyState`와 동일한 Heading+muted Body로 맞춰 4사이즈/2웨이트 시스템 준수.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - 외부 서비스 설정 불필요. (수동 확인 시 seed 백엔드 `:8080` 기동 + `npm run dev` 권장 — verification 참고)

## Next Phase Readiness
- `HealthCard`가 export되어 08-03 `DashboardPage`가 최상단 전폭으로 배치 가능. 그 아래 품목 카드 그리드(`ItemCard` + `useItems`/`useLatestPrice`)를 08-03에서 조립.
- 블로커 없음.

---
*Phase: 08-dashboard*
*Completed: 2026-06-25*
