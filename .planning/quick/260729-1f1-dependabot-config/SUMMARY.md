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

## 후속 실행 (2026-07-29): major ignore 정책 전환

병합 직후 Dependabot이 밀린 의존성으로 **10개 PR을 한꺼번에** 열었다(1회성 catch-up, 정상). 이걸 관찰하면서 초기 계획("major는 개별 PR")의 약점이 드러났다:

- **CI가 잡아준 major (안전망 작동, 빨강)**: spring-boot 3.4.1→4.1.0(#9), gradle 8→9(#8), vite 6→8(#11), typescript 5→7(#13), @vitejs/plugin-react 4→6(#14).
- **가짜 green major (위험)**: 도커 베이스 이미지(temurin 21→25 #5, node 20→26 #7)와 배포용 액션 묶음(#6 = checkout v7·setup-node v7·buildx v4·login v4·build-push v7). 이들은 `images`/`deploy` job에서만 실행되는데 **그 job이 PR CI에서 skip**되므로 green이 실제 검증을 뜻하지 않는다. @types/node 26(#12)도 major.

→ **정책 전환: "major 개별 PR" → "major ignore(5개 생태계 전부)"**. `dependency-name: "*"` + `update-types: ["version-update:semver-major"]`를 npm·gradle·github-actions·docker×2에 추가. github-actions 그룹도 minor/patch로 한정.

**보안 안전성(핵심 검증)**: GitHub 공식 문서상 `update-types` 기반 ignore는 "버전 업데이트"에만 적용되고 **"보안 업데이트"에는 적용되지 않는다**. 즉 이번 GHSA처럼 수정이 8.3.0(major)에만 있어도 보안 PR은 계속 자동으로 온다 — major ignore가 보안 감시를 뚫지 않음을 문서로 확정한 뒤 적용했다. (`versions` 필드로 막으면 보안까지 막히는 함정과 대비 — 그래서 `update-types`를 씀.)

**실행 결과**:
- ✅ 안전한 npm minor/patch 묶음 **#10(13개 업데이트) 머지** — Dependabot의 실제 이득 회수. main 머지라 프로덕션 재배포(승인 게이트 통과 필요).
- ❌ **#6은 머지 안 함** — major 5종 묶음이라 정책상 ignore 대상. 다음 스캔 때 자동 close.
- 열린 major 8개(#5·#7·#8·#9·#11·#12·#13·#14)는 ignore 정책 병합 후 다음 스캔 때 자동 close.

**정직성**: PyYAML을 이번엔 설치해 **라이브러리 파싱까지 통과 확인**(이전 SUMMARY의 "구조 검증만" 한계 해소).
