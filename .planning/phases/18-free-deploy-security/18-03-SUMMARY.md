---
phase: 18-free-deploy-security
plan: 03
status: complete
date: 2026-07-08
requirements: [DEPLOY-03, DEPLOY-04]
---

# Phase 18-03 Summary — prod docker-compose (보안 네트워킹)

**보안 네트워킹 prod 스택. DB/Redis/app 포트 미공개(caddy만 80/443), redis requirepass, 시크릿 env-only. 수집/캐시/event-impact 로직 0줄.**

## Changes
- **`docker-compose.prod.yml`**(신규): app·postgres·redis에 **`ports:` 없음**(내부 `internal` 네트워크 전용), **caddy만 `80:80`/`443:443` publish**. app `SPRING_PROFILES_ACTIVE=prod` + env(POSTGRES_URL=`jdbc:postgresql://postgres:5432/...`, REDIS_HOST=redis, 시크릿). redis `command: ["redis-server","--requirepass","${REDIS_PASSWORD}"]` + AUTH 헬스체크. postgres pgdata 볼륨 + pg_isready. 전 서비스 `restart: unless-stopped` + `depends_on` 헬스 게이트. caddy는 `build: {context: frontend, dockerfile: Dockerfile}`(18-02 정정 반영).
- **`.env.prod.example`**(신규): 빈 값 + `openssl rand -hex 32` 가이드(123456789·lostark 금지 명시). POSTGRES_URL/REDIS_HOST는 compose가 세팅함을 주석.
- **`.gitignore`**: `!.env.prod.example` 예외 추가(`.env.*` 무시에서 example만 추적).
- **`src/main/resources/application.yml`**: `spring.data.redis.password: ${REDIS_PASSWORD:}` 추가 — 빈 기본값은 `RedisPassword.none()`이라 dev/test 무영향.

## Verification
- `docker compose -f docker-compose.prod.yml --env-file .env.prod.example config` **valid**.
- 포트 publish: caddy `80:80`·`443:443`만 — postgres/redis/app에 5432/6379/8080 host publish **없음**(grep 확인).
- `git check-ignore`: `.env.prod` 무시 O, `.env.prod.example` 추적 가능 O.
- **`./gradlew build` BUILD SUCCESSFUL(2m46s, EXIT 0)** — redis password 추가가 Testcontainers(비밀번호 없는 redis) dev/test 무영향임을 IT로 실증.
- 수집/캐시/event-impact 로직 diff 0줄.

## Notes
- redis `RedisPassword.of(blank) → none()`이므로 빈 `REDIS_PASSWORD`는 미AUTH(dev/test), 실값은 AUTH(prod). 전체 빌드 그린으로 검증됨.
- 실제 `up -d --build` 스택 기동은 VM에서(런북 18-04) — 여기선 compose 구문·정적 검증까지.
