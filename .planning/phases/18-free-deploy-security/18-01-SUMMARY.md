---
phase: 18-free-deploy-security
plan: 01
status: complete
date: 2026-07-08
requirements: [DEPLOY-01, DEPLOY-04]
---

# Phase 18-01 Summary — 앱 컨테이너화 + prod 프로파일

**앱을 프로덕션 컨테이너화하고 prod 프로파일을 신설. 비루트 멀티스테이지 Dockerfile + actuator 하드닝(show-details never) + WatchlistSeeder prod 확장. Core Value 로직 0줄.**

## Changes
- **`src/main/resources/application-prod.yml`**(신규): `management.endpoint.health.show-details: never`(base의 `always` override) + `management.endpoints.web.exposure.include: health` + `collection.initial-delay-ms: 10000`(dev 미러 — 시더 선행) + `logging.level.root: INFO`. datasource/redis/lostark/admin은 base `${...}` 유효라 미재선언.
- **`src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java`**: `@Profile({"dev","seed"})` → `@Profile({"dev","seed","prod"})` + Javadoc 보강(prod에서 22개 시드·SeedDataRunner는 seed 전용이라 prod 합성 미구동).
- **`Dockerfile`**(신규): 멀티스테이지(temurin:21-jdk `bootJar -x test` → temurin:21-jre). **비루트 `appuser`(uid 1001)**·`MaxRAMPercentage=75`·시크릿 미굽기(런타임 env).
- **`.dockerignore`**(신규): `.env`·`.env.*`·`build`·`.git`·`frontend`·`node_modules`·`.planning`·`docs` 제외.

## Verification
- `./gradlew compileJava` **그린**(EXIT 0).
- grep 6/6: `show-details: never` · `initial-delay-ms: 10000` · seeder `"prod"` · `USER appuser` · `eclipse-temurin:21-jre` · `.dockerignore` `.env`.
- Core Value 가드: `PriceCollector`·`price_snapshot`·`EventImpactService` diff 0줄.

## Notes
- Docker 이미지 빌드(`docker build`)는 이 환경에서 미실행 — VM/Docker 있는 환경에서 검증(런북 18-04). compileJava로 앱 측 컴파일 정합만 확인.

## Task Commits
1. prod 프로파일 + 컨테이너화 — (아래 커밋)
