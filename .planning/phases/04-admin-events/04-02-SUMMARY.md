---
phase: 04-admin-events
plan: 02
subsystem: auth
tags: [spring-security, shared-secret, once-per-request-filter, authentication-entry-point, testcontainers]

# Dependency graph
requires:
  - phase: 04-admin-events
    provides: "04-01 admin endpoints (/api/admin/events, /api/admin/items) + AdminAuth test helper to gate"
  - phase: 03-read-api-cache
    provides: "public read surface (GET /api/items/**, /api/health/collection) + {timestamp,status,error,message} contract to mirror"
provides:
  - "Shared-secret SecurityFilterChain gating /api/admin/** (401 on the JSON contract, ADMIN-03)"
  - "Hand-rolled AdminSecretFilter (constant-time compare, fail-closed) + AdminAuthenticationEntryPoint (401 JSON)"
  - "admin.api.secret env placeholder (${ADMIN_API_SECRET:}) + test value; .env.example entry"
affects: [event-impact, phase-5, deploy]

# Tech tracking
tech-stack:
  added: ["org.springframework.boot:spring-boot-starter-security"]
  patterns:
    - "Single shared-secret gate: header-based OncePerRequestFilter + SecurityFilterChain, no user/role model (v2)"
    - "Security errors speak the same {timestamp,status,error,message} contract via a custom AuthenticationEntryPoint (filter precedes DispatcherServlet)"
    - "permitAll the public read surface explicitly so adding security causes no read regression; STATELESS + CSRF off for a header-auth JSON API"

key-files:
  created:
    - src/main/java/com/lostark/tracker/security/SecurityConfig.java
    - src/main/java/com/lostark/tracker/security/AdminSecretFilter.java
    - src/main/java/com/lostark/tracker/security/AdminAuthenticationEntryPoint.java
    - src/test/java/com/lostark/tracker/security/AdminAuthGateIT.java
  modified:
    - build.gradle
    - src/main/resources/application.yml
    - src/main/resources/application-test.yml
    - .env.example
    - src/test/java/com/lostark/tracker/health/CollectionHealthIT.java

key-decisions:
  - "401 (not 403) emitted directly by AdminAuthenticationEntryPoint because the security filter runs before @RestControllerAdvice (D-02)"
  - "Blank/unset secret => fail closed (every /api/admin/** is 401) while public reads + collection keep running (D-01)"
  - "FilterRegistrationBean disables Boot's duplicate servlet registration of the @Component filter so it runs only in the security chain"

patterns-established:
  - "Constant-time secret comparison via MessageDigest.isEqual; secret never logged or echoed"
  - "Every IT carries @ActiveProfiles(\"test\") so the @Scheduled collector stays disabled (deterministic)"

requirements-completed: [ADMIN-03]

# Metrics
duration: ~12 min
completed: 2026-06-24
---

# Phase 4 Plan 02: Shared-Secret Admin Gate Summary

**Hand-rolled X-Admin-Secret OncePerRequestFilter + SecurityFilterChain gating /api/admin/** with a constant-time compare and fail-closed blank-secret policy, returning 401 on the same {timestamp,status,error,message} contract via a custom AuthenticationEntryPoint — public reads + collection untouched, 04-01 ITs authenticate with zero retrofit.**

## Performance

- **Duration:** ~12 min
- **Completed:** 2026-06-24T07:54:49Z
- **Tasks:** 2
- **Files modified:** 9 (4 created, 5 modified)

## Accomplishments
- `spring-boot-starter-security` + `SecurityConfig` filter chain: `/api/admin/**` `.authenticated()`, explicit permitAll for GET `/api/items/**` + `/api/health/**` + `/actuator/**`, `anyRequest()` permitAll, CSRF off, STATELESS (D-01).
- `AdminSecretFilter` (`OncePerRequestFilter`): constant-time `MessageDigest.isEqual` compare of `X-Admin-Secret` to the env secret; blank secret never authenticates (fail closed); never logs the header/secret.
- `AdminAuthenticationEntryPoint`: 401 (not 403) with the shared `ApiErrorResponse` JSON (the filter precedes `@RestControllerAdvice`, D-02); generic message, no secret echoed.
- `admin.api.secret: ${ADMIN_API_SECRET:}` placeholder + `.env.example` entry; `application-test.yml` sets `test-admin-secret` so the 04-01 ITs authenticate unchanged.
- `AdminAuthGateIT` (5 tests) + full suite green (67 tests, 0 failures): no/wrong secret → 401 contract, correct secret → 201, public GETs stay 200 without a secret; no Phase 1-3 regression.

## Task Commits

1. **Task 1: Spring Security + shared-secret filter chain** - `add728f` (feat)
2. **Task 2: Auth-gate IT + CollectionHealthIT profile fix** - `f61d769` (test)

## Files Created/Modified
- `security/SecurityConfig.java` — filter chain, permitAll surface, STATELESS, CSRF off, entry point, addFilterBefore; FilterRegistrationBean disabling duplicate registration.
- `security/AdminSecretFilter.java` — constant-time header gate, fail-closed.
- `security/AdminAuthenticationEntryPoint.java` — 401 JSON contract.
- `build.gradle` — +spring-boot-starter-security.
- `application.yml` / `application-test.yml` / `.env.example` — admin.api.secret placeholder + test value + env doc.
- `test/security/AdminAuthGateIT.java` — gate end-to-end IT.
- `test/health/CollectionHealthIT.java` — +@ActiveProfiles("test") (regression fix, see Deviations).

## Decisions Made
- Followed plan as specified for the gate design (D-01/D-02). Added a `FilterRegistrationBean` to disable Boot's auto-registration of the `@Component` filter so it runs exclusively inside the security chain (idiomatic; OncePerRequestFilter would dedup either way).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] @ActiveProfiles("test") added to CollectionHealthIT**
- **Found during:** Task 2 (full-suite verification)
- **Issue:** The full suite failed with a `NullPointerException` in `CollectionHealthIT.healthReflectsLatestRunAndLeaksNoSecret` (`lastRunAt()` null). Root cause: `CollectionHealthIT` was the only IT missing `@ActiveProfiles("test")`, so its context ran the `@Scheduled` collector with `initialDelay=0`; a RUNNING `collection_run` (finishedAt=null) became the latest row and raced the health read. Adding the Phase 4 test classes shifted the Spring context-cache/execution order and exposed this latent Phase 3 defect (it passed in isolation).
- **Fix:** Added `@ActiveProfiles("test")` so `collection.initial-delay-ms=3600000` keeps the collector from auto-firing — the exact purpose documented in `application-test.yml`. Aligns the test with every other IT's convention.
- **Files modified:** src/test/java/com/lostark/tracker/health/CollectionHealthIT.java
- **Verification:** Full `./gradlew test -PdockerApiVersion=1.44` green (67 tests, 0 failures) across two consecutive runs.
- **Committed in:** f61d769 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking — pre-existing test-isolation defect exposed by the new test classes).
**Impact on plan:** No production-code or scope change; a one-line test annotation aligning the outlier IT with the project convention. `LatestPriceCacheIT` shares the same default-profile pattern but passed and was left untouched (out of scope).

## Issues Encountered
- See Deviation 1 — the only issue, diagnosed and fixed.

## User Setup Required
**The admin secret must be provided at deploy time.** Set `ADMIN_API_SECRET` in `.env` (never committed). A blank/unset value fails closed — every `/api/admin/**` request returns 401 while the public read surface and the collection pipeline keep running.

## Next Phase Readiness
- ADMIN-01..03 complete: the admin surface is gated and the event log it curates feeds Phase 5 (event-impact) and the Phase 3 timeline.
- No blockers. The hard gate for Phase 5 (event-impact) is the milestone-level go/no-go decision recorded in PROJECT.md.

---
*Phase: 04-admin-events*
*Completed: 2026-06-24*