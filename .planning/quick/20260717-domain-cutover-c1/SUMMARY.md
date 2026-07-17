---
task: 도메인 이관 커밋 #1 — 듀얼 서빙 Caddyfile
quick_id: 260717-gvl
status: complete
date: 2026-07-17
commit: daa2f88
pushed: false
---

# SUMMARY — 도메인 이관 커밋 #1 (듀얼 서빙)

`loaket.kr`(신규 대표)로의 2단계 컷오버 중 1단계. `frontend/Caddyfile` **하나만** 바꿔
신규 도메인과 옛 duckdns를 **동시에 서빙**하도록 만들었다. 커밋은 만들었고 **push는 대기**(사용자 게이트).

## 무엇을 바꿨나 (frontend/Caddyfile 1파일, +33/−13)

- **`(public)` 스니펫 신설** = admin `/404` 4블록 + `import site` + 보안 헤더 7종. 공개 도메인 공용.
  - 이전엔 이 규칙이 `{$SITE_ADDRESS}` 블록에 인라인이라 duckdns를 추가하려면 복붙해야 했다 →
    한쪽만 고쳐져 갈라질 위험. 스니펫으로 묶어 **한 곳**에서 두 도메인에 동일 적용.
- **`{$SITE_ADDRESS}` { import public }** — 신규 apex(loaket.kr) 공개 서빙.
- **`www.{$SITE_ADDRESS}` { redir https://{$SITE_ADDRESS}{uri} permanent }** — www→apex 301.
  Caddy가 env를 인라인 확장하므로 도메인명 하드코딩 없음.
- **`lostark-tracker.duckdns.org` { import public }** — 옛 도메인 병행(⚠️ 임시, 커밋 #2에서 삭제).
- **`http://:8081` { import site }** — tailnet 관리자, 무변경.

**안 바꾼 것**: `ci.yml` 스모크 URL(duckdns 유지) · README · runbook · .env.prod.example → 전부 커밋 #2.
**절대 무변경**: 백엔드·프론트 애플리케이션 코드(0줄).

## 🔑 하드 스톱을 caddy validate로 증명

Caddyfile은 `lostark-web` 이미지에 구워지고(`frontend/Dockerfile:13`), `SITE_ADDRESS`는 VM `.env.prod`
(`compose:82`)에서 온다. 그래서 **배포 전에 VM `.env.prod`의 SITE_ADDRESS를 loaket.kr로 먼저 바꿔야** 한다.
안 바꾸면 `{$SITE_ADDRESS}`(=duckdns)와 하드코딩 `lostark-tracker.duckdns.org` 블록이 같은 호스트 →
Caddy가 안 뜬다(공개 사이트까지 중단). docker `caddy:2-alpine`으로 결정적 확인:

| env | 결과 | exit |
|---|---|---|
| `SITE_ADDRESS=loaket.kr` | `Valid configuration` | 0 |
| `SITE_ADDRESS=lostark-tracker.duckdns.org` | `Error: adapting config using caddyfile: ambiguous site definition: lostark-tracker.duckdns.org` | 1 |

`caddy fmt --diff` = 변경 없음(포맷 정상).

## 상태

- 커밋 **daa2f88**(feat(deploy), frontend/Caddyfile 1파일). **push 안 함.**
- push 전 사용자 액션: VM `/opt/lostark-price-tracker/.env.prod`에서 `SITE_ADDRESS=loaket.kr`로 변경 →
  변경 완료 확인 → 그 후에만 push(자동 배포 트리거).

## 다음 (커밋 #2 — 전환 확정)

신규 도메인 라이브 확인 후: Caddyfile에서 duckdns 블록 삭제 + `ci.yml` 스모크 URL 2줄(→loaket.kr) +
README·runbook §3·.env.prod.example 도메인 교체 → push → 스모크가 새 도메인 때림(통과). 이후 duckdns
IP 갱신 크론 제거 + 서브도메인 폐기.
