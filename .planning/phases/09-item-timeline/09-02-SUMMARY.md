---
phase: 09-item-timeline
plan: 02
subsystem: ui
tags: [shadcn-select, react-query, async-boundary, shared-components, timeline]

requires:
  - phase: 09-item-timeline
    provides: "09-01 shadcn select 블록 (frontend/src/components/ui/select.tsx)"
  - phase: 07-frontend-foundation
    provides: "useItems/useLatestPrice 훅, AsyncBoundary, Card/Skeleton 블록, formatKst"
provides:
  - "ItemSelect — controlled 드롭다운 품목 셀렉터 (D-05), _shared 공용 (Phase 9·10 재사용)"
  - "LatestPriceCard — 타임라인 경량 최신가 카드, 자체 useLatestPrice + AsyncBoundary (D-07)"
affects: [09-05, 10-event-impact]

tech-stack:
  added: []
  patterns:
    - "_shared 폴더에 셀렉터/카드 추출 → 의존성이 impact→timeline로 흐르지 않음 (D-07)"
    - "controlled 컴포넌트(value/onChange) + 라우터 무지 → Phase 10 동일 import 재사용"
    - "radix select 문자열 값 ↔ number 경계 변환"

key-files:
  created:
    - frontend/src/features/_shared/ItemSelect.tsx
    - frontend/src/features/_shared/LatestPriceCard.tsx
  modified: []

key-decisions:
  - "셀렉터 실패는 inline 처리(disabled trigger + '품목을 불러오지 못했어요') — 화면 ErrorState로 전체 블랭크 금지"
  - "error 상태에서도 SelectTrigger를 <Select disabled>로 감싸 radix 컨텍스트 크래시 회피"
  - "최신가는 toLocaleString('ko-KR') 전체 자릿수 + ' G' (축약은 차트 y축 소관)"

patterns-established:
  - "단일 선택 화면 경량 카드: identity(displayName)는 항상 보이고 가격 영역만 AsyncBoundary로 격리"

requirements-completed: [TIME-01]

duration: 8min
completed: 2026-06-26
---

# Phase 9 Plan 02: 공용 셀렉터 + 최신가 카드 Summary

**Phase 9·10이 공유하는 controlled ItemSelect 드롭다운 + 자체 AsyncBoundary로 격리된 타임라인 경량 LatestPriceCard — TIME-01의 "선택 품목 → 최신가 카드" 절반 전달**

## Performance

- **Duration:** ~8 min
- **Completed:** 2026-06-26
- **Tasks:** 2
- **Files created:** 2

## Accomplishments
- `ItemSelect`: `useItems()` 옵션, radix `Select`에 string↔number 경계 변환, pending=Skeleton·error=disabled trigger+힌트 inline 처리
- `LatestPriceCard`: `useLatestPrice(itemId)` + 자체 `<AsyncBoundary onRetry>`, 품목명(Heading)/최신가(tabular-nums 전체 자릿수+" G")/수집시각(KST) 수직 배치
- 둘 다 `frontend/src/features/_shared/`에 두어 D-07(impact↔timeline 공유) 실현
- 사전 구축된 데이터 레이어(Phase 7) 배당: 성공 경로만 작성, 단위 실패 격리는 자동 상속

## Task Commits

1. **Task 1: ItemSelect (controlled dropdown)** — `35120f6` (feat)
2. **Task 2: LatestPriceCard (lightweight card)** — `bba97df` (feat)

## Files Created/Modified
- `frontend/src/features/_shared/ItemSelect.tsx` — controlled 드롭다운 (value/onChange, useItems)
- `frontend/src/features/_shared/LatestPriceCard.tsx` — 경량 최신가 카드 (useLatestPrice + 자체 AsyncBoundary)

## Decisions Made
- 셀렉터 실패를 화면 전체 ErrorState로 올리지 않고 inline 힌트로 격리(페이지 블랭크 방지)
- radix `SelectTrigger`는 `Select.Root` 컨텍스트 필요 → error 경로도 `<Select disabled>`로 래핑
- 최신가는 전체 자릿수 표기(축약 금지) — 축약은 차트 y축의 역할

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. (error 상태 disabled trigger를 radix 컨텍스트 안전하게 `<Select disabled>`로 감싼 것은 계약상 "disabled trigger" 요구의 정상 구현.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 09-05가 `ItemSelect`를 `useTimelineParams.setItem`에, `LatestPriceCard`를 선택 품목에 배선
- `npm run build` 그린, Java `src/` 무변경 확인

## Self-Check: PASSED

- key-files 디스크 존재: ItemSelect.tsx, LatestPriceCard.tsx ✓
- `git log --grep="09-02"` 2 커밋 ✓
- 전 task `<acceptance_criteria>`/`<verification>` 재실행 통과 (greps, npm run build 0 errors, _shared 위치, no Java src/ change) ✓

---
*Phase: 09-item-timeline*
*Completed: 2026-06-26*
