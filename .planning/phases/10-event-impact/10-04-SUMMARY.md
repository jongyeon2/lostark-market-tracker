---
phase: 10-event-impact
plan: 04
subsystem: ui
tags: [react, typescript, react-query, react-router, error-handling, composition]

requires:
  - phase: 10-event-impact
    provides: "10-01 useImpactParams + impactFormat; 10-02 WindowControls + CorrelationBanner; 10-03 EventImpactTable + EventImpactCards"
  - phase: 09-item-timeline
    provides: "_shared ItemSelect + LatestPriceCard, ApiError.status branching pattern, D-06 first-item auto-select"
provides:
  - "frontend/src/features/impact/ImpactPage.tsx — assembled event-impact screen (banner + controls + latest-price + results), URL-wired, ApiError 400/404/200-empty branching — IMPCT-01..04"
affects: []

tech-stack:
  added: []
  patterns:
    - "Results scope as an inner component (ImpactResults) with its own React Query + error branch, independent of LatestPriceCard's AsyncBoundary (D-12)"
    - "ApiError.status drives distinct 400/404 copy; 200-empty handled separately (no generic '오류 발생')"

key-files:
  created: []
  modified:
    - frontend/src/features/impact/ImpactPage.tsx

key-decisions:
  - "200-empty renders the 10-UI-SPEC heading '등록된 이벤트가 없어요' + body in an EmptyState-shaped block rather than the shared EmptyState (whose fixed heading '표시할 데이터가 아직 없어요' conflicts with the contract) — avoids a duplicate/wrong heading"
  - "onResetWindow = setWindow(24) wired to the 400 alert's '24시간으로 보기' CTA (D-03 recovery)"
  - "Results area renders only when itemId != null; both table and cards render and CSS toggles (D-09)"

patterns-established:
  - "URL searchParams single source of truth wired through useImpactParams to dumb controlled components"

requirements-completed: [IMPCT-01, IMPCT-02, IMPCT-03, IMPCT-04]

duration: 8 min
completed: 2026-06-27
---

# Phase 10 Plan 04: ImpactPage Assembly Summary

**The full event-impact screen wired together — always-on 상관≠인과 banner, URL-driven selector + window controls, isolated latest-price card, and an honest results area that branches 400/404/200-empty into distinct copy — completing IMPCT-01..04.**

## Performance

- **Duration:** ~8 min (incl. human-verify checkpoint)
- **Started:** 2026-06-27
- **Completed:** 2026-06-27
- **Tasks:** 3 (2 auto + 1 human-verify checkpoint)
- **Files modified:** 1

## Accomplishments
- `ImpactPage.tsx`: replaced the Phase-7 skeleton with the assembled screen — `CorrelationBanner` (content-top, always-on), control bar `[ItemSelect][WindowControls]`, `LatestPriceCard` (own AsyncBoundary), and the results area. `useImpactParams` is the single state source; D-06 auto-selects the first item when `?item=` is absent.
- `ImpactResults` (inner scope): `useEventImpact(itemId, window)` with `ApiError.status` branching — 400 ("윈도우 값을 다시 확인해 주세요" + "24시간으로 보기" CTA), 404 ("존재하지 않는 품목이에요"), network → shared `ErrorState`, 200-empty → "등록된 이벤트가 없어요" block (D-11). Success renders `EventImpactTable` + `EventImpactCards` (responsive mixed list, D-09).
- Human-verify checkpoint **approved** by the developer (상관≠인과 가시성, insufficient 희소/stale 납득성, % 부합, KST 정합, 400/404/빈 상태 카피, 반응형 전환).

## Task Commits

1. **Task 1: page shell + URL wiring + D-06 default selection** - `662930a` (feat)
2. **Task 2: results area + ApiError 400/404/200-empty branching** - `6942440` (feat)
3. **Task 3: human-verify checkpoint** - approved (no code change)

## Files Created/Modified
- `frontend/src/features/impact/ImpactPage.tsx` - assembled event-impact screen

## Decisions Made
- 200-empty uses a custom EmptyState-shaped block with the contract heading "등록된 이벤트가 없어요" (see deviation below).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Contract conflict] 200-empty heading rendered via custom block, not shared EmptyState**
- **Found during:** Task 2 (results area 200-empty branch)
- **Issue:** The shared `EmptyState` hard-codes the heading "표시할 데이터가 아직 없어요" and only the body is overridable. The 10-UI-SPEC Copywriting Contract requires the heading "등록된 이벤트가 없어요" for the 200-empty case. Following the plan's "render an inline title together with EmptyState" literally would show two stacked headings (one wrong).
- **Fix:** Rendered an `EmptyState`-shaped block (same centered layout/classes) with the exact contract heading + body, instead of invoking `EmptyState`.
- **Files modified:** frontend/src/features/impact/ImpactPage.tsx
- **Verification:** `grep "관리자가 이벤트를 등록하면"` passes; copy matches 10-UI-SPEC verbatim; build green.
- **Committed in:** `6942440` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 contract conflict)
**Impact on plan:** Honors the authoritative UI-SPEC copy contract exactly with no extra scope. No behavioral change beyond correct empty-state copy.

## Issues Encountered
None. Build green throughout; Java `src/` untouched; route tree unchanged.

## User Setup Required
None - no external service configuration required. Visual verification uses the seed backend
(`docker compose up -d` + `./gradlew bootRun --args='--spring.profiles.active=seed'`) and `npm run dev`.

## Next Phase Readiness
- Phase 10 plans complete (4/4). IMPCT-01..04 delivered. Ready for phase verification and completion.
- Milestone v1.1 (Frontend Demo Dashboard) headline screen done; remaining: demo docs/static serving (Phase 11).

---
*Phase: 10-event-impact*
*Completed: 2026-06-27*
