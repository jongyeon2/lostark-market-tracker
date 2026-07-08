---
phase: 18-free-deploy-security
plan: 04
status: complete-artifacts (Task 3 라이브 배포는 사용자 수동 대기)
date: 2026-07-08
requirements: [DEPLOY-01, DEPLOY-03]
---

# Phase 18-04 Summary — Oracle VM 배포 런북 (수동 배포)

**Oracle Always Free VM 수동 배포 런북 + 부팅 기동 systemd 유닛 작성. 실제 VM 배포(Task 3)는 사용자 수동 체크포인트.**

## Changes
- **`deploy/lostark.service`**(신규): systemd oneshot(RemainAfterExit) — 부팅 시 `docker compose ... --env-file .env.prod up -d --build` 기동, `WantedBy=multi-user.target`, `After=docker.service`.
- **`docs/deploy/oracle-vm-runbook.md`**(신규): 9섹션 한국어 런북 — ①VM 프로비저닝(ARM Ampere·capacity 재시도) ②Docker 설치 ③DuckDNS ④**방화벽 이중 개방**(OCI 보안목록 + VM iptables, Oracle 함정 경고, SSH 22 제한) ⑤`.env.prod` 강시크릿(openssl rand·123456789 금지) ⑥`up -d --build`(Caddy 자동 인증서) ⑦systemd ⑧라이브 검증 체크리스트 ⑨운영(업데이트·백업).

## Verification
- systemd 유닛: compose up ExecStart · `WantedBy=multi-user.target` · `After=docker.service` (grep 4/4).
- 런북: `iptables`·`duckdns`·`openssl rand`·`up -d --build`·`Security List`·`0.0.0.0/0` (grep 6/6).

## Pending (사용자 수동 — Task 3)
- **실제 Oracle VM 배포**는 사용자 계정/VM 필요 → 실행자 접근 밖. 런북 따라 배포 후:
  - 공개 URL(HTTPS)로 3화면 + 관리자 로그인 동작
  - `docker compose logs app`에 collection_run SUCCESS 축적
  을 확인하면 **DEPLOY-01 실측 충족**. 이 상태가 18-05 라이브 보안 검증의 대상.

## Notes
- 앱 코드·수집/캐시/event-impact diff 0줄(문서 + systemd 유닛만).
