---
phase: 04-admin-events
plan: 01
subsystem: api
tags: [spring-boot, jpa, rest, flyway, postgres, testcontainers, admin-crud]

# Dependency graph
requires:
  - phase: 01-foundation-task-0
    provides: "locked game_event / tracked_item model, Flyway forward + ddl-auto=validate, error contract {timestamp,status,error,message}"
  - phase: 03-read-api-cache
    provides: "ApiExceptionHandler/@RestControllerAdvice, GET /api/items active filter, GameEventRepository window finder"
provides:
  - "POST/GET/PUT/DELETE /api/admin/events (event admin CRUD, ADMIN-01)"
  - "POST/DELETE /api/admin/items (item create/soft-delete/reactivate, ADMIN-02)"
  - "ResourceNotFoundException (404), DuplicateResourceException (409), MethodArgumentNotValidException (400) handlers on the shared error contract"
  - "Flyway V3 UNIQUE(external_item_id) constraint"
  - "AdminAuth test helper (X-Admin-Secret) — ready for the 04-02 gate with zero retrofit"
affects: [04-02, event-impact, phase-5]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Admin write layer (web.admin controllers + admin services) bound to request DTOs, never entities (mass-assignment guard)"
    - "Soft-delete (active=false) preserving FK history; idempotent delete"
    - "Find-by-natural-key upsert branch: 201 fresh / 200 reactivate / 409 active-duplicate"

key-files:
  created:
    - src/main/java/com/lostark/tracker/web/admin/AdminEventController.java
    - src/main/java/com/lostark/tracker/web/admin/AdminItemController.java
    - src/main/java/com/lostark/tracker/admin/AdminEventService.java
    - src/main/java/com/lostark/tracker/admin/AdminItemService.java
    - src/main/java/com/lostark/tracker/web/dto/GameEventRequest.java
    - src/main/java/com/lostark/tracker/web/dto/GameEventResponse.java
    - src/main/java/com/lostark/tracker/web/error/ResourceNotFoundException.java
    - src/main/java/com/lostark/tracker/web/error/DuplicateResourceException.java
    - src/main/resources/db/migration/V3__add_tracked_item_external_id_unique.sql
    - src/test/java/com/lostark/tracker/support/AdminAuth.java
    - src/test/java/com/lostark/tracker/admin/AdminEventControllerIT.java
    - src/test/java/com/lostark/tracker/admin/AdminItemControllerIT.java
  modified:
    - src/main/java/com/lostark/tracker/web/error/ApiExceptionHandler.java
    - src/main/java/com/lostark/tracker/domain/GameEvent.java
    - src/main/java/com/lostark/tracker/web/ItemController.java
    - src/test/java/com/lostark/tracker/SchemaRoundTripIT.java

key-decisions:
  - "Generalized 404 via new ResourceNotFoundException (admin layer) kept separate from read-path ItemNotFoundException — no read-contract regression (D-09 choice)"
  - "AdminAuth test helper created in Task 2's commit (not Task 3) so the updated SchemaRoundTripIT stays compilable per atomic commit"
  - "GameEvent.replace(...) is the single intentful mutation; no raw setters for timestamps (D-06/D-07)"

patterns-established:
  - "Admin CRUD = web.admin controller + admin service, DTO-bound; 201/200/204 + 404/409/400 on the unified error contract"
  - "Soft delete preserves time-series provenance (FK without ON DELETE CASCADE)"

requirements-completed: [ADMIN-01, ADMIN-02]

# Metrics
duration: ~60 min
completed: 2026-06-24
---

# Phase 4 Plan 01: Admin Event + Item CRUD Summary

**Admin CRUD for game events (full-replace PUT with mutable occurred_at) and watchlist items (soft-delete + reactivate/409 branch) on the locked model, with a V3 UNIQUE constraint and ResourceNotFound/Duplicate/Validation handlers extending the shared error contract — all admin ITs pre-sending X-Admin-Secret for a zero-retrofit gate.**

## Performance

- **Duration:** ~60 min
- **Completed:** 2026-06-24T07:42:20Z
- **Tasks:** 3
- **Files modified:** 16 (12 created, 4 modified)

## Accomplishments
- `AdminEventController` + `AdminEventService`: POST 201 (entity-stamped created_at/updated_at), GET (occurred_at desc), PUT full-replace (occurred_at change advances updated_at), DELETE 204 — 404 on missing id, 400 on @Valid (ADMIN-01).
- `AdminItemController` + `AdminItemService`: POST 201 fresh / 200 reactivate / 409 active-duplicate, DELETE 204 soft-delete (idempotent, row + history preserved, instant GET /api/items reflection), 404 on missing id (ADMIN-02).
- Public `POST /api/items` removed — the public surface is now read-only GET (D-04).
- Error contract extended: `ResourceNotFoundException` → 404, `DuplicateResourceException` → 409, new `MethodArgumentNotValidException` → 400 handler (D-09, D-10).
- Flyway `V3` adds `UNIQUE(external_item_id)` (constraint-only; ddl-auto=validate stays green, D-05).
- `AdminAuth` helper + two Testcontainers ITs (9 event + 6 item tests) all send `X-Admin-Secret` so 04-02 gates them with zero retrofit.

## Task Commits

1. **Task 1: Event admin CRUD + error-contract extensions** - `416d5f5` (feat)
2. **Task 2: Item admin CRUD, soft-delete, V3 unique, remove public POST** - `185ce39` (feat)
3. **Task 3: Admin event + item integration tests** - `e75c2f6` (test)

## Files Created/Modified
- `web/admin/AdminEventController.java`, `web/admin/AdminItemController.java` — gated admin REST surfaces.
- `admin/AdminEventService.java`, `admin/AdminItemService.java` — write services (upsert branch + soft delete).
- `web/dto/GameEventRequest.java`, `GameEventResponse.java` — event DTOs (DTO-bound, no mass-assignment).
- `web/error/ResourceNotFoundException.java`, `DuplicateResourceException.java` — admin 404/409 signals.
- `web/error/ApiExceptionHandler.java` — +404/409/400 handlers (read-path handlers untouched).
- `domain/GameEvent.java` — `replace(...)` full-replace mutation; timestamps untouched.
- `web/ItemController.java` — public `@PostMapping` removed; read-only GET retained.
- `db/migration/V3__add_tracked_item_external_id_unique.sql` — UNIQUE constraint.
- `test/support/AdminAuth.java`, `admin/AdminEventControllerIT.java`, `admin/AdminItemControllerIT.java`, `SchemaRoundTripIT.java` (updated).

## Decisions Made
- D-09 choice: new general `ResourceNotFoundException` for the admin layer (kept separate from `ItemNotFoundException`) so messages read "Event/Item N not found" with no read-path regression.
- Reactivation returns 200 (vs 201 fresh create) via an `UpsertResult.created` flag (D-05/D-08 discretion).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created AdminAuth test helper in Task 2 instead of Task 3**
- **Found during:** Task 2 (SchemaRoundTripIT update)
- **Issue:** Task 2's `SchemaRoundTripIT` edit POSTs to `/api/admin/items` via `AdminAuth.entity(...)`, but the plan listed `AdminAuth` under Task 3 — committing Task 2 first would leave the test tree non-compilable.
- **Fix:** Created `src/test/java/com/lostark/tracker/support/AdminAuth.java` as part of the Task 2 commit (first point of reference). Task 3 then only adds the two admin ITs.
- **Files modified:** src/test/java/com/lostark/tracker/support/AdminAuth.java
- **Verification:** `./gradlew compileTestJava` clean after Task 2; full admin IT suite green.
- **Committed in:** 185ce39 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking — forward dependency on a shared test helper).
**Impact on plan:** No scope change — same files, the helper simply lands one commit earlier so each commit compiles.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required (the admin secret + Spring Security arrive in 04-02).

## Next Phase Readiness
- Admin endpoints (`/api/admin/events`, `/api/admin/items`) and the `AdminAuth` helper are in place; 04-02 drops the shared-secret `OncePerRequestFilter` + `SecurityFilterChain` in front with zero IT retrofit.
- `application-test.yml` still needs `admin.api.secret: test-admin-secret` (04-02 Task 1) so the pre-sent header authenticates once the gate exists.

---
*Phase: 04-admin-events*
*Completed: 2026-06-24*