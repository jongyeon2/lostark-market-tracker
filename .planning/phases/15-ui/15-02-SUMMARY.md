---
phase: 15-ui
plan: 02
subsystem: ui
tags: [react, typescript, shadcn, sessionstorage, react-router, admin-auth, tanstack-query]

# Dependency graph
requires:
  - phase: 07-frontend-foundation
    provides: "Vite/React/TS 토대 + shadcn new-york 블록(button/card) + cn 헬퍼 + api.ts request/ApiError 패턴 + createBrowserRouter 라우트 트리 + HealthCard(자체완결)"
provides:
  - "신규 최상위 /admin 라우트 — 공개 AppLayout과 분리된 셸, 공개 TopNav 미노출(D-04)"
  - "전체 DOM 게이트(D-05, ADMINUI-06): 미로그인 = 로그인 폼만 마운트, 콘솔 서브트리 DOM 미생성"
  - "sessionStorage 시크릿 세션(D-01): 새로고침 유지·탭 종료 소멸; 재-하이드레이션"
  - "adminRequest 래퍼(X-Admin-Secret 자동 첨부) + probeAdminSecret 로그인 probe(D-02) + 전역 401 자동 로그아웃(D-03)"
  - "콘솔 셸(헤더 + 로그아웃) + 수집 상태 섹션(HealthCard 재사용, D-11) + 이벤트/워치리스트 placeholder 슬롯"
  - "수기 작성 shadcn input/textarea 블록"
affects: [15-03, 15-04, admin-console, event-crud, watchlist]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "관리자 write 호출은 lib/api.ts의 adminRequest로 단일화 — 호출부는 X-Admin-Secret 헤더를 다루지 않음"
    - "React Context(AdminAuthProvider/useAdminAuth)로 세션 상태 단일 출처; probe→200/401/throw 3분기"
    - "전역 401 훅(setAdminUnauthorizedHandler)으로 자동 로그아웃 — 모든 admin 401이 세션 종료로 수렴"
    - "게이트는 disable가 아닌 미마운트(D-05): 미인증 시 콘솔 서브트리 JSX 자체를 반환하지 않음"

key-files:
  created:
    - "frontend/src/components/ui/input.tsx, textarea.tsx — 수기 작성 shadcn 블록"
    - "frontend/src/features/admin/auth/adminSecret.ts — sessionStorage 시크릿 추상화(유일 접근 모듈)"
    - "frontend/src/features/admin/auth/AdminAuthContext.tsx — 세션 authority"
    - "frontend/src/features/admin/auth/AdminLoginForm.tsx — 중앙 로그인 카드"
    - "frontend/src/features/admin/AdminRoute.tsx — 전체 게이트"
    - "frontend/src/features/admin/AdminConsolePage.tsx — 콘솔 셸(헤더/로그아웃 + 섹션 스택)"
    - "frontend/src/features/admin/CollectionSection.tsx — 수집 상태(HealthCard 재사용)"
    - "frontend/src/features/admin/EventSection.tsx, WatchlistSection.tsx — 15-03/15-04 placeholder"
  modified:
    - "frontend/src/lib/api.ts — adminRequest/probeAdminSecret/setAdminUnauthorizedHandler 추가"
    - "frontend/src/main.tsx — 최상위 /admin sibling 라우트 추가"

key-decisions:
  - "시크릿은 sessionStorage(localStorage 아님, D-01)에 보관 — 노출 창을 단일 탭 세션으로 최소화; XSS 잔여 위험은 단일 사용자 데모로 수용(AUTH-V2 경계)"
  - "게이트는 버튼 disable가 아닌 콘솔 서브트리 미마운트(D-05) — ADMINUI-06 '노출되지 않는다'에 정확 부합"
  - "이벤트/워치리스트를 placeholder로 미리 마운트 → 15-03/15-04가 해당 파일만 교체(AdminConsolePage 미수정, wave 충돌 회피)"
  - "input/textarea는 Windows shadcn CLI @-dir 버그로 CLI 대신 수기 작성; dialog/toast 미도입(인라인 폼·확인·메시지로 대체)"

patterns-established:
  - "adminRequest 단일 write 클라이언트 + 전역 401 인터셉트 → 자동 로그아웃"
  - "probe 기반 로그인(부작용 없는 GET)으로 200/401/연결실패 3분기 피드백"

requirements-completed: [ADMINUI-01, ADMINUI-02, ADMINUI-05, ADMINUI-06]

# Metrics
duration: 35min
completed: 2026-07-03
---

# Phase 15 Plan 02: 프론트 admin 인증·세션·셸 파운데이션 Summary

**시크릿 게이트 `/admin` 콘솔 파운데이션 — sessionStorage 세션(새로고침 유지·탭 종료 소멸), X-Admin-Secret 자동 첨부 adminRequest + probe 로그인 + 전역 401 자동 로그아웃, 미로그인 시 쓰기 UI DOM 미마운트, 수집 상태 카드 완성 + 이벤트/워치리스트 placeholder 슬롯**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-07-03T11:05Z
- **Completed:** 2026-07-03T11:35Z
- **Tasks:** 3
- **Files modified:** 12 (10 created, 2 modified)

## Accomplishments
- **인증 배관(Task 1):** 수기 작성 `input`/`textarea` 블록, `adminSecret.ts`(sessionStorage 유일 접근, 고정 키 `lostark.admin.secret`), `api.ts`의 `adminRequest`(X-Admin-Secret 자동 첨부 + 401 시 전역 훅) · `probeAdminSecret`(부작용 없는 `GET /api/admin/events` probe, 200/401/throw)
- **세션 authority(Task 2):** `AdminAuthProvider`/`useAdminAuth` — probe 로그인(200 저장+authenticated / 401 인라인 거부 카피 / throw 연결실패 카피), 마운트 시 sessionStorage 재-하이드레이션(D-01), 전역 401 훅 등록→자동 로그아웃(D-03); 중앙 정렬 password 로그인 폼(UI-SPEC 카피 verbatim, Enter 제출)
- **라우트·셸(Task 3):** 최상위 `/admin` sibling 라우트(D-04), 전체 DOM 게이트(D-05 — 미로그인 시 콘솔 서브트리 미마운트), 콘솔 헤더(로그아웃 버튼, ADMINUI-06) + 3섹션 스택(D-06), 수집 상태 섹션(HealthCard 재사용, D-11/ADMINUI-05), 이벤트/워치리스트 placeholder
- 공개 `/dashboard·/timeline·/impact`·TopNav·백엔드 **무변경** — 변경은 `frontend/` 한정

## Task Commits

1. **Task 1: input/textarea + adminRequest + adminSecret** - `58c26b9` (feat)
2. **Task 2: admin auth context + login form** - `8ebadc8` (feat)
3. **Task 3: /admin route + gate + shell + collection** - `fcf631f` (feat)

## Files Created/Modified
- `frontend/src/components/ui/input.tsx`·`textarea.tsx` - 수기 shadcn new-york 폼 블록
- `frontend/src/features/admin/auth/adminSecret.ts` - sessionStorage 시크릿 추상화(유일 접근 모듈)
- `frontend/src/lib/api.ts` - `adminRequest`·`probeAdminSecret`·`setAdminUnauthorizedHandler`
- `frontend/src/features/admin/auth/AdminAuthContext.tsx` - 세션 authority(probe 로그인·재-하이드레이션·전역 401 훅)
- `frontend/src/features/admin/auth/AdminLoginForm.tsx` - 중앙 password 로그인 카드(UI-SPEC 카피)
- `frontend/src/features/admin/AdminRoute.tsx` - 전체 DOM 게이트
- `frontend/src/features/admin/AdminConsolePage.tsx` - 콘솔 셸(헤더/로그아웃 + 섹션 스택)
- `frontend/src/features/admin/CollectionSection.tsx` - 수집 상태(HealthCard 재사용)
- `frontend/src/features/admin/EventSection.tsx`·`WatchlistSection.tsx` - 15-03/15-04 placeholder
- `frontend/src/main.tsx` - 최상위 `/admin` sibling 라우트

## Decisions Made
- 시크릿 sessionStorage 보관(D-01) — 노출 창 최소화; XSS 잔여 위험은 단일 사용자 데모로 수용(AUTH-V2 경계, honesty 서사)
- 게이트 = 미마운트(D-05), disable 아님 — ADMINUI-06 문구 정확 부합
- placeholder 선마운트로 15-03/15-04가 AdminConsolePage 미수정 → wave 충돌 회피

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None. 초기 작성 시 `FormEvent`(deprecated 힌트)를 인라인 추론 핸들러로 정리해 기존 코드 관례에 맞춤(빌드는 그 전에도 통과). `npm run build`(tsc -b + vite) exit 0.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 15-03(이벤트 CRUD)·15-04(워치리스트)가 얹힐 `EventSection`/`WatchlistSection` 슬롯 + `adminRequest`(write) + TanStack QueryClient 준비 완료.
- 15-03이 도입할 첫 mutation(`invalidateQueries`)은 기존 `queryClient` 단일 인스턴스 위에서 동작.

---
*Phase: 15-ui*
*Completed: 2026-07-03*
