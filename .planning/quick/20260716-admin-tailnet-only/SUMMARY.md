---
task: /admin을 tailnet 전용으로 전환
quick_id: 260716-k35
status: complete
date: 2026-07-16
---

# SUMMARY — /admin tailnet 전용 (피싱 방아쇠 제거)

앱 코드 **0줄** · 마이그레이션 **0** · 프론트 재빌드 불필요.

## 왜

구글 세이프 브라우징이 `lostark-tracker.duckdns.org`를 **"방문자를 속여 개인정보를 노출하도록
유도함"**(사회공학=피싱)으로 분류했다(투명성 보고서 실조회, 2026-07-16). 방아쇠는 **평판 없는
무료 DDNS 도메인 위의 공개된 비밀번호 폼**(`/admin`). 폼 자체는 죄가 없다 — 정식 도메인이었으면
아마 안 걸렸다. duckdns가 채점을 나쁘게 만든 것이다.

**도메인 이전 전에 이걸 먼저 없앤다.** 안 그러면 같은 방아쇠를 들고 새 도메인으로 이사하는 셈이다.
지금 duckdns는 이미 플래그돼 있어 **잃을 게 없고**, 문제가 나도 새 도메인이 아니라 여기서 난다.

## 산출물

| 파일 | 변경 |
|---|---|
| `frontend/Caddyfile` | `(site)` 스니펫 추출 + 공개 블록에 관리자 404 4줄 + `http://:8081` 리스너 |
| `docker-compose.prod.yml` | `caddy.ports`에 `"8081:8081"` |
| `docs/deploy/oracle-vm-runbook.md` | §4(b) **iptables 오독 경고 + Docker 경로 정정**, §4(c) 3겹 방화벽, §4(d) 관리자 접속, §8 체크리스트 갱신 |

## 🔑 계획대로 안 된 것 — `handle`은 경로를 하나만 받는다

`handle /admin /admin/*`로 썼는데 **Caddy가 파싱 자체를 거부**했다
(`wrong argument count ... after '/admin/*'`). 경로마다 `handle`을 하나씩 쓰도록 고쳤다.

이름 있는 matcher(`@admin path /admin /admin/*`)로 묶을 수도 있었지만 **일부러 안 썼다** —
Caddy는 `handle`을 **경로 문자열 길이**로 정렬해 구체적인 것을 먼저 태우는데, 경로를 직접 적어야
그 정렬이 작동한다. 아래가 그 증명이다.

## 🔑 최대 위험이었던 가정을 실측으로 확정했다

**`/api/admin/*`이 `/api/*`보다 먼저 잡히는가** — 틀렸으면 관리자 API가 그대로 프록시돼
**막은 줄 알고 뚫려 있는** 최악의 상태가 된다. 목 백엔드(traefik/whoami) + 실제 Caddy 컨테이너로 확인:

**공개 리스너**

| 경로 | 결과 |
|---|---|
| `/admin` · `/admin/foo` | **404** (본문 0바이트) |
| `/api/admin/events` · `/api/admin/coupons` | **404 — 프록시 안 됨** ✅ |
| `/api/health/collection` · `/api/items` · `/actuator/health` | 200 프록시 |
| `/` · `/timeline` | 200 SPA |

**tailnet 리스너 :8081** — `/admin` 200 SPA · `/api/admin/events` 200 프록시 · `/` 200 SPA

**Caddy가 경로 구체성 순으로 정렬하는 게 증명됐다** — 가정이 아니라 실측이다.

부수 확인: 보안 헤더 **7종 유지**, `Server` 제거, 404 본문 **0바이트**(정보 미유출),
경계값 `/administrator`·`/adminx`·`/api/administrators`는 **과잉 차단 없음**(200 SPA).
`caddy validate` Valid · `caddy fmt` 무차이 · `compose config` 포트 80/443/**8081** 확인.

## 런북에서 정정한 것 (내 오판 2건)

**① "`iptables -L`로 보면 방화벽이 다 열려 있다"** — 아니다. `-L`은 **`in` 인터페이스 컬럼을
출력하지 않아서** `-i lo`가 `ACCEPT all -- 0.0.0.0/0`으로 보인다. `-v`로 보니 5번은 루프백,
1번은 `tailscale0`의 22였다. **런북에 `-v` 필수를 명시**했다.

**② "§4(b)의 80/443 INPUT 규칙은 Docker 트래픽엔 무의미하다"** — 단정했는데 **틀렸을 가능성이 크다.**
7·8번 카운터가 148/89로 **0이 아니다.** Docker 기본값 `userland-proxy`가 호스트 포트를 직접 열어
일부 트래픽이 INPUT을 타는 것으로 보인다. → **DNAT/FORWARD와 userland-proxy 두 경로가 다 있을 수
있으므로 `DOCKER-USER`와 `INPUT` 양쪽에 DROP을 넣도록** 계획을 바꿨다.

## 판단

- **404, 403 아님.** 403은 "여기 뭔가 있는데 막았다"고 알려준다. 404는 아무것도 안 알려준다.
- **`(site)` 스니펫으로 공통 추출.** 공개와 tailnet이 서로 다른 앱을 서빙하기 시작하면
  "tailnet에선 되는데요"가 시작된다.
- **tailnet IP로 직접 bind하지 않는다.** 더 안전해 보이지만 재부팅 시 Docker가 Tailscale보다 먼저
  뜨면 bind 실패로 **Caddy 컨테이너가 통째로 안 떠서 공개 사이트까지 죽는다.** 관리자 편의로
  사이트를 걸 수 없다. 0.0.0.0에 열고 방화벽으로 막는다 — 22번에 이미 쓰는 사고방식.
- **평문 HTTP가 맞다.** tailnet은 WireGuard가 암호화한다. Let's Encrypt는 tailnet 이름에 인증서를
  못 주고 자체 서명은 브라우저 경고라 더 나쁘다.
- **22번 규칙은 안 건드린다.** 비상 복구 경로다.

## ⚠️ 정직히 — 이걸로 못 막는 것

**관리자 JS는 여전히 번들에 있다.** 경로만 막았다. 그래도 목적은 달성한다: 크롤러는 `/admin`에서
404를 받아 **볼 폼이 없고**, `/api/admin/*`이 공개에서 404라 UI를 억지로 띄워도 **전 동작이 실패**한다.
번들에서 빼려면 빌드를 둘로 쪼개야 하는데 얻는 것 대비 과하다(코드는 이미 공개 저장소에 있다).

## 미검증 (사용자 몫)

실제 VM 배포 · iptables 3줄 · **OCI 보안목록에 8081이 없는지** · VM 밖에서 8081 차단 확인 ·
tailnet 실접속 · 재부팅 후 유지. 순서 주의: **tailnet 접근이 되는 걸 확인한 뒤** 공개 경로를 닫는다
(반대로 하면 관리자 화면에 스스로 잠긴다) — 다만 이번 배포는 둘이 한 커밋이라 배포 직후 바로
tailnet 확인이 필요하다.
