---
quick_id: 260812-efi
slug: caddyfile-spa-cache-header-hardening
description: Caddyfile SPA 캐시 헤더 하드닝 — /assets/* 전용 handle immutable, 그 외 no-cache 재검증
date: 2026-08-12
status: complete
commits: [07e67a8]
---

# SUMMARY — Caddyfile SPA 캐시 헤더 하드닝 (quick-260812-efi)

`frontend/Caddyfile` 1파일 수정. 백엔드 0줄, 프론트 JS/TS 0줄.

## 문제

배포 후 `loaket.kr/timeline`에서 **첫 로드 = 옛 칩 UI, 새로고침 = 새 아코디언 UI**. 데이터(전율·
재련보조 10)는 양쪽 동일 → 프론트 정적 자산 캐시 문제. 근본 원인: SPA 서빙 `handle`이
`Cache-Control`을 전혀 안 줘서, Caddy `file_server` 기본값(ETag/Last-Modified만)에 대해 브라우저가
RFC 7234 **휴리스틱 캐싱**으로 옛 `index.html`을 재검증 없이 재사용 → 옛 번들 해시(옛 UI) 로드.
새로고침만 메인 문서를 강제 재검증하므로 새 UI가 떴다.

## 변경 (보안 하드닝 버전)

| 파일 | 변경 |
|---|---|
| `frontend/Caddyfile` | `(site)` 스니펫에 `handle /assets/* {}` 전용 블록 추가(immutable) + catch-all `handle {}`에 `Cache-Control "no-cache"` 한 줄 추가 |

- **`/assets/*`** → `Cache-Control: public, max-age=31536000, immutable`. 전용 handle이라 SPA
  fallback 미적용 → 없는 해시는 index.html이 아니라 404(**HTML이 JS URL로 immutable 캐시되는 오염
  차단**). 콘텐츠 해시 파일명이라 내용이 바뀌면 URL이 바뀌므로 1년 캐시 안전.
- **그 외(index.html + `/timeline`·`/admin` SPA fallback)** → `Cache-Control: no-cache`(매번 재검증).
- `(site)`는 공개(apex)·tailnet(:8081) 공유 → 한 번 수정으로 양쪽 적용.

## 보안 관점 (사용자 요청)

- immutable 대상은 `/assets/*`(공개 정적 JS/CSS)뿐 — 비밀 없음, `public` 정당.
- `/api/*`·`/actuator/*`는 앞선 별도 handle에서 처리 → 사용자 데이터·헬스·관리자 API가 캐시 헤더
  영향을 **구조적으로** 안 받음.
- `no-cache`는 오히려 보안 이득: 응답 헤더가 본문과 함께 캐시되므로, `index.html`을 매번 재검증하면
  CSP·X-Frame-Options 등 **보안 헤더가 stale 없이 최신 전파**된다(옛 셸=옛 취약 헤더 재사용 방지).
- `no-store`가 아닌 `no-cache`인 이유: 셸에 PII/토큰이 없어 재검증(304 효율 유지)이 올바른 균형.

## 검증

- ✅ 편집 구간 구조 검증(중괄호 균형 + 파일 내 기존 `handle /api/*`·`header`·`root`/`file_server`
  용법과 동일). `caddy validate`는 로컬 미가용(docker 미기동 + caddy 바이너리 없음)이라 못 돌림.
- ⏳ **최종 게이트 = 배포**: 파싱 오류면 Caddy 컨테이너가 안 떠서 CI 스모크 테스트가 loud 실패.
  배포 후 확인:
  - `curl -sI https://loaket.kr/assets/<hash>.js | grep -iE 'cache-control|content-security'`
    → `public, max-age=31536000, immutable` **+** CSP 동시 노출
  - `curl -sI https://loaket.kr/ | grep -i cache-control` → `no-cache`

## 주의 (자가 치유 한계)

이 수정은 **앞으로의 배포**를 고친다. 이미 옛 `index.html`을 휴리스틱 캐시로 든 브라우저는 신선
기간이 끝나거나 한 번 더 재검증(새로고침)해야 새 no-cache 헤더를 받는다 — 이후 모든 배포부터는
재방문자도 첫 로드에 즉시 최신본을 받는다. 브라우저에 이미 박힌 캐시를 소급 삭제하진 못한다.

## 후속

- 브랜치 `quick/260812-efi-caddyfile-cache-headers`(main에서 분기). PR → CI 그린 → 배포(승인)로
  적용된다. 배포 후 위 curl 스모크로 확인.
