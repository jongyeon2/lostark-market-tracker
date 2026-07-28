---
quick_id: 260729-1f1
slug: dependabot-config
description: .github/dependabot.yml 신설 — 공급망 표면(npm·gradle·github-actions·docker) 주간 의존성 스캔 자동화
date: 2026-07-29
status: in-progress
---

# Quick Task 260729-1f1 — Dependabot 상시 의존성 스캔

## 왜 — High 경보를 "그때 마침 봤다"로 넘기고 싶지 않다

계기: GitHub Security 탭에 **GHSA-qwww-vcr4-c8h2** (react-router RSC Mode CSRF Bypass, CVSS **High 7.1**) 경보가 떴다.

분석 결과 **이 앱엔 실제 영향이 없다**:

- 경보 전제: *"This only affects your application if you are using the unstable RSC APIs."*
- 실측: 순수 클라이언트 SPA(`createBrowserRouter` + `RouterProvider` + `createRoot`), 서버 런타임 없음(`vite build` 정적 산출물), `react-router.config.*` 없음, `unstable`/SSR 함수 grep **0건**.
- 결론: 취약 코드 경로가 존재하지 않음. 경보는 Dependabot이 **버전 범위**(`react-router 7.18.0 ∈ [7.12.0, 8.3.0)`)로만 매칭해 뜬 것.

문제는 **감시가 수동이었다는 것**이다. 이번엔 우연히 Security 탭을 봤지만, 다음 취약점은 못 볼 수 있다. `.github/dependabot.yml`이 없어서(확인함) 자동 버전 업데이트·PR이 하나도 없다.

## 무엇을 만드나

`.github/dependabot.yml` (version 2) — 저장소의 **공급망 표면 전체**를 주간 스캔:

| ecosystem | directory | 근거 |
|---|---|---|
| `npm` | `/frontend` | `frontend/package.json` — 이번 경보의 출처 |
| `gradle` | `/` | 루트 `build.gradle`·`settings.gradle` (Spring Boot 백엔드) |
| `github-actions` | `/` | `.github/workflows/ci.yml` — 액션 버전도 공급망 |
| `docker` | `/` | 루트 `Dockerfile` (백엔드 베이스 이미지) |
| `docker` | `/frontend` | `frontend/Dockerfile` |

요청 범위는 npm+gradle이었으나, **목적이 "이 위험군(공급망) 대비"**라 같은 표면인 actions·docker도 포함(파일 하나, 전부 주석). PR 리뷰에서 트리밍 가능.

## 핵심 설계 결정

- **minor/patch는 그룹으로 묶고 major는 개별 PR** — react-router v8 같은 브레이킹 체인지는 격리 검토해야 한다. 이번 사건 자체가 "major 자동 병합 금지"의 산 증거.
- **주간(월 09:00 KST)** — 데일리는 PR 노이즈, 월간은 늦음. 포트폴리오 규모엔 주간이 적정.
- **`open-pull-requests-limit: 5`** — 방치된 PR이 무한정 쌓이는 것 방지.
- **정직성**: dependabot.yml은 "버전 업데이트"를 자동화한다. GHSA 경보 자체를 닫는 것(Dismiss)은 GitHub UI에서 사람이 근거를 남기고 해야 하는 별도 작업 — 이 파일이 그 경보를 자동으로 없애 주지는 않는다.

## 범위 밖 (하지 않는 것)

- **react-router v8 마이그레이션** — 실제 위험이 없으므로 백로그. 별도 phase.
- **npm `overrides`로 transitive 강제** — v7 dom + v8 core 혼용은 런타임 정합성 위험이 취약점보다 커서 기각.
- **GHSA-qwww-vcr4-c8h2 Dismiss** — 사용자가 GitHub UI에서 "Vulnerable code is not used" 사유로 직접 처리.
- **`npm audit` CI 게이트** — v2 후보. 지금은 Dependabot으로 충분.

## 검증

- YAML 파싱 유효(구조 검증).
- 다섯 `directory` 경로가 실제 파일 위치와 일치(`/frontend` package.json, `/` build.gradle, `/` ci.yml, `/`·`/frontend` Dockerfile — 전부 확인함).
- 브랜치에서 커밋 → PR 생성. 병합은 사용자.
- 병합 후 Dependabot이 스스로 업데이트 PR을 열기 시작하는 것이 정상 동작(기대치).
