---
phase: 10-event-impact
plan: 02
subsystem: ui
tags: [react, typescript, shadcn, tailwind, alert, controlled-component]

requires:
  - phase: 09-item-timeline
    provides: RangeControls preset+input pattern, shadcn alert block
  - phase: 07-frontend-foundation
    provides: shadcn alert/button blocks, lucide-react
provides:
  - "frontend/src/features/impact/WindowControls.tsx — controlled window controls: preset 6/24/72h + native number input, no range bounding (D-03), holds no router state"
  - "frontend/src/features/impact/CorrelationBanner.tsx — always-on 상관≠인과 notice (alert default + Info), no dismiss, verbatim copy (D-10, IMPCT-04)"
affects: [10-04]

tech-stack:
  added: []
  patterns:
    - "Controlled presentational components (props only) wired to URL state by the page (10-04)"
    - "Client is not a trust boundary: out-of-range window flows to backend 400, not clamped"

key-files:
  created:
    - frontend/src/features/impact/WindowControls.tsx
    - frontend/src/features/impact/CorrelationBanner.tsx
  modified: []

key-decisions:
  - "WindowControls never bounds the range (D-03) — Math.trunc only blocks non-integers; 0/169 reach the backend so the 400 UI is demonstrable"
  - "CorrelationBanner uses the neutral default Alert variant + Info (not the red error variant) — it is interpretation guidance, not an error; no close affordance (over-reading guard, D-10)"

patterns-established:
  - "A preset button is active only on exact window equality (window === n)"

requirements-completed: [IMPCT-01, IMPCT-04]

duration: 3 min
completed: 2026-06-27
---

# Phase 10 Plan 02: Window Controls + Correlation Banner Summary

**Controlled window preset/number controls (no range bounding so backend 400 stays demonstrable) plus the always-on, non-dismissible "상관 ≠ 인과" notice that is the face of the project's credibility.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-06-27
- **Completed:** 2026-06-27
- **Tasks:** 2
- **Files modified:** 2 (created)

## Accomplishments
- `WindowControls.tsx`: mirrors RangeControls — preset 6/24/72h (selected = accent outline + `aria-pressed`) + native number input labeled "윈도우(시간)"; `Math.trunc` filters non-integers but never bounds the range (D-03); holds no router/query state (10-04 wires it).
- `CorrelationBanner.tsx`: shadcn `Alert` (default variant, neutral slate) + lucide `Info`, title "상관 ≠ 인과", verbatim body from 10-UI-SPEC; no close affordance (always visible, D-10).

## Task Commits

1. **Task 1: WindowControls** - `31429cf` (feat)
2. **Task 2: CorrelationBanner** - `356f5dc` (feat)

## Files Created/Modified
- `frontend/src/features/impact/WindowControls.tsx` - preset + number window controls
- `frontend/src/features/impact/CorrelationBanner.tsx` - always-on correlation≠causation notice

## Decisions Made
None beyond the plan — D-01/D-03/D-10 locked as specified.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
The plan's automated verify greps forbid the literal tokens `clamp`/`destructive`/`onClose` in the
files. Initial explanatory comments mentioned them ("No clamp", "NOT destructive", "no onClose"),
tripping the negative greps even though the code never uses those constructs. Reworded the comments
("no range bounding", "the default Alert variant not the red error one", "no dismiss callback") so
the verify gate passes while the intent is preserved. Build always passed (comments-only change).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Wave 1 complete (10-01 + 10-02). Ready for Wave 2 (10-03 results table/cards), then Wave 3 (10-04 page assembly, human-verify checkpoint).

---
*Phase: 10-event-impact*
*Completed: 2026-06-27*
