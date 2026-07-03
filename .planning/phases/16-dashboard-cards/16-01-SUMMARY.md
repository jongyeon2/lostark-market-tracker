---
phase: 16-dashboard-cards
plan: 01
subsystem: ui
tags: [react, react-router, tailwind, dashboard, deep-link]

requires:
  - phase: 07-frontend-foundation
    provides: URL-as-state 타임라인 계약(useTimelineParams가 ?item=을 단일 출처로 읽어 자동 선택, D-04)
  - phase: 14
    provides: v1.2 enrichment(ItemIcon·RoleBadge) — 무회귀 유지 대상
provides:
  - ItemCard에서 의미 없는 item.category 코드 줄 제거(CARD-01)
  - 카드 전체를 /timeline?item={id} 시맨틱 <Link> 딥링크로 래핑(CARD-02)
  - hover elevation/border 피드백 + focus-visible accent 링(D-05/D-06)
affects: [dashboard, timeline, ui-consistency]

tech-stack:
  added: []
  patterns:
    - "카드 전체를 react-router <Link>로 감싸는 딥링크(?item= 단일 파라미터, 타임라인 기본 30일 위임)"

key-files:
  created:
    - .planning/phases/16-dashboard-cards/16-01-SUMMARY.md
  modified:
    - frontend/src/features/dashboard/ItemCard.tsx

key-decisions:
  - "category 코드 줄은 itemGroup 등으로 대체하지 않고 완전 제거(D-01)"
  - "onClick+useNavigate 대신 시맨틱 <Link>로 새 탭/키보드/우클릭 기본 동작 확보(D-03)"
  - "딥링크는 ?item={id} 하나만 — from/to 미전달, 타임라인 최근 30일 기본이 처리(D-04)"
  - "accent(ring-ring)는 focus-visible 링에만, hover는 elevation+border 강조로 accent fill 금지(D-05)"

patterns-established:
  - "대시보드 카드 딥링크: <Link to=/timeline?item={id}>로 <Card> 래핑, 내부 자식은 전부 비인터랙티브 유지(단일 <a>)"

requirements-completed: [CARD-01, CARD-02]

duration: ~10min
completed: 2026-07-03
---

# Phase 16: 대시보드 카드 개선 Summary

**ItemCard에서 의미 없는 category 코드(예 "50010") 줄을 제거하고, 카드 전체를 `/timeline?item={id}` 시맨틱 `<Link>` 딥링크로 감싸 — enrichment·가격·수집시각은 무회귀로 유지**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-07-03
- **Completed:** 2026-07-03
- **Tasks:** 1
- **Files modified:** 1 (ItemCard.tsx)

## Accomplishments
- **CARD-01/D-01:** `CardHeader`의 `<p>{item.category}</p>` 코드 줄 완전 제거 — 카드는 아이콘·품목명·역할 배지·골드 가격·수집 시각만 표시(itemGroup 등 대체 없음).
- **CARD-02/D-03·D-04:** `<Card>` 전체를 `<Link to={`/timeline?item=${item.id}`}>`로 래핑 — 시맨틱 `<a>`라 Ctrl/중클릭 새 탭·Enter·우클릭 메뉴가 기본 동작. `?item=` 하나만 전달, `useTimelineParams`가 단일 출처로 읽어 자동 선택(타임라인 화면/선택 메커니즘 변경 0).
- **D-02 무회귀:** `ItemIcon`·`RoleBadge`·`data.minPrice.toLocaleString`·`formatKst`·`useLatestPrice` 팬아웃·pending/error/success 인라인 분기 그대로 유지.
- **D-05/D-06:** accent(`ring-ring`)는 `focus-visible:ring-*`에만 예약, hover는 `hover:shadow-md`+`hover:border-foreground/20` elevation·border 강조(accent fill 없음)·`cursor-pointer`. 내부 자식 전부 비인터랙티브 → 단일 `<a>` 유지, 중첩 인터랙티브 요소 0.

## Task Commits

Each task was committed atomically:

1. **Task 1: category 줄 제거 + /timeline 딥링크 <Link> 래핑** — `c52596b` (feat)

## Files Created/Modified
- `frontend/src/features/dashboard/ItemCard.tsx` — category 코드 줄 제거 + `<Card>`를 `/timeline?item={id}` `<Link>`로 래핑 + hover/focus-visible 마감. 헤더 주석에 Phase 16 CARD-01/CARD-02 근거 추가.

## Decisions Made
None - 플랜의 D-01~D-06 결정을 명세 그대로 실행.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- IDE에서 `AdminConsolePage.tsx`가 `./WatchlistSection`을 못 찾는다는 stale TypeScript 진단이 떴으나, 해당 파일은 실존하고 `npm run build`(tsc -b && vite build)가 그린으로 통과 — false positive로 확인(본 변경과 무관).

## Verification
- `npm run build`(tsc -b && vite build) 그린 (2616 modules transformed, ✓ built).
- grep 게이트 전부 통과: `{item.category}` 부재, `from 'react-router-dom'`·`/timeline?item=`·`focus-visible:ring`·`ItemIcon`·`RoleBadge`·`formatKst` 존재, `useNavigate` 부재.
- `git diff --name-only` = `frontend/src/features/dashboard/ItemCard.tsx` 1개 — 백엔드 `src/` 0줄.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- v1.3 마일스톤의 Phase 16(대시보드 카드 개선) 실행 완료. 수동 UAT 후보: 대시보드에서 카드 클릭 → `/timeline?item={id}` 이동해 해당 품목 선택된 차트 확인, Ctrl+클릭 새 탭·Tab 포커스 링·우클릭 메뉴 동작 확인.

---
*Phase: 16-dashboard-cards*
*Completed: 2026-07-03*
