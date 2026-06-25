---
phase: 07-frontend-foundation
plan: 03
subsystem: ui
tags: [react-router, react-router-dom, navigation, async-boundary, error-boundary, shadcn]

requires:
  - phase: 07-01
    provides: shadcn blocks (button/card/skeleton/alert/navigation-menu), UI-SPEC tokens, @ alias
  - phase: 07-02
    provides: useItems() (+ 4 hooks), ApiError-throwing client, QueryClientProvider, formatKst
provides:
  - "React Router shell (createBrowserRouter): /dashboard·/timeline·/impact under AppLayout, / -> /dashboard"
  - "TopNav (brand + 3 text NavLinks, active=accent blue-600) + AppLayout (max-w-7xl + 32px padding)"
  - "Shared LoadingState/EmptyState/ErrorState carrying the exact UI-SPEC copy (D-07)"
  - "AsyncBoundary: maps a React Query result to the right state component (D-08)"
  - "RootErrorBoundary errorElement (no white-screen, no stack leak)"
  - "DashboardPage live-renders useItems() through AsyncBoundary — the FND-02 end-to-end proof"
affects: [dashboard, timeline, impact, demo-surface]

tech-stack:
  added: [react-router-dom]
  patterns: ["real-URL routing (refresh/deep-link/back work)", "centralized async UX via AsyncBoundary (screens write only the success path)", "UI-SPEC copy owned by shared state components", "route errorElement fallback"]

key-files:
  created: [frontend/src/components/state/LoadingState.tsx, frontend/src/components/state/EmptyState.tsx, frontend/src/components/state/ErrorState.tsx, frontend/src/components/state/AsyncBoundary.tsx, frontend/src/components/layout/TopNav.tsx, frontend/src/components/layout/AppLayout.tsx, frontend/src/components/RootErrorBoundary.tsx, frontend/src/features/dashboard/DashboardPage.tsx, frontend/src/features/timeline/TimelinePage.tsx, frontend/src/features/impact/ImpactPage.tsx]
  modified: [frontend/src/main.tsx, frontend/package.json]
  deleted: [frontend/src/App.tsx]

key-decisions:
  - "createBrowserRouter + route errorElement for the top-level fallback (idiomatic v7, catches route render errors)"
  - "Router lives in main.tsx (contains the route tree); the 07-01 placeholder App.tsx is removed rather than left dangling"
  - "max-w-7xl (≈ screen-xl, 1280px) instead of the Tailwind-v3-only max-w-screen-xl utility (removed in v4)"

patterns-established:
  - "Each screen: hook -> derive status/isEmpty/refetch -> <AsyncBoundary>; only the success path is hand-written"
  - "Route tree structured so Phase 9-10 add filters as URL searchParams without restructuring (D-04)"

requirements-completed: [FND-02, FND-03, FND-04]

duration: 14min
completed: 2026-06-25
---

# Phase 7 Plan 03: App Shell & Navigation Summary

**A React-Router app shell with a top nav (active-state accent), shared Loading/Empty/Error state components behind a thin AsyncBoundary, and a Dashboard that live-renders `useItems()` through the Vite proxy — the FND-02 end-to-end proof, verified against the running seed backend.**

## Performance

- **Duration:** ~14 min
- **Started:** 2026-06-25
- **Completed:** 2026-06-25
- **Tasks:** 4
- **Files modified:** 12 (10 created, main.tsx/package.json modified, App.tsx removed)

## Accomplishments
- Shared `LoadingState` / `EmptyState` / `ErrorState` own the exact UI-SPEC copy (불러오는 중… / 표시할 데이터가 아직 없어요 / 백엔드에 연결하지 못했어요… + 다시 불러오기) — no generic wording (D-07).
- `AsyncBoundary` maps a React Query result (pending/error/success + isEmpty) to the right state, so screens write only the success path and the error CTA re-runs the query (D-08, D-02).
- React Router (`createBrowserRouter`) gives real `/dashboard`·`/timeline`·`/impact` URLs with a `/`→`/dashboard` redirect under a persistent `TopNav`+`AppLayout` shell; active route = accent blue-600 (FND-03). `RootErrorBoundary` prevents white-screen crashes.
- `DashboardPage` live-renders `useItems()` through `AsyncBoundary` (FND-02 / FND-04 on a real screen); Timeline/Impact are intentional empty skeletons for Phases 9-10.

## Task Commits

1. **Task 1: shared state components** - `7c73444` (feat)
2. **Task 2: AsyncBoundary** - `0de2919` (feat)
3. **Task 4: route pages (Dashboard live + skeletons)** - `3248d46` (feat)
4. **Task 3: React Router shell (TopNav/AppLayout/RootErrorBoundary)** - `a9c4133` (feat)

_Tasks executed in dependency order (state → boundary → pages → router) so each commit builds green; the router in Task 3 imports the Task-4 pages._

## Files Created/Modified
- `frontend/src/components/state/{LoadingState,EmptyState,ErrorState,AsyncBoundary}.tsx`
- `frontend/src/components/layout/{TopNav,AppLayout}.tsx`, `frontend/src/components/RootErrorBoundary.tsx`
- `frontend/src/features/{dashboard/DashboardPage,timeline/TimelinePage,impact/ImpactPage}.tsx`
- `frontend/src/main.tsx` — router wiring inside QueryClientProvider; `frontend/src/App.tsx` removed

## Decisions Made
- Top-level fallback via React Router `errorElement` (`RootErrorBoundary` + `useRouteError`) — idiomatic v7, catches route render errors, renders user-facing copy only (no stack leak, T-0703-03).
- `max-w-7xl` instead of `max-w-screen-xl` (the latter was removed in Tailwind v4); both ≈ 1280px.

## Deviations from Plan

None — plan executed as written. Tasks were committed in dependency order (1, 2, 4, 3) rather than numeric order so every commit's `npm run build` stays green (the router in Task 3 imports the Task-4 pages); the work and commit labels match the plan tasks exactly.

## Issues Encountered
None.

## Verification (runtime, against the running seed backend)
- `npm run build` (tsc + vite): green, 0 type errors.
- `npm run dev` boots (HTTP 200 on `/`, title 로스트아크 시세 트래커); `/dashboard` deep-link serves the SPA (history fallback 200).
- **FND-02 proven end-to-end:** `GET /api/items` via the Vite proxy returned 200 `application/json` real data — `[{"id":1,"externalItemId":"66102101","displayName":"수호석 조각","category":"50010","active":true}, …]` — with no CORS error. The shape matches `trackedItemSchema` exactly, so `.parse` succeeds and the Dashboard renders the item list.
- `git status --short -- src/` empty — the Java backend is byte-for-byte unchanged.

## User Setup Required
None new. For non-empty screens keep the seed backend up: `SPRING_PROFILES_ACTIVE=seed ./gradlew bootRun` (it was running on :8080 during verification); with it down, `/dashboard` shows the shared ErrorState (no crash).

## Next Phase Readiness
- **Phase 7 complete** — FND-01..05 satisfied. The shell, typed query hooks, AsyncBoundary, and KST helper are ready for **Phase 8 (Dashboard widgets)** and **Phase 9-10** (Timeline/Impact) to fill the skeleton routes; route tree is searchParams-ready (D-04).

---
*Phase: 07-frontend-foundation*
*Completed: 2026-06-25*
