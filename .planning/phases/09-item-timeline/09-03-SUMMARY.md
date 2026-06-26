---
phase: 09-item-timeline
plan: 03
subsystem: ui
tags: [react-router, url-state, searchparams, kst, date-input, timeline]

requires:
  - phase: 07-frontend-foundation
    provides: React Router 셸(useSearchParams), formatKst off-by-9h 가드, Button shadcn 블록
provides:
  - "useTimelineParams() — ?item=&from=&to= URL-as-state 훅, 기본 최근 30일 윈도우 (D-03/D-04)"
  - "RangeControls — 7/30/90일 프리셋 + KST 인식 native date 입력, controlled 컴포넌트 (TIME-05)"
affects: [09-05, 10-event-impact]

tech-stack:
  added: []
  patterns:
    - "URL searchParams = 필터 상태 단일 출처 (딥링크·스크린샷 재현)"
    - "off-by-9h 가드를 입력 경계까지 확장: KST 달력일 표시 ↔ UTC instant 송신"
    - "controlled 컴포넌트 + 라우터 지식 분리 → Phase 10 재사용 가능"

key-files:
  created:
    - frontend/src/features/timeline/useTimelineParams.ts
    - frontend/src/features/timeline/RangeControls.tsx
  modified: []

key-decisions:
  - "from/to는 ISO-8601 UTC instant 문자열로 유지 (백엔드 ISO.DATE_TIME 계약)"
  - "기본 윈도우 최근 30일은 반환하되 URL에 즉시 기록하지 않음 (bare /timeline 클린 유지)"
  - "to<=from 픽은 클라이언트에서 막지 않음 — 400 경로를 09-05에서 도달 가능하게 유지"

patterns-established:
  - "날짜 입력 KST 경계 변환: 시작일 → {date}T00:00:00+09:00, 종료일 → {date}T23:59:59+09:00 → toISOString"

requirements-completed: [TIME-05]

duration: 9min
completed: 2026-06-26
---

# Phase 9 Plan 03: 시간범위 상태 레이어 Summary

**URL searchParams를 단일 출처로 하는 useTimelineParams 훅(기본 30일) + KST 경계에서 UTC instant로 변환하는 7/30/90일 프리셋·날짜 입력 RangeControls**

## Performance

- **Duration:** ~9 min
- **Completed:** 2026-06-26
- **Tasks:** 2
- **Files created:** 2

## Accomplishments
- `useTimelineParams()`: `?item=&from=&to=` 읽기/쓰기, from/to 부재 시 최근 30일 ISO instant 기본값(D-03), `setItem`/`setRange`가 기존 파라미터 병합(D-04)
- `RangeControls`: 7/30/90일 프리셋(활성=accent outline, 비활성=secondary) + 시작일/종료일 native `<input type="date">`
- off-by-9h 가드 입력 경계 적용: 표시값은 `Asia/Seoul` 달력일, onChange는 KST 일 경계 UTC instant로 환원
- `to<=from` 픽 통과 허용 → 400 경로(09-05)가 도달 가능

## Task Commits

1. **Task 1: useTimelineParams URL-state hook** — `85a8a89` (feat)
2. **Task 2: RangeControls (presets + KST date inputs)** — `971e2bd` (feat)

## Files Created/Modified
- `frontend/src/features/timeline/useTimelineParams.ts` — URL-as-state 훅 (itemId, from, to, setItem, setRange)
- `frontend/src/features/timeline/RangeControls.tsx` — controlled 프리셋/날짜 컨트롤 (from/to/onRangeChange)

## Decisions Made
- 기본 30일을 URL에 eager-write 하지 않아 bare `/timeline` URL을 깨끗하게 유지
- 프리셋 활성 판정은 현재 span(일) 대비 ±1일 허용 오차로 비교
- 날짜 입력 KST→UTC 변환은 `Intl`(en-CA, Asia/Seoul) 기반, 수동 +9h 시프트 금지

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 09-05가 `useTimelineParams` ↔ `RangeControls`를 배선하고 `?item=`을 ItemSelect와 연결
- `npm run build` 그린, Java `src/` 무변경 확인

## Self-Check: PASSED

- key-files 디스크 존재: useTimelineParams.ts, RangeControls.tsx ✓
- `git log --grep="09-03"` 2 커밋 ✓
- 전 task `<acceptance_criteria>`/`<verification>` 재실행 통과 (greps, npm run build 0 errors, to<=from 비차단, no Java src/ change) ✓

---
*Phase: 09-item-timeline*
*Completed: 2026-06-26*
