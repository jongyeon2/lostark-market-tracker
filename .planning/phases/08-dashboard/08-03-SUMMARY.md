---
phase: 08-dashboard
plan: 03
subsystem: ui
tags: [react, react-query, shadcn, tailwind, dashboard, fan-out]

# Dependency graph
requires:
  - phase: 08-dashboard (08-02)
    provides: "HealthCard 전폭 proof-of-life 카드(자체 AsyncBoundary)"
  - phase: 08-dashboard (08-01)
    provides: "StatusBadge / SummaryMarker (HealthCard 경유 간접)"
  - phase: 07-frontend-foundation
    provides: "useItems()/useLatestPrice(id) 훅, <AsyncBoundary>, formatKst, shadcn Card/Skeleton, trackedItemSchema/latestPriceSchema, ApiError(non-2xx throw)"
provides:
  - "ItemCard — 품목 1개 통합 카드(D-01): displayName(Heading)+category(Label) 상단(항상 노출), 하단은 자체 useLatestPrice(item.id) fan-out + 카드별 인라인 async 분기(pending→skeleton, error/404→'최신가 아직 없음', success→minPrice tabular-nums + collectedAt formatKst) (DASH-03/04, D-03/D-07)"
  - "DashboardPage — '대시보드' 제목 + 전폭 HealthCard + useItems 반응형 카드 그리드(1→2→3열), 그리드 레벨 AsyncBoundary(0개→EmptyState), 백엔드 응답 순서 유지(D-01/D-02), Phase-7 임시 목록 대체"
affects: [09-timeline, 10-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "카드별 fan-out(D-03/D-04): 품목당 useLatestPrice(id) 개별 호출 — React Query가 N요청 병렬·캐시·재시도 독립 처리, 한 카드 실패가 이웃·health에 무영향"
    - "가격 영역은 공유 AsyncBoundary 미사용 — status 직접 인라인 분기로 404/미수집을 카드 레벨 빈('최신가 아직 없음')으로 처리(에러 화면 아님, 08-CONTEXT line 39)"
    - "그리드 레벨 vs 카드 레벨 vs health 레벨 경계 분리(D-07/D-08): 각 영역이 독립 로딩/에러/빈"

key-files:
  created:
    - frontend/src/features/dashboard/ItemCard.tsx
  modified:
    - frontend/src/features/dashboard/DashboardPage.tsx

key-decisions:
  - "ItemCard 가격 영역은 status 직접 분기(공유 AsyncBoundary 미사용) — AsyncBoundary의 error 경로는 화면 전체 '다시 불러오기' ErrorState라 단일 가격 셀에 부적합; 404/미수집은 카드 레벨 빈으로"
  - "displayName/category는 가격 분기 바깥에 배치 — 이미 로드된 식별자는 가격 로딩/실패와 무관하게 항상 노출"
  - "minPrice는 toLocaleString('ko-KR')로 천단위 구분 + tabular-nums — 골드 가격 가독성, 자릿수 정렬 유지"
  - "data?.map 순서 그대로 렌더, .sort 없음(D-02) — 백엔드가 displayName 정렬 보장"

patterns-established:
  - "feature/dashboard 그리드 화면: 전폭 focal 카드 + 그리드 레벨 AsyncBoundary + 항목별 fan-out 카드 — Phase 9~10 selector/차트 화면의 기준 패턴"

requirements-completed: [DASH-03, DASH-04]

# Metrics
duration: 4 min
completed: 2026-06-25
---

# Phase 08 Plan 03: Dashboard Assembly Summary

**`DashboardPage`를 '대시보드' 제목 + 전폭 `HealthCard` + `useItems` 반응형 카드 그리드로 재작성하고, 품목당 자체 `useLatestPrice` fan-out으로 minPrice/collectedAt를 표시하는 `ItemCard`를 추가 — 'health → 무엇을 추적 → 지금 얼마'의 단일 세로 스크롤 대시보드 완성.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-06-25T12:43:47Z
- **Completed:** 2026-06-25T12:47:40Z
- **Tasks:** 2
- **Files modified:** 2 (1 생성, 1 재작성)

## Accomplishments
- `ItemCard`(DASH-03/04, D-01): 상단 displayName(Heading)+category(Label) 항상 노출, 하단은 자체 `useLatestPrice(item.id)` fan-out + 카드별 인라인 분기(pending→skeleton, error/404→'최신가 아직 없음' neutral, success→minPrice tabular-nums + 수집 시각 formatKst)
- `DashboardPage`(DASH-03, D-01/D-02): '대시보드' 제목 + 전폭 HealthCard + `useItems` 그리드(`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`, gap lg) — 백엔드 순서 유지, 그리드 레벨 AsyncBoundary(0개→Phase-7 EmptyState), Phase-7 임시 `<li>` 목록 제거
- 3중 경계 격리(완료조건 #5, D-07/D-08): health / 그리드 / 카드별 가격이 각자 로딩·에러·빈을 그려 한 곳의 실패가 화면을 비우지 않음
- fan-out 비용(D-04)을 정직하게 수용: 배치 엔드포인트 없음(백엔드 변경=범위 밖→Deferred), seed 워치리스트는 소규모라 N요청 허용
- 새 훅·폴링·selector·차트·쓰기 표면 없음, `npm run build` 무오류, Java `src/` 무변경

## Task Commits

1. **Task 1: ItemCard — per-card useLatestPrice fan-out + 인라인 async 분기** - `8ee7df0` (feat)
2. **Task 2: DashboardPage — HealthCard + 반응형 ItemCard 그리드** - `73408fc` (feat)

**Plan metadata:** (이 SUMMARY 커밋)

## Files Created/Modified
- `frontend/src/features/dashboard/ItemCard.tsx` (생성) - 품목 통합 카드: 식별자 항상 노출 + 카드별 latest fan-out
- `frontend/src/features/dashboard/DashboardPage.tsx` (재작성) - 제목 + 전폭 HealthCard + useItems 반응형 그리드, 임시 목록 대체

## Decisions Made
- **가격 영역 인라인 분기:** ItemCard의 latest 영역은 공유 `<AsyncBoundary>`를 쓰지 않고 `status`를 직접 분기 — 404/미수집을 화면 ErrorState가 아닌 카드 레벨 빈('최신가 아직 없음')으로 표현(08-CONTEXT line 39 재량).
- **식별자 분리 노출:** displayName/category를 가격 분기 바깥에 둬 가격 로딩/실패와 독립적으로 항상 보이게 함.
- **가격 포맷:** `toLocaleString('ko-KR')` 천단위 구분 + `tabular-nums`로 골드 가격 가독성·정렬 확보(스코프 내 표시 개선, 단위 라벨은 미명시라 미추가).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. (Task 2 작성 중 설명 주석의 '`<li>`' 단어가 플랜 자동 검증 `! grep "<li"`에 걸려, 실제 `<li>` JSX 제거 사실을 흐리지 않도록 주석 문구를 'temporary placeholder list'로 다듬어 검증을 깨끗이 통과시킴 — 동작 변화 없음)

## User Setup Required
None - 외부 서비스 설정 불필요. (수동 확인 시 seed 백엔드 `:8080` 기동 + `npm run dev` → `/dashboard` 권장)

## Next Phase Readiness
- Phase 8 Dashboard 완성: health 카드 + 품목 카드 그리드(최신가 fan-out)가 한 화면에서 동작.
- Phase 9(타임라인)·10(이벤트 영향)은 품목 selector 도입 후 ItemCard→차트 딥링크로 자연 연결 가능(현 범위 밖).
- 블로커 없음.

---
*Phase: 08-dashboard*
*Completed: 2026-06-25*
