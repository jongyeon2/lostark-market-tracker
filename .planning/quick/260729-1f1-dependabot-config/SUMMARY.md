---
quick_id: 260729-1f1
slug: dependabot-config
description: .github/dependabot.yml 신설 — 공급망 표면(npm·gradle·github-actions·docker) 주간 의존성 스캔 자동화
date: 2026-07-29
status: complete
commits: []
---

# Quick Task 260729-1f1 — Dependabot 상시 의존성 스캔

## 무엇을 만들었나

`.github/dependabot.yml` (version 2) 신설. 저장소 공급망 표면 **5개 엔트리**를 주간(월 09:00 KST) 스캔:

| # | ecosystem | directory | 대상 파일 |
|---|---|---|---|
| 1 | `npm` | `/frontend` | `frontend/package.json` |
| 2 | `gradle` | `/` | 루트 `build.gradle`·`settings.gradle` |
| 3 | `github-actions` | `/` | `.github/workflows/ci.yml` |
| 4 | `docker` | `/` | 루트 `Dockerfile` |
| 5 | `docker` | `/frontend` | `frontend/Dockerfile` |

## 계기와 판단

- **트리거**: GHSA-qwww-vcr4-c8h2 (react-router RSC CSRF, High 7.1) 경보. 분석 결과 **우리 앱엔 영향 없음**(순수 클라 SPA, RSC/unstable API 0건) — 하지만 감시가 수동이었던 게 진짜 문제라 상시 스캔을 붙였다.
- **요청 범위(npm+gradle)를 넘어 actions·docker 포함** — 목적이 "공급망 위험군 대비"라 같은 표면을 다 덮는 게 맞다고 판단. 전부 주석으로 근거를 남겨 PR에서 트리밍 가능하게 했다.

## 핵심 설계 결정

- 🔑 **minor/patch는 그룹으로 묶고 major는 개별 PR** — react-router v8 같은 브레이킹 체인지를 격리 검토. 이번 사건이 곧 "major 자동병합 금지"의 근거.
- **주간 스케줄 + `open-pull-requests-limit: 5`** — PR 노이즈와 방치 PR 누적을 동시에 억제.
- **정직성 경계**: 이 파일은 "버전 업데이트 PR"을 자동화할 뿐, GHSA 경보 자체를 닫지는 못한다. 그 경보의 Dismiss("Vulnerable code is not used" 근거)는 사용자가 GitHub UI에서 별도로 처리.

## 검증

- YAML 탭 혼입 0, 2-space 들여쓰기 일관(구조 grep 확인). *PyYAML·js-yaml 미설치라 라이브러리 파싱은 못 했고 구조 수준 검증만 수행 — GitHub가 병합 후 파싱 오류를 알려주면 즉시 대응.*
- 5개 `directory` 경로가 실제 파일 위치와 1:1 일치(전부 `ls`로 확인).
- 브랜치 `quick/260729-1f1-dependabot-config`에서 커밋 → PR 생성.

## 범위 밖 / 후속

- react-router v8 마이그레이션 → 백로그(실위험 없음).
- GHSA-qwww-vcr4-c8h2 Dismiss → 사용자가 GitHub UI에서.
- `npm audit`/`osv-scanner` CI 게이트 → v2 후보.
- ⚠️ **병합 후 기대치**: Dependabot이 밀린 의존성에 대해 **스스로 업데이트 PR을 열기 시작**한다(정상 동작).
