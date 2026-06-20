---
phase: 01-foundation-task-0
plan: 03
subsystem: api
tags: [lostark-api, restclient, spike, rate-limit, jwt, data-model-lock]

# Dependency graph
requires:
  - phase: 01-01
    provides: Spring Boot skeleton, RestClient dep, spike profile wiring, .env.example
  - phase: 01-02
    provides: locked 4-table schema the spike confirms against reality
provides:
  - Task 0 verification of the real markets API (fields, matching rule, rate limit)
  - Ratified data-model lock (D-05 matching rule, D-06 avg_price/trade_count disposition)
  - LostarkSpikeClient (RestClient) + @Disabled spike test + spike profile
  - TASK0-FINDINGS.md (field matrix, exit-gate verdict) + design-doc lock
affects: [02-collection-pipeline, 03-read-api-cache, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns: [restclient-spike, env-sourced-jwt-with-whitespace-hardening, disabled-manual-spike]

key-files:
  created:
    - src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java
    - src/test/java/com/lostark/tracker/spike/MarketsApiSpikeTest.java
    - src/test/resources/application-spike.yml
    - .planning/phases/01-foundation-task-0/TASK0-FINDINGS.md
  modified:
    - docs/design/yeonjong-unknown-design-20260619-221517.md

key-decisions:
  - "D-06: avg_price + trade_count ARE provided but DAILY (detail Stats[]); list gives YDayAvgPrice only"
  - "Model lock (ratified): keep min_price; add avg_price via V2 (YDayAvgPrice); trade_count out of per-tick (v2 daily-stats)"
  - "D-05/DATA-03: stable API Id -> external_item_id + display_name; no fuzzy combo"
  - "Rate limit 100/min confirmed; x-ratelimit-* headers present"

patterns-established:
  - "Spike = RestClient client + @Disabled @SpringBootTest under spike profile; never runs in CI"
  - "JWT key: env-only; client strips a leading bearer prefix AND all whitespace (paste-wrap defense)"

requirements-completed: [DATA-03]

# Metrics
duration: ~spike (multi-step, incl. 401 root-cause)
completed: 2026-06-20
---

# Phase 01 / Plan 03: Task 0 API Verification + Model Lock Summary

**Real markets API verified once: avg_price/trade_count are daily-only (detail Stats[]), a stable Id settles the matching rule, rate limit is 100/min — model lock ratified, exit gate PASS.**

## Performance

- **Completed:** 2026-06-20
- **Tasks:** 3 (2 auto + 1 blocking human checkpoint, ratified)
- **Files:** 3 created, 1 modified

## Accomplishments
- Called the real `markets/items` (list) + `markets/items/{id}` (detail) once via `LostarkSpikeClient` (RestClient, D-03)
- **D-06 resolved:** `avg_price` (`YDayAvgPrice` in list; `Stats[].AvgPrice` daily in detail) and `trade_count` (`Stats[].TradeCount`, daily, detail only) — both available, both DAILY; list has no intraday volume
- **D-05/DATA-03 resolved:** stable integer `Id` → `external_item_id` + `display_name`; no fuzzy combo needed
- **Rate limit confirmed:** 100/min/key; `x-ratelimit-{limit,remaining,reset}` headers returned
- **Exit-gate verdict: PASS**; model lock ratified at the blocking checkpoint
- `TASK0-FINDINGS.md` written; design doc Data Model Decisions / Task 0 Exit Criteria marked LOCKED

## Task Commits

1. **Task 1: spike client + @Disabled spike test (spike profile)** — `8b9b2ba` (feat)
2. **Task 2: run spike, capture findings, lock model + design doc** — `5a7bfbb` (feat)
3. **Task 3: ratify model lock (blocking human checkpoint)** — approved "권장안대로 잠금" (no code commit)

## Files Created/Modified
- `spike/LostarkSpikeClient.java` — RestClient call to markets/items + detail; key normalization
- `spike/MarketsApiSpikeTest.java` — @Disabled spike; captures search + detail Stats
- `application-spike.yml` — base-url + ${LOSTARK_API_KEY}
- `TASK0-FINDINGS.md` — field matrix, matching rule, rate-limit reality, exit-gate verdict, V2 plan
- design doc — LOCKED Task 0 outcome

## Decisions Made (ratified)
- Keep `price_snapshot.min_price` (= `CurrentMinPrice`).
- Add `avg_price` via **`V2__add_price_metrics.sql`** (= `YDayAvgPrice`, free with the list call) — Phase 2.
- Keep `trade_count` OUT of per-tick `price_snapshot` (daily-only; per-tick fetch doubles rate cost) → optional v2 daily-stats table.
- Matching: `external_item_id` = API `Id`, `display_name` = `Name`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Blocking] 401 from a paste-corrupted JWT**
- **Found during:** Task 2 — every call returned `401 Authorization has been denied`, even after re-issuing the key.
- **Root cause (proven via a logging TCP proxy + char-level inspection):** the key pasted into `.env` had 3 literal spaces injected at ~160-char wrap points, corrupting the JWT signature.
- **Fix:** `LostarkSpikeClient` strips a leading `bearer ` prefix and **all whitespace** from the configured key.
- **Verification:** all endpoints returned 200 after cleaning; spike test green through the real client.
- **Committed in:** `5a7bfbb`

**2. [Adjust] Spike query needed a leaf CategoryCode**
- Parent code `50000` returned 0 items; switched the spike to leaf `50010` (재련 재료) and added `getItemDetail()` to capture daily `Stats[]`.
- **Committed in:** `5a7bfbb`

---
**Total deviations:** 2 (both necessary to obtain a real response). No scope creep.

## Issues Encountered
- Initial 401s consumed extra diagnosis; resolved (see above). The local Docker/Testcontainers API-version issue from 01-01 (api.version=1.44) remained fixed.

## User Setup Required
- `LOSTARK_API_KEY` in `.env` (gitignored) — required ONLY to re-run the spike locally. Not needed for CI or the normal test suite (spike is `@Disabled`).

## Next Phase Readiness
- **Phase 1 complete.** Data model is locked on measured reality. Ready for **Phase 2 (Collection Pipeline)**.
- Carry-forward for Phase 2: `V2__add_price_metrics.sql` to add `avg_price`; collect `CurrentMinPrice`→`min_price` per tick; honor 100/min via the token bucket using `x-ratelimit-*` + `Retry-After`.

---
*Phase: 01-foundation-task-0*
*Completed: 2026-06-20*
