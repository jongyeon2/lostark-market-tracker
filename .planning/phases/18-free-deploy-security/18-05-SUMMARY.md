---
phase: 18-free-deploy-security
plan: 05
status: complete-static (라이브 항목은 배포 후 사용자 수동)
date: 2026-07-08
requirements: [DEPLOY-03]
---

# Phase 18-05 Summary — 배포 직전 보안 검증 게이트

**DEPLOY-03 게이트를 6항목 체크리스트로 문서화하고 저장소 정적 항목을 실제 검증(4/4 PASS). 라이브 항목은 배포 후 사용자 off-box 검증.**

## Changes
- **`docs/deploy/security-checklist.md`**(신규): 6항목(관리자 보호·키 미노출·DB/Redis 비공개·HTTPS·CORS·actuator) 검증 방법+통과 기준+상태 표 + go/no-go. "전 항목 PASS 전까지 go-live 금지" 명시.

## 저장소 정적 검증 결과 (실행자, 실측)
1. **관리자 쓰기 보호** ✅ — `AdminSecretFilter:58` `MessageDigest.isEqual`(상수시간)·blank fail-closed, `SecurityConfig:35` `/api/admin/**` authenticated.
2. **API 키/시크릿 미노출** ✅ — `git grep eyJ…`(코드/설정) 0 매칭 + `git log --all -G eyJ…`(히스토리) 0 매칭 + `.env`/`.env.prod` gitignore.
3. **DB/Redis 포트 비공개** 🟡 정적 PASS — compose에 caddy만 80/443, DB/Redis/app 미공개. (라이브 off-box 포트 스캔 대기)
4. **HTTPS 강제** 🟡 정적 PASS — Caddyfile 자동 TLS+HSTS. (라이브 인증서·리다이렉트 대기)
5. **CORS 정책** ✅ — `grep @CrossOrigin|allowedOrigin|setAllowedOrigins` 0 매칭(동일 출처).
6. **actuator 하드닝** ✅ — prod yml `show-details: never` + health만 노출.

## Verification
- 정적 **4/4 PASS**(1·2·5·6) + 3·4는 정적 부분 PASS·라이브 대기.
- 체크리스트에 "go-live 금지" 게이트 명시.

## Pending (사용자 수동, 배포 후)
- off-box: `nc -zv <도메인> 5432/6379` 폐쇄, `http→https` 리다이렉트·인증서 유효, 잘못된 X-Admin-Secret → 401.
- 6항목 전수 PASS 시 **go-live 승인**.

## Notes
- 앱 로직·수집/캐시/event-impact diff 0줄(검증 전용 문서).
