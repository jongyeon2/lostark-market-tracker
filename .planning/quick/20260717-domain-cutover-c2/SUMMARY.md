---
task: 도메인 이관 커밋 #2 — 전환 확정 (duckdns 제거)
quick_id: 260717-i2l
status: complete
date: 2026-07-17
commit: 0a5ce22
pushed: false
---

# SUMMARY — 도메인 이관 커밋 #2 (전환 확정)

2단계 컷오버 완료. 커밋 #1로 `loaket.kr`가 라이브가 된 걸 확인한 뒤, 옛 duckdns 병행 서빙을 접고
저장소의 운영 도메인 참조를 `loaket.kr`로 정착시켰다. 앱 코드 0줄. 커밋은 만들었고 **push는 대기**.

## 무엇을 바꿨나 (운영 5파일)

| 파일 | 변경 목적 |
|---|---|
| `frontend/Caddyfile` | `lostark-tracker.duckdns.org` 임시 서빙 블록 **제거**. apex(`{$SITE_ADDRESS}`) 공개 서빙 + www→apex 301 + `:8081` tailnet 관리자만 남김. `(public)` 스니펫의 /admin 404·보안헤더 유지 |
| `.github/workflows/ci.yml` | 배포 후 스모크 URL 2줄(L174·176) → `https://loaket.kr` |
| `.env.prod.example` | SITE_ADDRESS 예시 → `loaket.kr` |
| `docs/deploy/oracle-vm-runbook.md` | §3 DuckDNS 절차 → 가비아 A레코드(@·www) 재작성 + SITE_ADDRESS·검증 curl 3곳(§5·§10.2·§10.4) → `loaket.kr` |
| `README.md` | 라이브 데모 링크 5곳 → `loaket.kr` |

## `lostark-tracker.duckdns.org` 전체 검색 → 처리

- **운영 참조(교체)**: 위 5파일.
- **역사 기록(보존)**: `.planning/**` 16파일 — 과거 phase(18-CONTEXT/18-02·03·04-PLAN/18-04-SUMMARY/19-02-PLAN/28-UI-SPEC),
  quick(20260716-admin-tailnet-only, 20260717-domain-cutover-c1, 260713-glz/h3s/e1o), STATE·ROADMAP.
  그 시점의 사실·결정 기록이라 바꾸면 역사가 왜곡된다 → 그대로 둔다.
- **잔존**: `.playwright-mcp/*.yml`(untracked 스크린샷 스냅샷) 1건 — git 미추적 부산물이라 무시.
- 최종 확인: 저장소 tracked 파일에 운영 `lostark-tracker.duckdns.org` **0건**.

## 검증

- `caddy fmt --diff` → clean(exit 0). *(Write가 끝 개행을 빠뜨려 fmt exit 1 → `caddy fmt --overwrite`로 개행 정규화 후 clean)*
- `caddy validate`(SITE_ADDRESS=loaket.kr) → **`Valid configuration`**(exit 0).
- `ci.yml` YAML 파싱(yq) → jobs `backend/frontend/images/deploy` 4개 정상.
- 운영 파일 남은 `duckdns` = runbook §3 마이그레이션 각주 2줄(의도적 역사 표기)뿐.

## 상태 / 다음

- 커밋 **0a5ce22**(운영 5파일) + docs 커밋. **push 안 함** — 사용자 검토 후 지시.
- push 시: web 이미지 재빌드(duckdns 블록 없음) → 자동 배포 → 옛 도메인 서빙 종료 → 스모크는 `loaket.kr`(이미 라이브라 통과).
- 배포 성공 후 **사용자 작업**: VM에서 DuckDNS IP 갱신 크론 제거(설정돼 있었다면) + DuckDNS 서브도메인 폐기.
