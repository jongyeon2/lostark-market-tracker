---
quick_id: 260812-efi
slug: caddyfile-spa-cache-header-hardening
description: Caddyfile SPA 캐시 헤더 하드닝 — /assets/* 전용 handle immutable, 그 외 no-cache 재검증
date: 2026-08-12
status: planned
---

# PLAN — Caddyfile SPA 캐시 헤더 하드닝 (quick-260812-efi)

## 배경 / 문제

배포 후 `loaket.kr/timeline`에서 **첫 로드 = 옛 칩 UI, 새로고침 = 새 아코디언 UI**로 렌더되는 버그.
데이터(전율·재련보조)는 양쪽 동일 → 백엔드가 아니라 **프론트 정적 자산 캐시** 문제.

**근본 원인:** `frontend/Caddyfile`의 SPA 서빙 `handle` 블록이 `Cache-Control`을 전혀 안 준다.
Caddy `file_server` 기본값은 `ETag`/`Last-Modified`만 주므로, 브라우저가 RFC 7234 **휴리스틱
캐싱**으로 옛 `index.html`을 재검증 없이 재사용 → 옛 번들 해시(옛 UI) 로드. 새로고침은 메인 문서를
강제 재검증하므로 새 `index.html` → 새 번들 → 새 UI가 뜬다.

## 해결 (보안 하드닝 버전)

SPA 표준 2-갈래 캐시로 전환하되, `/assets/*`를 **전용 handle로 분리**한다:

- **`/assets/*` (콘텐츠 해시 자산)** → `Cache-Control: public, max-age=31536000, immutable`.
  전용 handle이라 SPA fallback을 타지 않아 "없는 해시 = 404"(HTML이 JS URL로 immutable 캐시되는
  오염 차단). 파일명에 해시가 있어 내용이 바뀌면 URL이 바뀌므로 1년 캐시가 안전하다.
- **그 외 전부 (index.html + `/timeline`·`/admin` SPA fallback)** → `Cache-Control: no-cache`
  (매번 재검증). 배포 즉시 새 번들 참조가 전파되고, 응답 헤더도 함께 캐시되므로 CSP 등 **보안
  헤더도 stale 없이 최신 전파**된다(보안 이득).

`/api/*`·`/actuator/*`는 앞선 별도 handle에서 처리되어 이 캐시 헤더의 영향을 받지 않는다(사용자
데이터·헬스·관리자 API가 immutable 캐시되는 경로가 구조적으로 없음).

## Task 1 — Caddyfile `(site)` 스니펫 캐시 헤더 추가

- **files:** `frontend/Caddyfile`
- **action:** `(site)` 스니펫의 마지막 catch-all `handle {}` 앞에 `handle /assets/* {}` 전용 블록을
  추가(immutable). catch-all `handle {}`에는 `header Cache-Control "no-cache"` 한 줄 추가.
  `(site)`는 공개(apex)·tailnet(:8081) 두 리스너가 공유하므로 한 번 수정으로 양쪽 적용.
- **verify:** `caddy validate`(가능 시)로 파싱 확인. 로컬 미가용이면 구문이 파일 내 기존
  `handle /api/*`·`header` 용법과 동일함을 육안 확인. 배포 후 스모크:
  `curl -sI https://loaket.kr/assets/<hash>.js | grep -i cache-control` → immutable,
  `curl -sI https://loaket.kr/ | grep -i cache-control` → no-cache, CSP 동시 노출 확인.
- **done:** `/assets/*`는 immutable, 나머지는 no-cache 헤더를 서빙. 백엔드 0줄, JS/TS 0줄.

## 비목표 / 가드

- 백엔드·프론트 코드·docker-compose·크론·백업 무변경. Caddyfile 1파일만.
- 보안 헤더 블록(CSP·HSTS 등)·admin 404 규칙 무변경.
- 배포는 별도(머지 → 이미지 빌드 → 승인). 이 PLAN은 소스 변경까지만.
