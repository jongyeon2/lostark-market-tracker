# Phase 18: 무료 라이브 배포 + 보안 검증 - Context

**Gathered:** 2026-07-08
**Status:** Ready for planning
**Source:** 대화형 디스커션(호스팅 결정 포함) — 사용자 확정 결정 캡처

<domain>
## Phase Boundary

완성된 앱(수집 파이프라인 + 3화면 데모 + 관리자 콘솔)을 **무료 호스팅에 상시 배포**해 공개 URL로 서빙한다. 프론트는 정적/단일 출처로 서빙하고, 실 키·시크릿은 서버 env only, **배포 직전 보안 검증 게이트를 통과**한다. 이 마일스톤(v1.3)의 마지막 페이즈.

**이 페이즈가 하는 것:** 배포 산출물(Dockerfile·prod compose·Caddyfile·prod 프로파일·런북) 저작 + 보안 하드닝 + 보안 검증 게이트.
**이 페이즈가 하지 않는 것:** CD 자동화(명시적 v2 = CD-V2-01, "1회성 데모 배포엔 수동으로 충분" — REQUIREMENTS Out of Scope). 수동 배포로 라이브를 먼저 세우고, 자동화는 라이브 이후 후속.

**불변 제약(상시 가드):** 프론트에서 Lostark API 직접 호출 금지 · API key는 백엔드 env only · 실키/시크릿을 코드·문서·로그·커밋에 미기재 · 수집/캐시/event-impact 로직은 Core Value(수집 신뢰성) 가드 하에서만.
</domain>

<decisions>
## Implementation Decisions (LOCKED — 사용자 확정)

### D-01 · 호스팅 = Oracle Cloud Always Free VM 한 대
전 구성 요소를 **docker-compose로 단일 박스**에 배치(app + postgres + redis + caddy). 항상 켜짐 → `PriceCollector`의 `@Scheduled`(fixedDelay 10분) 수집기가 24/7 구동돼 Core Value(빠짐없는 수집)를 배포 환경에서 충족한다. (Render 무료처럼 idle sleep하는 티어는 스케줄러가 죽어 부적합 → 상시 VM 채택.)
- **함정 인지:** ARM(Ampere A1) 무료 인스턴스는 "Out of capacity"가 잦음 → 리전 변경/재시도. AMD 마이크로(1GB)는 JVM+PG+Redis에 빠듯 → ARM 2~4GB 목표.

### D-02 · 리버스 프록시/TLS = Caddy + DuckDNS
Caddy를 리버스 프록시로 두고 **Let's Encrypt 자동 HTTPS**(HTTP→HTTPS 강제)를 받는다. 도메인은 **무료 DuckDNS 서브도메인**(사용자 확정). Caddy가 **유일한 공개 진입점**(80/443).

### D-03 · 프론트 서빙 = Caddy 정적 직접 서빙(동일 출처)
Caddy가 `frontend/dist`를 **정적 파일로 직접 서빙**(사용자 확정)하고 SPA 딥링크는 `try_files → /index.html` fallback으로 처리. `/api/*`·`/actuator/*`만 `reverse_proxy app:8080`. **동일 출처**라 CORS 불필요.
- **근거(코드 확인):** `frontend/src/lib/api.ts`는 이미 상대경로 `/api/...`로 `fetch`(주석에 "same-origin" 명시) → **프론트 코드 0줄** 변경으로 동작.

### D-04 · prod 프로파일 신설 + 수집 부트스트랩 미러
`application-prod.yml`을 신설:
- `management.endpoint.health.show-details: never` (base는 `always` — 공개 노출용 하드닝).
- `collection.initial-delay-ms: 10000` (dev와 동일 — `WatchlistSeeder`(ApplicationRunner)가 첫 스케줄 tick 전에 22개 워치리스트 upsert를 끝내도록; 안 그러면 빈 tracked_item에 첫 tick).
- datasource/redis는 base의 env 플레이스홀더 그대로(compose가 내부 호스트명 `postgres`/`redis` + 비밀번호 주입).
- **`WatchlistSeeder` `@Profile({"dev","seed"})` → `{"dev","seed","prod"}`로 확장**(prod에서 큐레이션 22개 시드 — 수집 대상이 있어야 수집 가능). 이는 데이터 시드 컴포넌트 1줄 변경이며 Core Value 수집 로직 무변경.
- **`SeedDataRunner`(합성 seed)는 `@Profile("seed")`** → prod 미구동(정상 — 실데이터만). `LostarkSpikeClient`(`@Profile("spike")`)도 미구동.
- **`DetailStatsBackfillRunner`는 `@Profile("!test")`** → prod 구동(gap 백필 동작).

### D-05 · 보안 네트워킹 (최소 노출)
- **postgres/redis: host 포트 미publish** — docker 내부 네트워크 전용(외부에서 5432/6379 도달 불가).
- **redis `--requirepass ${REDIS_PASSWORD}`** — 내부 전용이어도 심층 방어.
- **app :8080도 host 미publish** — Caddy 경유로만 도달.
- **오직 caddy만 80:80 / 443:443 publish.**
- **방화벽 이중 개방:** OCI 보안목록(클라우드 방화벽) **AND** VM 내부 iptables 둘 다 80/443 개방(Oracle Ubuntu 이미지는 기본 iptables가 80/443 차단 — 대표 함정). SSH 22는 본인 IP로 제한.

### D-06 · 시크릿 주입 (env-only, 강생성)
모든 시크릿은 `.env.prod`(**gitignore**) → 컨테이너 env로 주입. 코드·문서·로그·커밋 미기재.
- `ADMIN_API_SECRET` = `openssl rand -hex 32`로 **강생성**(dev의 `123456789` 폐기 — 이 값은 프로덕션에 절대 미사용).
- `POSTGRES_PASSWORD`·`REDIS_PASSWORD` = 강생성(dev 기본 `lostark` 폐기).
- `LOSTARK_API_KEY` = 실 JWT 키(재발급 가능).
- `SITE_ADDRESS` = DuckDNS 도메인, ACME 이메일.
- `.env.prod.example`(커밋 O)는 빈 값 + 생성 가이드만.

### D-07 · 배포 방식 = 수동 우선
SSH 접속 → `git pull` → `docker compose -f docker-compose.prod.yml up -d --build` → systemd 유닛으로 부팅 시 자동 기동. **수동 → (라이브 이후) 자동화** 순서(사용자 확정). CD 파이프라인은 명시적 v2(CD-V2-01) — 이 페이즈 밖.

### D-08 · DEPLOY-03 보안 검증 게이트 (go/no-go)
배포 직전 체크리스트 전수 통과 후에만 go-live:
1. 관리자 쓰기 보호(X-Admin-Secret 상수시간 비교·blank fail-closed·강시크릿),
2. API 키/시크릿 미노출(코드·문서·로그·git 히스토리),
3. DB/Redis 포트 비공개(compose 검토 + off-box 포트 스캔),
4. HTTPS 강제(자동 인증서·HTTP→HTTPS 리다이렉트),
5. CORS 정책(동일 출처 → 백엔드에 permissive `@CrossOrigin`/wildcard 부재 확인),
6. actuator health-only + `show-details: never`.

### Claude's Discretion
- Dockerfile 베이스 이미지 태그·JVM 메모리 플래그 구체 값, Caddyfile 보안 헤더 세트, systemd 유닛 파일 내용, 런북 문서 세부 문구 — 구현 시 상식적 기본값.

## Core Value 가드
`PriceCollector`·`price_snapshot`·`LatestPriceCache`·`WindowQueryService`·`DownsampleService`·`EventImpactService`의 **로직은 0줄 변경**. Phase 18은 배포 산출물(Dockerfile·compose·Caddyfile·prod yml·문서) + `WatchlistSeeder` 프로파일 1줄 + actuator 노출 하드닝만 건드린다.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 배포 대상 앱 구성
- `docker-compose.yml` — 기존 dev 인프라(postgres+redis, 포트 publish O). prod compose의 출발점이나 **포트 미publish로 하드닝**해야 함.
- `src/main/resources/application.yml` — base 설정(env 플레이스홀더·actuator `show-details: always`). prod가 오버라이드.
- `src/main/resources/application-dev.yml` — 수집 부트스트랩(`collection.initial-delay-ms: 10000`) 원형 — prod가 미러.
- `.env.example` — 시크릿 env 컨벤션(빈 값·주석) 원형 — `.env.prod.example`가 따름.

### 동일 출처 근거
- `frontend/src/lib/api.ts` — 상대경로 `/api/...` fetch(same-origin 주석). Caddy 프록시 대상.

### 프로파일/시드
- `src/main/java/com/lostark/tracker/collect/WatchlistSeeder.java` — `@Profile({"dev","seed"})` → prod 추가 대상, 22개 큐레이션 upsert.
- `src/main/java/com/lostark/tracker/collect/PriceCollector.java` — `@Scheduled`, 프로파일 무제한 → prod 24/7 구동.
- `src/main/java/com/lostark/tracker/seed/SeedDataRunner.java` — `@Profile("seed")` → prod 미구동.

### 보안 기존 자산 (배포 게이트가 검증)
- `src/main/java/com/lostark/tracker/security/SecurityConfig.java` — CSRF off·STATELESS·permitAll GET·`/api/admin/**` authenticated.
- `src/main/java/com/lostark/tracker/security/AdminSecretFilter.java` — X-Admin-Secret 상수시간 비교·blank fail-closed.
</canonical_refs>

<specifics>
## Specific Ideas

**목표 아키텍처(단일 박스):**
```
[인터넷] ──HTTPS──▶ Caddy(리버스 프록시·자동 TLS·80/443만 공개)
                       ├── /            → file_server(frontend/dist, SPA fallback)
                       └── /api,/actuator → reverse_proxy app:8080
                                              ├── postgres:16 (볼륨·포트 미공개)
                                              └── redis:7 (requirepass·포트 미공개)
```
동일 출처 → CORS 소멸. Caddy가 3개 보안 항목(HTTPS·CORS·app/DB 비노출)을 구조적으로 해결.
</specifics>

<deferred>
## Deferred Ideas

- **CD 자동 배포 파이프라인** — v2(CD-V2-01). 라이브 이후 GitHub Actions로 확장(사용자: "수동→자동화" 순서).
- **관측성(Micrometer)** — OPS-V2, 이 게이트는 보안 검증까지.
- **DuckDNS 대신 유료 도메인** — 필요 시 후속(현재 무료 DuckDNS 확정).
- **pg_dump 백업 크론** — nice-to-have, 데모엔 볼륨 영속으로 충분.
</deferred>

---

*Phase: 18-free-deploy-security*
*Context gathered: 2026-07-08 via 대화형 디스커션*
