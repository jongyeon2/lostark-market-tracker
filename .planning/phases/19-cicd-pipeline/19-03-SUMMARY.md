---
phase: 19-cicd-pipeline
plan: 03
status: complete
requirements: [CICD-04]
commits: []
---

# 19-03 SUMMARY — 운영 문서 마무리 (첫 실배포 반영 · Runbook · --build 제거)

## 무엇을 했나

첫 실배포가 성공한 CI/CD 파이프라인을 운영 문서·설정에 정직하게 반영했다(앱 로직 0줄). **실사 기준**: 2026-07-14 GitHub Actions run #30(커밋 `916bd3a`) — backend·frontend·images·deploy 전 job 성공, VM에 `sha-916bd3a` 4컨테이너 Up, 공개 HTTPS `/actuator/health`={"status":"UP"} 확인.

- **운영 VM 소스 빌드 제거**: `deploy/lostark.service` ExecStart의 `up -d --build` → `up -d`(GHCR 이미지 pull 기반, 부팅 시 로컬 이미지로 기동). `docker-compose.prod.yml` 헤더 주석도 pull 기반으로 갱신. compose `build:` 블록은 **오프라인 폴백으로 유지**. 이미 성공한 `ci.yml` deploy job 로직은 **무변경 보존**(주석 1건만 정정).
- **정확성 정정**: 저장소는 **PRIVATE**(`jongyeon2/lostark-market-tracker`) — `ci.yml`·ROADMAP의 'public 저장소 무료' 문구를 'GitHub-hosted arm64'로 바로잡음(패키지 private + VM docker login 유지 근거와 일치).
- **운영 Runbook 추가**(`docs/deploy/oracle-vm-runbook.md` §10): 파이프라인 흐름·정상 배포·상태 확인(`compose ps/images`, `docker inspect` 태그, HTTPS health)·컨테이너 로그·**특정 sha 롤백**(+지속 롤백=`git revert`)·**배포 실패 진단 순서**(job/step별 분기)·**GHCR 인증 실패 대응**(VM 재로그인)·**Tailscale 연결 실패 대응**(`tailscale status`·OAuth·ACL·iptables)·**SSH 복구 경로 주의**. §4 SSH 주석에 Tailscale CI 경로 + 공개 22 `/32`는 **비상 복구용 유지**(폐쇄는 후속) 명시. §6/§9 수동 경로도 pull 기반으로 갱신.
- **README** "어떻게 배포했나"에 자동 배포 흐름(GitHub Actions→GHCR→Tailscale SSH→무중단 교체→HTTPS 헬스체크·롤백)을 클라이언트 친화적으로 1줄 추가.
- **planning 갱신**: STATE·ROADMAP·19-02-SUMMARY의 'deploy skip / 사용자 사전조치 대기' stale 문구 제거 + 첫 실배포 성공 기록. ROADMAP 19-03 체크·Phase 19 3/3·v1.4 완료.

## 검증

- `docker compose -f docker-compose.prod.yml config -q` **파싱 OK**(더미 env).
- `ci.yml` 변경은 **주석 한정**(git diff로 확인) → actionlint 결과 불변(19-02에서 exit 0 통과, actionlint는 주석 무시). *로컬 Docker 데몬 미기동으로 actionlint 재실행은 생략 — 주석-only diff로 등가 보장.*
- `git diff --check` 실제 경고 0(LF→CRLF는 기존 autocrlf 정보성).
- 앱 코드(수집/캐시/event-impact/서빙) **0줄** — Java/TS 테스트 대상 변화 없음. 다음 `main` push 시 CI가 전체 테스트를 게이트로 재실행.
- 시크릿 값·개인키·PAT **미기재**(문서엔 절차·플레이스홀더만).

## 남은 수동 작업 (문서에만 남김)

- [ ] **공개 SSH 22 폐쇄** — OCI Ingress 22 `/32` 제거. **선행**: Windows Tailscale 클라이언트로 운영자 SSH 접속 실검증(검증 전엔 비상 복구 경로 유지 위해 닫지 않음). 런북 §10.9.

## 커밋

- (config) `deploy/lostark.service`·`docker-compose.prod.yml`·`.github/workflows/ci.yml` — --build 제거·pull 기반·주석 정정
- (docs) `docs/deploy/oracle-vm-runbook.md`·`README.md` — 운영 Runbook + 자동 배포 반영
- (planning) 19-03-PLAN·19-03-SUMMARY·19-02-SUMMARY·ROADMAP·STATE — Phase 19 3/3 완료 + 첫 실배포 기록
