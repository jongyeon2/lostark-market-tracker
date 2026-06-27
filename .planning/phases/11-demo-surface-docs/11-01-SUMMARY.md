---
phase: 11-demo-surface-docs
plan: 01
subsystem: ui
tags: [react, tailwind, recharts, async-boundary, responsive, audit]

# Dependency graph
requires:
  - phase: 07-frontend-foundation
    provides: AsyncBoundary + LoadingState/EmptyState/ErrorState 공용 상태 컴포넌트, AppLayout 컨테이너(max-w-7xl px-8)
  - phase: 08-dashboard
    provides: DashboardPage(HealthCard + 반응형 ItemCard 그리드)
  - phase: 09-item-timeline
    provides: TimelinePage(ChartArea 인라인 상태 분기 + PriceTimelineChart ResponsiveContainer)
  - phase: 10-event-impact
    provides: ImpactPage(표↔카드 전환 hidden md:block / md:hidden, D-04)
provides:
  - 3화면 시각 마감 audit-and-fix 점검 결과(로딩/빈/에러 + 데스크톱/좁은폭 반응형 일관성 확인)
  - 점검 결과 전 화면 이미 일관 → frontend/src 무변경(클린 audit), build green로 회귀 차단 검증
affects: [11-02-screenshots, 11-03-docs, verify-work]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "감사 결과 무변경(no-op audit): Phase 7~10 human-verified 화면은 이미 일관 → 명백한 회귀만 수정(D-07)이라 수정 항목 0"
    - "상태 격리 2형태 공존: 단순 스코프=AsyncBoundary(status/isEmpty/onRetry), HTTP 상태별 정직 카피가 필요한 스코프(ChartArea/ImpactResults)=의도적 인라인 분기 + 공용 ErrorState 폴백(D-09)"

key-files:
  created:
    - .planning/phases/11-demo-surface-docs/11-01-SUMMARY.md
  modified: []

key-decisions:
  - "audit 결과 3화면 모두 이미 일관 → frontend/src 0줄 변경. 불필요한 diff를 만들지 않는 것이 D-07(리디자인 아님) 준수"
  - "Timeline ChartArea / Impact ImpactResults의 인라인 상태 분기는 회귀가 아니라 의도적 설계(D-09 400/404/200-empty 정직 카피, D-11 계약 헤딩)로 판정 — 공용 컴포넌트로 강제 정렬하지 않음"

patterns-established:
  - "no-op audit 증거화: 수정이 없어도 화면×상태 점검 체크리스트를 SUMMARY에 표로 남겨 '점검을 실제로 했음'을 증명"

requirements-completed: [DEMO-01]

# Metrics
duration: ~12 min
completed: 2026-06-27
---

# Phase 11 Plan 01: 데모 표면 시각 마감 (audit-and-fix) Summary

**Dashboard / Item Timeline / Event Impact 3화면의 로딩·빈·에러 상태 일관성과 데스크톱 우선 반응형(좁은폭 무붕괴)을 순회 점검 — 명백한 불일치·회귀가 없어 frontend/src 무변경, build green으로 회귀 차단 검증.**

## Performance

- **Duration:** ~12 min
- **Tasks:** 2 (둘 다 점검 결과 무수정)
- **Files modified:** 0 (frontend/src), 1 created (SUMMARY)

## Accomplishments
- 3화면 × (로딩/빈/에러) 상태 렌더링 일관성 점검 — 전부 공용 컴포넌트 또는 그것을 정확히 미러링하는 의도적 인라인 분기 사용
- 3화면 × (데스크톱 ~1440px / 좁은폭 ~768px) 반응형 점검 — 가로 스크롤·요소 겹침·표 오버플로 없음
- `cd frontend && npm run build`(tsc -b + vite build) 타입 에러 0 통과 — 회귀 없음 증명
- 다운스트림(11-02 스크린샷·11-03 README)이 "잘 나온 최종 화면"을 전제할 수 있음을 확정

## 점검 체크리스트 — Task 1: 상태 렌더링 일관성

| 화면 | 로딩 | 빈(empty) | 에러 | 판정 |
|------|------|-----------|------|------|
| Dashboard (item grid) | `AsyncBoundary`→`LoadingState`(skeleton) | `AsyncBoundary`→`EmptyState` | `AsyncBoundary`→`ErrorState(onRetry)` | ✅ 일관 (수정 불요) |
| Dashboard (HealthCard) | 자체 boundary(독립) | n/a | 자체 boundary | ✅ 일관 (D-07/D-08 독립 격리 의도) |
| Item Timeline (LatestPriceCard) | `AsyncBoundary`→`LoadingState` | n/a(단일값) | `AsyncBoundary`→`ErrorState` | ✅ 일관 |
| Item Timeline (ChartArea) | 인라인 차트 skeleton(`h-[360px]` + 동일 "불러오는 중…") | 인라인 "기간 내 데이터 없음" + 최근30일 CTA (EmptyState와 동일 클래스 `py-16/gap-4/max-w-md`) | 400/404 구분 카피 + 네트워크는 공용 `ErrorState` 폴백 | ✅ 일관 — 인라인은 D-09 정직 카피 의도(회귀 아님) |
| Event Impact (LatestPriceCard) | `AsyncBoundary`→`LoadingState` | n/a | `AsyncBoundary`→`ErrorState` | ✅ 일관 |
| Event Impact (ImpactResults) | 인라인 결과 skeleton(`h-[320px]` + 동일 helper) | 인라인 "등록된 이벤트가 없어요"(10-UI-SPEC 계약 헤딩, EmptyState와 동일 클래스) | 400/404 구분 카피 + 네트워크는 공용 `ErrorState` 폴백 | ✅ 일관 — D-11 계약 헤딩·D-09 정직 카피 의도 |

**공용 상태 컴포넌트 자체 점검:** `LoadingState`/`EmptyState`/`ErrorState`가 동일 토큰(`py-16`, `gap-4`, `text-muted-foreground`, `max-w-md`) 사용 — 화면 간 padding/아이콘/텍스트 정렬 들쭉날쭉 없음. 정렬 수정 불요.

## 점검 체크리스트 — Task 2: 데스크톱 우선 반응형 + 간격/정렬

| 점검 항목 | 결과 |
|-----------|------|
| 컨테이너 일관 | 3화면 모두 `AppLayout`의 `max-w-7xl px-8 py-8` 단일 컨테이너 소비 ✅ |
| 수직 리듬 일관 | 3화면 최상위 모두 `space-y-6` ✅ |
| H1 타이포 일관 | 3화면 모두 `text-[28px] leading-tight font-semibold` ✅ |
| 컨트롤바 gap | Timeline/Impact 모두 `flex flex-wrap items-end gap-4` ✅ |
| 좁은폭 — Dashboard 그리드 | `grid-cols-1 sm:grid-cols-2 lg:grid-cols-3` → 좁은폭 1열 collapse, 가로 스크롤 없음 ✅ |
| 좁은폭 — 컨트롤바 | `flex-wrap`으로 줄바꿈 ✅ |
| 좁은폭 — Event Impact 표 | `hidden md:block`(표) ↔ `md:hidden`(카드) 전환(D-04) → 좁은폭 표 오버플로 없음 ✅ |
| 좁은폭 — Timeline 차트 | `ResponsiveContainer width="100%" height={360} minHeight={280}` → 폭에 맞게 축소 ✅ |
| 좁은폭 — LatestPriceCard | `w-fit min-w-56`(224px < 768px) → 겹침/오버플로 없음 ✅ |

모바일 완성형 브레이크포인트(Recharts·네비 모바일 대응)는 D-08에 따라 **추가하지 않음**.

## Task Commits

코드 변경이 없어 per-task 프로덕션 커밋 없음(클린 audit). 본 plan의 유일 산출물은 점검 결과 SUMMARY.

**Plan metadata:** SUMMARY + 추적 파일 커밋 (docs: complete plan)

## Files Created/Modified
- `.planning/phases/11-demo-surface-docs/11-01-SUMMARY.md` — 화면×상태 점검 체크리스트(증거)
- frontend/src/** — **무변경**(audit 결과 명백한 불일치·회귀 없음)

## Decisions Made
- **무변경이 정답:** D-07은 "리디자인이 아니라 명백한 회귀·불일치만 수정". Phase 7~10이 human-verified라 점검 결과 수정 항목 0 → 불필요한 diff를 만들지 않음.
- **인라인 분기 ≠ 회귀:** ChartArea/ImpactResults의 인라인 상태 분기는 HTTP 상태별 정직 카피(D-09)·계약 헤딩(D-11)을 위한 의도적 설계. 공용 AsyncBoundary로 강제 정렬하면 정직성이 후퇴하므로 보존.

## Deviations from Plan

None - plan executed exactly as written. (점검 결과 수정 대상 불일치가 발견되지 않아 무수정으로 종료한 것은 plan이 명시적으로 허용한 경로 — "수정 없이 이미 일관한 화면은 그대로 둔다".)

## Issues Encountered
None. (build 청크 크기 경고는 기존부터 존재하던 무관 경고 — 타입 에러 아님.)

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 3화면 시각 마감이 일관(이미 일관) 확인됨 → 11-02 스크린샷이 담을 "잘 나온 최종 화면" 전제 충족.
- 백엔드 `src/` 0줄 변경, 데이터 계약·라우팅·URL 상태 정책 무변경(Phase 7~10 결정 보존).
- DEMO-01의 "재현 검증" 축은 11-03 README dry-run에서 최종 확정(문서-실행 일치·5분 재현).

---
*Phase: 11-demo-surface-docs*
*Completed: 2026-06-27*
