---
phase: 15-ui
plan: 01
subsystem: api
tags: [spring-boot, jpa, rest, admin, testcontainers, read-only]

# Dependency graph
requires:
  - phase: 04-admin
    provides: "AdminItemController /api/admin/items (POST/DELETE) + AdminSecretFilter/SecurityConfig /api/admin/** = .authenticated() gate + AdminAuth test helper + ApiErrorResponse contract"
  - phase: 01-foundation
    provides: "TrackedItem 엔티티(active 플래그·soft-delete) + TrackedItemRepository(JpaRepository) + TrackedItemResponse projection"
provides:
  - "read-only GET /api/admin/items — active+inactive 전체 tracked item을 TrackedItemResponse[]로 반환(결정적 정렬: active-first, displayName asc)"
  - "AdminItemService.listAll() — findAll() + 인메모리 정렬, 쓰기/캐시/수집 무접촉 read 메서드"
  - "15-04 워치리스트 섹션이 소비할 재활성(reactivation) source of truth (D-13)"
affects: [15-04, watchlist, admin-console]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "관리자 read 엔드포인트는 AdminEventController.list() shape 그대로 미러링(service.listAll().stream().map(Response::from).toList())"
    - "기존 /api/admin/** = .authenticated() 게이트 재사용 → 신규 인증/필터/보안 0줄(D-14)"

key-files:
  created: []
  modified:
    - "src/main/java/com/lostark/tracker/web/admin/AdminItemController.java — @GetMapping list() 추가"
    - "src/main/java/com/lostark/tracker/admin/AdminItemService.java — listAll() 추가"
    - "src/test/java/com/lostark/tracker/admin/AdminItemControllerIT.java — 200(active+inactive)·401(no-secret) IT 2건 추가"

key-decisions:
  - "공개 GET /api/items(findByActiveTrue, active-only, D-12)는 무변경 — 관리자 콘솔의 비활성 품목 출처는 신규 /api/admin/items로만 노출(D-13)"
  - "신규 DTO/repository 쿼리 없이 상속된 findAll() + TrackedItemResponse.from 재사용 — Core Value 가드(수집/캐시/event-impact 0줄, D-14)"
  - "정렬은 JPA insertion order 대신 Comparator(active desc → displayName CASE_INSENSITIVE asc)로 결정적 — 새로고침 간 안정적 목록"

patterns-established:
  - "관리자 콘솔용 read-only 확장은 기존 admin 게이트 아래 컨트롤러에 @GetMapping 1개 + service read 메서드 1개로 최소 확장"

requirements-completed: [ADMINUI-04]

# Metrics
duration: 15min
completed: 2026-07-03
---

# Phase 15 Plan 01: 백엔드 GET /api/admin/items 엔드포인트 Summary

**관리자 콘솔의 워치리스트 재활성 출처가 되는 read-only `GET /api/admin/items` — active+inactive 전체를 결정적 순서로 반환하며, 기존 admin 시크릿 게이트를 그대로 상속(신규 보안 0줄)**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-07-03T10:55Z
- **Completed:** 2026-07-03T11:02Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- `AdminItemService.listAll()` — 상속된 `findAll()`를 active-first·displayName-asc로 결정적 정렬해 반환하는 read-only 메서드(쓰기·캐시·수집 무접촉)
- `AdminItemController`에 `@GetMapping list()` 추가 — `AdminEventController.list()` shape를 그대로 미러링, `TrackedItemResponse::from`으로 매핑
- 신규 인증/필터/보안 코드 0줄: 기존 `/api/admin/**` = `.authenticated()` 규칙이 새 경로를 그대로 커버(D-14)
- Testcontainers(Postgres+Redis) IT 2건 — 시크릿 GET 200(active+inactive, 올바른 active 플래그) · no-secret GET 401(ApiErrorResponse 계약)로 재활성 출처(D-13)를 실 DB에서 실증

## Task Commits

Each task was committed atomically:

1. **Task 1: listAll() + GET /api/admin/items** - `e580a8b` (feat)
2. **Task 2: Testcontainers 200/401 IT** - `52632ab` (test)

## Files Created/Modified
- `src/main/java/com/lostark/tracker/web/admin/AdminItemController.java` - 신규 `@GetMapping list()` (active+inactive 전체 → `List<TrackedItemResponse>`)
- `src/main/java/com/lostark/tracker/admin/AdminItemService.java` - 신규 `listAll()` (findAll() + 결정적 정렬, read-only)
- `src/test/java/com/lostark/tracker/admin/AdminItemControllerIT.java` - `getItemsWithSecretReturnsActiveAndInactive`, `getItemsWithoutSecretReturns401Contract`

## Decisions Made
- 공개 `GET /api/items`는 무변경 유지(active-only, D-12) — 비활성 품목은 admin 게이트 뒤 `/api/admin/items`로만 노출(D-13, 정보 노출 T-1501-01 완화)
- 신규 DTO·repository 쿼리 없이 상속된 `findAll()` + `TrackedItemResponse.from` 재사용(D-14, Core Value 가드)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. `compileJava` exit 0, `AdminItemControllerIT`(기존 6건 + 신규 2건) 전부 green.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 15-04 워치리스트 섹션이 소비할 재활성 source of truth(`GET /api/admin/items`, active 플래그 포함) 준비 완료.
- 전체 `./gradlew build`(전 백엔드 회귀)는 Phase 15 검증 게이트에서 일괄 실행 예정 — 15-01은 순수 추가(신규 엔드포인트·신규 테스트)로 기존 경로 무변경.

---
*Phase: 15-ui*
*Completed: 2026-07-03*
