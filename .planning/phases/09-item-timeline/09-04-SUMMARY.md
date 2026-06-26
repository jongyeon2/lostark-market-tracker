---
phase: 09-item-timeline
plan: 04
subsystem: ui
tags: [recharts, line-chart, reference-line, event-markers, downsample, kst, timeline]

requires:
  - phase: 09-item-timeline
    provides: "09-01 recharts 의존성 + EVENT_MARKERS/MARKER_DASH/MARKER_STROKE_WIDTH/bucketWidthLabel 토큰"
  - phase: 07-frontend-foundation
    provides: "formatKst/toEpochMs off-by-9h 가드, timelineSchema(PricePoint/EventPoint), Badge 블록"
provides:
  - "PriceTimelineChart — min_price 라인 + eventType ReferenceLine 마커 + 커스텀 툴팁 (TIME-02/03/04)"
  - "EventMarkerLegend — 항상 4종 스와치+라벨 가로 범례 (D-01)"
  - "DownsampleBadge — '버킷 평균 · {ko}' 정직 배지, raw일 때 null (D-08)"
affects: [09-05]

tech-stack:
  added: []
  patterns:
    - "off-by-9h: x축 위치는 toEpochMs(UTC), tickFormatter/툴팁만 KST"
    - "다운샘플 이중 시각 신호: 점 유무(dot) + 배지"
    - "Recharts 단일 Tooltip 회피: 가격선=커스텀 Tooltip content, 마커=ReferenceLine label 내 SVG <title>"

key-files:
  created:
    - frontend/src/features/timeline/PriceTimelineChart.tsx
    - frontend/src/features/timeline/EventMarkerLegend.tsx
    - frontend/src/features/timeline/DownsampleBadge.tsx
  modified: []

key-decisions:
  - "마커 호버는 SVG <rect>+<title> 네이티브 툴팁으로 구현 — 차트당 Tooltip 1개 제약을 우회"
  - "x축 라벨은 KST M/D(en-US Asia/Seoul numeric), 툴팁은 formatKst 전체 KST"
  - "차트 prop은 Pick<Timeline,4>로 받되 미사용 필드(bucketWidth/Task1 events)는 destructure 점진화 (noUnusedLocals 준수)"

patterns-established:
  - "정성 데이터(eventType) 색은 단색 가격선 액센트와 다른 역할 — 점선 형태로 색 비의존 구분(접근성)"

requirements-completed: [TIME-02, TIME-03, TIME-04]

duration: 18min
completed: 2026-06-26
---

# Phase 9 Plan 04: 가격 라인 차트 + 이벤트 마커 Summary

**프로젝트 헤드라인 시각화 — UTC 위치/KST 라벨의 min_price 라인 위에 eventType별 점선 ReferenceLine 마커를 겹치고, 다운샘플을 점 유무+배지로 정직하게 신호하는 Recharts 차트 + 항상-4종 범례 + 정직 배지**

## Performance

- **Duration:** ~18 min
- **Completed:** 2026-06-26
- **Tasks:** 3
- **Files created:** 3

## Accomplishments
- `PriceTimelineChart`: `XAxis type="number" scale="time"`(UTC epoch 위치) + KST `tickFormatter`, compact ko-KR gold y축, accent blue `Line`(width 2), 다운샘플=점 없음/raw=점(≤60), 커스텀 `PriceTooltip`(KST + 전체 골드 + "n개 평균")
- 이벤트 마커: 각 이벤트를 `EVENT_MARKERS[type].color` 점선 `ReferenceLine`로, 호버 시 SVG `<title>`로 title+KST 표시(D-02)
- `EventMarkerLegend`: `Object.entries(EVENT_MARKERS)`로 항상 4종 스와치(색+형태)+라벨 가로 범례
- `DownsampleBadge`: downsampled일 때만 '버킷 평균 · {1시간/1일}' neutral 배지 + 정직 호버 툴팁
- 서사 정직성: 마커/툴팁 카피는 시점 겹침(상관)만 표현, 인과 단정 없음

## Task Commits

1. **Task 1: base price line chart** — `4d571b4` (feat)
2. **Task 2: event ReferenceLine markers + hover title + legend** — `945c6ba` (feat)
3. **Task 3: DownsampleBadge** — `922be20` (feat)

## Files Created/Modified
- `frontend/src/features/timeline/PriceTimelineChart.tsx` — Recharts LineChart + 마커 + 커스텀 툴팁
- `frontend/src/features/timeline/EventMarkerLegend.tsx` — 항상 4종 범례
- `frontend/src/features/timeline/DownsampleBadge.tsx` — 다운샘플 정직 배지

## Decisions Made
- Recharts 차트당 Tooltip 1개 제약 → 가격선은 커스텀 Tooltip content, 마커는 ReferenceLine label 내 SVG `<title>`로 분리(두 독립 호버)
- 마커 점선(MARKER_DASH '4 4')로 실선 가격선과 형태로도 구분(색 비의존, 접근성)
- 차트 미사용 prop(bucketWidth, Task1 단계 events)은 destructure 점진화로 noUnusedLocals/Parameters 준수

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - 컴파일/계약 정합] x축 라벨 포맷을 formatKst 문자열 트리밍 대신 전용 KST M/D 포매터로 구현**
- **Found during:** Task 1 (base chart)
- **Issue:** 플랜 action은 "use formatKst, trimming to M/D"를 제안하나, formatKst는 전체 KST datetime을 반환해 문자열 트리밍이 취약(로캘 포맷 의존)
- **Fix:** x축은 `Intl.DateTimeFormat('en-US',{ timeZone:'Asia/Seoul', month:'numeric', day:'numeric' })` 전용 포매터(KST M/D), 툴팁은 formatKst 전체 유지. 계약(잠금)은 "KST 라벨 + UTC 위치"이며 그대로 충족 — CONTEXT의 "x축 라벨 밀도/포맷은 Claude 재량" 범위
- **Files modified:** frontend/src/features/timeline/PriceTimelineChart.tsx
- **Verification:** npm run build 0 errors; 위치는 toEpochMs(UTC), 라벨만 KST 유지
- **Committed in:** 4d571b4 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 contract-정합 구현 선택)
**Impact on plan:** off-by-9h 계약·KST 표시 의도 그대로 충족, scope creep 없음.

## Issues Encountered
None. (편집 직후 LSP가 일시적으로 unused 진단을 보였으나 권위 있는 `tsc -b` 빌드에서 0 errors로 확인 — stale 진단.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 09-05가 `useTimeline` 데이터를 차트/범례/배지에 배선하고 차트 자체 `<AsyncBoundary>` + 상태 카피(D-09) 적용
- `npm run build` 그린, Java `src/` 무변경 확인

## Self-Check: PASSED

- key-files 디스크 존재: PriceTimelineChart.tsx, EventMarkerLegend.tsx, DownsampleBadge.tsx ✓
- `git log --grep="09-04"` 3 커밋 ✓
- 전 task `<acceptance_criteria>`/`<verification>` 재실행 통과 (greps: ResponsiveContainer/scale="time"/toEpochMs/notation compact/ReferenceLine/<title>/EVENT_MARKERS/Object.entries/bucketWidthLabel/버킷 평균/return null; npm run build 0 errors; no Java src/ change) ✓

---
*Phase: 09-item-timeline*
*Completed: 2026-06-26*
