---
phase: 17-real-data-transition
plan: 02
subsystem: docs
tags: [readme, demo-narrative, honest-engineering, seed-vs-realdata]

requires:
  - phase: 14-readme-narrative
    provides: README JSCODE 스타일 재구성(친근 인트로 + 엔지니어링 깊이) 기반
provides:
  - seed='로컬/테스트 전용 · 합성' 라벨 명확화 + 실데이터(dev)='라이브 실체' 헤드라인 재프레임(D-09)
  - README.md ↔ frontend/README.md 단일 진실 원천 정합(D-10)
affects: [17-03 real-data-verification, 18-deploy]

tech-stack:
  added: []
  patterns:
    - "정직성 이중 트랙 서사: seed(합성 즉시 재현) + dev(실수집 라이브 실체)"

key-files:
  created: []
  modified:
    - README.md
    - frontend/README.md

key-decisions:
  - "§데모 인트로도 함께 재편(D-09 범위는 §실행 방법이지만 §데모가 seed를 데모 실체로 프레이밍하고 있어 단일 진실 원천 정합상 필요)"
  - "'공개 배포 URL은 아직 없습니다'만 유지(원문 톤 계승) — 실제 URL/배포/prod 프로파일 등 Phase 18 항목은 미추가"

patterns-established:
  - "두 README가 seed=합성(로컬/테스트)·dev=실수집(라이브 실체)을 모순 없이 서술하는 단일 진실 원천"

requirements-completed: [REALDATA-02]

duration: 8min
completed: 2026-07-03
---

# Phase 17-02: README 데모 서사 재편 Summary

**데모 서사를 seed 합성 기준에서 실수집(dev) 실데이터 기준으로 재편 — seed는 '로컬/테스트 전용 합성'으로 존치하되 '합성'임을 숨기지 않고, 실데이터(dev)를 '라이브 데모의 실체'로 헤드라인화. 두 README 단일 진실 원천 정합, 코드 0줄**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-03
- **Completed:** 2026-07-03
- **Tasks:** 2
- **Files modified:** 2 (문서만)

## Accomplishments
- **D-09**: 루트 `README.md` §데모·§실행 방법 재편 — seed를 '로컬/테스트 전용 · 합성 8일치'로 명확히 라벨링(진입장벽 0 seed 경로 보존), 실데이터(dev, 실키 실수집)를 '라이브 데모의 실체'로 헤드라인화. '두 가지 실행 프로파일'을 dev(라이브 실체) vs seed(로컬/테스트 합성)로 재프레임
- **D-10**: `frontend/README.md`를 루트와 정합하게 재편 — 헤더·사전조건·정직성 노트에서 seed↔실데이터 역할 구분을 정직히 서술. 프론트가 출처와 무관하게 같은 백엔드 DTO만 소비(Vite 프록시·백엔드 0줄)한다는 사실 유지
- 정직성 이중 트랙 서사 확립 — seed가 '합성'임을 숨기지 않으면서 실데이터를 라이브 실체로 구분(면접 설명 포인트)
- 실키/`ADMIN_API_SECRET` 값 미기재(env 변수 이름만), Phase 18 항목(공개 URL·배포·prod 프로파일) 미추가, 코드 diff 0줄

## Task Commits

1. **Task 1: 루트 README seed 라벨 명확화 + 실데이터(dev) 헤드라인 재프레임(D-09)** - `ffe3502` (docs)
2. **Task 2: frontend/README seed↔실데이터 역할 구분 서술 + 루트 정합(D-10)** - `2051c5c` (docs)

## Files Created/Modified
- `README.md` - §데모 인트로·§한눈에 3단계·§두 가지 실행 프로파일·§프론트 데모 재프레임
- `frontend/README.md` - 헤더 인트로·사전조건·스크린샷 노트·정직성 노트 데이터 항목 재편

## Decisions Made
- §데모 인트로까지 재편(D-09의 명시 범위는 §실행 방법이나, §데모가 seed를 데모 실체로 프레이밍하고 있어 D-10 단일 진실 원천 정합을 위해 포함)
- '공개 배포 URL은 아직 없습니다'는 원문의 '라이브 배포는 없습니다' 톤을 계승한 사실 서술 — Phase 18 항목(실제 URL·Dockerfile·prod)은 추가하지 않음

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None. '실데이터' 리터럴이 grep 게이트 대상이라 §데모 인트로에 실데이터를 명시적으로 포함해 정합.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 03(검증 게이트)에서 이 서사대로 dev 실키 수집을 라이브 검증
- 코드 diff 0줄 — D-08 불변 가드 정합

---
*Phase: 17-real-data-transition*
*Completed: 2026-07-03*
