---
phase: 19-cicd-pipeline
plan: 02
status: complete
requirements: [CICD-03, CICD-04]
commits: [c5cde94]
---

# 19-02 SUMMARY — 배포 job (Tailscale SSH → VM pull+재기동+스모크)

## 무엇을 했나

`.github/workflows/ci.yml`의 `images` 뒤에 **게이트된 `deploy` job**을 추가했다(앱 로직 0줄).

- **트리거/게이트**: `main` push + `needs:[images]` + **`vars.DEPLOY_ENABLED == 'true'`**. 사전조치 전에는 skip → **커밋해도 CI 그린 유지**(조기 배포·붉은 CI 없음). `environment: production` + `concurrency(group: deploy-production)`.
- **접근**: `tailscale/github-action@v3`(OAuth)로 러너를 프라이빗 tailnet에 임시 연결 → VM tailnet 주소로 **키 기반 SSH**. **공개 SSH(22) 개방 불필요**(/32 하드닝 유지 또는 22 폐쇄 가능).
- **배포**: `docker-compose.prod.yml`을 VM `/opt/lostark-price-tracker/`로 `scp` → `IMAGE_TAG=sha-<커밋>`으로 `docker compose pull && up -d --remove-orphans`(**VM 빌드 0**). 패키지 **private 유지** — VM의 1회 `docker login`으로 pull하므로 **CI엔 GHCR 자격증명 미주입**.
- **검증/롤백**: 공개 HTTPS 스모크(`/actuator/health`=UP·`/api/health/collection`=200, 재기동 대비 `--retry-all-errors`). 실패 시 loud fail. 롤백=이전 `sha-` 태그로 재실행(19-03 런북).
- **보안**: 모든 시크릿은 `env:`로 전달(shell 인터폴레이션 0). top-level `permissions: contents: read` 유지.

## 검증
- **actionlint 통과**(exit 0, 경고 0) + 구조 확인(deploy·게이트·needs·environment·concurrency·Tailscale·scp·IMAGE_TAG pull·스모크·env 전달).
- `DEPLOY_ENABLED` 미설정 시 job skip → 커밋 후에도 CI 그린(다음 push에서 확인).

## 라이브 배포는 사용자 사전조치 필요 (1회)

`DEPLOY_ENABLED=true` 켜기 전에:
1. **Tailscale**: tailnet + OAuth client(tag:ci) → secrets `TS_OAUTH_CLIENT_ID`/`TS_OAUTH_SECRET`. ACL에 `tag:ci → VM:22` 허용.
2. **VM**: `tailscale up`(설치·가입) → tailnet IP → secret `VM_HOST`, `VM_USER`.
3. **VM**: `read:packages` PAT로 `docker login ghcr.io -u jongyeon2`(1회, config.json 잔존). PAT는 VM에만.
4. **VM**: 배포 SSH 공개키를 `authorized_keys`에 → 개인키를 secret `SSH_PRIVATE_KEY`.
5. **VM**: iptables가 `tailscale0`를 막지 않는지 확인(필요 시 허용) → 이후 공개 22 폐쇄 가능.
6. **GitHub**: `production` environment + 위 secrets 등록.
7. **GitHub**: 변수 `DEPLOY_ENABLED=true` → 다음 main push부터 무인 배포+스모크.

## 커밋
- `c5cde94` feat(19-02): ci.yml deploy job — Tailscale SSH → VM pull+재기동+스모크 (게이트)
