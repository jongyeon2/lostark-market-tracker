---
task: 도메인 이관 커밋 #2 — 전환 확정 (duckdns 제거)
quick_id: 260717-i2l
status: complete
date: 2026-07-17
---

# PLAN — 도메인 이관 커밋 #2 (전환 확정)

2단계 컷오버의 2단계. 커밋 #1(듀얼 서빙)이 배포돼 `loaket.kr`가 라이브·인증서·www 리다이렉트까지
확인됐다. 이제 옛 duckdns 병행 서빙을 종료하고 저장소의 운영 도메인 참조를 `loaket.kr`로 정착시킨다.

## 선행 확인 (사용자 보고)
- GitHub Actions build/deploy/smoke 성공, `https://loaket.kr/dashboard` 정상, HTTPS 인증서 정상.
- `www.loaket.kr` → `loaket.kr` 301, 옛 `lostark-tracker.duckdns.org`도 아직 정상(롤백 여유 살아있음).
- `loaket.kr/admin` = 404. VM `.env.prod`는 이미 `SITE_ADDRESS=loaket.kr`.

## 범위 (운영 5파일만)
1. `frontend/Caddyfile` — `lostark-tracker.duckdns.org` 임시 서빙 블록 **제거**. `{$SITE_ADDRESS}`(apex)
   공개 서빙 + `www.{$SITE_ADDRESS}`→apex 301 + `http://:8081` tailnet 관리자만 남긴다. `(public)`
   스니펫의 `/admin` 404·보안헤더 유지.
2. `.github/workflows/ci.yml` — 스모크 URL 2곳 → `https://loaket.kr`.
3. `.env.prod.example` — SITE_ADDRESS 예시 → `loaket.kr`.
4. `docs/deploy/oracle-vm-runbook.md` — §3 DuckDNS 절차 → 가비아 A레코드(@·www) 재작성 + SITE_ADDRESS·검증 curl 3곳 → `loaket.kr`.
5. `README.md` — 라이브 데모 링크 5곳 → `loaket.kr`.

**역사 보존(교체 안 함)**: `.planning/**` 16파일의 duckdns 참조 — 과거 phase/quick 계획·요약·회고·로드맵. 그 시점 기록이라 보존.
**무변경**: 백엔드·프론트 앱 코드 0줄. VM `.env.prod`(이미 loaket.kr). DuckDNS 갱신 크론 실제 제거는 사용자 작업.

## 검증
- `caddy fmt` clean + `caddy validate`(SITE_ADDRESS=loaket.kr) → Valid.
- `ci.yml` YAML 파싱(yq) — backend/frontend/images/deploy 4잡.
- 저장소 전체 `lostark-tracker.duckdns.org` 잔존 0(untracked 스크린샷 제외).

## 하드 스톱
- 커밋 생성하되 **push 대기** — 사용자 검토 후 push 지시.
- push 시 duckdns 블록이 사라진 web 이미지가 배포되어 옛 도메인 서빙 종료, 스모크는 loaket.kr를 때린다.
