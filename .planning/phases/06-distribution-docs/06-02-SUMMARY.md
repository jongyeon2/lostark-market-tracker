---
phase: 06-distribution-docs
plan: 02
subsystem: docs
tags: [readme, documentation, demo, curl, mermaid, distribution, onboarding]

# Dependency graph
requires:
  - phase: 06-distribution-docs
    provides: "06-01 ci.yml workflow (badge target) + seed profile (demo run)"
  - phase: 03-read-api
    provides: "GET /api/items, /prices, /latest, /health/collection contracts + DTOs"
  - phase: 04-admin-events
    provides: "/api/admin/** X-Admin-Secret write surface + GameEventRequest/Response"
  - phase: 05-event-impact
    provides: "GET /api/items/{id}/event-impact + EventImpactItem fields"
provides:
  - "README.md as the single reproducible reviewer entry point (badge, architecture, trade-offs, setup+seed, curl+JSON, retention)"
affects: [distribution, onboarding, hiring-review]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JSON-as-UI: curl + captured sample JSON sells the headline feature (no frontend)"

key-files:
  created: []
  modified:
    - README.md

key-decisions:
  - "Every documented route/field verified against the real controllers/DTOs at write time — no invented endpoints"
  - "Secrets are env placeholders ($ADMIN_API_SECRET) only; no real key committed"
  - "README written in Korean (Korean hiring-reviewer audience) with English code/curl/JSON/identifiers"

patterns-established:
  - "Reviewer-facing README: badge → 3-step reproduce → architecture → honest trade-offs → setup/seed → curl+JSON → limits/retention"

requirements-completed: [DIST-04]

# Metrics
duration: ~12 min
completed: 2026-06-25
---

# Phase 6 Plan 2: README Demo Surface Summary

**README.md rewritten into the single reproducible reviewer entry point — CI badge, mermaid architecture diagram, honest Redis/rate-limit/@Async trade-offs, seed-mode setup, ≥3 curl + 5 sample JSON blocks against the real API, and a clone→seed→curl event-impact reproduction.**

## Performance

- **Duration:** ~12 min
- **Completed:** 2026-06-25
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- README opens with the project one-liner + CI badge (`actions/workflows/ci.yml/badge.svg`, points at 06-01) and a copy-paste **3-step reproduce** block (`docker compose up` → seed profile → `curl .../event-impact` returns a non-empty `events` array with `status:"ok"`) — the literal ROADMAP success criterion 4.
- `mermaid` architecture diagram + a collect→store→serve→correlate paragraph.
- Honest design section: **왜 Redis** (cache-aside + token bucket), **왜 레이트리밋** (per-key ~100/min), **왜 @Async** (admitted showcase), Approach A→B story, and **상관 ≠ 인과** for `change_rate`.
- Local setup (`.env` → `docker compose up` → dev-with-key OR seed-without-key) + the `ADMIN_API_SECRET` fail-closed note.
- Demo: **6 `curl` invocations** and **5 `json` sample responses** across `GET /api/items`, `/prices`, `/event-impact`, the admin `POST /api/admin/events` (X-Admin-Secret), plus the `{timestamp,status,error,message}` 400 contract; an endpoint table for `/latest`, `/health/collection`, `/actuator/health`, admin items/events PUT/DELETE.
- API-limits/scope note + the retention one-liner (no-deletion MVP / rollup·partitioning v2).

## Task Commits

1. **Task 1: Rewrite README.md into the reproducible demo surface** — `8b8d101` (docs)

**Plan metadata:** this SUMMARY commit (docs)

## Files Created/Modified
- `README.md` — full rewrite from the 685-byte placeholder into the reviewer-facing demo surface

## Decisions Made
- Routes and JSON fields were cross-checked against `ItemController`/`PricesController`/`EventImpactController`/`HealthController`/`Admin*Controller` and the real DTO records (`TrackedItemResponse`, `TimelineResponse`/`PricePoint`/`EventPoint`, `EventImpactItem`, `GameEventResponse`, `ApiErrorResponse`) — camelCase field names match Jackson's default record serialization; no invented endpoints or fields.
- All curl examples use `$ADMIN_API_SECRET` (env placeholder); no real secret committed.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None for this plan. (Phase-level: see [06-USER-SETUP.md](./06-USER-SETUP.md) — pushing to GitHub turns the badge green; that is the only step not verifiable locally.)

## Next Phase Readiness
- Phase 6 plans complete (2/2). Ready for phase verification + milestone completion.
- README docs-only change; the full `./gradlew build` (86 tests, 0 failures from 06-01) is unaffected.

---
*Phase: 06-distribution-docs*
*Completed: 2026-06-25*
