---
phase: 18-free-deploy-security
plan: 02
status: complete
date: 2026-07-08
requirements: [DEPLOY-02]
---

# Phase 18-02 Summary — Caddy 리버스 프록시 + 프론트 정적 서빙

**동일 출처 웹 진입점 구성. Caddy가 자동 HTTPS + 정적 SPA 서빙 + /api·/actuator 프록시. 프론트 소스 0줄.**

## Changes
- **`frontend/Caddyfile`**(신규): `{$SITE_ADDRESS}` 자동 HTTPS(Let's Encrypt·`email {$ACME_EMAIL}`), `handle /api/*`·`/actuator/*` → `reverse_proxy app:8080`, `handle {}` → `root * /srv` + `try_files {path} /index.html`(SPA fallback) + `file_server`. 보안 헤더 HSTS·nosniff·X-Frame-Options:DENY·Referrer-Policy·`-Server`.
- **`frontend/Dockerfile`**(신규): 멀티스테이지 `node:20-alpine`(`npm ci && npm run build`) → `caddy:2-alpine`(dist→/srv, Caddyfile→/etc/caddy/Caddyfile).
- **`frontend/.dockerignore`**(신규): node_modules·dist·.env·Dockerfile 제외.

## Deviation (계획 대비)
- 계획은 `Caddyfile`(repo 루트) + web 빌드 컨텍스트=repo 루트였으나, **실행 중 정정**: 앱 이미지도 루트 컨텍스트를 쓰므로 하나의 루트 `.dockerignore`가 두 빌드를 동시에 지배 → 앱 빌드용 `frontend` 제외가 web 빌드(프론트 필요)와 충돌. 해결: **web 이미지는 `frontend/`를 빌드 컨텍스트**로 쓰고 `Caddyfile`을 `frontend/Caddyfile`에 배치(각 빌드가 자기 `.dockerignore`를 가짐). 앱 이미지는 그대로 루트 컨텍스트. → 18-03 compose는 `web.build: {context: frontend, dockerfile: Dockerfile}`.

## Verification
- `cd frontend && npm run build` **그린**(EXIT 0, built in 3.71s, dist 산출). 청크 크기 경고는 기존 이슈(무관).
- grep 5/5: `reverse_proxy app:8080` · `try_files` · `Strict-Transport-Security` · `caddy:2` · `COPY Caddyfile`.
- 프론트 소스(`src/`) diff 0줄 — 서빙 계층만 추가.

## Notes
- Docker/Caddy 이미지 빌드·`caddy validate`는 이 환경 미실행 — VM에서 검증(런북 18-04). 로컬 npm 빌드로 dist 재현성 확인.
