# Oracle Cloud VM 배포 런북 (무료 상시 배포)

로스트아크 거래소 시세 트래커를 **Oracle Cloud Always Free VM 한 대**에 `docker-compose`로 배포한다. Caddy가 자동 HTTPS + 정적 프론트 서빙 + `/api` 프록시를 담당하고(동일 출처 → CORS 없음), 앱·PostgreSQL·Redis는 내부 네트워크에 격리한다.

> **선행 조건**: 이 저장소의 Phase 18-01~03 산출물이 필요하다 — `Dockerfile`, `frontend/Dockerfile`+`frontend/Caddyfile`, `docker-compose.prod.yml`, `.env.prod.example`.
>
> **보안 원칙(전 과정 불변)**: 실 시크릿은 VM의 `.env.prod`에만 존재. 코드·문서·로그·커밋에 미기재. `.env.prod`은 절대 커밋 금지.

목표 아키텍처:

```
[인터넷] ──HTTPS(443)──▶ Caddy (자동 TLS · 유일한 공개 진입점)
                           ├── /              → file_server(frontend dist, SPA fallback)
                           └── /api,/actuator → reverse_proxy app:8080
                                                  ├── postgres:16 (내부망·포트 미공개)
                                                  └── redis:7 (requirepass·포트 미공개)
```

---

## 1. VM 프로비저닝 (Oracle Cloud Always Free)

1. [Oracle Cloud](https://www.oracle.com/cloud/free/) 계정 생성(신용카드 확인, 과금은 Always Free 자원 내에선 없음).
2. **Compute → Instances → Create Instance**:
   - Image: **Ubuntu 22.04 LTS**(또는 24.04)
   - Shape: **VM.Standard.A1.Flex**(Ampere ARM) — OCPU 1~2 / RAM **최소 2GB, 권장 4GB**(JVM+PG+Redis+Caddy).
   - SSH 키: 로컬 공개키 등록(`~/.ssh/id_ed25519.pub`).
3. **⚠️ "Out of capacity"** 가 뜨면 ARM 물량이 없는 것 — **가용성 도메인(AD)이나 리전을 바꿔 재시도**하거나 잠시 후 다시 시도한다. (AMD `E2.1.Micro`는 RAM 1GB라 이 스택엔 빠듯 → ARM 권장.)
4. 생성 후 **Public IP** 를 기록한다.

```bash
# 로컬에서 접속
ssh ubuntu@<VM_PUBLIC_IP>
```

## 2. 서버 초기화 (Docker 설치)

```bash
sudo apt update && sudo apt -y upgrade

# Docker Engine + compose plugin (공식 편의 스크립트)
curl -fsSL https://get.docker.com | sudo sh

# sudo 없이 docker 사용 (재로그인 필요)
sudo usermod -aG docker $USER
newgrp docker

docker --version && docker compose version
```

## 3. 도메인 연결 (가비아)

1. 가비아에서 도메인 구매(운영: `loaket.kr`).
2. 가비아 DNS 관리 → **A 레코드 2개** 추가(둘 다 VM Public IP):
   - `@`   → VM Public IP
   - `www` → VM Public IP
3. 전파 확인: `nslookup loaket.kr` / `nslookup www.loaket.kr` → VM Public IP.

> Caddy가 `SITE_ADDRESS`(apex)로 Let's Encrypt 인증서를 자동 발급하고, `www`는 apex로 301 리다이렉트한다(`frontend/Caddyfile`).
> Oracle VM Public IP는 고정(reserved)이라 DuckDNS식 IP 갱신 크론은 불필요하다.
> (v1은 무료 DuckDNS 서브도메인을 썼으나 2026-07-17 `loaket.kr`로 이전 — VM에 남은 DuckDNS 갱신 크론은 별도 제거.)

## 4. 방화벽 이중 개방 ⚠️ (보안 핵심)

Oracle VM은 **두 겹의 방화벽**이 있다. **둘 다** 80/443을 열어야 접속된다. (이 단계 누락이 "방화벽 열었는데 접속 안 됨"의 대표 원인.)

### (a) OCI 보안목록 / NSG (클라우드 방화벽)

OCI 콘솔 → 인스턴스의 VCN → **Security Lists**(또는 NSG) → **Ingress Rules 추가**:

| Source CIDR | Protocol | Dest Port | 용도 |
|-------------|----------|-----------|------|
| `0.0.0.0/0` | TCP | **80** | HTTP(→HTTPS 리다이렉트·ACME 챌린지) |
| `0.0.0.0/0` | TCP | **443** | HTTPS |
| `<내 IP>/32` | TCP | **22** | SSH (전체 공개 금지 — 본인 IP로 제한) |

> **SSH 하드닝(적용됨)**: 22번은 `0.0.0.0/0`이 아니라 **본인 공인 IP `/32`로만** 개방한다(`curl -s ifconfig.me`로 확인). 심층 방어로 VM의 `/etc/ssh/sshd_config`에서 `PasswordAuthentication no`(키 전용 로그인)를 확인한다.
>
> ⚠️ **유동 IP 주의**: 가정용 회선은 공인 IP가 바뀔 수 있다. IP 변경으로 SSH가 막히면 이 Ingress 규칙의 Source를 새 IP로 다시 수정한다. IP가 자주 바뀌어 번거로우면 22를 열어두되 **키 전용 인증 + `fail2ban`** 으로 대체 방어한다.
>
> **CI/CD 배포는 공개 22를 쓰지 않는다(Phase 19)**: GitHub Actions runner가 **Tailscale**로 프라이빗 tailnet에 임시 접속한 뒤 VM의 tailnet 주소로 SSH한다(`tag:ci → tag:server:22`, VM iptables는 `tailscale0`의 22만 허용). 따라서 위 공개 22 `/32`는 이제 **운영자 비상 복구용**이다. ⚠️ **아직 닫지 말 것** — Tailscale/tailnet 장애 시 VM에서 잠기지 않도록, Windows Tailscale 클라이언트로 운영자 SSH가 실제 되는지 검증한 뒤 폐쇄하는 것을 **후속 보안 작업**으로 §10에 남긴다.

### (b) VM 내부 iptables

Oracle Ubuntu 이미지는 기본 iptables가 80/443을 **차단**한다(끝의 `REJECT`). 직접 열고 영속화:

```bash
sudo iptables -I INPUT 6 -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save     # iptables-persistent 패키지 필요 시: sudo apt -y install iptables-persistent
```

> ⚠️ **규칙을 볼 땐 반드시 `-v`를 붙여라.** `iptables -L`은 **`in` 인터페이스 컬럼을 출력하지 않는다.**
> 그래서 `-i lo`(루프백 전용)가 `ACCEPT all -- 0.0.0.0/0 0.0.0.0/0`으로 보여 **"방화벽이 전부
> 열려 있다"고 오독**하게 된다. 실측(2026-07-16) 현재 INPUT은 이렇다:
>
> ```
> sudo iptables -L INPUT -n -v --line-numbers
> 1  ACCEPT tcp -- tailscale0  dpt:22    ← CI 배포용 tailnet SSH
> 5  ACCEPT all -- lo                    ← 루프백 (전체 허용 아님)
> 6  ACCEPT tcp -- *           dpt:22    ← 운영자 공개 SSH(OCI가 /32로 제한)
> 7  ACCEPT tcp -- *           dpt:80
> 8  ACCEPT tcp -- *           dpt:443
> 9  REJECT all -- *                     ← 실제로 작동 중
> ```

> 🔑 **Docker 공개 포트는 INPUT만으로 막을 수 없다.** Docker는 `nat/PREROUTING`에서 DNAT로 목적지를
> 컨테이너 IP로 바꾸므로, 그 패킷은 **INPUT이 아니라 FORWARD**를 탄다. `iptables -I INPUT --dport
> <포트> -j DROP`을 넣고 "막았다"고 믿으면 **실제로는 열려 있는** 최악의 상태가 된다.
> 차단은 `DOCKER-USER` 체인에 넣어야 Docker 규칙보다 먼저 걸린다.
>
> 다만 위 7·8번 규칙의 패킷 카운터가 **0이 아니다**(148/89, 실측). Docker 기본값인
> `userland-proxy`가 호스트 포트를 직접 열어 일부 트래픽은 INPUT을 타기 때문으로 보인다. 즉
> **두 경로가 다 존재할 수 있으므로, 무언가를 확실히 막으려면 `DOCKER-USER`와 `INPUT` 양쪽에 넣는다**
> (§4(c)가 그렇게 한다).

### (c) 8081 — tailnet 전용 관리자 포트 ⚠️

관리자 화면(`/admin`)은 **공개 인터넷에 존재하지 않는다**(§4(d)). Caddy가 `:8081`에 따로 열어주고,
거기 닿을 수 있는 건 tailnet뿐이다. **3겹으로 막는다:**

| 층 | 조치 | 역할 |
|---|---|---|
| **OCI 보안목록** | 8081 Ingress 규칙을 **만들지 않는다** | 🔑 **실질 차단** — 클라우드 계층이라 패킷이 VM에 닿지도 않는다 |
| VM `DOCKER-USER` | tailscale0 외 8081 DROP | DNAT/FORWARD 경로 방어 |
| VM `INPUT` | tailscale0 외 8081 DROP | userland-proxy 경로 방어 |

```bash
sudo iptables -I DOCKER-USER 1 ! -i tailscale0 -p tcp --dport 8081 -j DROP
sudo iptables -I INPUT 7      ! -i tailscale0 -p tcp --dport 8081 -j DROP
sudo netfilter-persistent save
```

> **22번 규칙(1·6번)은 건드리지 않는다** — 비상 복구 경로다. 잠기면 VM에 못 들어간다.

### (d) 관리자 접속 — tailnet에서만

```
http://<VM_TAILNET_IP>:8081/admin      # VM의 tailnet IP (sudo tailscale ip -4)
```

노트북에서 Tailscale을 켠 상태여야 한다. 공개 도메인의 `/admin`·`/api/admin/*`은 **404**다.

**평문 HTTP인 게 맞다**: tailnet은 WireGuard가 이미 암호화한다. Let's Encrypt는 tailnet 이름으로
인증서를 못 주고, 자체 서명을 쓰면 브라우저 경고가 떠서 오히려 나쁘다.

**왜 이렇게 하나**: 구글 세이프 브라우징이 이 사이트를 **"방문자를 속여 개인정보를 노출하도록 유도함"**
(사회공학=피싱)으로 분류했고(2026-07-16 확인), 방아쇠는 **평판 없는 무료 DDNS 도메인 위의 공개된
비밀번호 폼**이었다. 크롤러가 `/admin`에서 404를 받으면 볼 폼이 없다. 덤으로 관리자 API가 공개에
없으니 시크릿을 추측당할 표면 자체가 사라진다.

> ⚠️ **이걸로 관리자 JS가 번들에서 빠지지는 않는다**(경로만 막는다). 그래도 목적은 달성한다 —
> 크롤러는 폼을 못 보고, `/api/admin/*`이 공개에서 404라 UI를 억지로 띄워도 전 동작이 실패한다.
> 번들에서 빼려면 빌드를 둘로 쪼개야 하는데 얻는 것 대비 과하다(코드는 이미 공개 저장소에 있다).

## 5. 앱 배치 + 시크릿 (`.env.prod`)

```bash
sudo mkdir -p /opt/lostark-price-tracker
sudo chown $USER:$USER /opt/lostark-price-tracker
git clone <이 저장소 URL> /opt/lostark-price-tracker
cd /opt/lostark-price-tracker

cp .env.prod.example .env.prod
```

`.env.prod`을 편집해 **강시크릿**을 채운다:

```bash
# 강시크릿 3개 생성
openssl rand -hex 32   # → POSTGRES_PASSWORD
openssl rand -hex 32   # → REDIS_PASSWORD
openssl rand -hex 32   # → ADMIN_API_SECRET (⚠️ dev의 123456789 절대 금지)
```

`.env.prod` 필수 값:
- `POSTGRES_PASSWORD` / `REDIS_PASSWORD` / `ADMIN_API_SECRET` = 위 openssl 생성값
- `LOSTARK_API_KEY` = 로스트아크 개발자 포털 JWT(재발급 가능)
- `SITE_ADDRESS` = `loaket.kr` (운영 대표 도메인)
- `ACME_EMAIL` = 본인 이메일(Let's Encrypt 계정)
- (모니터링 핑 URL — 선택이지만 권장) `COLLECTION_PING_URL`(§12) · `BACKUP_PING_URL`(§11.1⑤) — 시크릿. 비우면 감시만 꺼지고 서비스는 정상

> **재확인**: `.env.prod`은 `.gitignore`가 무시한다. `git status`에 나타나면 안 된다.

## 6. 스택 기동 (Caddy 인증서 자동 발급)

이미지는 CI(GitHub Actions)가 GHCR에 올린 것을 **pull**해 기동한다 — VM에서 소스 빌드하지 않는다(4GB ARM 부담 제거). 패키지가 **private**이므로 VM에서 **1회 GHCR 로그인**이 필요하다:

```bash
# read:packages 스코프 PAT로 1회 로그인(자격증명은 ~/.docker/config.json에 남아 이후 pull은 무인)
echo <GHCR_PAT> | docker login ghcr.io -u jongyeon2 --password-stdin

# 최신 이미지 pull + 기동 (VM 빌드 없음)
docker compose -f docker-compose.prod.yml --env-file .env.prod pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

- DNS 전파(수 분) 후, Caddy가 `SITE_ADDRESS`로 Let's Encrypt 인증서를 **자동 발급**한다.
- 발급 로그 확인:

```bash
docker compose -f docker-compose.prod.yml logs -f caddy
# "certificate obtained successfully" 류 로그가 뜨면 성공
```

> **오프라인 폴백**: GHCR에 접근할 수 없을 때만 compose의 `build:` 블록으로 로컬 빌드도 가능하다(`... up -d --build`, ARM에서 수 분~십수 분). 정상 운영·CI/CD 배포는 **pull 기반**이다.

## 7. 부팅 시 자동 기동 (systemd)

재부팅 후에도 스택이 자동으로 뜨도록:

```bash
sudo cp deploy/lostark.service /etc/systemd/system/lostark.service
sudo systemctl daemon-reload
sudo systemctl enable --now lostark
systemctl status lostark
```

(각 컨테이너의 `restart: unless-stopped`와 함께 이중으로 상시 가동 → `@Scheduled` 수집기가 24/7 구동.)

## 8. 라이브 검증 체크리스트

- [ ] `https://<도메인>` 접속 → 브라우저 **자물쇠(유효 인증서)** 표시
- [ ] `http://<도메인>` → **https로 리다이렉트**
- [ ] 대시보드 / 타임라인 / 이벤트영향 **3화면 렌더**
- [ ] `/timeline` 같은 딥링크 **새로고침해도 정상**(SPA fallback)
- [ ] 공개 도메인의 `/admin`·`/api/admin/events` → **404**(§4(d)) — 공개 인터넷엔 관리자가 없다
- [ ] **tailnet**에서 `http://<VM_TAILNET_IP>:8081/admin` → 로그인 화면 + **시크릿 로그인 동작**(강시크릿), 잘못된 시크릿은 401
- [ ] **VM 밖에서** `curl --max-time 5 http://<공인IP>:8081/` → **타임아웃/거부**(8081이 공개면 안 됨)
- [ ] **보안 응답 헤더 7종** 존재(아래 `curl`) + `Server` 헤더 제거됨
- [ ] 브라우저 콘솔 **CSP 위반 = 무해한 `eval` 1건뿐**(차트·아이콘·Select 정상 렌더)
- [ ] 수집 축적 확인:

```bash
docker compose -f docker-compose.prod.yml logs app | grep -i collection
# collection_run SUCCESS / 스냅샷 축적 확인 (첫 tick은 initial-delay 후)
```

### 보안 헤더 · CSP 검증

`frontend/Caddyfile`의 `header` 블록이 모든 응답에 다음 헤더를 실어야 한다:

| 헤더 | 값(요지) |
|------|----------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://cdn-lostark.game.onstove.com; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=(), payment=() …` (미사용 기능 차단) |
| `Cross-Origin-Opener-Policy` | `same-origin` |
| ~~`Server`~~ | **제거**(`-Server`) |

```bash
# 헤더 존재 확인 (7종 + Server 없음)
curl -sI https://<도메인> | grep -iE 'content-security-policy|permissions-policy|cross-origin-opener|strict-transport|x-content-type|x-frame|referrer-policy'
curl -sI https://<도메인> | grep -i '^server:' || echo "server 헤더 없음(OK)"
```

**CSP 근거(실측 검증됨)** — 이 정책은 라이브 자산과 대조해 도출했다:
- `style-src`에 `'unsafe-inline'` 은 **필수**다. Recharts가 차트 색·범례를 런타임 inline `style`로 주입하므로 빼면 차트가 깨진다.
- `img-src`에 `cdn-lostark.game.onstove.com` 은 **필수**다(아이템 아이콘 atlas + 배너 CDN).
- `script-src`에 `'unsafe-eval'` 은 **넣지 않는다**. 번들의 유일한 `eval`은 라이브러리 일회성 기능 프로브(무해)로, 차단해도 대시보드·타임라인·이벤트영향·Radix Select가 전부 정상 동작함을 확인했다. 그래서 프로덕션 콘솔에 `Refused to evaluate … 'unsafe-eval'` 경고 **1줄**이 남는데, 이는 **기능 영향 0**이며 하드닝이 작동한다는 신호다(넣으면 XSS 방어가 약해지므로 유지).
- `COEP require-corp` 는 **의도적으로 제외**한다(onstove CDN 이미지가 CORP 헤더를 안 줘서 넣으면 아이콘이 깨진다).

> 배포 후 **보안 게이트(18-05)** 의 라이브 항목(off-box 포트 스캔·401 등)과 **게이트 밖 하드닝**(위 CSP/보안 헤더 · SSH `/32` 제한)까지 통과해야 최종 go-live. (이 프로젝트는 quick `260713-e1o`에서 CSP·헤더를, §4 SSH 제한을 라이브에 적용·검증 완료.)

## 9. 운영 (일상 명령)

> **정상 배포는 자동이다** — `main`에 push하면 CI/CD가 빌드·GHCR push·VM 재배포·스모크까지 무인 처리한다(아래 **§10**). 아래 명령은 상태 점검·수동 개입용이다.

```bash
cd /opt/lostark-price-tracker

# 상태 / 로그
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app

# 수동 재배포(자동을 안 쓰거나 특정 태그로 띄울 때) — VM 빌드 없음, GHCR pull
IMAGE_TAG=sha-<커밋> docker compose -f docker-compose.prod.yml --env-file .env.prod pull
IMAGE_TAG=sha-<커밋> docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans

# DB 백업은 자동이다 — §11 참조. 수동으로 한 번 뜨려면:
scripts/backup-db.sh
```

## 10. 자동 배포 (CI/CD 파이프라인) · 운영 Runbook

`main` push 한 번으로 빌드→배포→검증이 무인 처리된다. **첫 실배포 성공: 2026-07-14, GitHub Actions run #30(커밋 `916bd3a`)** — `ghcr.io/jongyeon2/lostark-app:sha-916bd3a`·`lostark-web:sha-916bd3a`가 VM에 떠서 4컨테이너 Up(postgres·redis healthy), 공개 HTTPS `/actuator/health`={"status":"UP"} 확인.

**파이프라인 흐름** (`.github/workflows/ci.yml`):

```
main push
 → backend  (./gradlew build — Testcontainers)      ┐ 게이트: 실패 시 배포 안 됨
 → frontend (npm ci && npm run build, tsc)           ┘
 → images   (arm64 app·web 빌드 → GHCR push: sha-<커밋> + latest)
 → deploy   (DEPLOY_ENABLED=true 일 때):
      Tailscale 접속(tag:ci) → VM SSH → compose scp
      → IMAGE_TAG=sha-<커밋> docker compose pull && up -d --remove-orphans
      → 공개 HTTPS 스모크(/actuator/health=UP · /api/health/collection=200)
```

- **접근**: runner는 공개 SSH(22)가 아니라 **Tailscale tailnet 경유**로 VM에 SSH한다. 앱/레지스트리 시크릿은 CI에 넣지 않는다 — VM이 1회 `docker login ghcr.io`로 private 이미지를 pull한다.
- **불변 이미지**: `sha-<커밋>` 태그로 배포·롤백. `latest`도 함께 갱신(부팅 시 systemd가 사용).

### 10.1 정상 배포

특별한 조작 없이 `main`에 push(또는 PR 머지)하면 위 흐름이 자동 실행된다. GitHub → **Actions** 탭에서 진행/성공을 본다. deploy job이 초록이면 스모크까지 통과한 것.

### 10.2 배포 상태 확인

```bash
# (GitHub) Actions 탭 → 최신 run → deploy job 초록 여부 + Smoke test 단계 로그

# (VM) 무엇이 떠 있나 — 컨테이너 + 실제 이미지 태그
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml images        # app/web 이미지 태그(sha-<커밋>) 확인
docker inspect --format '{{.Config.Image}}' $(docker compose -f docker-compose.prod.yml ps -q app)

# (외부) 공개 HTTPS 헬스
curl -fsS https://loaket.kr/actuator/health          # {"status":"UP"}
curl -fsS https://loaket.kr/api/health/collection    # 200 + 수집 상태
```

### 10.3 컨테이너 로그

```bash
docker compose -f docker-compose.prod.yml logs -f app        # 앱(수집·Flyway·에러)
docker compose -f docker-compose.prod.yml logs -f caddy      # TLS·프록시
docker compose -f docker-compose.prod.yml logs app | grep -i collection   # 수집 축적
```

### 10.4 롤백 (특정 sha 이미지로)

배포가 나쁜 커밋을 올렸을 때, **이전 정상 커밋의 sha 태그**로 즉시 되돌린다:

```bash
# 이전 정상 커밋 짧은 SHA(7자) — git log 또는 GHCR 패키지 태그 목록에서 확인
cd /opt/lostark-price-tracker
IMAGE_TAG=sha-<이전정상커밋> docker compose -f docker-compose.prod.yml --env-file .env.prod pull
IMAGE_TAG=sha-<이전정상커밋> docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans
curl -fsS https://loaket.kr/actuator/health   # {"status":"UP"} 확인
```

- 위 수동 롤백은 **일시적**이다 — 다음 `main` 배포나 재부팅(systemd `:latest`)이 다시 최신을 띄운다.
- **지속 롤백**은 둘 중 하나: (a) 문제 커밋을 `git revert`해서 `main`에 push → CI가 깨끗한 forward 배포로 되돌림(권장), 또는 (b) `.env.prod`에 `IMAGE_TAG=sha-<이전정상커밋>`을 고정.

### 10.5 배포 실패 진단 순서

GitHub → Actions → 실패한 run에서 **어느 job/step**이 붉은지부터 본다:

1. **backend / frontend 실패** → 테스트·빌드 문제. 배포 전 게이트라 **VM 영향 0**(라이브는 이전 버전 유지). 로그 보고 코드 수정 후 재푸시.
2. **images 실패** → 이미지 빌드/GHCR push. Dockerfile 또는 `packages: write` 권한 확인.
3. **deploy 실패** → 단계별로:
   - `Connect to Tailscale` → **§10.7 Tailscale 대응**.
   - `Copy compose to VM` / `Pull image & restart` 의 SSH 실패(`Permission denied`/`timeout`) → 키(`SSH_PRIVATE_KEY`)·`VM_HOST`(tailnet IP)·VM `authorized_keys`·iptables 확인(**§10.8 SSH**).
   - `Pull image & restart` 의 pull 실패(`unauthorized`/`denied`/`manifest unknown`) → **§10.6 GHCR 인증**.
   - `Smoke test` 실패 → 이미지는 떴으나 앱이 안 뜬 것. VM에서 `docker compose logs app`(Flyway·DB 연결·포트) 확인. 회복 안 되면 **§10.4 롤백**.

### 10.6 GHCR 인증 만료/실패 대응

- **증상**: deploy의 pull 단계에서 `unauthorized` / `denied` / `manifest unknown`.
- **원인**: VM의 `docker login` PAT 만료·삭제, 또는 `~/.docker/config.json` 손상.
- **대응** (VM에서):

```bash
echo <새_PAT> | docker login ghcr.io -u jongyeon2 --password-stdin   # read:packages 스코프
docker compose -f docker-compose.prod.yml pull                       # 재확인
```

  classic PAT 만료 시 GitHub에서 재발급(read:packages). PAT는 **VM에만** 두고 CI/코드/문서에 넣지 않는다.

### 10.7 Tailscale 연결 실패 대응

- **증상**: deploy `Connect to Tailscale` step 실패, 또는 이후 SSH가 `VM_HOST`(tailnet IP)에 timeout.
- **점검**:

```bash
# (VM) tailnet 연결·온라인 여부
sudo tailscale status
sudo tailscale ip -4        # 이 값이 GitHub secret VM_HOST와 일치하는지
```

- **대응**: VM에서 `sudo tailscale up` 재연결. CI용 OAuth client가 만료/삭제됐으면 재발급 후 secrets `TS_OAUTH_CLIENT_ID`/`TS_OAUTH_SECRET` 갱신. ACL에 `tag:ci → tag:server:22` 규칙, VM iptables에 `tailscale0` 22 ACCEPT 규칙이 남아 있는지 확인.

### 10.8 SSH 복구 경로 주의사항

- CI 배포 SSH는 **Tailscale tailnet 경유**(공개 22 아님). 운영자 수동 접속 경로는 두 갈래: **(a)** 공개 22 `/32`(집 공인 IP — **비상 복구용, 현재 유지**), **(b)** Tailscale 경유.
- ⚠️ **공개 22를 완전히 닫기 전에** Tailscale 경유 운영자 SSH가 실제로 되는지 반드시 먼저 확인한다. 안 그러면 tailnet 장애 시 VM에서 잠긴다.
- iptables에 `tailscale0`의 22 ACCEPT와 공개 `/32` 규칙이 **둘 다** 있어야 이중 경로가 유지된다.

### 10.9 후속 보안 작업 (아직 미실행)

- [ ] **공개 SSH 22 폐쇄** — OCI Ingress에서 22 `/32` 규칙 제거. **선행 조건**: Windows Tailscale 클라이언트로 운영자 SSH 접속이 실제로 되는지 검증 완료. (검증 전엔 비상 복구 경로가 사라지므로 닫지 않는다.)

## 11. DB 백업 · 복원 ⚠️ (되돌릴 수 없는 유일한 자산)

사이트가 죽는 건 재배포로 몇 분이면 복구된다. **PG의 시계열은 아니다.** 보석 시세는 경매장 이력
API가 아예 없어 소실되면 영구히 못 만들고, 가격은 상세 통계로 ~2주가 한계이며, 이벤트·쿠폰은
관리자가 손으로 넣은 것이다. 그래서 백업은 **PostgreSQL 하나만** 지킨다 — Redis는 캐시라, Caddy
인증서는 Let's Encrypt가 재발급하므로 대상이 아니다.

| | |
|---|---|
| 스크립트 | `scripts/backup-db.sh` (cron이 하루 1회 · **03:17 UTC = 12:17 KST**, 서버는 `Etc/UTC`) |
| 방식 | `pg_dump -Fc` — **무중단**(PG는 MVCC라 수집 중에도 일관된 스냅샷) |
| 목적지 | 버킷 `lostark-backup` · Japan Central (Osaka) · Standard · Private (Always Free 20GB) |
| 객체명 | `db/YYYY-MM-DD.dump` (UTC) |
| 인증 | **쓰기 전용 버킷 PAR** — VM에 OCI CLI·API 키 없음 |
| 보관 | **서버측 수명주기** — 스크립트엔 삭제 로직이 없다(권한 자체가 없음). 실수명은 최대 60일(§11.1 ②) |
| 감시 | healthchecks.io Check `lostark-db-backup-prod` (Period 1 day / Grace 1 hour) |
| RPO | **24시간** — 최악의 경우 하루치가 영구 소실된다 |

> **가동 실적**: 첫 실제 백업 성공 **2026-07-16** — `db/2026-07-16.dump` 약 **345.88 KiB** 업로드,
> 별도 컨테이너 복원 + 행 수 대조까지 통과(§11.2). 이후 cron이 매일 자동 실행한다.

### 11.1 최초 설정 (1회)

> 아래는 2026-07-16에 실제로 구축한 순서다. 재구축 시 그대로 따르면 된다.

**① 버킷** — OCI 콘솔 → Object Storage → Create Bucket
- 이름 `lostark-backup` / Standard / **Private** / Encryption: Oracle-managed key
- **Object Versioning: Enabled** ⚠️ 쓰기 전용 PAR도 *덮어쓰기*는 되므로, 버저닝이 없으면 기존
  백업을 쓰레기로 덮을 수 있다. 켜두면 이전 버전이 남는다.

**② 수명주기 규칙 — 반드시 2개** ⚠️ 버킷 → Lifecycle Policy Rules

버저닝을 켰으므로 규칙이 **하나로는 부족하다**. "최신 버전 삭제"가 돌면 객체가 사라지는 게 아니라
**이전 버전으로 내려앉기** 때문에, 이전 버전을 치우는 규칙이 없으면 계속 쌓인다.

| 이름 | Target | Action | Days | Prefix |
|---|---|---|---|---|
| `delete-db-latest-30d` | **Latest Version of Objects** | Delete | 30 | `db/` |
| `delete-db-previous-30d` | **Previous Versions of Objects** | Delete | 30 | `db/` |

> **그래서 데이터 실수명은 30일이 아니라 최대 60일이다** — 30일 뒤 이전 버전으로 내려가고, 다시
> 30일 뒤 소멸한다. **60일 전 백업이 콘솔에 보여도 정상이다.** 용량은 346 KiB/일 × 60 ≈ **21 MB**로
> 20 GB 한도에는 무의미한 수준이니 그대로 둔다.
>
> 참고로 두 번째 규칙을 빠뜨려도 당장 터지진 않는다(연 ~126 MB). 다만 **상한이 없는 증가**라
> 방치할 이유도 없다.

삭제를 **서버가** 한다. VM이 털려도 백업을 지울 수 없는 이유가 이것이다.

**③ 수명주기 실행용 IAM 정책** ⚠️ 이게 없으면 규칙이 조용히 안 돈다

수명주기는 Object Storage **서비스**가 대신 실행한다. 그 서비스에 삭제 권한을 주지 않으면 규칙은
콘솔에 **Enabled로 멀쩡히 보이는데 아무 일도 일어나지 않는다** — 실패 로그도, 알림도 없다.
객체가 30일이 지나도 안 사라지면 여기부터 의심한다.

Identity → Policies → Create Policy (**테넌시 루트 컴파트먼트**에 생성):

```
Allow service objectstorage-<리전> to manage object-family in compartment <컴파트먼트명>
```

- `<리전>`은 버킷 리전의 서비스 이름(예: Osaka → `ap-osaka-1`). 콘솔 정책 빌더가 후보를 보여준다.

**④ 쓰기 전용 PAR** — 버킷 → Pre-Authenticated Requests → Create
- Type: **Bucket** / Access: **Permit object writes** (읽기·목록 조회 **주지 말 것**)
- Object listing: **Disabled**
- 생성 직후 뜨는 URL을 **그때 복사**한다(다시 못 본다). `.../o/`로 끝나야 한다.

> ⚠️ **현재 PAR 만료일: 2028-07-16.** 만료되면 백업이 멈춘다(§11.4).
> **재발급 알림: 2028-06-16**(만료 한 달 전) — 달력에 등록해 둘 것.
> 재발급 후 `.env.prod`의 `BACKUP_PAR_URL`만 교체하면 된다. **재시작·재배포 불필요** —
> 스크립트가 매 실행마다 파일을 새로 읽는다.

**⑤ 데드맨 스위치** — [healthchecks.io](https://healthchecks.io) 가입(무료) → Check 생성
- 이름 `lostark-db-backup-prod` / Schedule **Simple** / Period **1 day** / Grace **1 hour**
  → 25시간 무신호면 알림(**Discord 연동** — §12에서 collection 체크와 함께 설정)
- Ping URL 복사

**⑥ `.env.prod`에 추가** (VM에서만, 커밋 금지)

```bash
BACKUP_PAR_URL=https://objectstorage.<리전>.oraclecloud.com/p/<토큰>/n/<네임스페이스>/b/lostark-backup/o/
BACKUP_PING_URL=https://hc-ping.com/<uuid>
```

**⑦ cron 설치** ⚠️ Oracle Ubuntu 이미지엔 cron이 **기본으로 없다**

```bash
sudo apt install -y cron
sudo systemctl enable --now cron
systemctl is-active cron     # → active
command -v crontab           # → /usr/bin/crontab
```

**⑧ 첫 실행 + cron 등록**

```bash
cd /opt/lostark-price-tracker && ./scripts/backup-db.sh   # 수동 1회 — 성공 확인 후 등록
( crontab -l 2>/dev/null; echo '17 3 * * * cd /opt/lostark-price-tracker && ./scripts/backup-db.sh >> $HOME/backup.log 2>&1' ) | crontab -
```

> 서버가 `Etc/UTC`이므로 **03:17 UTC = 12:17 KST**에 돈다.
> 등록 후 cron과 **똑같은 명령**을 한 번 수동 실행해 로그 리다이렉션까지 확인할 것:
> `./scripts/backup-db.sh >> "$HOME/backup.log" 2>&1 && tail -n 100 ~/backup.log`

### 11.2 🔑 복원 — 일회용 컨테이너로 먼저 검증

**복원해본 적 없는 백업은 백업이 아니라 희망이다.** 운영에 바로 밀어넣기 전에 반드시 여기서 확인한다.

⚠️ **PAR로는 다운로드가 안 된다.** 쓰기 전용이라 GET 권한이 없다 — 이건 설계된 대가다.
**OCI 콘솔 → 버킷 → 객체 → Download**로 받는다. (급하면 읽기 PAR을 따로 발급하고 **쓰고 나서
지운다**.)

```bash
# 1) 일회용 PG16 기동 (운영 DB와 완전히 별개)
docker run --rm -d --name pg-verify \
  -e POSTGRES_PASSWORD=verify -e POSTGRES_USER=verifyuser -e POSTGRES_DB=verifydb postgres:16

# 2) 덤프를 넣고 복원 — --exit-on-error 필수(아래 경고 참조)
docker cp ~/db_2026-07-16.dump pg-verify:/tmp/r.dump
docker exec pg-verify pg_restore -U verifyuser -d verifydb \
  --no-owner --no-privileges --exit-on-error /tmp/r.dump
echo "종료 코드: $?"    # 0이어야 한다

# 3) 행 수 확인 — 추정치(n_live_tup)가 아니라 실제 COUNT(*)로 본다
docker exec pg-verify psql -U verifyuser -d verifydb -c "
SELECT 'price_snapshot', COUNT(*) FROM price_snapshot UNION ALL
SELECT 'gem_price_snapshot', COUNT(*) FROM gem_price_snapshot UNION ALL
SELECT 'item_daily_stats', COUNT(*) FROM item_daily_stats UNION ALL
SELECT 'tracked_item', COUNT(*) FROM tracked_item UNION ALL
SELECT 'game_event', COUNT(*) FROM game_event UNION ALL
SELECT 'coupon', COUNT(*) FROM coupon;"

# 4) 정리
docker stop pg-verify
rm -f ~/db_2026-07-16.dump
```

> ⚠️ **`--exit-on-error`를 빼지 마라.** `pg_restore`는 기본적으로 에러를 만나도 **계속 진행하고**,
> 끝에 `errors ignored on restore: N`만 찍은 뒤 **종료 코드 0으로 끝날 수 있다.** 종료 코드만 보고
> "복원 성공"이라 판단하면 **일부만 복원된 DB를 통과시킨다** — 검증의 의미가 사라진다.
> 이 옵션은 첫 에러에서 멈춘다.

> 🔑 **행 수가 조금 다른 건 정상이다 — 백업이 깨진 게 아니다.** 덤프를 뜬 뒤에도 수집기가 계속
> 돌기 때문에, **`price_snapshot`과 `collection_run`은 "지금" 운영 DB보다 적게 나온다.**
> 실제 2026-07-16 검증 결과가 그랬다:
>
> | 테이블 | 복원본 | 그때의 운영 | 판정 |
> |---|---|---|---|
> | `price_snapshot` | 22,946 | 23,044 | ⬆️ 정상 — 계속 수집 중 |
> | `collection_run` | 806 | 808 | ⬆️ 정상 — 계속 수집 중 |
> | `item_daily_stats` | 1,107 | 1,107 | ✅ 일치 |
> | `gem_price_snapshot` | 90 | 90 | ✅ 일치 |
> | `tracked_item` | 49 | 49 | ✅ 일치 |
> | `flyway_schema_history` | 9 | 9 | ✅ 일치 |
> | `game_event` | 2 | 2 | ✅ 일치 |
> | `coupon` | 1 | 1 | ✅ 일치 |
>
> 즉 **10분마다 추가되는 두 테이블은 늘어나는 게 맞고, 나머지 6개가 어긋나면 그때 의심한다.**
> 정확히 대조하고 싶으면 덤프 시각으로 잘라서 세라 — `WHERE collected_at <= '<덤프 시각>'`.

### 11.3 복원 — 운영에 적용 ☠️ 파괴적

**`--clean`은 기존 객체를 DROP한다. 되돌릴 수 없다.** 11.2를 통과한 덤프에만, 정말 필요할 때만.

```bash
cd /opt/lostark-price-tracker
docker compose -f docker-compose.prod.yml --env-file .env.prod stop app   # 쓰기 멈춤
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
  sh -c 'cat > /tmp/r.dump' < ~/db_2026-07-16.dump
docker compose -f docker-compose.prod.yml --env-file .env.prod exec postgres \
  pg_restore -U lostark -d lostark --clean --if-exists --no-owner --exit-on-error /tmp/r.dump
docker compose -f docker-compose.prod.yml --env-file .env.prod start app
```

> 덤프에는 `flyway_schema_history`도 들어 있으므로 스키마와 데이터가 한 시점으로 일관되게 돌아온다.
> 복원 후 앱이 뜨면서 Flyway가 `validate`로 확인한다.

### 11.4 백업이 멈췄을 때

**증상**: healthchecks.io → **Discord**로 "no ping"(DOWN) 알림. 또는 `~/backup.log`에 `ERROR`. (백업 실패 시 `die()`가 `/fail`을 즉시 쏘므로 25시간을 기다리지 않고 바로 온다 — §12.)

`backup-db.sh`는 **성공했을 때만** ping을 보낸다. 어느 단계든 실패하면 `set -e`가 즉시 끊어
ping에 도달하지 못한다 — 즉 **알림이 왔다는 건 백업이 실제로 안 됐다는 뜻**이다(원인은 몰라도).

```bash
cd /opt/lostark-price-tracker && ./scripts/backup-db.sh   # 직접 돌려 에러 메시지를 본다
```

| 메시지 | 원인 |
|---|---|
| `업로드 실패 (HTTP 404)` / `(HTTP 401)` | **PAR 만료·취소가 가장 흔하다**(만료 2028-07-16) → §11.1 ④로 재발급 후 `.env.prod`의 `BACKUP_PAR_URL` 교체. 재시작 불필요 |
| `덤프가 유효하지 않다` | 디스크 참(`df -h`) 또는 PG 이상. **이 경우 업로드하지 않는다** — 깨진 덤프가 정상 백업을 덮는 걸 막는다 |
| `BACKUP_PAR_URL이 비어 있다` | `.env.prod` 항목 누락 |
| `환경파일 없음` | 경로 문제. cron이 `cd /opt/lostark-price-tracker`를 하는지 확인(`crontab -l`) |
| 로그가 아예 없음 | cron 자체가 안 돈다 → `systemctl is-active cron`(§11.1 ⑦). 이미지에 cron이 없어 설치가 필요했던 전례가 있다 |

> **객체가 30일이 지나도 안 사라진다면** 백업 실패가 아니라 수명주기 문제다 — §11.1 ③의 IAM 정책이
> 없으면 규칙이 Enabled로 보여도 실행되지 않는다. 그리고 §11.1 ②대로 실수명은 **최대 60일**이다.

> ⚠️ **ping은 성공했는데 알림이 온다면** healthchecks 쪽 장애다(스크립트는 `WARN: ... ping 전송
> 실패`를 남긴다). 백업 자체는 됐다 — 오탐이다. 거짓 경보가 거짓 침묵보다 낫기에 이렇게 뒀다.

## 12. 능동 모니터링 · 알림 (사이트 · 수집 · 백업 → Discord)

혼자 운영하는 무료 VM이 **"죽어도 모르는"** 상태를 없앤다. 세 신호를 각각 다른 도구로 잡아 **Discord로 능동 통지**한다. 설계 근거·기각안은 `docs/specs/2026-07-17-monitoring-alerting-design.md`, 코드 구현은 quick-260718-jrz 참조.

| 신호 | 잡는 것 | 도구 | 주기 / 유예 |
|---|---|---|---|
| 사이트 · TLS | Caddy·VM·앱·DB 다운, 인증서 만료 | UptimeRobot(외부 프로브) | 5분 |
| 수집 | 스케줄러 정지, 전량 실패(키 만료 등) | healthchecks.io 데드맨 + 즉시 `/fail` | Period 10분 / Grace 5분 |
| 백업 | 덤프·업로드 실패, PAR 만료 | healthchecks.io 데드맨 + 즉시 `/fail`(§11) | Period 1일 / Grace 1시간 |

> 외부 프로브(빠름·독립)와 데드맨(느림·파이프라인 내부)의 겹침은 의도적이다 — 하나가 죽어도 다른 하나가 남는다.

### 12.1 시크릿 취급 — 어디에 무엇을 두나 ⚠️

| 값 | 보관 위치 | 비고 |
|---|---|---|
| `COLLECTION_PING_URL` · `BACKUP_PING_URL` (hc-ping) | **VM `.env.prod`만** | 앱/스크립트가 핑 쏠 대상. 커밋 금지 |
| **Discord 웹훅 URL** | **healthchecks·UptimeRobot 대시보드만** | 코드·`.env.prod`·깃 어디에도 두지 않는다 |

핑 URL을 아는 자는 "정상"을 위조해 알림을 영구 침묵시킬 수 있다 → 앱은 이 URL을 로그에도 남기지 않는다(예외 클래스명만 로깅). `.env.prod`은 `chmod 600` 유지.

### 12.2 수집 하트비트 (healthchecks.io)

`PriceCollector`가 매 틱(10분) 결과를 데드맨에 핑한다 — **성공 아이템 수(`succeeded`)로 판단**: `>0`이면 기본 URL, `==0`(빈 워치리스트·키 만료 등 전량 실패)이면 `/fail`로 즉시 알림. 신호가 끊기면(스케줄러 사망) 15분 후 데드맨이 운다. `COLLECTION_PING_URL`이 비면 앱은 조용히 감시만 끄고 수집은 정상(fail-open).

**체크 생성** — healthchecks.io → Check
- 이름 `lostark-collection-prod` / Schedule **Simple** / Period **10 minutes** / Grace **5 minutes**
- Discord Integration 활성화 / Ping URL 복사 → `.env.prod`

**`.env.prod`에 추가** (VM에서만, 커밋 금지)

```bash
COLLECTION_PING_URL=https://hc-ping.com/<uuid>
```

**compose 배선** — `docker-compose.prod.yml`의 `app.environment`에 이미 있다:

```yaml
COLLECTION_PING_URL: ${COLLECTION_PING_URL:-}
```

> `COLLECTION_PING_URL`은 compose를 통해 **app 컨테이너**로 주입된다. 반면 `BACKUP_PING_URL`은 **호스트의 백업 스크립트**가 `.env.prod`를 직접 읽으므로 app 컨테이너엔 전달하지 않는다(§11) — 정상 구조다.

**app만 안전하게 재생성** (핑 URL 추가·변경 후 반영. postgres·redis는 안 건드림):

```bash
cd /opt/lostark-price-tracker
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps --force-recreate app
```

> 운영 compose 명령은 **항상 `--env-file .env.prod`와 `-f docker-compose.prod.yml`을 함께** 쓴다. `--no-deps`가 의존 서비스 동반 재생성을 막고, `--force-recreate`가 새 env를 확실히 반영한다. 재생성 후 컨테이너 안에 값이 들어갔는지 확인: `docker compose ... exec app printenv COLLECTION_PING_URL`(값 자체를 남에게 보이는 화면에 띄우지 말 것).

### 12.3 사이트 · TLS (UptimeRobot)

Spring은 DB·Redis 컴포넌트가 죽으면 `/actuator/health`에서 **HTTP 503**을 내므로, 상태코드 감시만으로 Caddy·TLS·앱·DB·Redis를 한 번에 덮는다(`show-details: never`라 본문은 `{"status":"UP"}`만).

- Monitor type: **HTTP(s)** / URL `https://loaket.kr/actuator/health` / Interval **5분**
- Tags: `loaket`, `production`, `health`
- Alert Contact: **Discord**(웹훅 URL을 UptimeRobot 대시보드에 붙여넣기 — 저장 시 Test Notification으로 채널 도착 확인)

### 12.4 라이브 검증 실적 (2026-07-18~19 완료)

**알림이 실제로 오는 걸 눈으로 확인**해야 감시가 검증된 것이다(초록불만으론 거짓 안심).

- ✅ 수집 데드맨: `lostark-collection-prod`에 `/fail` → Discord **DOWN**, 정상 핑 → Discord **UP** 복구
- ✅ 백업 데드맨: `lostark-db-backup-prod`에 `/fail` → Discord **DOWN**, 정상 핑 → Discord **UP** 복구
- ✅ 사이트: UptimeRobot 모니터 URL을 `/actuator/nope`로 잠시 변경 → HTTP 404 감지·Discord **DOWN**, `/actuator/health`로 복구 → Discord **UP**
- ✅ 배포 후 healthchecks에서 Java 앱의 실제 collection 핑 수신 확인, `https://loaket.kr/actuator/health` = `{"status":"UP"}`
- ✅ 백업 cron 자율 실행 실증(2026-07-19): `journalctl -u cron`에서 **07-17·18·19** 03:17 UTC 자동 실행, `backup.log`에 **07-16~19 4일 연속 업로드 성공(HTTP 200)**, `lostark-db-backup-prod` 매일 12:17 KST OK·현재 UP — cron→pg_dump→유효성검사→Object Storage 업로드→200→정상 핑 전 구간 검증
- 테스트 종료 후 세 체크 모두 **UP**

### 12.5 앱 로그에 핑 URL이 노출되지 않는지 검사 ⚠️

**반드시 `-c`(개수만)로** — 기본 `grep`은 매칭 라인을 출력해 URL이 화면에 찍힐 수 있다:

```bash
docker logs lostark-price-tracker-app-1 2>&1 | grep -c -E 'hc-ping\.com|hchk\.io'   # 기대값 0
docker logs lostark-price-tracker-app-1 2>&1 | grep -c -i 'heartbeat'               # 있어도 예외 클래스명 경고뿐(URL 없음)
```

저장소 문서에 실 URL이 없는지도 확인(플레이스홀더 `<uuid>` 외 0건이어야):

```bash
git grep -nI -e 'hc-ping.com' -e 'hchk.io' -e 'discord.com/api/webhooks' -- docs
```
