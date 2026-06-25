---
phase: 07-frontend-foundation
plan: 02
subsystem: api
tags: [zod, tanstack-query, react-query, typescript, fetch, intl, kst]

requires:
  - phase: 07-01
    provides: frontend/ scaffold, @ alias, Vite /api proxy, package.json, src/lib dir
provides:
  - "src/lib/schemas.ts — 5 zod schemas (single source of truth) + z.infer types for all read DTOs"
  - "src/lib/api.ts — typed client validating every response at the boundary with .parse (throws on non-2xx + drift)"
  - "src/lib/queryClient.ts + queries.ts — single QueryClient (fetch-on-mount policy) + 5 typed React Query hooks"
  - "src/lib/formatKst.ts — native-Intl UTC->KST display helper + toEpochMs() for axis math (off-by-9h guard)"
  - "main.tsx wrapped in QueryClientProvider"
affects: [07-03, dashboard, timeline, impact]

tech-stack:
  added: [zod, "@tanstack/react-query"]
  patterns: ["zod schema as single source of truth (z.infer types, no separate interfaces)", "boundary .parse validation — loud-fail on drift/nulls", "React Query server-state cache mirroring the v1.0 Redis serving cache", "display=KST via Intl / math=raw UTC epoch"]

key-files:
  created: [frontend/src/lib/schemas.ts, frontend/src/lib/api.ts, frontend/src/lib/queryClient.ts, frontend/src/lib/queries.ts, frontend/src/lib/formatKst.ts, frontend/src/vite-env.d.ts]
  modified: [frontend/src/main.tsx, frontend/package.json]

key-decisions:
  - "staleTime 5m + retry 1 (seed data effectively static; no polling/interval — D-02)"
  - "ApiError carries only HTTP status + path (no response body / headers logged — T-0702-02)"
  - "zod 4.x: object schemas strip unknown keys by default (no .strict) for forward-compat (D-06)"

patterns-established:
  - "Every endpoint: typed fn -> schema.parse -> typed hook; screens consume only validated data"
  - "Nullable backend fields modeled with .nullable() so insufficient_data nulls fail loudly"

requirements-completed: [FND-02, FND-05]

duration: 12min
completed: 2026-06-25
---

# Phase 7 Plan 02: Typed, Validated Server-State Layer Summary

**zod-as-single-source schemas for the 5 read DTOs, a `.parse`-at-the-boundary typed API client, fetch-on-mount TanStack Query hooks, and a native-Intl `formatKst()` — honest, drift-proof data at the edge for the three screens.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-06-25
- **Completed:** 2026-06-25
- **Tasks:** 4
- **Files modified:** 8

## Accomplishments
- `schemas.ts`: 5 zod schemas (collectionHealth / trackedItems / latestPrice / timeline / eventImpact) mirroring the measured backend records field-for-field, with `.nullable()` on every backend-nullable field and enums for EventType + `ok`/`insufficient_data`; TS types are `z.infer`-derived (D-05).
- `api.ts`: one async fn per endpoint hitting the relative proxied `/api/...` paths; `request()` throws `ApiError` on non-2xx and each fn returns `schema.parse(json)` so 400/404/drift fail loudly into React Query (D-06, D-12).
- `queryClient.ts` + `queries.ts`: single `QueryClient` (refetchOnWindowFocus:false, 5m staleTime, no polling — D-02) + 5 typed hooks with stable queryKeys (D-01); `main.tsx` now provides the client.
- `formatKst.ts`: `formatKst()` renders KST via `Intl.DateTimeFormat('ko-KR',{ timeZone:'Asia/Seoul' })` and `toEpochMs()` keeps axis math on the raw UTC instant — no tz library, no manual +9h (FND-05, D-09).

## Task Commits

1. **Task 1: zod schemas (single source of truth)** - `382fca9` (feat)
2. **Task 2: typed API client with boundary .parse** - `611b256` (feat)
3. **Task 3: QueryClient + 5 hooks + provider** - `a11f44c` (feat)
4. **Task 4: formatKst() UTC->KST helper** - `cc11bbb` (feat)

## Files Created/Modified
- `frontend/src/lib/schemas.ts` — 5 zod schemas + z.infer types
- `frontend/src/lib/api.ts` — typed `.parse` client, throws on non-2xx
- `frontend/src/lib/queryClient.ts` — D-02 fetch-on-mount policy
- `frontend/src/lib/queries.ts` — 5 typed React Query hooks
- `frontend/src/lib/formatKst.ts` — Intl KST display + epoch-ms accessor
- `frontend/src/main.tsx` — wrapped in QueryClientProvider
- `frontend/src/vite-env.d.ts` — vite/client module types (scaffold fix)

## Decisions Made
- `staleTime` 5 min + `retry` 1: seed data is effectively static; user retry is the 07-03 "다시 불러오기" refetch, no auto-polling (D-02).
- `ApiError` exposes only HTTP status + path — no response body/headers logged (T-0702-02).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing scaffold file] Added `src/vite-env.d.ts`**
- **Found during:** Task 3 (wiring `main.tsx`)
- **Issue:** The 07-01 scaffold omitted the standard Vite `vite-env.d.ts`; the editor LSP flagged the `import './index.css'` side-effect import as missing a module declaration.
- **Fix:** Added `frontend/src/vite-env.d.ts` with `/// <reference types="vite/client" />` (standard Vite template file).
- **Files modified:** frontend/src/vite-env.d.ts
- **Verification:** `npm run build` green; CSS/asset side-effect imports type-resolve.
- **Committed in:** `a11f44c` (Task 3 commit)

---

**Total deviations:** 1 auto-fixed (1 missing scaffold file)
**Impact on plan:** Restores the standard Vite typing baseline; no scope creep.

## Issues Encountered
- The Task-4 `grep` acceptance check (`grep -nE "date-fns-tz|dayjs|luxon"` must return nothing) initially tripped because the doc comment named those libraries verbatim. Rephrased the comment so the check proves no import exists — no code change.

## User Setup Required
None — library layer only; no screen renders yet. Live data still requires the seed backend on :8080 (proven in 07-03).

## Next Phase Readiness
- Ready for **07-03**: `useItems()` (+ the other 4 hooks), `ApiError`-throwing client, and `formatKst` are importable for the AsyncBoundary, TopNav shell, and the Dashboard live-render proof.

---
*Phase: 07-frontend-foundation*
*Completed: 2026-06-25*
