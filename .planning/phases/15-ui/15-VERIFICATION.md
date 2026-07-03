---
phase: 15-ui
verified_at: 2026-07-03
verdict: PASS
plans_verified: [15-01, 15-02, 15-03, 15-04]
requirements_verified: [ADMINUI-01, ADMINUI-02, ADMINUI-03, ADMINUI-04, ADMINUI-05, ADMINUI-06]
---

# Phase 15 Verification — 관리자 콘솔 UI

**Verdict: ✅ PASS** — Phase 15가 약속한 산출물(시크릿 게이트 `/admin` 콘솔 + 이벤트/워치리스트 CRUD + 수집 상태 모니터링)이 코드에 실재하며, 신규 백엔드는 read-only `GET /api/admin/items` 1개(D-13)뿐(수집/캐시/event-impact/인증 0줄), 전체 `./gradlew build`(모든 Testcontainers IT)·frontend `npm run build`(tsc) 그린.

## Goal-Backward 분석

**Phase 목표:** 기존 백엔드 `/api/admin/*`(X-Admin-Secret 게이트, 백엔드 인증 무변경)을 소비하는 프론트 관리자 콘솔 — 시크릿 로그인 뒤에서 게임 이벤트·워치리스트를 관리하고 수집 상태를 모니터링하며, 미로그인 사용자에겐 쓰기 UI를 노출하지 않고 로그아웃으로 세션을 종료한다.

| 요구사항 | 약속 | 코드 증거 | 판정 |
|----------|------|-----------|------|
| ADMINUI-01 | 시크릿 로그인 후 `X-Admin-Secret` 자동 첨부 + 새로고침 세션 유지 | `api.ts` `adminRequest`가 `X-Admin-Secret: getAdminSecret()` 자동 첨부; `adminSecret.ts` sessionStorage 고정 키 `lostark.admin.secret`; `AdminAuthContext` 마운트 시 `getAdminSecret()`로 status 재-하이드레이션 | ✅ |
| ADMINUI-02 | 잘못된 시크릿 거부(401) + 명확한 오류 | `probeAdminSecret` 401→`false`; `AdminAuthContext.login`이 '시크릿이 올바르지 않습니다. 다시 확인해 주세요.' notice 설정; 연결 실패는 별도 카피로 분기 | ✅ |
| ADMINUI-03 | 게임 이벤트 목록 + 등록·수정·삭제 | `EventSection` 인라인 폼(Select/Input/datetime-local/Textarea) + `useAdminEvents`(occurred_at desc) + `useCreateEvent`/`useReplaceEvent`/`useDeleteEvent`(invalidate); KST→UTC(`kstLocalToUtcIso`), 삭제 인라인 2단계 확인 | ✅ |
| ADMINUI-04 | 워치리스트 추가·비활성(soft-delete)·재활성 | 백엔드 `AdminItemController.list()`(active+inactive, 15-01) + `WatchlistSection` 추가 폼·`useAddItem`·`useDeactivateItem`(204 soft-delete, 인라인 확인)·`useReactivateItem`(동일 externalItemId re-POST, D-13); 409→'이미 활성 상태인 품목이에요.' | ✅ |
| ADMINUI-05 | 최근 수집 실행 이력을 콘솔에서 확인 | `CollectionSection`이 기존 `<HealthCard/>`(GET /api/health/collection) 재사용 — 시도·성공·실패 카운트·마지막 실행 시각, 백엔드 0줄(D-11) | ✅ |
| ADMINUI-06 | 미로그인 시 쓰기 UI 미노출 + 로그아웃으로 세션 종료 | `AdminRoute` 전체 DOM 게이트: `status==='anonymous'` → `<AdminLoginForm/>`만 반환(콘솔 서브트리 미마운트, disable 아님, D-05); `AdminConsolePage` 헤더 '로그아웃' 버튼 → `logout()`(clearAdminSecret) | ✅ |

## 성공 기준(ROADMAP) 검증

1. 시크릿 로그인 후 인증·새로고침 세션 유지 — ✅ (adminRequest 헤더 + sessionStorage 재-하이드레이션)
2. 잘못된 시크릿 거부(401) + 명확한 오류 — ✅ (probe 401 분기 + 인라인 카피)
3. 이벤트 등록·수정·삭제 + 목록 반영 — ✅ (EventSection CRUD + invalidate→refetch)
4. 워치리스트 추가·비활성·재활성 — ✅ (WatchlistSection + 15-01 endpoint)
5. 수집 이력 확인 · 미로그인 쓰기 UI 미노출 · 로그아웃 세션 종료 — ✅ (HealthCard + 전체 게이트 + 로그아웃)

## 빌드·게이트 검증

- **backend build:** `./gradlew build` **BUILD SUCCESSFUL** — 모든 Testcontainers IT 포함(신규 `AdminItemControllerIT` 200 active+inactive/401 no-secret 2건 + 기존 admin/collection/event-impact 회귀 전부 그린).
- **frontend build:** `npm run build`(tsc -b && vite build) **그린** (청크 사이즈 경고는 기존 사항, 실패 아님).
- **Core Value 가드(신규 백엔드 read-only 1개):** `git diff` 상 신규 백엔드 변경은 `AdminItemController`(@GetMapping 1개)·`AdminItemService`(listAll)·`AdminItemControllerIT`뿐 — SecurityConfig/AdminSecretFilter/수집/캐시/event-impact **0줄**(D-14).
- **시크릿 미노출 불변:** `grep` — admin/`api.ts`에 `console.*` **0건**, 시크릿 값을 렌더하는 텍스트 노드 **0건**; `ApiError`는 status+path만; 입력은 `type="password"`.
- **전역 401 자동 로그아웃(D-03):** `AdminAuthContext`가 `setAdminUnauthorizedHandler`로 세션 만료 로그아웃 등록; `adminRequest`가 401 시 호출.
- **dialog/toast 미도입:** 인라인 폼(D-07)·인라인 2단계 확인(D-09)·인라인 상태 메시지로 대체 — 신규 블록은 `input`/`textarea`(수기 작성)만.

## 변경 파일 (계획 정합)

- **백엔드(15-01):** `AdminItemController.java`·`AdminItemService.java`·`AdminItemControllerIT.java` — PLAN `files_modified` 일치.
- **프론트(15-02~04):** `components/ui/{input,textarea}.tsx`, `features/admin/**`(auth 3 + AdminRoute/AdminConsolePage/Collection/Event/Watchlist Section), `lib/{api,schemas,queries,kstDatetime}.ts`, `main.tsx` — PLAN `files_modified` 일치.
- **계획 외 변경(문서화된 deviation, 15-03):** `components/state/{AsyncBoundary,EmptyState}.tsx`에 하위호환 optional prop(`emptyHeading`/`errorMessage`) 추가 — 공개 화면 기본값 유지(무영향), UI-SPEC verbatim 빈/오류 카피를 공유 boundary로 전달하기 위함. 15-04도 재사용.

## 미해결 항목 / 수동 액션

- **시각·기능 UAT(권장, 비차단):** admin 백엔드(`:8080`, 유효 `admin.api.secret`) + `npm run dev`로 로그인 게이트·이벤트/워치리스트 CRUD·KST 입력→UTC 저장·삭제/비활성 확인 흐름·전역 401 로그아웃을 브라우저에서 직접 확인 권장(`/gsd-verify-work 15`).
- **code-review 게이트:** 이 환경에서 GSD 서브에이전트(gsd-code-reviewer)는 permission-denied이므로 자동 리뷰 미실행 — 각 태스크의 acceptance_criteria를 인라인으로 검증(grep + build)하며 진행함(비차단).

## 결론

코드 산출물이 phase 목표와 ADMINUI-01..06, 성공 기준 1..5를 충족. backend/frontend 빌드 그린, 신규 백엔드 read-only 1개(Core Value 가드 유지), 시크릿 누출 0. **PASS** — 남은 것은 선택적 시각/기능 UAT뿐.
