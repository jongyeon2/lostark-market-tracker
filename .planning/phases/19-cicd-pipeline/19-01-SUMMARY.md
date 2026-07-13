---
phase: 19-cicd-pipeline
plan: 01
status: complete
requirements: [CICD-01, CICD-02]
commits: [940901b, 97d44b6]
---

# 19-01 SUMMARY — GHCR 이미지화 + CI 게이트

## 무엇을 했나

CI를 CD-준비 파이프라인으로 확장했다. 앱 로직(수집/캐시/event-impact/서빙) **0줄**.

### Task 1 — `docker-compose.prod.yml` GHCR image 참조 (커밋 `940901b`)
- app → `image: ghcr.io/jongyeon2/lostark-app:${IMAGE_TAG:-latest}`
- caddy → `image: ghcr.io/jongyeon2/lostark-web:${IMAGE_TAG:-latest}`
- `build:` 는 로컬 폴백으로 유지. **VM은 `IMAGE_TAG=sha-xxx ... pull && up -d`로 완성 이미지만 받아 실행**(4GB ARM에서 빌드 안 함).
- postgres/redis 포트 미공개·caddy만 80/443 정책 무변경.

### Task 2 — `.github/workflows/ci.yml` 3-job 파이프라인 (커밋 `97d44b6`)
- **backend**: 기존 `./gradlew build`(Testcontainers) 게이트 유지
- **frontend**(신규): `npm ci` + `npm run build`(`tsc -b && vite build`) — 타입체크+빌드 게이트
- **images**(신규): `main` push + 두 게이트 그린일 때만(`needs:[backend,frontend]`). **linux/arm64**(Oracle Ampere)로 app(`Dockerfile`)·web(`frontend/Dockerfile`) 빌드 → GHCR `sha-<커밋>`+`latest` 푸시. `runs-on: ubuntu-24.04-arm`(네이티브), gha 캐시.
- 인증 = `GITHUB_TOKEN` + job-level `packages: write`만. **앱 시크릿(.env.prod) CI 미주입**.

## 검증

- `docker compose -f docker-compose.prod.yml --env-file .env.prod.example config` → **유효**, image가 `:latest`(IMAGE_TAG 기본)로 해석, 포트 정책 무변경.
- `.github/workflows/ci.yml` → **actionlint(공식 린터) 통과**(경고 0) + 구조 검증(3 job·`needs` 게이트·arm64 2·`sha`+`latest` 태그·프론트 게이트·`GITHUB_TOKEN`만·앱 시크릿 참조 0).
- 수집/서빙 로직 diff 0줄.

## 아직 안 된 것 / 다음

- **실 이미지 push는 다음 `main` push 때 최초 발생** — 그때 Actions에서 arm64 이미지 2개가 GHCR에 올라가는지 확인 필요.
- **1회성(사용자/19-02)**: 첫 push 후 GHCR 패키지 2개(`lostark-app`·`lostark-web`)를 **public으로 전환**(VM에서 `docker login` 불필요). arm 미스매치는 배포 대표 실패 원인 → 19-02 스모크 전 `docker manifest inspect`로 arch 확인 권장.
- **19-02**(배포 job, Tailscale SSH pull+재기동+스모크)는 **사용자 사전 조치 필요**: Tailscale 계정·키 발급 + VM 설치. **19-03**(런북·README·systemd `--build` 제거·롤백·배지).
- `ubuntu-24.04-arm` 러너 미가용 시 `ubuntu-latest` + `docker/setup-qemu-action` + `platforms: linux/arm64` 폴백(빌드 느려짐) — 워크플로우 주석에 명시.

## 커밋
- `940901b` feat(19-01): docker-compose.prod.yml GHCR image 참조
- `97d44b6` feat(19-01): ci.yml 3-job 파이프라인
