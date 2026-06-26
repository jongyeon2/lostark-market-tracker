---
phase: 10-event-impact
plan: 01
subsystem: ui
tags: [react, typescript, shadcn, tailwind, react-router, url-state, formatting]

requires:
  - phase: 07-frontend-foundation
    provides: eventImpactSchema/EventImpactItem zod types, formatKst, cn util, shadcn card/badge convention
  - phase: 09-item-timeline
    provides: useTimelineParams mirror source, EVENT_MARKERS tokens, _shared controlled-component pattern
provides:
  - "frontend/src/components/ui/table.tsx — hand-authored shadcn (new-york) table block (Table/TableHeader/TableBody/TableFooter/TableHead/TableRow/TableCell/TableCaption), Windows shadcn-add bug avoided"
  - "frontend/src/features/impact/useImpactParams.ts — ?item=&window= URL-state hook (useTimelineParams mirror), default window=24 non-eager (D-02), no clamp (D-03)"
  - "frontend/src/features/impact/impactFormat.ts — formatChangeRate(ratio×100 %), changeRateColorClass(상승빨강/하락파랑 D-05), formatPrice, STATUS_BADGE_META (D-07), insufficientReason (희소/stale via anchor-null only, D-08)"
affects: [10-02, 10-03, 10-04]

tech-stack:
  added: []
  patterns:
    - "URL-as-state hook mirrors useTimelineParams (not copy): item shared, window integer added"
    - "Domain formatters as a single pure-function source so table and cards never drift"
    - "Hand-authored shadcn block to dodge the Windows `npx shadcn add` literal-@/-dir bug"

key-files:
  created:
    - frontend/src/components/ui/table.tsx
    - frontend/src/features/impact/useImpactParams.ts
    - frontend/src/features/impact/impactFormat.ts
  modified: []

key-decisions:
  - "changeRate is a backend RATIO (post/pre-1) — formatChangeRate is the single ×100 site; forgetting it would silently turn +12.3% into +0.1%"
  - "changeRateColorClass uses explicit hex (#DC2626/#1D4ED8/#64748B) NOT index.css --up/--down tokens — Korean convention (상승=빨강) is the opposite meaning axis"
  - "insufficientReason derives 희소 vs stale from anchor nullness ALONE — the 30-minute backend threshold is intentionally never referenced (decoupling)"

patterns-established:
  - "Single-source domain formatters consumed by both responsive table and card views"
  - "Non-eager URL default + no client clamp so backend 400 stays demonstrable downstream"

requirements-completed: [IMPCT-01, IMPCT-02, IMPCT-03]

duration: 1 min
completed: 2026-06-27
---

# Phase 10 Plan 01: Reusable Building Blocks Summary

**Hand-authored shadcn table block + `?item=&window=` URL-state hook + pure changeRate/color/status/insufficient-reason formatters that lock display rules for the rest of Phase 10.**

## Performance

- **Duration:** ~1 min
- **Started:** 2026-06-27
- **Completed:** 2026-06-27
- **Tasks:** 3
- **Files modified:** 3 (created)

## Accomplishments
- `table.tsx`: new-york shadcn table family hand-written with `data-slot` + `cn`, no Radix dep, no literal `@/` dir (Windows shadcn-add bug avoided per 10-UI-SPEC Registry Safety).
- `useImpactParams.ts`: mirrors `useTimelineParams` — `?item=&window=` as the single state source; default window 24h returned but never eagerly written (D-02); out-of-range values never clamped (D-03) so 10-04 can demo the backend 400 UI.
- `impactFormat.ts`: `formatChangeRate` is the single place that ×100s the backend ratio (→ `+12.3%`/`-4.0%`/`0.0%`, no `-0.0%`); `changeRateColorClass` encodes the Korean convention (상승=빨강 `#DC2626` / 하락=파랑 `#1D4ED8` / 보합 `#64748B`); `STATUS_BADGE_META` (ok→비교 가능 green, insufficient_data→데이터 부족 amber); `insufficientReason` derives 희소 vs stale from anchor-nullness alone (no 30-min threshold).

## Task Commits

1. **Task 1: hand-authored shadcn table block** - `e965929` (feat)
2. **Task 2: useImpactParams URL-state hook** - `1afd446` (feat)
3. **Task 3: impactFormat domain formatters** - `7babeb6` (feat)

## Files Created/Modified
- `frontend/src/components/ui/table.tsx` - shadcn new-york table primitives
- `frontend/src/features/impact/useImpactParams.ts` - ?item=&window= URL-state hook
- `frontend/src/features/impact/impactFormat.ts` - changeRate/color/price/status/insufficient formatters

## Decisions Made
None beyond the plan — D-01/D-02/D-05/D-06/D-07/D-08 locked exactly as specified.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. `npm run build` (tsc -b + vite build) passed with 0 type errors; Java `src/` untouched.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 10-03 can consume `table.tsx` + `impactFormat` formatters directly (Wave 2).
- 10-02 (Wave 1, parallel) builds WindowControls/CorrelationBanner; 10-04 wires `useImpactParams`.

---
*Phase: 10-event-impact*
*Completed: 2026-06-27*
