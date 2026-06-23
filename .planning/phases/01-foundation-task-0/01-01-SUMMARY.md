---
phase: 01-foundation-task-0
plan: 01
subsystem: infra
tags: [spring-boot, gradle, java21, postgres, redis, flyway, testcontainers, docker-compose]

# Dependency graph
requires: []
provides:
  - 빌드 가능한 Gradle (Groovy) / Java 21 / Spring Boot 3.4.1 앱 스켈레톤 (com.lostark.tracker 하위)
  - docker-compose Postgres 16 + Redis 7 개발 인프라 (.env로 시크릿 주입)
  - 공유 Testcontainers 베이스 (PostgresRedisContainers) — 이후 모든 통합 테스트 재사용
  - ddl-auto=validate + Flyway 활성 구성, 환경 플레이스홀더에서 주입
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
    - .gitignore (이미 존재; .env 무시 / .env.example 추적 확인)

key-decisions:
  - "Redis 테스트 컨테이너는 Testcontainers core GenericContainer 사용 (com.redis:testcontainers-redis 의존성 없음)"
  - "테스트 JVM의 Docker api.version=1.44 핀 — 엔진 29.x가 docker-java 기본 v1.32(< MinAPIVersion 1.40)를 거부"
  - "Testcontainers BOM을 1.21.3(최신 안정판)으로 상향"

patterns-established:
  - "Flyway가 스키마 소유; JPA는 ddl-auto=validate만 (D-02)"
  - "모든 통합 테스트는 PostgresRedisContainers 상속; 커넥션 속성은 @DynamicPropertySource로 주입"
  - "모든 시크릿은 env 플레이스홀더 경유; .env는 gitignore, 빈 값의 .env.example만 추적"

requirements-completed: [DIST-01]

# Metrics
duration: ~50분
completed: 2026-06-20
---

# Phase 01 / Plan 01: 워킹 스켈레톤 요약

**docker-compose Postgres 16 + Redis 7에 연결되어 부팅되는 Gradle/Java 21/Spring Boot 3.4.1 앱, 공유 Testcontainers 베이스와 실제 컨테이너 위에서 그린인 스모크 통합 테스트.**

## 성능

- **소요 시간:** ~50분 (대부분 Docker Engine 29.x ↔ Testcontainers API 버전 비호환 진단)
- **완료:** 2026-06-20
- **태스크:** 2개
- **변경 파일:** 14개(생성) + 1개 확인

## 주요 성과
- Java 21 위 빌드 가능한 Spring Boot 3.4.1 스켈레톤 (`./gradlew build` 그린)
- 자격증명을 `.env`로 외부화한 docker-compose Postgres 16 + Redis 7
- 공유 `PostgresRedisContainers` Testcontainers 베이스 (싱글톤 PG + Redis, `@DynamicPropertySource`) — Phase 2-5의 영속성/테스트 계약
- `SmokeContextTest`가 실제 컨테이너 위에서 전체 Spring 컨텍스트를 부팅하고 통과
- `ddl-auto=validate` + Flyway 활성; DB/Redis는 env 플레이스홀더에서만 연결(커밋된 시크릿 없음)

## 태스크 커밋

1. **Task 1: Gradle 스캐폴드 + Spring Boot 앱 + 프로파일 구성** — `02e5bba` (feat)
2. **Task 2: docker-compose + 공유 Testcontainers 베이스 + 스모크 IT** — `4b5b7a9` (feat)

## 생성/수정 파일
- `build.gradle` / `settings.gradle` — Spring Boot 3.4.1, Java 21 toolchain, 의존성, Testcontainers 1.21.3, `api.version` 테스트 핀
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*` — Gradle 8.11.1 래퍼
- `src/main/java/com/lostark/tracker/LostarkPriceTrackerApplication.java` — `@SpringBootApplication` 엔트리포인트
- `src/main/resources/application{,-dev,-test}.yml` — base/dev/test 프로파일
- `docker-compose.yml` — Postgres 16 + Redis 7 (핀, 헬스체크, `${...}` 자격증명)
- `.env.example` — PG/Redis 개발 기본값 + 빈 `LOSTARK_API_KEY`
- `src/test/java/.../support/PostgresRedisContainers.java` — 공유 TC 베이스
- `src/test/java/.../SmokeContextTest.java` — 컨텍스트 부팅 스모크 IT

## 결정 사항
- **Redis는 GenericContainer** 사용 (`com.redis:testcontainers-redis` 대신; 버전 가용성 불확실, core 의존성으로 충분).
- **`api.version=1.44` 핀** 테스트 태스크에 — 아래 이슈 참조.
- **Testcontainers 1.21.3** (최신 안정판; 버전 상향 자체가 해결책은 아니었음).

## 계획 대비 이탈

### 자동 수정 이슈

**1. [블로킹] 이전(중단된) 실행기가 남긴 Gradle 래퍼가 불완전**
- **발견 시점:** Task 1 — `gradlew`가 0바이트, `gradlew.bat`·`gradle-wrapper.jar` 누락으로 `./gradlew` 실행 불가.
- **수정:** Spring Initializr 스타터에서 유효한 래퍼(gradlew + gradlew.bat + gradle-wrapper.jar) 부트스트랩; 프로젝트의 핀 고정 Gradle 8.11.1 속성 유지(Spring Boot 3.4.x는 Gradle 8.x 지원, 9.x 아님).
- **검증:** `./gradlew --version` → Java 21 위 Gradle 8.11.1.
- **커밋:** `02e5bba`

**2. [블로킹] 검증 불가한 Redis 테스트 의존성 제거**
- **수정:** `com.redis:testcontainers-redis:2.2.2` 제거; Testcontainers core의 `GenericContainer("redis:7")` 사용.
- **커밋:** `02e5bba` / `4b5b7a9`

---
**총 이탈:** 2건(둘 다 블로킹, 실행 가능한 빌드에 필수). 스코프 확장 없음.

## 마주친 이슈

**Docker Engine 29.5.3이 Testcontainers를 HTTP 400으로 거부(해결됨).**
- **증상:** 모든 통합 테스트가 `Could not find a valid Docker environment ... BadRequestException (Status 400)`로 실패, 반면 `docker` CLI는 정상.
- **근본 원인(로깅 TCP 프록시로 증명):** Testcontainers/docker-java가 `GET /v1.32/info` 발행 — API **v1.32**, Docker Engine 29.x는 `MinAPIVersion`이 **1.40**이라 거부. `DOCKER_API_VERSION` 환경변수는 Testcontainers가 무시; docker-java는 **`api.version` 시스템 속성**을 따름.
- **수정:** `test` 태스크에 `systemProperty 'api.version', '1.44'` (`-PdockerApiVersion=`로 오버라이드 가능). 이식성: 1.44는 Docker 25+부터 현재까지 지원되어 Linux CI에서도 동작.
- **참고:** 진단 중 TCP 데몬 노출을 켰지만 **불필요**했음 — API 버전이 맞으면 npipe로 동작. 사용자는 "Expose daemon on tcp://localhost:2375"를 다시 꺼도 됨.

## 사용자 셋업 필요
이 플랜에는 없음. (로컬 전제: Docker Desktop 실행 + Java 21. Lostark API 키는 01-03에서만 필요.)

## 다음 페이즈 준비도
- 영속성/테스트 스켈레톤이 **01-02**(4테이블 Flyway DDL + JPA 엔티티 `ddl-auto=validate`, `PostgresRedisContainers` 상속) 준비 완료.
- 블로커 없음. `avg_price`/`trade_count`는 01-03 Task 0 스파이크 대기로 이연(D-06).

---
*Phase: 01-foundation-task-0*
*완료: 2026-06-20*
