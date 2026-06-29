---
phase: 14-frontend-icons-fallback-docs
plan: 01
subsystem: ui
tags: [react, typescript, zod, tailwind, lucide-react, shadcn]

requires:
  - phase: 13-backend-enrichment-seed
    provides: 4개 read DTO(trackedItem/latestPrice/timeline/eventImpact)에 베이크된 iconUrl/itemGroup/roleGroup enrichment + roleGroup ∈ {DEALER,SUPPORT,MATERIAL}|null
provides:
  - 4개 read zod 스키마에 enrichment 3필드(iconUrl/itemGroup/roleGroup) 추가 — roleGroup=z.enum nullable, eventImpact는 평면 형태
  - roleGroup 공유 모듈(RoleGroup 타입 재노출·ROLE_LABEL 한글·sortByRole 정렬 헬퍼) — 14-02 정렬/그룹핑 단일 출처
  - 역할 3색 토큰(--role-dealer/--role-support/--role-material) + @theme inline --color-role-* 유틸
  - 공용 RoleBadge(solid 색배경 pill + 한글 라벨, ui/badge.tsx 0줄)
  - 공용 ItemIcon(고정 슬롯 + null/onError 역할색 글리프 fallback, 시프트 0)
affects: [14-02 4화면 소비, 14-03 docs]

tech-stack:
  added: []
  patterns:
    - "역할색을 semantic 색군으로 편입(accent blue-600과 분리) — StatusBadge의 --up/--down 선례 확장"
    - "RoleBadge는 ui/badge.tsx cva 무변경 + className 오버라이드 소비(StatusBadge 선례)"
    - "ItemIcon 고정 px 슬롯 + useState 1회성 onError 플래그 → 로딩/실패/부재 동일 footprint(레이아웃 시프트 0)"
    - "zod 단일 출처에 enrichment를 .nullable()로 추가 — roleGroup enum이 미상 값 boundary loud-fail(D-06)"

key-files:
  created:
    - frontend/src/features/_shared/roleGroup.ts
    - frontend/src/features/_shared/RoleBadge.tsx
    - frontend/src/features/_shared/ItemIcon.tsx
  modified:
    - frontend/src/lib/schemas.ts
    - frontend/src/index.css

key-decisions:
  - "eventImpactSchema를 실제 백엔드 EnrichedEventImpactResponse 평면 형태({itemId,window,iconUrl,itemGroup,roleGroup,events})에 정렬 — CONTEXT/UI-SPEC의 중첩 enrichment 객체 서술은 부정확(코드가 진실, src 0줄 검증)"
  - "roleGroup을 z.enum 3값 nullable로 모델링해 미상 값을 .parse boundary에서 loud-fail(D-06)"
  - "역할 3색을 accent(blue-600)가 아닌 semantic 색군으로 토큰화(D-01) — rose-600/emerald-700/amber-700, 흰 텍스트 대비 ≥4.7:1 AA"

patterns-established:
  - "공용 _shared 컴포넌트(ItemIcon/RoleBadge) + roleGroup 모듈을 14-02 4화면이 import만 해서 소비"

requirements-completed: [ICON-01]

duration: 15min
completed: 2026-06-29
---

# Phase 14 Plan 01: 공유 기반(아이콘·역할 배지·fallback) Summary

**4개 read zod 스키마에 enrichment(iconUrl/itemGroup/roleGroup)를 평면 형태로 추가하고, 역할 3색 토큰·공용 ItemIcon(null/onError 역할색 글리프 fallback)·RoleBadge·roleGroup 정렬 모듈을 단일 출처로 잠금 — 백엔드 src/ 0줄**

## Performance

- **Duration:** 약 15 min
- **Completed:** 2026-06-29
- **Tasks:** 3
- **Files modified:** 5 (생성 3 + 수정 2)

## Accomplishments

- **zod 계약 확장:** trackedItem/latestPrice/timeline/eventImpact 4개 스키마에 iconUrl/itemGroup/roleGroup 추가. roleGroup은 `z.enum(['DEALER','SUPPORT','MATERIAL']).nullable()`로 미상 값을 .parse boundary에서 loud-fail. eventImpact는 실제 백엔드 평면 record에 맞춰 `{itemId, window, iconUrl, itemGroup, roleGroup, events}`로 교정(중첩 객체 아님).
- **roleGroup 공유 모듈:** ROLE_LABEL(딜러/서포터/융화재료) + sortByRole(원본 불변 새 배열, DEALER→SUPPORT→MATERIAL→null 말미, ko-KR localeCompare) — 14-02 셀렉터·대시보드 정렬/그룹핑의 단일 출처.
- **역할색 토큰:** index.css `:root`에 rose-600/emerald-700/amber-700 토큰 + `@theme inline`에 `--color-role-*` 노출 → `bg-role-*` 유틸 생성. accent(--primary/--ring blue-600) 0줄.
- **RoleBadge:** className 오버라이드로 solid 색배경 pill + 흰 텍스트 + 한글 라벨. ui/badge.tsx 0줄. roleGroup null이면 null 반환.
- **ItemIcon:** 고정 px 슬롯(md 32 / sm 20) + iconUrl==null OR onError 둘 다에서 역할색 글리프 타일(ScrollText/FlaskConical/Package) fallback — 레이아웃 시프트 0, lucide-react만 사용.

## Task Commits

각 태스크는 원자 커밋:

1. **Task 1: 4개 zod 스키마 enrichment + roleGroup 모듈** — `cdf83d4` (feat)
2. **Task 2: 역할색 토큰(index.css) + RoleBadge** — `ec6354a` (feat)
3. **Task 3: 공용 ItemIcon(고정 슬롯 + fallback)** — `9ece4fd` (feat)

## Files Created/Modified

- `frontend/src/lib/schemas.ts` — roleGroupSchema enum + 4개 read 스키마에 enrichment 3필드(eventImpact 평면 교정)
- `frontend/src/features/_shared/roleGroup.ts` (신규) — RoleGroup·ROLE_LABEL·sortByRole 단일 출처
- `frontend/src/index.css` — 역할 3색 토큰 + @theme inline --color-role-* 유틸
- `frontend/src/features/_shared/RoleBadge.tsx` (신규) — solid 역할 배지
- `frontend/src/features/_shared/ItemIcon.tsx` (신규) — 고정 슬롯 + null/onError 역할색 글리프 fallback

## Decisions Made

- **eventImpact 평면 교정:** 백엔드 `EnrichedEventImpactResponse` record를 직접 확인해 평면 형태 확정(CONTEXT/UI-SPEC 중첩 서술 무시). 4개 DTO 모두 enrichment가 최상위 평면 필드임을 src에서 검증.
- **roleGroup enum nullable:** 백엔드는 String이지만 값이 3종+null로 한정 → zod enum으로 미상 값 loud-fail(D-06).
- **역할색 semantic 편입:** accent(blue-600)와 충돌 회피, 흰 텍스트 AA 대비 충족 색조 선택(D-01).

## Deviations from Plan

None - plan executed exactly as written.

(plan이 미리 명시한 eventImpact 평면 교정은 계획된 작업이며, 백엔드 record 확인으로 검증만 추가했다 — 일탈 아님.)

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 14-02가 import만 하면 소비할 기반 완성: `<ItemIcon>`·`<RoleBadge>`·`sortByRole`·`ROLE_LABEL` + enrichment를 노출하는 4개 zod 스키마.
- `npm run build`(tsc -b && vite build) 그린, 백엔드 `src/` diff 0줄, 신규 npm 의존 0.

---
*Phase: 14-frontend-icons-fallback-docs*
*Completed: 2026-06-29*
