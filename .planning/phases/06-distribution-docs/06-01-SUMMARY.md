---
phase: 06-distribution-docs
plan: 01
subsystem: infra
tags: [github-actions, ci, testcontainers, seed, demo-data, spring-profile, postgres]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: TrackedItem/PriceSnapshot/GameEvent entities + repositories, Flyway V1-V3, PostgresRedisContainers test base
  - phase: 03-read-api
    provides: GET /api/items/{id}/prices timeline read (TimelineResponse)
  - phase: 05-event-impact
    provides: EventImpactService anchor metric + 30-min STALENESS_ALLOWANCE, GET /api/items/{id}/event-impact
provides:
  - GitHub Actions CI (.github/workflows/ci.yml) running ./gradlew build on every push/PR
  - seed Spring profile that fills synthetic 8-day history + 2 demo events with no LOSTARK_API_KEY
  - SyntheticDemoData injectable @Component (unit-testable seed() generator)
  - SyntheticDemoDataIT proving non-empty timeline + event-impact "ok" + idempotency
affects: [06-02-readme, distribution, demo, onboarding]

# Tech tracking
tech-stack:
  added: [GitHub Actions (actions/checkout@v4, actions/setup-java@v4 temurin 21)]
  patterns:
    - "Generator/Runner split: profile-free @Component holds logic (testable), thin @Profile runner invokes at boot"
    - "Idempotent synthetic seed via DATA-01 unique-key existsBy guard + game_event count() guard"
    - "Off-grid event placement (5 min) to land distinct fresh pre/post anchors under the staleness contract"

key-files:
  created:
    - .github/workflows/ci.yml
    - src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java
    - src/main/java/com/lostark/tracker/seed/SeedDataRunner.java
    - src/main/resources/application-seed.yml
    - src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java
  modified:
    - src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java

key-decisions:
  - "CI runs the exact ./gradlew build a reviewer runs locally — no -PdockerApiVersion override (build defaults api.version=1.44, accepted by ubuntu-latest's Docker engine)"
  - "Events placed 5 min OFF the 10-min grid so EventImpactService picks distinct fresh pre/post anchors -> real non-zero change_rate (on-grid would make pre==post, rate 0)"
  - "SyntheticDemoData has NO @Profile (unit-testable); only SeedDataRunner is @Profile(\"seed\")"
  - "Deterministic sine+jitter price walk seeded off item id (reproducible run-to-run); adjacent ticks forced to differ"

patterns-established:
  - "Profile-free generator @Component + thin @Profile(\"seed\") ApplicationRunner ordering (@Order(1) WatchlistSeeder -> @Order(2) SeedDataRunner)"
  - "Demo seed honors the live unique key + staleness contract instead of bypassing them"

requirements-completed: [DIST-02, DIST-03]

# Metrics
duration: ~25 min
completed: 2026-06-25
---

# Phase 6 Plan 1: CI + Seed Profile Summary

**GitHub Actions CI running the full `./gradlew build` Testcontainers suite on every push/PR, plus a `seed` Spring profile that fills 8 days of synthetic 10-min snapshots + 2 grid-anchored demo events (no API key) — proven end-to-end by SyntheticDemoDataIT.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-06-25
- **Tasks:** 3
- **Files created:** 5, **modified:** 1

## Accomplishments
- `.github/workflows/ci.yml` (name `CI`) runs on every push + PR on ubuntu-latest with Temurin 21, `chmod +x ./gradlew`, then `./gradlew build` — the exact full Testcontainers suite a reviewer runs locally. `permissions: contents: read`; no CD/credentials/matrix. Stable filename backs the 06-02 badge.
- `seed` profile: `SyntheticDemoData.seed()` writes, per active item, 1152 deterministic 10-min `min_price` snapshots over 8 days (idempotent via `existsByTrackedItem_IdAndCollectedAt`) + 2 demo `game_event`s (seeded only when `game_event` is empty). `SeedDataRunner` (`@Profile("seed") @Order(2)`) invokes it after `WatchlistSeeder` (now `@Order(1)`). `application-seed.yml` parks the `@Scheduled` collector (1h initial-delay) so no `LOSTARK_API_KEY` is needed.
- `SyntheticDemoDataIT` (Testcontainers) proves: non-empty `GET /prices`, `GET /event-impact?window=24` with ≥1 `status:"ok"` + non-null **non-zero** `changeRate`, and a second `seed()` leaves the snapshot count unchanged.
- Full `./gradlew build` green: **86 tests, 0 failures, 1 skipped** (the `@Disabled` Task-0 spike). No Phase 1–5 regression (was 84; +2 from this IT). No schema/dependency change (Flyway V1–V3 intact).

## Task Commits

1. **Task 1: GitHub Actions CI workflow** — `e20a675` (ci)
2. **Task 2: SyntheticDemoData + SeedDataRunner + application-seed.yml + WatchlistSeeder @Order** — `53d1d04` (feat)
3. **Task 3: SyntheticDemoDataIT** — `9add344` (test)

**Plan metadata:** this SUMMARY commit (docs)

## Files Created/Modified
- `.github/workflows/ci.yml` — CI: push+PR, ubuntu-latest, Temurin 21, `./gradlew build` (full Testcontainers suite)
- `src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java` — profile-free @Component; idempotent 8-day snapshot + 2-event generator
- `src/main/java/com/lostark/tracker/seed/SeedDataRunner.java` — @Profile("seed") @Order(2) ApplicationRunner calling seed()
- `src/main/resources/application-seed.yml` — seed profile: 1h collector initial-delay (no API key needed)
- `src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java` — added @Order(1) only (profile + curated list untouched)
- `src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java` — Testcontainers IT: timeline + event-impact "ok" + idempotency

## Decisions Made
- CI runs the unmodified `./gradlew build` (no `-PdockerApiVersion` override) — ubuntu-latest's Docker engine accepts the build's default `api.version=1.44`, so Testcontainers boots Postgres 16 + Redis 7 identically to local.
- The generation logic is a profile-free, injectable `@Component` so it is unit-testable on Testcontainers without profile gymnastics; only the thin runner is `@Profile("seed")`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Plan mechanism would defeat its own success criterion] Demo events placed 5 min OFF the 10-min grid instead of exactly ON it**
- **Found during:** Task 2 (SyntheticDemoData event placement)
- **Issue:** The plan's `must_haves` prescribed placing demo events EXACTLY on a snapshot instant, expecting `pre = gap-0 tick` and `post = next tick (10 min away)`. But `EventImpactService`'s anchor tie-rule (a snapshot exactly at `occurred_at` is a valid anchor on BOTH sides) makes `pre` and `post` the SAME gap-0 snapshot → `change_rate = 0.0000`. That yields `status:"ok"` but a zero rate, defeating the plan's stated goal of "a real `change_rate` ... not all insufficient_data ... demos with actual numbers."
- **Fix:** Place each event 5 minutes off the grid (midway between two seeded ticks). `pre` becomes the tick 5 min before, `post` the tick 5 min after — two DISTINCT snapshots, both inside the 30-min `STALENESS_ALLOWANCE` → `status:"ok"` with a REAL non-zero `change_rate`. The price walk additionally forces adjacent ticks to differ so the two anchors never coincide in value.
- **Files modified:** src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java
- **Verification:** SyntheticDemoDataIT asserts the "ok" event's `changeRate.signum() != 0`; passes.
- **Committed in:** `53d1d04` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 plan-mechanism correction)
**Impact on plan:** Delivers the plan's actual intent (real, non-zero `change_rate` demo). No scope creep; the on-grid vs off-grid choice is the only behavioral change, and it strengthens the headline demo.

## Issues Encountered
None.

## User Setup Required
**One manual action required.** See [06-USER-SETUP.md](./06-USER-SETUP.md): push the branch to GitHub once so the CI workflow runs and the README badge turns green — the only step not verifiable locally.

## Next Phase Readiness
- Ready for **06-02** (README rewrite): the `ci.yml` workflow the badge points at and the `seed` profile the demo run uses both exist and are proven.
- The full suite is green, so 06-02 (docs-only) builds on a clean foundation.

---
*Phase: 06-distribution-docs*
*Completed: 2026-06-25*
