---
phase: 01-foundation-task-0
plan: 01
subsystem: infra
tags: [spring-boot, gradle, java21, postgres, redis, flyway, testcontainers, docker-compose]

# Dependency graph
requires: []
provides:
  - Buildable Gradle (Groovy) / Java 21 / Spring Boot 3.4.1 app skeleton under com.lostark.tracker
  - docker-compose Postgres 16 + Redis 7 dev infrastructure (secrets via .env)
  - Shared Testcontainers base (PostgresRedisContainers) reused by all later integration tests
  - ddl-auto=validate + Flyway-enabled config wired from environment placeholders
affects: [02-collection-pipeline, 03-read-api-cache, 04-admin-events, 05-event-impact, 06-distribution-docs]

# Tech tracking
tech-stack:
  added: [spring-boot-3.4.1, spring-data-jpa, spring-data-redis, spring-actuator, flyway-core, flyway-database-postgresql, postgresql-driver, testcontainers-1.21.3, junit5]
  patterns: [flyway-owns-schema + jpa-validate-only, shared-testcontainers-base via @DynamicPropertySource, env-sourced secrets]

key-files:
  created:
    - build.gradle
    - settings.gradle
    - gradlew / gradlew.bat / gradle/wrapper/gradle-wrapper.jar+properties
    - src/main/java/com/lostark/tracker/LostarkPriceTrackerApplication.java
    - src/main/resources/application.yml (+ application-dev.yml, application-test.yml)
    - docker-compose.yml
    - .env.example
    - src/test/java/com/lostark/tracker/support/PostgresRedisContainers.java
    - src/test/java/com/lostark/tracker/SmokeContextTest.java
  modified:
    - .gitignore (already present; verified .env ignored / .env.example tracked)

key-decisions:
  - "Redis test container uses Testcontainers core GenericContainer (no com.redis:testcontainers-redis dependency)"
  - "Pin Docker api.version=1.44 for the test JVM — engine 29.x rejects docker-java default v1.32 (< MinAPIVersion 1.40)"
  - "Bumped Testcontainers BOM to 1.21.3 (latest stable)"

patterns-established:
  - "Flyway owns schema; JPA ddl-auto=validate only (D-02)"
  - "Every integration test extends PostgresRedisContainers; connection props injected via @DynamicPropertySource"
  - "All secrets via env placeholders; .env gitignored, only .env.example (empty values) tracked"

requirements-completed: [DIST-01]

# Metrics
duration: ~50min
completed: 2026-06-20
---

# Phase 01 / Plan 01: Walking Skeleton Summary

**Gradle/Java 21/Spring Boot 3.4.1 app that boots against docker-compose Postgres 16 + Redis 7, with a shared Testcontainers base and a green smoke integration test on real containers.**

## Performance

- **Duration:** ~50 min (most of it diagnosing a Docker Engine 29.x ↔ Testcontainers API-version incompatibility)
- **Completed:** 2026-06-20
- **Tasks:** 2
- **Files modified:** 14 (created) + 1 verified

## Accomplishments
- Buildable Spring Boot 3.4.1 skeleton on Java 21 (`./gradlew build` green)
- docker-compose Postgres 16 + Redis 7 with credentials externalized to `.env`
- Shared `PostgresRedisContainers` Testcontainers base (singleton PG + Redis, `@DynamicPropertySource`) — the persistence/test contract for Phases 2–5
- `SmokeContextTest` boots the full Spring context against real containers and passes
- `ddl-auto=validate` + Flyway enabled; DB/Redis wired purely from env placeholders (no committed secrets)

## Task Commits

1. **Task 1: Gradle scaffold + Spring Boot app + profile config** — `02e5bba` (feat)
2. **Task 2: docker-compose + shared Testcontainers base + smoke IT** — `4b5b7a9` (feat)

## Files Created/Modified
- `build.gradle` / `settings.gradle` — Spring Boot 3.4.1, Java 21 toolchain, deps, Testcontainers 1.21.3, `api.version` test pin
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*` — Gradle 8.11.1 wrapper
- `src/main/java/com/lostark/tracker/LostarkPriceTrackerApplication.java` — `@SpringBootApplication` entrypoint
- `src/main/resources/application{,-dev,-test}.yml` — base/dev/test profiles
- `docker-compose.yml` — Postgres 16 + Redis 7 (pinned, healthchecks, `${...}` creds)
- `.env.example` — PG/Redis dev defaults + empty `LOSTARK_API_KEY`
- `src/test/java/.../support/PostgresRedisContainers.java` — shared TC base
- `src/test/java/.../SmokeContextTest.java` — context-boot smoke IT

## Decisions Made
- **GenericContainer for Redis** instead of `com.redis:testcontainers-redis` (uncertain version availability; core dependency is enough).
- **Pin `api.version=1.44`** in the test task — see Issues below.
- **Testcontainers 1.21.3** (latest stable; the bump itself was not the fix).

## Deviations from Plan

### Auto-fixed Issues

**1. [Blocking] Gradle wrapper was incomplete from the prior (aborted) executor**
- **Found during:** Task 1 — `gradlew` was 0 bytes, `gradlew.bat` and `gradle-wrapper.jar` were missing, so `./gradlew` could not run.
- **Fix:** Bootstrapped a valid wrapper (gradlew + gradlew.bat + gradle-wrapper.jar) from a Spring Initializr starter; kept the project's pinned Gradle 8.11.1 properties (Spring Boot 3.4.x supports Gradle 8.x, not 9.x).
- **Verification:** `./gradlew --version` → Gradle 8.11.1 on Java 21.
- **Committed in:** `02e5bba`

**2. [Blocking] Removed unverifiable Redis test dependency**
- **Fix:** Dropped `com.redis:testcontainers-redis:2.2.2`; used `GenericContainer("redis:7")` from Testcontainers core.
- **Committed in:** `02e5bba` / `4b5b7a9`

---
**Total deviations:** 2 (both blocking, necessary for a runnable build). No scope creep.

## Issues Encountered

**Docker Engine 29.5.3 rejected Testcontainers with HTTP 400 (resolved).**
- **Symptom:** Every integration-test run failed with `Could not find a valid Docker environment ... BadRequestException (Status 400)`, while the `docker` CLI worked fine.
- **Root cause (proven via a logging TCP proxy):** Testcontainers/docker-java issued `GET /v1.32/info` — API **v1.32**, which Docker Engine 29.x rejects because its `MinAPIVersion` is **1.40**. The `DOCKER_API_VERSION` env var is ignored by Testcontainers; docker-java honors the **`api.version` system property**.
- **Fix:** `systemProperty 'api.version', '1.44'` on the `test` task (overridable via `-PdockerApiVersion=`). Portable: 1.44 is supported by Docker 25+ through current, so it also works on Linux CI.
- **Note:** TCP daemon exposure was enabled mid-diagnosis but turned out to be **unnecessary** — the npipe works once the API version is correct. The user can safely disable "Expose daemon on tcp://localhost:2375" again.

## User Setup Required
None for this plan. (Local prerequisites: Docker Desktop running + Java 21. The Lostark API key is only needed in plan 01-03.)

## Next Phase Readiness
- Persistence/test skeleton is ready for **01-02** (4-table Flyway DDL + JPA entities under `ddl-auto=validate`, extending `PostgresRedisContainers`).
- No blockers. `avg_price`/`trade_count` remain deferred pending the 01-03 Task 0 spike (D-06).

---
*Phase: 01-foundation-task-0*
*Completed: 2026-06-20*
