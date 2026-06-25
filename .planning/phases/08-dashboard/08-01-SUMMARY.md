---
phase: 08-dashboard
plan: 01
subsystem: ui
tags: [react, shadcn, tailwind, lucide, badge, status]

# Dependency graph
requires:
  - phase: 07-frontend-foundation
    provides: "shadcn new-york/slate init, components.json, @ alias, cn 헬퍼, --up/--down/--warning/--neutral 시맨틱 토큰(index.css), button/card/skeleton/alert/navigation-menu 블록, collectionHealthSchema(status/summaryMessage)"
provides:
  - "shadcn 공식 badge 블록(frontend/src/components/ui/badge.tsx) — 07-01에서 미룬 것을 추가, Registry Safety PASS"
  - "StatusBadge — health status 4값(SUCCESS/PARTIAL_SUCCESS/FAILED/NO_RUNS)을 시맨틱 색+한국어 라벨+lucide 아이콘 배지 1개로 매핑, unknown은 원문 라벨 neutral 폴백 (DASH-02 / D-05)"
  - "SummaryMarker — summaryMessage(AUTH_ERROR/RATE_LIMITED)를 고정 카테고리컬 라벨로, null/미지값은 미표시 — 시크릿 비노출 (DASH-02 / D-06)"
affects: [08-02 HealthCard, 08-03 DashboardPage]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "표현 전용 프리미티브: props in, useQuery/fetch/zod 없음 — 데이터 와이어링은 08-02/08-03 소관"
    - "상태 매핑 테이블(Record<string, Meta>) + unknown 폴백 — 도메인 값 누락/미지를 라벨로 노출(honest-data)"
    - "시맨틱 색 = Badge variant 위에 className으로 tailwind-merge 우선 적용(bg-up/10 text-up 등), accent(blue-600) 아님"

key-files:
  created:
    - frontend/src/components/ui/badge.tsx
    - frontend/src/features/dashboard/StatusBadge.tsx
    - frontend/src/features/dashboard/SummaryMarker.tsx
  modified: []

key-decisions:
  - "shadcn CLI가 통합 radix-ui 패키지를 새 의존성으로 추가하려 해 거부 — 기존 @radix-ui/react-slot(button.tsx가 이미 사용)로 Slot import를 교체해 '신규 의존성 0' must_have 충족"
  - "StatusBadge는 Badge variant=secondary + 시맨틱 className 합성으로 '연한 배경+진한 텍스트' 구현(tailwind-merge가 className 우선)"
  - "SummaryMarker는 알려진 카테고리만 매핑하고 그 외/null은 return null — 원문 summaryMessage를 절대 DOM에 echo하지 않아 시크릿 누출 경로 자체가 없음(T-0801-01 완화)"

patterns-established:
  - "feature/dashboard 표현 컴포넌트: 매핑 테이블 + unknown 폴백 + 아이콘+텍스트 라벨 동반(접근성, 색-only 금지)"

requirements-completed: [DASH-02]

# Metrics
duration: 6 min
completed: 2026-06-25
---

# Phase 08 Plan 01: Status Presentation Primitives Summary

**health status 4등급(정상/일부 실패/전체 실패/수집 대기)을 시맨틱 색+한국어 라벨+lucide 아이콘 배지 하나로 표현하는 `StatusBadge`와, summaryMessage를 시크릿 없이 카테고리컬 라벨로만 노출하는 `SummaryMarker` — 공식 shadcn badge 블록 위에 구축한 순수 표현 프리미티브.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-06-25T12:33:04Z
- **Completed:** 2026-06-25T12:38:58Z
- **Tasks:** 3
- **Files modified:** 3 (생성)

## Accomplishments
- 07-01에서 미뤘던 공식 shadcn `badge` 블록을 `src/components/ui/badge.tsx`에 추가(Registry Safety PASS) — 08-02 health 카드가 합성 가능
- `StatusBadge`: 백엔드 실측 4값을 08-UI-SPEC 매핑(SUCCESS→정상/up/CircleCheck, PARTIAL_SUCCESS→일부 실패/warning/TriangleAlert, FAILED→전체 실패/down/CircleX, NO_RUNS→수집 대기/neutral/Clock)으로 렌더, 미지값은 원문 라벨 neutral 폴백 (D-05, FAILED를 별도 '위험' 등급으로 유지)
- `SummaryMarker`: AUTH_ERROR→'인증 오류'(down), RATE_LIMITED→'레이트리밋'(warning), null/미지→미표시 — 원문 값 echo 없음으로 시크릿 비노출을 화면 레벨에서 증명 (D-06)
- 두 컴포넌트 모두 표현 전용(useQuery/fetch/zod 없음), 모든 배지/마커에 텍스트 라벨 동반(색-only/아이콘-only 금지), `npm run build` 무오류, Java `src/` 무변경

## Task Commits

각 태스크는 원자적으로 커밋:

1. **Task 1: shadcn `badge` 블록 추가** - `21b635e` (feat)
2. **Task 2: StatusBadge — status 4등급 매핑 + unknown 폴백** - `ae97c43` (feat)
3. **Task 3: SummaryMarker — summaryMessage 진단 마커** - `7284807` (feat)

**Plan metadata:** (이 SUMMARY 커밋)

## Files Created/Modified
- `frontend/src/components/ui/badge.tsx` - shadcn new-york Badge 블록(cva variants, @/lib/utils cn), Slot은 기존 @radix-ui/react-slot 사용
- `frontend/src/features/dashboard/StatusBadge.tsx` - status string → {라벨, 시맨틱 색, lucide 아이콘} 배지, unknown은 원문 라벨 neutral 폴백
- `frontend/src/features/dashboard/SummaryMarker.tsx` - summaryMessage 카테고리컬 마커(인증 오류/레이트리밋), null/미지는 null 반환

## Decisions Made
- **신규 의존성 거부:** shadcn 4.11 CLI가 생성한 badge는 `import { Slot } from "radix-ui"`(통합 패키지)를 써 `radix-ui`를 새 의존성으로 추가했다. must_have("No new runtime dependency")를 지키기 위해 import를 기존 `@radix-ui/react-slot`(button.tsx가 이미 사용)로 바꾸고 `radix-ui` 추가를 되돌렸다(package.json/lock 원복, node_modules에서 56개 패키지 prune).
- **시맨틱 색 우선:** Badge `variant=secondary` 위에 `bg-*/10 text-*` className을 합성 — tailwind-merge가 className의 bg/text 유틸을 우선시켜 variant 기본색을 덮는다. accent(blue-600)와 의미 색을 역할 분리.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] shadcn CLI의 Windows `@` 경로 버그 + 불필요한 radix-ui 의존성**
- **Found during:** Task 1 (shadcn badge 추가)
- **Issue:** `npx shadcn@latest add badge --yes`가 Windows에서 `@` alias를 리터럴 디렉터리로 해석해 `frontend/@/components/ui/badge.tsx`에 파일을 생성했고, 통합 `radix-ui` 패키지를 새 런타임 의존성으로 추가했다(must_have "신규 의존성 0" 위반).
- **Fix:** 공식 레지스트리 출력 내용은 유지하되 (a) 파일을 `src/components/ui/badge.tsx`로 직접 작성, (b) `Slot` import를 기존 `@radix-ui/react-slot`로 교체(`Slot.Root`→`Slot`), (c) `git checkout`으로 package.json/package-lock.json의 `radix-ui` 추가 원복, (d) 리터럴 `@/` 디렉터리 삭제, (e) `npm install`로 node_modules 정리(56개 prune).
- **Files modified:** frontend/src/components/ui/badge.tsx (생성), package.json/package-lock.json (원복, net 변경 0)
- **Verification:** `grep '"radix-ui"' package.json` 없음, `npm run build` exit 0, badge.tsx가 `@/lib/utils`의 cn + cva 사용
- **Committed in:** `21b635e` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** CLI 환경 이슈 대응 — 플랜이 명시한 hand-author 폴백 경로를 따랐다. 산출물 형태·공식 레지스트리 충실도·"신규 의존성 0"을 모두 충족. 스코프 변화 없음.

## Issues Encountered
None - 계획된 작업은 문제 없이 진행. (위 deviation은 CLI 환경 이슈로 자동 대응)

## User Setup Required
None - 외부 서비스 설정 불필요.

## Next Phase Readiness
- `StatusBadge`·`SummaryMarker`·`Badge`가 준비되어 08-02 `HealthCard`가 즉시 합성 가능(useCollectionHealth 성공 분기에서 status→StatusBadge, summaryMessage→SummaryMarker).
- 블로커 없음.

---
*Phase: 08-dashboard*
*Completed: 2026-06-25*
