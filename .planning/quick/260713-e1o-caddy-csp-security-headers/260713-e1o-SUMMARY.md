---
quick_id: 260713-e1o
slug: caddy-csp-security-headers
date: 2026-07-13
status: complete
commit: e347b65
---

# Quick Task 260713-e1o 완료 — Caddy CSP + 보안 헤더 보강

## 무엇을 했나

`frontend/Caddyfile`의 `header` 블록에 3개 보안 헤더 추가:

1. **Content-Security-Policy** (핵심 — XSS 심층 방어)
   ```
   default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';
   img-src 'self' data: https://cdn-lostark.game.onstove.com; font-src 'self';
   connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self';
   frame-ancestors 'none'
   ```
2. **Permissions-Policy** — geolocation/camera/mic/payment/usb 등 미사용 기능 차단
3. **Cross-Origin-Opener-Policy: same-origin** — 브라우징 컨텍스트 격리

## 왜 이 값인가 — 라이브 실측 검증

추정 대신 Playwright `page.route`로 후보 CSP를 라이브 응답 헤더에 주입하고 리로드,
`securitypolicyviolation` 이벤트를 구조적으로 수집:

| 항목 | 실측 | 반영 |
|------|------|------|
| 진입 script | `/assets/index-*.js`(module, self), 인라인 script 0 | `script-src 'self'` |
| stylesheet | `/assets/index-*.css`(self) | `style-src 'self'` |
| 런타임 inline style | Recharts 차트/범례 18개 요소 `style="background-color:…"` | `'unsafe-inline'` 필수 |
| 폰트 | `/assets/inter-*.woff2`(@fontsource 번들) | `font-src 'self'` |
| 이미지 | `cdn-lostark.game.onstove.com`(아이콘 atlas+배너)+self favicon | `img-src` 에 CDN 허용 |
| API | 전부 same-origin `/api/*` | `connect-src 'self'` |

**eval 판정:** unsafe-eval 없이도 유일 위반은 `script-src <- eval` 1건 —
번들의 `get value` 게터 내 `new Function()`(빈 body) = 라이브러리 일회성 로드타임
기능 프로브. eval 차단 상태로 timeline·dashboard·impact 3페이지 정상 렌더 + 콘솔 에러 0,
**Radix Select 드롭다운도 정상**(22개 옵션·floating-ui 위치계산 OK). → 기능 의존 아님,
`'unsafe-eval'` 미포함(하드닝 유지). 프로덕션 콘솔에 blocked-eval 경고 1줄만 남음(무해).

**의도적 제외:** COEP `require-corp`(onstove CDN 이미지 CORP 미제공 → 아이콘 깨짐),
`upgrade-insecure-requests`(불필요, 하위리소스 이미 전부 https).

## 검증

- `docker run caddy:2 caddy validate` → **Valid configuration** (문법·헤더 어댑팅 OK)
- 라이브 실측: 3페이지 + Select 인터랙션에서 CSP 위반 = 무해 eval 1건 외 0

## 남은 것 (사용자 조치)

라이브 반영은 VM 재배포 필요:
```
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build caddy
```
재배포 후 `curl -sI https://lostark-tracker.duckdns.org | grep -i content-security`로 확인.

## Core Value 가드

수집/캐시/서빙/event-impact 로직 0줄. 순수 엣지 헤더(Caddy 1파일, +7줄).

## 커밋

- `e347b65` feat(caddy): CSP + 보안 헤더 보강 (Permissions-Policy·COOP) [260713-e1o]
