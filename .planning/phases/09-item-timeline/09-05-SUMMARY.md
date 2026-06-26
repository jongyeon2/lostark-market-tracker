---
phase: 09-item-timeline
plan: 05
subsystem: ui
tags: [timeline, composition, url-state, error-handling, async-boundary, react-query]

requires:
  - phase: 09-item-timeline
    provides: "09-02 ItemSelect/LatestPriceCard, 09-03 useTimelineParams/RangeControls, 09-04 PriceTimelineChart/EventMarkerLegend/DownsampleBadge"
  - phase: 07-frontend-foundation
    provides: "useItems/useTimeline 훅, ApiError(status), ErrorState/Alert 블록, React Router 셸"
provides:
  - "TimelinePage — 셀렉터+카드+컨트롤+차트를 useTimelineParams에 배선한 완성 타임라인 화면 (TIME-01)"
  - "ApiError.status 기반 400/404/200-empty 구분 처리 + '최근 30일 보기' CTA (TIME-05, D-09)"
  - "D-06 진입 기본 선택(?item= 우선, 없으면 첫 품목 자동) + D-03 최근 30일 기본 윈도우"
affects: [10-event-impact, 11-demo-surface]

tech-stack:
  added: []
  patterns:
    - "차트/최신가 카드 독립 에러 스코프 — 한쪽 실패가 다른 쪽을 가리지 않음"
    - "ApiError.status 분기로 200-empty/400/404/네트워크를 각각 구분된 카피로 처리"
    - "URL searchParams 단일 출처 + 진입 자동 선택으로 데모 친화 비어있지 않은 첫 화면"

key-files:
  created: []
  modified:
    - frontend/src/features/timeline/TimelinePage.tsx

key-decisions:
  - "차트 영역은 itemId != null일 때만 렌더(가드)하여 useTimeline이 유효 id로만 fetch"
  - "200-empty/네트워크는 공용 상태 패턴 유지, 400/404만 타임라인 전용 inline Alert(고유 heading 필요, D-09)"
  - "체크포인트 폴리시: 400/404 Alert 폭 max-w-xl→max-w-md (UI 사이징만, 로직 불변)"

patterns-established:
  - "상태 정직성: 일반 '오류 발생' 대신 status별 문제+다음 행동 카피 (honest-data 에토스의 화면 연장)"

requirements-completed: [TIME-01, TIME-05]

duration: 22min
completed: 2026-06-26
---

# Phase 9 Plan 05: 타임라인 화면 조립 Summary

**셀렉터+최신가 카드+기간 컨트롤+이벤트 마커 차트를 useTimelineParams(URL 상태)에 배선한 완성 화면 — 진입 시 자동 선택으로 비어있지 않고, ApiError.status로 400/404/200-empty를 각각 정직하게 구분 (TIME-01/05, human-verified)**

## Performance

- **Duration:** ~22 min (체크포인트 검증 포함)
- **Completed:** 2026-06-26
- **Tasks:** 3 (2 auto + 1 blocking human-verify checkpoint)
- **Files modified:** 1

## Accomplishments
- `TimelinePage`가 Phase-7 스켈레톤을 대체: 제목 → 컨트롤 바(ItemSelect+RangeControls) → LatestPriceCard → 차트 영역
- `useTimelineParams` ↔ `ItemSelect.onChange`/`RangeControls.onRangeChange`/`useTimeline(from,to)` 배선
- D-06 진입 기본 선택: `?item=` 우선, 없으면 `useItems()` 첫 품목 자동(effect) → 진입 즉시 비어있지 않은 차트
- D-09 상태 정직성: `ApiError.status` 분기로 400('조회 기간을 다시 확인…')/404('존재하지 않는 품목…')/200-empty('이 기간에는 시세 데이터가 없어요'+'최근 30일 보기' CTA)/네트워크를 각각 구분
- 차트·최신가 카드 독립 에러 스코프 유지
- **Human-verify 체크포인트 통과**: 마커 서사·다운샘플 정직성·KST off-by-9h 정렬·3종 TIME-05 상태 모두 사용자 확인(approved)

## Task Commits

1. **Task 1: compose page + wire URL state + default selection** — `26f61ae` (feat)
2. **Task 2: chart area with status-branched 400/404/200-empty** — `1c00cdb` (feat)
3. **Task 3: human-verify checkpoint** — 승인됨; 체크포인트 폴리시 `8bea4d8` (style)

## Files Created/Modified
- `frontend/src/features/timeline/TimelinePage.tsx` — 완성 타임라인 화면(조립 + URL 배선 + 상태 분기 + 폴리시)

## Decisions Made
- 차트 영역은 `itemId != null` 가드로 렌더(유효 id로만 useTimeline fetch)
- 고유 heading이 필요한 400/404는 타임라인 전용 inline Alert(공용 ErrorState는 heading 고정이라 부적합, D-09 NOTE)
- 200-empty/네트워크 에러는 앱 전역 공용 상태 패턴과 일관 유지

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 4 - 사용자 요청 / UI 폴리시] 400·404 Alert 폭 축소**
- **Found during:** Task 3 (human-verify 체크포인트 리뷰)
- **Issue:** 400 상태 알림 박스가 `max-w-xl`(576px)로 넓어 짧은 문구 대비 내부 여백이 커 최신가 카드 아래 "큰 빈 박스"처럼 보임(사용자 지적, 화면 균형 어색)
- **Fix:** 400·404 inline Alert를 `max-w-md`(448px)로 축소, 좌측(컨트롤 바 기준선) 정렬 유지. 문구·status 분기 로직 불변(사용자 명시: UI 배치/크기만)
- **Files modified:** frontend/src/features/timeline/TimelinePage.tsx
- **Verification:** npm run build 0 errors; 사용자 재확인 "approved"
- **Committed in:** 8bea4d8 (style)

---

**Total deviations:** 1 (체크포인트에서 사용자 요청 UI 폴리시)
**Impact on plan:** 기능 로직 불변, 시각 균형만 개선. scope creep 없음.

## Issues Encountered
None. (체크포인트 폴리시 1건은 사용자 요청으로 정상 처리 후 재승인.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 9 (Item Timeline) 5개 플랜 전부 완료, TIME-01~05 전달 + human-verified
- Phase 10(Event Impact)이 `_shared` ItemSelect/LatestPriceCard + useTimelineParams 재사용 가능(D-07)
- `npm run build` 그린, Java `src/` 무변경, /timeline 라우트·AppLayout 불변(페이지 본문만 교체)

## Self-Check: PASSED

- key-files 디스크 존재: TimelinePage.tsx(교체됨) ✓
- `git log --grep="09-05"` 2 feat + 1 style 커밋 ✓
- 전 task `<acceptance_criteria>`/`<verification>` 재실행 통과 (greps: useTimelineParams/ItemSelect/RangeControls/[0].id/ApiError/useTimeline/존재하지 않는 품목/이 기간에는…/최근 30일 보기; npm run build 0 errors; no Java src/ change) ✓
- Task 3 blocking human-verify: 사용자 "approved" ✓

---
*Phase: 09-item-timeline*
*Completed: 2026-06-26*
