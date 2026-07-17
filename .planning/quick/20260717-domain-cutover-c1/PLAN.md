---
task: 도메인 이관 커밋 #1 — 듀얼 서빙 Caddyfile
quick_id: 260717-gvl
status: in-progress
date: 2026-07-17
---

# PLAN — 도메인 이관 커밋 #1 (듀얼 서빙)

2단계 컷오버의 1단계. `loaket.kr`(신규 대표)과 `lostark-tracker.duckdns.org`(옛)를
**동시에 서빙**하도록 `frontend/Caddyfile`만 바꾼다. 롤백 여유를 남긴 채 새 도메인을 라이브로 올리는 게 목적.

## 확정 사실 (사용자 제공·실측)
- 신규 대표: `loaket.kr` (apex, `{$SITE_ADDRESS}`가 될 값)
- www: `www.loaket.kr` → apex 301 permanent
- 옛: `lostark-tracker.duckdns.org` (컷오버 임시 병행, 커밋 #2에서 삭제)
- VM 공인 IP: `161.33.33.36` / 가비아 A레코드 `@`·`www` 둘 다 지정·전파 확인됨(nslookup)
- 배포 메커니즘: Caddyfile은 `lostark-web` 이미지에 구워짐(`frontend/Dockerfile:13`) →
  변경은 커밋→CI 재빌드→VM pull&up. `SITE_ADDRESS`는 VM `.env.prod`(`compose:82`). main push→자동 배포+스모크.

## 범위 (커밋 #1)
**변경**: `frontend/Caddyfile` 단 하나.
- `(site)` 스니펫 유지(서빙 규칙: /api·/actuator 프록시, SPA fallback).
- `(public)` 스니펫 신설 = admin /404 4블록 + `import site` + 보안 헤더. → 공개 도메인 공용.
- `{$SITE_ADDRESS} { import public }` — 신규 apex 공개 서빙.
- `www.{$SITE_ADDRESS} { redir https://{$SITE_ADDRESS}{uri} permanent }` — www→apex 301.
- `lostark-tracker.duckdns.org { import public }` — 옛 도메인 병행(임시).
- `http://:8081 { import site }` — tailnet 관리자, 무변경.

**변경 안 함(커밋 #2로 미룸)**: `ci.yml` 스모크 URL(duckdns 유지), `README`/`runbook`/`.env.prod.example`.
**절대 무변경**: 백엔드·프론트 애플리케이션 코드.

## 검증
`docker run caddy:2-alpine caddy validate`로 두 env 케이스:
1. `SITE_ADDRESS=loaket.kr` → **통과**해야 함(정상 컷오버 상태).
2. `SITE_ADDRESS=lostark-tracker.duckdns.org` → **중복 호스트로 실패**해야 함
   (하드코딩 duckdns 블록과 충돌 — 이게 곧 "배포 전 .env.prod 먼저 바꿔라"의 근거).

## 하드 스톱 (사용자 지시)
- 커밋은 만들되 **push 금지**.
- 사용자가 VM `.env.prod`의 `SITE_ADDRESS=loaket.kr` 변경 완료를 **확인하기 전엔** push/배포 중단.
- 이유: `SITE_ADDRESS`가 duckdns인 채 이 Caddyfile이 배포되면 호스트 중복으로 Caddy가 안 뜬다(사이트 전체 중단).
