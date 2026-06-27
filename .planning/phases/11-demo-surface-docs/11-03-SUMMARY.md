---
phase: 11-demo-surface-docs
plan: 03
subsystem: docs
tags: [readme, docs, vite-proxy, reproduction, demo, honest-data]

# Dependency graph
requires:
  - phase: 11-demo-surface-docs
    provides: 11-01 시각 마감(잘 나온 화면), 11-02 확정 스크린샷 파일명·상대경로 PNG
  - phase: 07-frontend-foundation
    provides: Vite 프록시(server.proxy['/api']→:8080, VITE_API_TARGET), KST 표시/UTC 계산 정직성
provides:
  - frontend/README.md 신규 — 재현 핵심(seed→npm run dev→3화면+스크린샷+프록시 설명) 단일 출처(D-06)
  - 루트 README '프론트 데모' 포인터 섹션 추가(D-05) — 기존 curl 섹션 회귀 없이 보존
  - frontend/.env.example 정리(stray 문자 제거, HEAD 클린 복원)
  - DEMO-01 5분 재현 dry-run 검증(명령-package.json 일치·스크린샷 경로 유효·build green)
affects: [verify-work, milestone-complete]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "README 분업(D-05): 루트=짧은 포인터, frontend/README=상세 단일 출처 → 최신화 지점 1곳"
    - "정직성 카피 일관: 상관≠인과·seed 합성 데이터·정적서빙 v2(범위 결정)를 문서가 흐리지 않음"

key-files:
  created:
    - frontend/README.md
    - .planning/phases/11-demo-surface-docs/11-03-SUMMARY.md
  modified:
    - README.md
    - frontend/.env.example

key-decisions:
  - "루트 README 인트로 'JSON이 곧 UI/프론트엔드 없음' 문장이 프론트 데모 추가로 거짓이 되어 정직하게 갱신(Rule 1 편차) — curl/한눈에/로컬실행/그외 섹션은 무변경 보존"
  - "정적 서빙(DEMO-03)은 D-01로 v2 연기 — '범위 결정'으로 frontend/README에 한 줄 정직 기록(완료조건 5 의도된 미충족)"
  - "frontend/.env.example 정리는 stray 'c' 제거 → HEAD 클린 상태와 동일해져 별도 diff 없음(정리=복원)"

patterns-established:
  - "DEMO-01 dry-run 검증: 문서 실행 명령 ↔ package.json 스크립트 대조 + 스크린샷 경로 존재 + npm run build로 '5분 재현'이 빈말 아님을 증명"

requirements-completed: [DEMO-01, DEMO-02]

# Metrics
duration: ~15 min
completed: 2026-06-27
---

# Phase 11 Plan 03: 데모 문서 표면 + 재현 검증 Summary

**frontend/README에 재현 핵심(seed 백엔드 → `npm run dev` → 3화면 + 스크린샷 + Vite 프록시 설명)을 단일 출처로 일원화하고, 루트 README에 '프론트 데모' 포인터 섹션을 curl 섹션 회귀 없이 추가 — DEMO-01 5분 재현을 명령-package.json 일치·경로 유효·build green로 dry-run 검증.**

## Performance

- **Duration:** ~15 min
- **Tasks:** 3 (frontend/README+env / 루트 README 포인터 / DEMO-01 dry-run)
- **Files created:** 1 (frontend/README.md), **modified:** 2 (README.md, frontend/.env.example)

## Accomplishments
- **frontend/README.md 신규(D-06):** 사전조건(seed 기동) → `npm install && npm run dev` → 3화면(라우트 표)+상대경로 스크린샷 임베드 → Vite 프록시 1문단(왜 백엔드 0줄 변경으로 `/api`가 동일 출처인지) → 정직성(KST 표시/UTC 계산·seed 합성·상관≠인과·정적서빙 v2)
- **루트 README '프론트 데모' 포인터 섹션(D-05):** 3단계 요약 + 대표 스크린샷 1장 + `frontend/README` 위임 링크 + 'curl로도 브라우저로도 동일 read API' 정직 카피. 기존 §'데모 — curl + 샘플 JSON' 무변경 보존.
- **frontend/.env.example 정리:** 워킹트리 stray 문자 제거 → `VITE_API_TARGET` 예시가 frontend/README 프록시 안내와 일치(HEAD 클린 복원).
- **DEMO-01 dry-run 검증 통과:** 명령(dev/build/preview) ↔ package.json 일치, 스크린샷 경로가 11-02 확정 3파일 가리킴, `cd frontend && npm run build`(tsc 포함) 통과, curl 섹션 회귀 없음, 백엔드+프론트 src 무변경.

## 5분 재현 체크리스트 (UAT walkthrough용)

리뷰어가 클린 클론에서 README만 따라 3화면을 비어있지 않게 재현하는 경로:

1. **인프라:** `docker compose up -d` (Postgres 16 + Redis 7)
2. **seed 백엔드:** `./gradlew bootRun --args='--spring.profiles.active=seed'` (API 키 불필요)
3. **프론트:** `cd frontend && npm install && npm run dev` → `http://localhost:5173`
4. **3화면 URL:**
   - Dashboard `http://localhost:5173/` — 헬스 카드 + 활성 품목 워치리스트
   - Item Timeline `http://localhost:5173/timeline?item=<품목ID>` — 라인 차트 + 이벤트 마커(생략 시 최근 30일)
   - Event Impact `http://localhost:5173/impact?item=<품목ID>&window=24` — 변화율 결과 표 + 상관≠인과 배너
5. **스크린샷 존재:** `frontend/docs/screenshots/{dashboard,item-timeline,event-impact}.png` ✅ (11-02에서 커밋)

> 실제 클린 클론 human walkthrough(브라우저로 3화면 직접 확인)는 verify-work/verify-phase로 이월 — 본 plan은 문서-실행 일치를 dry-run으로 검증.

## ROADMAP 완료조건 5 (정적 서빙) — 의도된 미충족

**DEMO-03(Spring `resources/static` 단일 출처 정적 서빙)은 D-01로 v2 연기 — 의도된 스코프 결정**(못 해서가 아님). 채택 시 Vite `base`/`build.outDir`→`resources/static` 배선, React Router SPA fallback, Dockerfile 빌드 스테이지, 정적/`/api` 경로 충돌 처리를 동반해 v1.1 '백엔드 무변경 프론트 데모' 범위를 초과. 데모 재현은 'seed 백엔드 + `npm run dev`'(이미 동작)로 충분. frontend/README에 한 줄로 정직히 기록.

## Task Commits

1. **Task 1: frontend/README + .env.example 정리** — `7dd7383` (docs) (.env.example은 stray 제거로 HEAD 복원 → diff 없음)
2. **Task 2: 루트 README 프론트 데모 포인터 섹션** — `ffd5dbe` (docs)
3. **Task 3: DEMO-01 재현 dry-run 검증** — 파일 변경 없음(검증 + 본 SUMMARY 기록)

**Plan metadata:** SUMMARY + 추적 파일 커밋 (docs: complete plan)

## Files Created/Modified
- `frontend/README.md` — 재현 핵심 단일 출처(사전조건·실행·3화면·스크린샷·프록시·정직성)
- `README.md` — '프론트 데모' 포인터 섹션 추가(curl 섹션 보존) + 인트로 1문장 정직성 갱신 + stray 빈 줄 복원
- `frontend/.env.example` — stray 문자 제거(HEAD 클린 복원)

## Decisions Made
- **인트로 정직성 갱신:** "프론트엔드는 없습니다 — JSON이 곧 UI"가 프론트 데모 추가로 거짓이 되므로 "백엔드가 헤드라인 + 브라우저 프론트 데모도 제공"으로 갱신. honest-data 에토스 + plan의 '두 길을 제시' 의도에 부합.
- **정적 서빙 v2 정직 기록:** 과대 약속 회피 — '범위 결정'임을 문서가 명시.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug/Inconsistency] 루트 README 인트로 문장 정직성 갱신**
- **Found during:** Task 2 (루트 README 프론트 데모 섹션 추가)
- **Issue:** 인트로 9번 줄 "프론트엔드는 없습니다 — JSON이 곧 UI입니다"가 같은 README에 프론트 데모 섹션을 추가하면서 자기모순(거짓 진술)이 됨. 면접관 대상 문서의 정직성·일관성을 해침.
- **Fix:** "백엔드가 헤드라인입니다 — JSON이 곧 API 표면입니다. … 같은 read API를 소비하는 브라우저 프론트 데모(3화면)도 선택적으로 제공합니다"로 1문장 갱신. 백엔드-우선 프레이밍 보존.
- **Files modified:** README.md (인트로 1줄)
- **Verification:** curl/한눈에/로컬실행/그외 섹션 앵커 전부 grep 보존 확인. plan의 '두 길을 제시(curl·브라우저)' 의도와 일치.
- **Committed in:** ffd5dbe (Task 2 commit)

**2. [Rule 1 - Bug] 루트 README stray 이중 빈 줄 복원**
- **Found during:** Task 2
- **Issue:** 세션 시작 시 워킹트리에 §'그 외 엔드포인트' 헤딩 아래 사고성 이중 빈 줄이 미커밋 상태로 존재(curl 섹션 영역).
- **Fix:** 단일 빈 줄로 복원 → curl/그외 구역이 HEAD와 정확히 일치(회귀 0).
- **Files modified:** README.md
- **Verification:** `git diff HEAD`에서 curl/그외 구역 무변경 확인(변경은 인트로 1줄 + 프론트 데모 섹션 추가뿐).
- **Committed in:** ffd5dbe (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 Rule 1 정합성/회귀 복원).
**Impact on plan:** 둘 다 문서 무결성·회귀 방지를 위한 최소 수정. curl 데모 표면 보존(ROADMAP 완료조건 3) 강화. 스코프 크립 없음.

## Issues Encountered
None. (build 청크 크기 경고는 기존 무관 경고.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- **Phase 11 완료** — DEMO-01(재현 검증·문서화)·DEMO-02(스크린샷+문서 표면) 충족. DEMO-03(정적 서빙)은 D-01로 v2 의도 연기.
- 데모 표면(시각 마감 + 스크린샷 3장 + 루트/frontend README)이 완성 — 면접관이 README만으로 'curl로도, 브라우저로도' 재현 가능.
- 권장 다음 단계: `/gsd-verify-work 11`(클린 클론 5분 재현 human walkthrough) → `/gsd-complete-milestone`(v1.1 Frontend Demo Dashboard).

---
*Phase: 11-demo-surface-docs*
*Completed: 2026-06-27*
