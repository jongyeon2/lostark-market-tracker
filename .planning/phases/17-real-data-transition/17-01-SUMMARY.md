---
phase: 17-real-data-transition
plan: 01
subsystem: ui
tags: [react, tanstack-query, empty-state, collection-health, honest-data]

requires:
  - phase: 07-frontend-foundation
    provides: AsyncBoundary/EmptyState/LoadingState/ErrorState 상태 컴포넌트 + useCollectionHealth 훅
  - phase: 08-dashboard
    provides: HealthCard/StatusBadge (GET /api/health/collection 4-grade status 소비 선례) + ItemCard per-card 팬아웃
provides:
  - collection_run 상태 기반 '수집 중'/'데이터 없음' 단일 파생 헬퍼(collectionEmptyState.ts, D-03)
  - seed 하드코딩 제거된 실데이터 기준 EmptyState 공유 기본 copy(D-05)
  - 3화면(Dashboard·Timeline·Impact) collection-aware 빈 상태 표시(D-04)
affects: [17-03 real-data-verification, 18-deploy]

tech-stack:
  added: []
  patterns:
    - "collection_run 상태 → 프론트 빈 상태 판정 단일 출처(deriveCollectionEmptyKind)"

key-files:
  created:
    - frontend/src/lib/collectionEmptyState.ts
  modified:
    - frontend/src/components/state/EmptyState.tsx
    - frontend/src/features/dashboard/ItemCard.tsx
    - frontend/src/features/timeline/TimelinePage.tsx
    - frontend/src/features/impact/ImpactPage.tsx

key-decisions:
  - "D-03 판정 규칙: NO_RUNS/FAILED/AUTH_ERROR/RATE_LIMITED → 'no-data', 그 외(SUCCESS/PARTIAL/비종단) → 'collecting'. health undefined(pending)는 소비 측이 이미 처리하므로 안전 기본값 'collecting'"
  - "Impact events-empty는 collection-health를 프레이밍 입력으로만 사용(주 원인=관리자 이벤트 부재) — 새 훅 미도입, copy만 재편"

patterns-established:
  - "빈 상태 판정 단일 출처: 화면 컴포넌트가 CollectionHealth.status 리터럴을 각자 재해석하지 않고 deriveCollectionEmptyKind(kind)로만 분기"

requirements-completed: [REALDATA-03]

duration: 12min
completed: 2026-07-03
---

# Phase 17-01: 프론트 빈 상태 정직 표시 Summary

**collection_run 상태(GET /api/health/collection)를 무변경 소비하는 단일 파생 헬퍼로 3화면이 첫 수집 전/직후에도 빈 화면 대신 '수집 중'/'데이터 없음'을 정직히 구분 표시 — 백엔드 0줄, 새 의존성 0**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-07-03
- **Completed:** 2026-07-03
- **Tasks:** 3
- **Files modified:** 5 (신규 1 + 수정 4)

## Accomplishments
- **D-05**: `EmptyState.tsx`의 seed 하드코딩 기본 body(`'seed 프로파일 백엔드를 기동하면 … SPRING_PROFILES_ACTIVE=seed'`)를 실데이터 기준 중립 문구로 교체 — 공유 기본값이 더 이상 프로파일/env 명을 전제·노출하지 않음(T-17-01 완화)
- **D-03**: `collectionEmptyState.ts` 신설 — `CollectionHealth`를 입력받아 '수집 중'(SUCCESS/PARTIAL/비종단) vs '데이터 없음'(NO_RUNS/FAILED/진단 marker)을 판정하는 순수 함수. 3화면 빈 상태 판정 단일 출처
- **D-04 Dashboard**: `ItemCard`가 공유 `useCollectionHealth`(queryKey dedupe → 추가 네트워크 0)로 미수집 시 '최신가 수집 중' vs '데이터 없음' 구분 표시. 고정 '최신가 아직 없음' 제거, 인라인 매핑·딥링크·팬아웃 무회귀
- **D-04 Timeline**: `ChartArea` snapshots-empty를 '이 품목 시계열 수집 중' 프레이밍으로 재편, '최근 30일 보기'(onResetRange)·400/404/network 분기 보존
- **D-04 Impact**: `ImpactResults` events-empty body를 '전후 비교엔 더 많은 데이터·이벤트 필요'로 재편, per-row insufficient_data·에러 분기 무회귀

## Task Commits

1. **Task 1: EmptyState 실데이터 copy 교체(D-05) + collection_run 파생 헬퍼 신설(D-03)** - `b607f79` (feat)
2. **Task 2: Dashboard ItemCard collection-aware 최신가 빈 상태(D-04 Dashboard)** - `b526362` (feat)
3. **Task 3: Timeline·Impact 화면별 빈 상태 프레이밍(D-04 Timeline/Impact)** - `e2bc561` (feat)

## Files Created/Modified
- `frontend/src/lib/collectionEmptyState.ts` (신규) - `deriveCollectionEmptyKind`/`isCollecting` — collection_run 상태 → '수집 중'/'데이터 없음' 판정 단일 출처
- `frontend/src/components/state/EmptyState.tsx` - seed 하드코딩 제거된 실데이터 기준 기본 copy
- `frontend/src/features/dashboard/ItemCard.tsx` - per-card collection-aware 빈 상태
- `frontend/src/features/timeline/TimelinePage.tsx` - ChartArea '시계열 수집 중' 프레이밍
- `frontend/src/features/impact/ImpactPage.tsx` - ImpactResults '더 많은 데이터·이벤트 필요' 프레이밍

## Decisions Made
- 판정 임계값(Claude's Discretion): run 존재/status 값 기반. `status ∈ {NO_RUNS, FAILED}` 또는 `summaryMessage ∈ {AUTH_ERROR, RATE_LIMITED}`면 'no-data', 그 외는 'collecting'. health undefined는 'collecting' 기본
- Impact는 collection-health를 프레이밍 입력으로만 활용(주 공백 원인=관리자 이벤트 부재) — 새 훅 미도입, copy-only 재편으로 결정

## Deviations from Plan
None - plan executed exactly as written. (verify grep가 doc-comment 잔존 '최신가 아직 없음' 2곳을 잡아내 주석 문구를 중립 표현으로 정리 — 코드 동작 변화 없음)

## Issues Encountered
- Task 2 verify(`! grep '최신가 아직 없음'`)가 처음 실패 — JSX가 아니라 원본/신규 doc-comment 2곳에 해당 문자열이 남아 있었음. 주석을 'collection-aware line' 등 중립 표현으로 바꿔 0건 달성. 기능 영향 없음
- LSP가 `AdminConsolePage.tsx → './WatchlistSection'` 미해결 및 방금 추가한 심볼 'never read'를 일시 표시했으나 모두 stale 오탐 — 실제 `tsc -b && vite build` 그린으로 확인

## User Setup Required
None - no external service configuration required. (실키 검증은 Plan 03 체크포인트)

## Next Phase Readiness
- Plan 03(검증 게이트)이 이 산출물 위에서 라이브 확인: 깨끗한 볼륨 dev 첫 수집 전/직후 3화면 '수집 중' 정직 표시(REALDATA-02/03)
- 백엔드 src/ 0줄·새 npm 의존성 0 — D-08 불변 가드 정합

---
*Phase: 17-real-data-transition*
*Completed: 2026-07-03*
