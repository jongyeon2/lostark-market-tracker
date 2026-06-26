---
phase: 10-event-impact
plan: 03
subsystem: ui
tags: [react, typescript, shadcn, table, badge, responsive, honest-data]

requires:
  - phase: 10-event-impact
    provides: "10-01 table.tsx block + impactFormat formatters (formatChangeRate/changeRateColorClass/formatPrice/STATUS_BADGE_META/insufficientReason)"
  - phase: 09-item-timeline
    provides: EVENT_MARKERS tokens, formatKst
provides:
  - "frontend/src/features/impact/ImpactStatusBadge.tsx — ImpactStatusBadge (ok=비교 가능/insufficient=데이터 부족) + EventTypeBadge (EVENT_MARKERS outline) (D-07)"
  - "frontend/src/features/impact/EventImpactTable.tsx — ≥md results table, ok/insufficient mixed (D-09), anchor evidence (D-08), direction color (D-05/D-06)"
  - "frontend/src/features/impact/EventImpactCards.tsx — <md card list, same impactFormat/badge source"
affects: [10-04]

tech-stack:
  added: []
  patterns:
    - "CSS-only responsive switch: table (hidden md:block) + cards (md:hidden) both render, CSS toggles — no JS breakpoint measurement"
    - "Table and cards consume the same impactFormat single source so display rules never diverge"

key-files:
  created:
    - frontend/src/features/impact/ImpactStatusBadge.tsx
    - frontend/src/features/impact/EventImpactTable.tsx
    - frontend/src/features/impact/EventImpactCards.tsx
  modified: []

key-decisions:
  - "ok and insufficient_data share one occurred_at-desc list, badge-separated (D-09) — no front-end re-sort (no sort() call)"
  - "insufficient rows replace null changeRate with the reason inlineLabel and expose anchor times (KST or 없음) as the 'why' evidence (D-08); 30-min threshold never referenced"
  - "Korean-convention direction color applied via changeRateColorClass (상승 red / 하락 blue / 보합 slate)"

patterns-established:
  - "Both responsive views render; Tailwind md breakpoint toggles visibility (D-04)"

requirements-completed: [IMPCT-02, IMPCT-03]

duration: 4 min
completed: 2026-06-27
---

# Phase 10 Plan 03: Results Table / Cards Summary

**Responsive table↔card views that mix ok + insufficient_data in one occurred_at-desc list, replace null changeRate with 희소/stale reasons + anchor evidence (honest-data peak), and apply the Korean-convention direction color.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-06-27
- **Completed:** 2026-06-27
- **Tasks:** 3
- **Files modified:** 3 (created)

## Accomplishments
- `ImpactStatusBadge.tsx`: `ImpactStatusBadge` (StatusBadge shape, color + label + lucide icon) and `EventTypeBadge` (EVENT_MARKERS outline + koLabel) — D-07.
- `EventImpactTable.tsx`: `hidden md:block` table; columns 이벤트/종류/발생 시각/상태/변화율/이전가/이후가; ok·insufficient mixed (D-09, no `sort(`); insufficient rows show reason inlineLabel + anchor times (D-08); direction color + ×100 % format (D-05/D-06).
- `EventImpactCards.tsx`: `md:hidden space-y-4` card list, one row = one card, same impactFormat/badge source; insufficient cards show the body copy + anchor evidence.

## Task Commits

1. **Task 1: ImpactStatusBadge + EventTypeBadge** - `0dce533` (feat)
2. **Task 2: EventImpactTable** - `529ac55` (feat)
3. **Task 3: EventImpactCards** - `232adc1` (feat)

## Files Created/Modified
- `frontend/src/features/impact/ImpactStatusBadge.tsx` - status + eventType badges
- `frontend/src/features/impact/EventImpactTable.tsx` - ≥md results table
- `frontend/src/features/impact/EventImpactCards.tsx` - <md card list

## Decisions Made
None beyond the plan — D-04/D-05/D-06/D-07/D-08/D-09 applied as specified. Anchor evidence lines
built as a single template-literal expression to avoid JSX whitespace splitting Korean words.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. Build green; Java `src/` untouched.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Wave 3 (10-04) can wire `EventImpactTable` + `EventImpactCards` into `ImpactPage` and render both for the responsive mixed list. 10-04 carries the human-verify checkpoint.

---
*Phase: 10-event-impact*
*Completed: 2026-06-27*
