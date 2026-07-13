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

## 3. DuckDNS 도메인 연결

1. [duckdns.org](https://www.duckdns.org) 로그인(GitHub/Google 등).
2. 원하는 서브도메인 생성(예: `lostark-tracker`) → 도메인은 `lostark-tracker.duckdns.org`.
3. **current ip** 칸에 VM의 Public IP를 넣고 **update** → 저장. **token** 을 기록.
4. (선택) IP가 바뀌어도 유지되도록 갱신 크론:

```bash
# <TOKEN>, <SUBDOMAIN> 치환
( crontab -l 2>/dev/null; echo '*/5 * * * * curl -s "https://www.duckdns.org/update?domains=<SUBDOMAIN>&token=<TOKEN>&ip=" >/dev/null' ) | crontab -
```

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

### (b) VM 내부 iptables

Oracle Ubuntu 이미지는 기본 iptables가 80/443을 **차단**한다. 직접 열고 영속화:

```bash
sudo iptables -I INPUT 6 -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save     # iptables-persistent 패키지 필요 시: sudo apt -y install iptables-persistent
```

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
- `SITE_ADDRESS` = `lostark-tracker.duckdns.org` (본인 서브도메인)
- `ACME_EMAIL` = 본인 이메일(Let's Encrypt 계정)

> **재확인**: `.env.prod`은 `.gitignore`가 무시한다. `git status`에 나타나면 안 된다.

## 6. 스택 기동 (Caddy 인증서 자동 발급)

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- DuckDNS DNS 전파(수 분) 후, Caddy가 `SITE_ADDRESS`로 Let's Encrypt 인증서를 **자동 발급**한다.
- 발급 로그 확인:

```bash
docker compose -f docker-compose.prod.yml logs -f caddy
# "certificate obtained successfully" 류 로그가 뜨면 성공
```

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
- [ ] 관리자 콘솔에서 **시크릿 로그인 동작**(강시크릿), 잘못된 시크릿은 401
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

## 9. 운영

```bash
# 업데이트 배포
cd /opt/lostark-price-tracker && git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# 로그 / 상태
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app

# (선택) DB 백업
docker compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U lostark lostark > backup_$(date +%F).sql
```
