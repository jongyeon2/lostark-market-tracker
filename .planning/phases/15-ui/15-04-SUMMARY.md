---
phase: 15-ui
plan: 04
subsystem: ui
tags: [react, typescript, tanstack-query, mutations, soft-delete, reactivate, admin, watchlist]

# Dependency graph
requires:
  - phase: 15-ui
    provides: "15-01 GET /api/admin/items(active+inactive, D-13) + 15-02 adminRequest/전역401/셸 placeholder + 15-03 mutation·invalidate 패턴·확장된 AsyncBoundary"
  - phase: 13-enrichment
    provides: "ItemIcon/RoleBadge/roleGroup(v1.2 아이템 아이콘·역할 배지 자산)"
  - phase: 07-frontend-foundation
    provides: "trackedItemSchema/trackedItemsSchema(재사용) + StatusBadge 색배지 선례 + AsyncBoundary/Table/Badge/Input 블록"
provides:
  - "워치리스트 CRUD 섹션(ADMINUI-04): active+inactive 목록 + 추가(201)·비활성(204 soft-delete)·재활성(200 re-POST)"
  - "AdminItemRequest 타입 + admin item api(list/add/deactivate) + item mutation 훅(재활성=add 재사용)"
  - "409(활성 중복) 특수 처리 → '이미 활성 상태인 품목이에요.' 인라인 경고"
affects: [admin-console]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "재활성 = 별도 엔드포인트 없이 동일 externalItemId re-POST(백엔드 upsert 분기, D-13)"
    - "per-row pending 표시 = mutation.isPending && mutation.variables?.id === row.id"
    - "활성/비활성 = StatusBadge 색배지 선례(semantic bg/10 + text, accent 아님)"

key-files:
  created: []
  modified:
    - "frontend/src/lib/schemas.ts — AdminItemRequest 타입(응답은 기존 trackedItemsSchema 재사용)"
    - "frontend/src/lib/api.ts — getAdminItems/addAdminItem/deactivateAdminItem"
    - "frontend/src/lib/queries.ts — useAdminItems + useAddItem/useDeactivateItem/useReactivateItem(invalidate)"
    - "frontend/src/features/admin/WatchlistSection.tsx — placeholder → 실 CRUD 섹션"

key-decisions:
  - "재활성은 addAdminItem 재사용(동일 externalItemId → 백엔드 200 재활성, D-13) — 별도 reactivate 엔드포인트/훅 없음"
  - "응답 스키마 신규 추가 없이 기존 trackedItemSchema/trackedItemsSchema 재사용(D-13 read-only 엔드포인트가 동일 DTO)"
  - "409는 generic 오류 대신 '이미 활성 상태인 품목이에요.' warning 톤으로 특수 처리(T-1504-04)"
  - "비활성=soft-delete(hard delete 아님) — 행+가격 이력 보존, 재활성으로 복구 가능(T-1504-01)"

patterns-established:
  - "동일 upsert 엔드포인트를 create/reactivate 양방향에 재사용하는 프론트 소비 패턴"

requirements-completed: [ADMINUI-04]

# Metrics
duration: 22min
completed: 2026-07-03
---

# Phase 15 Plan 04: 워치리스트 섹션 Summary

**워치리스트 CRUD 섹션 — 15-01의 admin-items(active+inactive) 소비로 비활성 품목 가시화, 인라인 추가(409 특수 처리)·파괴적 인라인 비활성 확인(soft-delete)·동일 externalItemId re-POST 재활성, invalidate 기반 목록 갱신으로 ADMINUI-04 마감**

## Performance

- **Duration:** ~22 min
- **Started:** 2026-07-03T12:10Z
- **Completed:** 2026-07-03T12:30Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- **데이터 레이어(Task 1):** `AdminItemRequest` 타입(응답은 기존 `trackedItemsSchema` 재사용), `getAdminItems`(15-01 active+inactive, D-13)·`addAdminItem`(create 201 겸 reactivate 200)·`deactivateAdminItem`(204 soft-delete), `useAdminItems` + 3 mutation(invalidate `['admin-items']`), 재활성은 `addAdminItem` 재사용
- **UI(Task 2):** `WatchlistSection` placeholder → 실 CRUD — 인라인 추가 폼(아이템 번호/표시 이름/분류), active+inactive 목록(ItemIcon+RoleBadge+활성/비활성 배지), 활성 행 파괴적 비활성 인라인 2단계 확인(D-09), 비활성 행 accent 재활성(per-row '재활성 중…'), 409 → '이미 활성 상태인 품목이에요.', mutation 성공/실패 인라인 피드백, `AsyncBoundary`(UI-SPEC verbatim 빈/오류 카피)
- v1.2 자산(ItemIcon/RoleBadge/StatusBadge 선례) + 15-01 엔드포인트 + 15-02 배관 + 15-03 mutation 패턴 전부 재사용 — 백엔드·공개 화면 0줄, 변경 `frontend/` 한정

## Task Commits

1. **Task 1: watchlist data layer** - `3f0544f` (feat)
2. **Task 2: WatchlistSection UI** - `4239a49` (feat)

## Files Created/Modified
- `frontend/src/lib/schemas.ts` - `AdminItemRequest`(응답은 trackedItemsSchema 재사용)
- `frontend/src/lib/api.ts` - `getAdminItems`/`addAdminItem`/`deactivateAdminItem`
- `frontend/src/lib/queries.ts` - `useAdminItems` + add/deactivate/reactivate mutations
- `frontend/src/features/admin/WatchlistSection.tsx` - 추가 폼·목록(배지)·비활성 확인·재활성·409/피드백

## Decisions Made
- 재활성 = 동일 externalItemId re-POST(D-13) — 별도 엔드포인트/훅 불필요
- 409 특수 처리로 활성 중복을 명확히 안내(T-1504-04)
- 비활성 soft-delete로 이력 보존·복구 가능(T-1504-01)

## Deviations from Plan

None - plan executed exactly as written. (15-03이 도입한 AsyncBoundary/EmptyState 하위호환 확장을 그대로 재사용 — 본 플랜에서 추가 변경 없음.)

## Issues Encountered
None. import-후-사용 edit 사이 stale unused-import diagnostic만 관찰; `npm run build` 최종 exit 0.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 15 4개 플랜 완료 — 관리자 콘솔(로그인·이벤트·워치리스트·수집 상태) 전 섹션 구현.
- 다음: Phase 15 검증(전체 백엔드 `./gradlew build` + 프론트 build) → VERIFICATION → phase.complete.

---
*Phase: 15-ui*
*Completed: 2026-07-03*
