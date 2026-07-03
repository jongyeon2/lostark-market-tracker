---
phase: 15-ui
plan: 03
subsystem: ui
tags: [react, typescript, tanstack-query, mutations, zod, timezone, crud, admin]

# Dependency graph
requires:
  - phase: 15-ui
    provides: "15-02 adminRequest(X-Admin-Secret) 래퍼 + 전역 401 로그아웃 + Input/Textarea 블록 + EventSection placeholder 슬롯"
  - phase: 07-frontend-foundation
    provides: "zod 경계 검증 패턴(schemas.ts) + useQuery 훅 규약(queries.ts) + 단일 queryClient + formatKst(표시) + AsyncBoundary/LoadingState/EmptyState/ErrorState + Select/Table 블록"
provides:
  - "게임 이벤트 CRUD 섹션(ADMINUI-03): GET 목록(occurred_at desc) + POST/PUT/DELETE, 인라인 폼(D-07)·인라인 삭제 확인(D-09)"
  - "gameEventResponseSchema/gameEventsSchema + AdminEventRequest — zod 경계를 write 경로로 확장"
  - "admin event api 4종(get/create/replace/delete) over adminRequest"
  - "이 프로젝트 첫 TanStack mutations(D-10): useCreateEvent/useReplaceEvent/useDeleteEvent + invalidateQueries(['admin-events'])"
  - "kstDatetime.ts — KST↔UTC write-side 변환(D-08, formatKst의 역방향)"
  - "AsyncBoundary/EmptyState 하위호환 확장(emptyHeading/errorMessage) — 15-04 재사용"
affects: [15-04, watchlist, event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TanStack useMutation + onSuccess invalidateQueries → refetch (낙관적 아님, 서버 상태 단일 출처)"
    - "KST wall-clock 입력 → 제출 시 UTC ISO('...Z') 단일 변환점(kstLocalToUtcIso), 편집 프리필은 역변환"
    - "파괴적 작업은 인라인 2단계 확인(dialog 미사용) — 행 액션 자리 치환"

key-files:
  created:
    - "frontend/src/lib/kstDatetime.ts — kstLocalToUtcIso/utcIsoToKstLocal"
  modified:
    - "frontend/src/lib/schemas.ts — gameEventResponseSchema/gameEventsSchema/AdminEventRequest"
    - "frontend/src/lib/api.ts — getAdminEvents/createAdminEvent/replaceAdminEvent/deleteAdminEvent"
    - "frontend/src/lib/queries.ts — useAdminEvents + 3 mutations(invalidate)"
    - "frontend/src/features/admin/EventSection.tsx — placeholder → 실 CRUD 섹션"
    - "frontend/src/components/state/AsyncBoundary.tsx, EmptyState.tsx — 하위호환 optional prop(deviation)"

key-decisions:
  - "occurred_at 정렬은 UTC ISO 문자열 lexicographic desc(=시간순) — 별도 파싱 불필요"
  - "발생 시각 단일 변환점 kstLocalToUtcIso(+9h 고정, KST DST 없음) — off-by-9h를 입력 경로까지 폐쇄(T-1503-01)"
  - "AsyncBoundary/EmptyState에 하위호환 optional prop 추가(공개 화면 기본값 무변경) — UI-SPEC verbatim 빈/오류 카피를 공유 컴포넌트로 전달(Rule 2 deviation)"

patterns-established:
  - "admin write = adminRequest + zod 경계 parse + useMutation(invalidate)"
  - "인라인 확인 = confirmingDeleteId 상태로 행 액션 치환"

requirements-completed: [ADMINUI-03]

# Metrics
duration: 30min
completed: 2026-07-03
---

# Phase 15 Plan 03: 이벤트 CRUD 섹션 Summary

**게임 이벤트 CRUD 섹션 — 인라인 등록/수정 폼(KST→UTC 변환)·목록(최신순)·파괴적 인라인 2단계 삭제 확인, 프로젝트 첫 TanStack mutation(invalidate→refetch)으로 서버 상태 단일 출처 유지**

## Performance

- **Duration:** ~30 min
- **Started:** 2026-07-03T11:40Z
- **Completed:** 2026-07-03T12:08Z
- **Tasks:** 2
- **Files modified:** 7 (1 created, 6 modified)

## Accomplishments
- **데이터 레이어(Task 1):** zod `gameEventResponseSchema`/`gameEventsSchema` + `AdminEventRequest`(write 경계), `adminRequest` 기반 event api 4종, `kstDatetime.ts`(KST↔UTC, D-08 — 라운드트립·자정 언더플로·월경계 실측 검증), 첫 TanStack mutations 3종 + `invalidateQueries(['admin-events'])`(D-10)
- **UI(Task 2):** `EventSection` placeholder → 실 CRUD — 인라인 폼(유형 Select·제목·발생 시각 KST datetime-local·설명 Textarea, D-07), 목록(occurred_at desc), 수정 프리필(UTC→KST), 파괴적 삭제 인라인 2단계 확인(D-09), mutation 성공/실패 인라인 피드백, `AsyncBoundary` 로딩/빈/오류(UI-SPEC verbatim 카피)
- occurredAt은 항상 UTC `...Z`로 전송(백엔드 `OffsetDateTime`) — 표시(formatKst)에 이어 입력 경로의 off-by-9h 폐쇄
- 변경은 `frontend/` 한정; 백엔드·공개 화면 무변경

## Task Commits

1. **Task 1: event data layer (schemas/api/kst/mutations)** - `bf04acc` (feat)
2. **Task 2: EventSection CRUD UI** - `78b085f` (feat)

## Files Created/Modified
- `frontend/src/lib/kstDatetime.ts` - `kstLocalToUtcIso`/`utcIsoToKstLocal`(D-08 write-side inverse)
- `frontend/src/lib/schemas.ts` - `gameEventResponseSchema`/`gameEventsSchema`/`AdminEventRequest`
- `frontend/src/lib/api.ts` - admin event CRUD 4종(over adminRequest)
- `frontend/src/lib/queries.ts` - `useAdminEvents` + 3 mutations(invalidate on success)
- `frontend/src/features/admin/EventSection.tsx` - 인라인 폼·목록·삭제 확인·피드백
- `frontend/src/components/state/AsyncBoundary.tsx`·`EmptyState.tsx` - 하위호환 optional prop(emptyHeading/errorMessage)

## Decisions Made
- occurred_at 정렬은 UTC ISO 문자열 lexicographic desc(시간순 동치)
- 발생 시각 단일 변환점(+9h 고정) — 입력 경로 off-by-9h 폐쇄(T-1503-01)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] AsyncBoundary/EmptyState에 하위호환 optional prop 추가**
- **Found during:** Task 2 (EventSection 목록의 빈/오류 상태)
- **Issue:** UI-SPEC은 빈 상태 heading '등록된 이벤트가 없어요'와 admin 연결 오류 카피를 요구하지만, 공유 `EmptyState`는 heading 고정, `AsyncBoundary`는 커스텀 error message를 `ErrorState`로 전달하지 않아 verbatim 카피 불가
- **Fix:** `EmptyState`에 `heading?`, `AsyncBoundary`에 `emptyHeading?`/`errorMessage?` optional prop 추가(기본값 = 기존 공개 화면 카피 → 무변경). EventSection이 이를 통해 UI-SPEC 카피 전달
- **Files modified:** frontend/src/components/state/AsyncBoundary.tsx, EmptyState.tsx
- **Verification:** `npm run build` exit 0; 공개 화면은 prop 미전달로 기존 카피 유지; 15-04가 동일 확장 재사용
- **Committed in:** `78b085f` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** 하위호환 확장으로 공개 화면 무영향, UI-SPEC 카피 계약 충족. 계획 외 파일 2개(공유 state 컴포넌트)로 범위 소폭 확장하되 frontend/ 한정 제약은 유지. Scope creep 없음.

## Issues Encountered
None. import-후-사용 edit 사이 stale unused-import diagnostic만 관찰(빌드는 최종 통과).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 15-04 워치리스트가 재사용할 mutation/invalidate 패턴 + adminRequest + 확장된 AsyncBoundary 준비 완료.
- 15-04는 15-01의 `GET /api/admin/items`(active+inactive)를 소비 — 백엔드 이미 준비됨.

---
*Phase: 15-ui*
*Completed: 2026-07-03*
