---
phase: 09-item-timeline
plan: 01
subsystem: ui
tags: [recharts, radix-select, shadcn, design-tokens, react, typescript]

requires:
  - phase: 07-frontend-foundation
    provides: shadcn/ui setup (slate/new-york), cn 유틸, eventTypeSchema(EventType enum), Vite+React+TS 스택
provides:
  - "recharts ^3 + @radix-ui/react-select ^2 의존성 (TIME-02 차트 라이브러리 + D-05 셀렉터 프리미티브)"
  - "손수 작성한 shadcn new-york select 블록 (frontend/src/components/ui/select.tsx)"
  - "EVENT_MARKERS — eventType→{color,koLabel} 단일 출처 (D-01 마커 팔레트)"
  - "bucketWidthLabel — 백엔드 hour/day를 한국어로 환원 (D-08, UI-SPEC '1h' 초안 정정)"
  - "MARKER_DASH / MARKER_STROKE_WIDTH — 점선 마커 스타일 토큰"
affects: [09-02, 09-04, 10-event-impact]

tech-stack:
  added: [recharts@^3, "@radix-ui/react-select@^2"]
  patterns:
    - "Windows shadcn add 우회: select 블록을 손수 src/components/ui/에 작성 (CLI의 literal @/ 디렉터리·radix-ui umbrella 버그 회피)"
    - "Record<EventType, …>로 색 매핑을 enum에 잠가 enum drift를 컴파일 에러로 전환"

key-files:
  created:
    - frontend/src/components/ui/select.tsx
    - frontend/src/features/timeline/eventMarkers.ts
  modified:
    - frontend/package.json
    - frontend/package-lock.json

key-decisions:
  - "recharts ^3.9.0 + @radix-ui/react-select ^2.3.1 설치, radix-ui umbrella 패키지는 추가하지 않음"
  - "select 블록은 npx shadcn add 대신 손수 작성 (Windows CLI 버그 회피)"
  - "bucketWidthLabel: 백엔드 실측값 hour/day를 '1시간'/'1일'로, 그 외는 raw 폴백 (정직성)"

patterns-established:
  - "디자인 토큰 단일 출처: 마커 색·라벨·버킷 카피를 eventMarkers.ts 한 곳에 잠그고 09-02/09-04가 소비"

requirements-completed: [TIME-02, TIME-03, TIME-04]

duration: 12min
completed: 2026-06-26
---

# Phase 9 Plan 01: 의존성 + 디자인 토큰 기반 Summary

**recharts·radix-select 설치 + 손수 작성한 shadcn select 블록 + 이벤트 마커 4색 팔레트/버킷 라벨 토큰 — Phase 9의 나머지 화면이 의존하는 기반을 한 번에 잠금**

## Performance

- **Duration:** ~12 min
- **Completed:** 2026-06-26
- **Tasks:** 3
- **Files created:** 2 / **modified:** 2

## Accomplishments
- `recharts@^3.9.0`, `@radix-ui/react-select@^2.3.1`를 dependencies에 추가 (umbrella 패키지 없음, npm install 그린)
- shadcn new-york `select` 블록을 손수 작성 — `@radix-ui/react-select` 위 styled wrapper, `data-slot` 속성, `cn`, lucide 아이콘으로 기존 card/badge/navigation-menu 컨벤션과 정합
- `eventMarkers.ts`에 `EVENT_MARKERS`(4색+라벨, D-01), `MARKER_DASH/STROKE`, `bucketWidthLabel`(D-08) 잠금
- 계약↔백엔드 정합: UI-SPEC 초안의 "1h"가 아니라 백엔드 실측 `hour`/`day`를 한국어로 환원

## Task Commits

1. **Task 1: recharts + radix-select 의존성 추가** — `97aa193` (chore)
2. **Task 2: shadcn select 블록 손수 작성** — `f764452` (feat)
3. **Task 3: 이벤트 마커 팔레트 + 버킷 라벨 토큰** — `cf0128e` (feat)

## Files Created/Modified
- `frontend/package.json` / `package-lock.json` — recharts + @radix-ui/react-select 추가
- `frontend/src/components/ui/select.tsx` — 손수 작성한 new-york Select 패밀리 (Select/Trigger/Value/Content/Item/Group/Label/Separator/ScrollButtons)
- `frontend/src/features/timeline/eventMarkers.ts` — EVENT_MARKERS + bucketWidthLabel + 마커 스타일 토큰

## Decisions Made
- npx shadcn add 대신 손수 작성으로 Windows CLI 버그(literal `@/` 디렉터리, radix-ui umbrella 주입) 회피
- `EVENT_MARKERS`를 `Record<EventType, …>`로 타이핑 → enum 멤버 변경 시 컴파일 에러로 강제 정합
- `bucketWidthLabel`을 hour/day 환원의 단일 지점으로 두어 09-04 배지가 정직하게 렌더

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. (lucide-react ^1.21.0 특이 버전이나 `ChevronDownIcon`/`CheckIcon`/`ChevronUpIcon` 모두 존재 확인 후 사용.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 09-02(ItemSelect)가 `select.tsx`를, 09-04(차트/범례/배지)가 `eventMarkers.ts`를 즉시 소비 가능
- `npm run build`(tsc -b + vite build) 그린, Java `src/` 무변경 확인

## Self-Check: PASSED

- key-files 디스크 존재 확인: select.tsx, eventMarkers.ts ✓
- `git log --grep="09-01"` ≥3 커밋 ✓
- 전 task `<acceptance_criteria>` / `<verification>` 재실행 통과 (deps resolve, grep, npm run build 0 errors, no Java src/ change) ✓

---
*Phase: 09-item-timeline*
*Completed: 2026-06-26*
