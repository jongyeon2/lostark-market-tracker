# 배포 직전 보안 검증 게이트 (DEPLOY-03)

**6개 항목 전부 PASS 전까지 go-live 금지.** 저장소 정적 항목은 실행자가 자동 검증(아래 결과), 노출면 항목은 배포 후 사용자가 off-box에서 수동 검증한다.

| # | 항목 | 검증 방법 | 통과 기준 | 상태 |
|---|------|-----------|-----------|------|
| 1 | 관리자 쓰기 보호 | `AdminSecretFilter` 상수시간 비교 + `SecurityConfig` `/api/admin/**` 인증 | 상수시간 비교·blank fail-closed·인증 규칙 존재 | ✅ 정적 PASS |
| 2 | API 키/시크릿 미노출 | `git grep`(JWT `eyJ…`) + git 히스토리 + `.env*` gitignore | 코드·설정·히스토리에 실키 0, `.env`/`.env.prod` 무시 | ✅ 정적 PASS |
| 3 | DB/Redis 포트 비공개 | compose 포트 grep + (라이브) off-box 포트 스캔 | caddy만 80/443, 5432/6379 미공개·외부 폐쇄 | 🟡 정적 PASS / 라이브 대기 |
| 4 | HTTPS 강제 | Caddyfile 자동 TLS+HSTS + (라이브) 인증서·리다이렉트 | 유효 인증서 + HTTP→HTTPS | 🟡 정적 PASS / 라이브 대기 |
| 5 | CORS 정책 | 백엔드 permissive CORS grep | wildcard `@CrossOrigin`/`allowedOrigin("*")` 없음(동일 출처) | ✅ 정적 PASS |
| 6 | actuator 하드닝 | `application-prod.yml` grep | `show-details: never` + health만 노출 | ✅ 정적 PASS |

---

## 저장소 정적 검증 (실행자, 2026-07-08)

### 1. 관리자 쓰기 보호 — ✅ PASS
- `AdminSecretFilter.java:58` `MessageDigest.isEqual(...)` — **상수시간 비교**(타이밍 공격 방어). blank 시크릿은 fail-closed(모든 `/api/admin/**` 401).
- `SecurityConfig.java:35` `.requestMatchers("/api/admin/**").authenticated()` — 관리자 경로 인증 요구.
- (라이브 수동) 잘못된 `X-Admin-Secret` → 401 확인 + `.env.prod`의 `ADMIN_API_SECRET`가 강시크릿(≠`123456789`).

### 2. API 키/시크릿 미노출 — ✅ PASS
- `git grep -nE "eyJ[A-Za-z0-9_-]{30,}" -- ':!*.md' ':!.planning' ':!*.example'` → **매칭 0**(코드·설정에 JWT 없음).
- `git log --all -G "eyJ…"` → **매칭 0**(히스토리에 JWT 유입 없음).
- `git check-ignore .env .env.prod` → 둘 다 무시(시크릿 커밋 방지).

### 3. DB/Redis 포트 비공개 — 🟡 정적 PASS / 라이브 대기
- `docker-compose.prod.yml`: publish 포트는 **caddy `80:80`/`443:443`만**. postgres/redis/app에 `ports:` 없음(내부 `internal` 네트워크 전용).
- **(라이브 수동)** off-box에서: `nc -zv <도메인> 5432` / `nc -zv <도메인> 6379` → **연결 실패(closed)** 확인.

### 4. HTTPS 강제 — 🟡 정적 PASS / 라이브 대기
- `frontend/Caddyfile`: 도메인 사이트 블록(Caddy 자동 Let's Encrypt) + `Strict-Transport-Security`(HSTS).
- **(라이브 수동)** `https://<도메인>` 인증서 유효 + `http://<도메인>` → **https 리다이렉트** 확인.

### 5. CORS 정책 — ✅ PASS
- `grep -rnE "@CrossOrigin|allowedOrigin|setAllowedOrigins|addAllowedOrigin" src/main/java` → **매칭 0**. 동일 출처(Caddy가 프론트+API 같은 도메인, 프론트는 상대 `/api`)라 CORS 자체가 불필요하며 permissive 설정도 없음.

### 6. actuator 하드닝 — ✅ PASS
- `application-prod.yml`: `management.endpoint.health.show-details: never` + `exposure.include: health`(health만 노출, 내부 구성/DB 상세 숨김).

---

## 라이브 검증 (사용자, 배포 후) — 대기

18-04 배포 완료 후 off-box에서:

```bash
# 3) DB/Redis 포트 폐쇄
nc -zv <도메인> 5432    # → 실패(closed) 기대
nc -zv <도메인> 6379    # → 실패(closed) 기대

# 4) HTTPS 강제
curl -sI http://<도메인> | grep -i location   # → https:// 리다이렉트
curl -sI https://<도메인> | head -1            # → 200/301, 유효 인증서

# 1) 관리자 쓰기 보호
curl -s -o /dev/null -w "%{http_code}" -X POST https://<도메인>/api/admin/events \
  -H "X-Admin-Secret: wrong"                   # → 401
```

**6개 항목 전부 PASS → go-live 승인.** 하나라도 실패면 배포 보류·수정.

---

## Go / No-Go

- [x] 1. 관리자 쓰기 보호 (정적)
- [x] 2. API 키/시크릿 미노출 (정적)
- [ ] 3. DB/Redis 포트 비공개 (라이브 off-box)
- [ ] 4. HTTPS 강제 (라이브)
- [x] 5. CORS 정책 (정적)
- [x] 6. actuator 하드닝 (정적)

**현재: 정적 4/4 PASS. 라이브 2항목(3·4·1의 401)은 배포 후 확인 → 전수 PASS 시 go-live.**
