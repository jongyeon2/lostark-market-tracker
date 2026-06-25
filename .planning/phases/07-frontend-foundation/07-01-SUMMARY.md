---
phase: 07-frontend-foundation
plan: 01
subsystem: ui
tags: [vite, react, typescript, tailwind, tailwindcss-v4, shadcn, react19, proxy]

requires:
  - phase: none (v1.1 first frontend plan, greenfield)
    provides: consumes the v1.0 backend read API (:8080) only via the dev proxy
provides:
  - "frontend/ Vite 6 + React 19 + TS app that boots with npm run dev and builds clean (tsc -b)"
  - "Vite dev proxy /api -> VITE_API_TARGET (default :8080, changeOrigin) — same-origin, zero backend CORS/changes"
  - "shadcn/ui (slate/new-york) initialised with button/card/skeleton/alert/navigation-menu + cn() helper"
  - "UI-SPEC design tokens encoded once in src/index.css (slate light theme + semantic up/down/warning/neutral, Inter font, @ alias)"
  - "feature-based dir skeleton (features/{dashboard,timeline,impact}, components/{ui,state}, lib)"
affects: [07-02, 07-03, dashboard, timeline, impact, demo-surface]

tech-stack:
  added: [vite, "@vitejs/plugin-react", react, react-dom, typescript, tailwindcss(v4), "@tailwindcss/vite", "class-variance-authority", clsx, tailwind-merge, lucide-react, "@radix-ui/react-slot", "@radix-ui/react-navigation-menu", "@fontsource-variable/inter", tw-animate-css]
  patterns: ["Vite server.proxy for same-origin dev (no backend CORS)", "shadcn token theming via CSS variables + @theme inline (single source of truth)", "@ -> ./src path alias mirrored in vite.config + tsconfig"]

key-files:
  created: [frontend/package.json, frontend/vite.config.ts, frontend/tsconfig.app.json, frontend/components.json, frontend/src/index.css, frontend/src/lib/utils.ts, frontend/src/components/ui/*.tsx, frontend/src/main.tsx, frontend/src/App.tsx, frontend/.env.example, frontend/.gitignore]
  modified: []

key-decisions:
  - "Tailwind v4 via @tailwindcss/vite plugin (no tailwind.config.js / postcss) — current shadcn Vite path"
  - "Inter loaded via bundled @fontsource-variable/inter (no runtime network) over a Google Fonts <link>"
  - "shadcn --accent kept as slate-100 (component hover surface); the brand blue accent lives in --primary/--ring to honor UI-SPEC 'accent reserved for' list"

patterns-established:
  - "Backend-unchanged constraint: all files under frontend/ only; git status -- src/ stays empty"
  - "Design tokens are CSS variables consumed by Tailwind utilities; Phases 8-10 reuse tokens, never raw hex"

requirements-completed: [FND-01, FND-02]

duration: 15min
completed: 2026-06-25
---

# Phase 7 Plan 01: Frontend Foundation Scaffold Summary

**Vite 6 + React 19 + TS + Tailwind v4 `frontend/` app with a `/api`→:8080 dev proxy and shadcn/ui (slate/new-york) pre-loaded with the Phase-7 blocks and UI-SPEC tokens — the shell every later v1.1 plan builds on.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-06-25
- **Completed:** 2026-06-25
- **Tasks:** 2
- **Files modified:** 26 (frontend scaffold + shadcn blocks + tokens)

## Accomplishments
- `frontend/` boots with `npm install && npm run dev`; `npm run build` (tsc -b + vite build) is green with zero type errors.
- Vite `server.proxy` fronts `/api` → `VITE_API_TARGET` (default `http://localhost:8080`, `changeOrigin`) so the browser is same-origin — no backend CORS, no change under the Java `src/`.
- shadcn/ui initialised (slate/new-york, `components.json`, `cn()`); blocks button/card/skeleton/alert/navigation-menu added under `src/components/ui/`.
- UI-SPEC tokens encoded once in `src/index.css` (background #F8FAFC, accent #2563EB, destructive #DC2626, semantic up #16A34A / down #DC2626 / warning #D97706 / neutral #64748B) + Inter variable font; `@` alias resolvable in both Vite and tsconfig.

## Task Commits

1. **Task 1: Scaffold Vite + React + TS app with /api proxy** - `da927e7` (feat)
2. **Task 2: Tailwind v4 + shadcn (slate/new-york) + UI-SPEC tokens** - `4122407` (feat)

## Files Created/Modified
- `frontend/vite.config.ts` — react + tailwind plugins, `/api` proxy, `@`→./src alias
- `frontend/tsconfig.app.json` — bundler mode + `@/*` path mapping
- `frontend/components.json` — shadcn new-york/slate config
- `frontend/src/index.css` — Tailwind v4 import + UI-SPEC token source of truth + Inter
- `frontend/src/lib/utils.ts` — `cn()` helper
- `frontend/src/components/ui/{button,card,skeleton,alert,navigation-menu}.tsx` — Phase-7 blocks
- `frontend/src/App.tsx` / `main.tsx` — placeholder root (real shell in 07-03)

## Decisions Made
- Tailwind v4 (`@tailwindcss/vite`) instead of v3+postcss — matches the current shadcn Vite install path; tokens live in `@theme inline` mapping CSS variables.
- Inter via bundled `@fontsource-variable/inter` (offline-safe) rather than a runtime Google Fonts `<link>`.
- shadcn `--accent` is slate-100 (hover surface); brand blue accent is `--primary`/`--ring` so the UI-SPEC "accent reserved for" list (active nav / primary CTA / focus) holds.

## Deviations from Plan

None — plan executed as written. The Tailwind plugin was wired into `vite.config.ts` in Task 1 (its dep is a Task-1 devDep) so the config is touched once; Task 2 added only the `@import`/tokens/blocks. Both are within the plan's stated discretion.

## Issues Encountered
None. `lucide-react` resolved to 1.x in this registry; `ChevronDownIcon` is exported and type-checks clean.

## User Setup Required
`npm run dev` boots the scaffold standalone, but the proxy target only returns data when the seed backend is up: `SPRING_PROFILES_ACTIVE=seed ./gradlew bootRun` (otherwise `/api/*` 502s until the backend runs). No env config is required for the scaffold itself.

## Next Phase Readiness
- Ready for **07-02**: `src/lib/` + `@` alias + `package.json` in place for zod schemas, the typed API client, React Query hooks, and `formatKst()`.
- shadcn blocks (skeleton/alert/button/card) ready for 07-03's shared state components; `navigation-menu` ready for the TopNav.

---
*Phase: 07-frontend-foundation*
*Completed: 2026-06-25*
