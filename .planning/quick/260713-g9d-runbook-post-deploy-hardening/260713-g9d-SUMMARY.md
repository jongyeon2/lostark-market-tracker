---
quick_id: 260713-g9d
slug: runbook-post-deploy-hardening
date: 2026-07-13
status: complete
commit: 0e783ad
---

# Quick Task 260713-g9d 완료 — 배포 런북 하드닝 반영

## 무엇을 했나

`docs/deploy/oracle-vm-runbook.md`에 Phase 18 배포 후 하드닝을 재현 가능하게 문서화(문서만, 코드 0줄):

1. **§4(a) SSH** — 표의 `<내 IP>/32` 아래에 하드닝 note 추가:
   - 22번은 본인 공인 IP `/32`로만 개방(`curl -s ifconfig.me`로 확인)
   - 심층 방어: `PasswordAuthentication no`(키 전용 로그인)
   - ⚠️ 유동 IP 시 Ingress Source 재수정 필요, 대안으로 키 전용+fail2ban

2. **§8 라이브 체크리스트** — 검증 항목 2개 추가:
   - 보안 응답 헤더 7종 존재 + Server 제거
   - 브라우저 CSP 위반 = 무해한 eval 1건뿐

3. **§8 신규 "보안 헤더·CSP 검증" 블록**:
   - 배포된 헤더 7종 표(HSTS·nosniff·X-Frame·Referrer·**CSP·Permissions-Policy·COOP**, Server 제거)
   - `curl -sI | grep` 검증 명령 2줄
   - CSP 근거: `style-src 'unsafe-inline'` 필수(Recharts), `img-src` onstove CDN 필수,
     `'unsafe-eval'` 미포함 근거(무해 eval 프로브), COEP 제외 근거
   - go-live 조건에 게이트 밖 하드닝(CSP·SSH) 포함

## 검증

- 마크다운 구조(표·코드블록·체크리스트·note) 유지, 3개 섹션 반영
- 재배포/신규 배포자가 런북만으로 하드닝 상태를 재현·검증 가능

## 커밋

- `0e783ad` docs(deploy): 런북에 배포 후 하드닝 반영 (SSH /32·보안 헤더·CSP 검증) [260713-g9d]

## 관련

- quick 260713-e1o (CSP·보안 헤더 코드 적용, 라이브 검증) 의 후속 문서화
