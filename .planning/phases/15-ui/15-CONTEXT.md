# Phase 15: 관리자 콘솔 UI - Context

**Gathered:** 2026-07-01
**Status:** Ready for planning

<domain>
## Phase Boundary

기존 백엔드 `/api/admin/*`(X-Admin-Secret 게이트, **인증 로직 무변경**)와 `/api/health/collection`을 소비하는 **프론트 관리자 콘솔**을 세운다. 시크릿 입력 로그인 게이트 뒤에서 (1) 게임 이벤트 CRUD, (2) 워치리스트 품목 추가·비활성(soft-delete)·재활성, (3) 최신 수집 상태 모니터링을 제공한다. 기존 React Router 셸(`/dashboard·/timeline·/impact`)에 신규 `/admin` 라우트를 얹고, TanStack Query **mutation**(신규 패턴) + zod 경계 검증 + `X-Admin-Secret` 자동 첨부로 구현한다. (ADMINUI-01..06)

**이 페이즈는 대부분 프론트 소비이나 순수 프론트는 아니다** — 재활성화 UX를 위해 백엔드 read-only 엔드포인트 **1개**(`GET /api/admin/items`, active+inactive 전체)를 추가한다(D-14). 수집/캐시/event-impact/기존 인증 로직은 **0줄**.

**불변 제약(상시 가드):**
- 프론트에서 Lostark Open API **직접 호출 금지** — 백엔드 DTO만 소비
- 관리자 시크릿·실 API 키를 코드·문서·로그·커밋에 **미기재**
- 신규 백엔드 코드는 `/api/admin/*` 게이트 뒤 **read-only**만 — 수집/캐시/event-impact/**인증 로직 0줄**(Core Value 가드)
- `npm run build`(tsc 포함) 통과

**범위 밖(다른 페이즈):** 대시보드 카드 개선(Phase 16) · 실데이터 전환(Phase 17) · 배포/보안 게이트(Phase 18) · 풀 유저/권한 모델(AUTH-V2) · CD(CD-V2).

</domain>

<decisions>
## Implementation Decisions

### 시크릿 로그인 & 세션 지속 (ADMINUI-01/02/06)
- **D-01:** 시크릿 보관 = **`sessionStorage`**. 탭 세션 동안 유지(새로고침 OK), 탭 종료 시 자동 소멸 → 노출 창 최소. 요구(새로고침 유지) 충족 + 포트폴리오 honesty 서술에 유리. `localStorage`(재방문 유지)는 XSS 노출 창이 길어 1인 데모엔 과함으로 기각.
- **D-02:** 로그인 검증 = **`GET /api/admin/events` probe**. 전용 verify 엔드포인트가 없으므로 부작용 없는 admin read를 1회 호출해 **200=유효 / 401=거부** 판정. 성공 시 이벤트 목록도 곧바로 확보. 명시적 로그인 버튼 → 즉각 피드백(ADMINUI-02 "명확한 거부 오류"와 정합).
- **D-03:** 세션 중 401 처리 = **전역 401 인터셉트 → 자동 로그아웃**. 로그인 이후 admin 요청이 401(시크릿 변경/만료)이면 저장 시크릿 폐기 + 로그인 화면 복귀 + 안내 메시지. 세션 종료(ADMINUI-06)와 일관, 무효 시크릿으로 로그인 상태에 남는 혼란 차단.

### 콘솔 진입 & 미로그인 가드 (ADMINUI-06)
- **D-04:** 콘솔 배치 = **신규 최상위 `/admin` 라우트**. 데모 3화면과 분리하고 **공개 TopNav에는 관리자 미노출**(직접 URL / 필요 시 footer 작은 링크로 진입). 공개 데모 표면을 깨끗이 유지. `main.tsx` 라우트 트리에 `/admin` 추가(기존 AppLayout 셸 재사용 여부는 재량 — 별도 관리자 셸도 가능).
- **D-05:** 미로그인 가드 = **전체 게이트 → 로그인 폼만 렌더**. `/admin` 하위 전체를 로그인 게이트로 감싸고, 미로그인 시 쓰기 UI **DOM 자체를 그리지 않는다**(로그인 폼만). ADMINUI-06 "노출되지 않는다" 문구에 정확히 부합 — 버튼 disable 방식(DOM에 잔존)보다 강함.
- **D-06:** 로그인 후 콘솔 내부 구조 = **단일 페이지 + 섹션/탭**. 한 화면에 이벤트 CRUD · 워치리스트 · 수집 상태 3섹션을 응집. 소규모 콘솔에 적합, 세션/가드 상태 관리 단순. 하위 라우트 분리(`/admin/events` 등)는 과함으로 기각.

### CRUD 폼 UX & 파괴적 작업 (ADMINUI-03/04)
- **D-07:** 폼 패턴 = **인라인 폼/섹션 내**. 단일 페이지 콘솔(D-06)과 일관, 목록 위/옆에 폼 배치. 모달 관리 오버헤드 없고 **shadcn `dialog` 블록 미설치**(현 `ui/`에 없음) 문제도 회피.
- **D-08:** 이벤트 `occurredAt` 입력 = **KST 입력 → 제출 시 UTC ISO(`...Z`) 변환**. `datetime-local`을 KST로 받아 UTC로 변환해 전송(백엔드 `GameEventRequest.occurredAt`은 UTC `OffsetDateTime` 기대). Phase 7 `formatKst` 표시 가드의 **쓰기 역방향** — off-by-9h를 쓰기 경로까지 연장.
- **D-09:** 파괴적 작업(이벤트 삭제 · 품목 비활성) = **확인 단계(인라인 또는 경량 다이얼로그)**. 데모 3화면엔 없던 파괴적 패턴이라 실행 전 확인 프롬프트. (즉시 실행 + undo 토스트는 undo/toast 구현 복잡도로 기각.)
- **D-10:** mutation 성공 후 목록 갱신 = **`invalidateQueries` → refetch**. TanStack Query 표준, 서버 상태 단일 출처 유지. Phase 7 D-01 패턴 연장. 낙관적 업데이트는 롤백·불일치 복잡도로 소규모 콘솔엔 과함.

### 수집 이력 모니터링 (ADMINUI-05)
- **D-11:** 수집 상태 표시 = **기존 `GET /api/health/collection` 재사용, 최신 상태 카드 1개**. 백엔드 **0줄**. 최신 run의 시도/성공/실패 카운트 · 시각 · 상태(`NO_RUNS` 포함)를 카드로. ADMINUI-05 "이력" 문구를 **"최신 수집 상태 모니터링"** 으로 해석(리스트형 이력 아님). 기존 `HealthCard`(대시보드) 재사용 가능.

### 워치리스트 재활성화 — 백엔드 read-only 추가 (ADMINUI-04)
- **D-12:** 재활성화 UX = **비활성 품목 목록을 보여주고 '재활성' 버튼 제공**. 공개 `GET /api/items`는 활성만 반환하므로, 관리자가 "무엇이 비활성인지" 볼 출처가 필요.
- **D-13:** 그 출처 = **신규 백엔드 read-only 엔드포인트 `GET /api/admin/items`** (active+inactive 전체, `TrackedItemResponse[]`의 `active` 플래그로 구별). 재활성은 기존 `POST /api/admin/items`(동일 `externalItemId` → soft-deleted 품목을 200으로 재활성)로 수행, 비활성은 기존 `DELETE /api/admin/items/{id}`(204 soft-delete).
- **D-14:** 신규 엔드포인트 가드 = **`/api/admin/*` 아래(기존 `AdminSecretFilter` 게이트 재사용) + read-only + 핵심 0줄**. 새 인증/필터 추가 없음(기존 `SecurityConfig`의 `/api/admin/**` = `.authenticated()`가 그대로 커버). 수집/캐시/event-impact 로직 0줄. `AdminItemController`에 `@GetMapping` 추가 + 서비스 read 메서드 + (필요 시) `TrackedItemRepository.findAll()` 소비.

### Claude's Discretion
- 로그인 폼 시각·카피, `password` 타입 입력·Enter 제출, 로그인 실패 오류 문구.
- 성공/오류 피드백 메커니즘(**토스트 라이브러리 미설치** → 인라인 상태 메시지 or 경량 토스트 자체 구현 — 재량).
- 이벤트 PUT(전체 교체) 수정 폼의 기존값 프리필 방식, `EventType` 셀렉트(LOA_ON/MAJOR_UPDATE/SEASON_END/BALANCE_PATCH) 라벨 문구.
- 품목 추가 폼의 `externalItemId` 입력(로아 아이템 번호 — 관리자 도메인 지식 전제, plain text 입력) · `displayName` · `category` 배치.
- `/admin` 셸을 기존 `AppLayout` 재사용할지 별도 관리자 셸로 둘지, 섹션 vs 탭 렌더 형태.
- `datetime-local` KST↔UTC 변환 구현 위치, zod admin 스키마 배치, admin API 클라이언트 함수 시그니처.
- `sessionStorage` 접근 추상화(훅/컨텍스트) 형태, `X-Admin-Secret` 자동 첨부 지점(공용 admin request 래퍼).
- `GET /api/admin/items` 정확 경로·정렬·응답 형태(기존 `TrackedItemResponse` 재사용 권장), 서비스/리포지토리 메서드명.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 이 페이즈 계획·범위·요구사항
- `.planning/ROADMAP.md` §"Phase 15: 관리자 콘솔 UI" — Goal + 5개 성공 기준 + v1.3 불변 제약
- `.planning/REQUIREMENTS.md` (ADMINUI-01..06) — 이 페이즈가 충족하는 요구사항 6건 + Out of Scope(풀 유저/권한 = AUTH-V2)
- `.planning/PROJECT.md` — Core Value(수집 신뢰성), v1.3 핵심 결정(관리자 인증 = 기존 X-Admin-Secret 재사용·백엔드 인증 무변경), Key Decisions 표(프론트=Vite 프록시·zod 단일출처·UTC→KST 가드·시크릿 게이트)

### 백엔드 API 계약 (이 페이즈가 소비/확장하는 것 — 필수 실측)
- `src/main/java/com/lostark/tracker/web/admin/AdminEventController.java` — 이벤트 CRUD: `POST /api/admin/events` 201, `GET` 200(occurred_at desc), `PUT /{id}` 200(전체 교체), `DELETE /{id}` 204
- `src/main/java/com/lostark/tracker/web/admin/AdminItemController.java` — 품목: `POST /api/admin/items` 201(생성)/200(재활성)/409(활성 중복), `DELETE /{id}` 204(soft-delete). **D-13이 여기 `GET` read-only 추가.**
- `src/main/java/com/lostark/tracker/web/dto/GameEventRequest.java` / `GameEventResponse.java` — 이벤트 폼 필드: `eventType`(enum LOA_ON/MAJOR_UPDATE/SEASON_END/BALANCE_PATCH)·`title`·`occurredAt`(UTC OffsetDateTime)·`description`(optional); 응답에 `id`/`createdAt`/`updatedAt` 추가
- `src/main/java/com/lostark/tracker/web/dto/TrackedItemRequest.java` / `TrackedItemResponse.java` — 품목 폼: `externalItemId`·`displayName`·`category`(optional); 응답: `id`/`externalItemId`/`displayName`/`category`/**`active`**/`iconUrl`/`itemGroup`/`roleGroup`
- `src/main/java/com/lostark/tracker/web/dto/CollectionHealthResponse.java` + `src/main/java/com/lostark/tracker/web/HealthController.java` — `GET /api/health/collection`(D-11): `lastRunAt`/`startedAt`/`itemsAttempted`/`itemsSucceeded`/`itemsFailed`/`status`(`NO_RUNS` 포함)/`summaryMessage`. **최신 1건만** 반환.
- `src/main/java/com/lostark/tracker/security/AdminSecretFilter.java` · `SecurityConfig.java` · `AdminAuthenticationEntryPoint.java` — `X-Admin-Secret` 헤더 상수시간 비교, `/api/admin/**` = `.authenticated()`, 401 JSON `{timestamp,status,error,message}`(`ApiErrorResponse`). STATELESS. **D-13 신규 엔드포인트가 그대로 이 게이트에 커버됨(인증 무변경).**

### 프론트 아키텍처·패턴 (재정의 금지)
- `.planning/phases/07-frontend-foundation/07-CONTEXT.md` — D-01/02(TanStack Query·fetch-on-mount), D-03/04(React Router 실경로·searchParams), D-05/06(zod 단일출처·경계 `.parse`), D-07/08(AsyncBoundary·상태 컴포넌트), D-11/12(feature 폴더·`api.ts`/`queries.ts`), D-09(formatKst)
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` — 디자인 시스템(shadcn slate·new-york, lucide, Inter), spacing/typography/color 토큰, 카피 계약. 관리자 콘솔 시각도 이 계약 위에서.
- `frontend/src/lib/api.ts` — 타입드 클라이언트(경계 `.parse`, `ApiError`). admin 함수(`X-Admin-Secret` 첨부·mutation) 추가 지점.
- `frontend/src/lib/schemas.ts` — zod 단일 출처. admin 요청/응답 스키마 추가.
- `frontend/src/main.tsx` — 라우트 트리(`/admin` 추가 지점).

[외부 ADR/spec 문서는 없음 — API 계약은 위 백엔드 소스 실측이 단일 출처.]

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/features/dashboard/HealthCard.tsx` — 수집 상태 카드(D-11). 관리자 콘솔 수집 섹션에서 재사용/참조.
- `frontend/src/features/dashboard/StatusBadge.tsx` · `frontend/src/features/impact/ImpactStatusBadge.tsx` — 색 배지 선례. 수집 상태·활성/비활성 배지에 활용.
- `frontend/src/features/_shared/ItemIcon.tsx` · `RoleBadge.tsx` · `roleGroup.ts` — 품목 목록(활성/비활성)에 아이콘·역할 배지 재사용(v1.2).
- `frontend/src/components/state/AsyncBoundary.tsx` · `LoadingState`/`EmptyState`/`ErrorState` — admin 조회·mutation 상태에 재사용(Phase 7 D-07/08).
- `frontend/src/components/ui/*` — `button`·`card`·`table`·`select`·`badge`·`alert`·`skeleton` 설치됨. **`dialog`·`input`·`form`·`toast`는 미설치** → 폼 입력/토스트는 신규 필요(D-07 인라인이 dialog 회피). ⚠ shadcn CLI Windows `@` 디렉터리 버그 — 블록 추가 시 `src/components/ui/`에 수기 작성.
- `frontend/src/lib/formatKst.ts` — KST 표시 헬퍼. D-08 KST→UTC 역변환의 짝.

### Established Patterns
- **zod `.parse`-at-boundary 단일 출처** + Vite 프록시 동일 출처(dev). admin 요청도 프록시 `/api/admin/*`로.
- **TanStack Query 서버 상태 캐시** — 지금까지 read 전용(query). 이 페이즈가 **mutation + `invalidateQueries`** 를 처음 도입(D-10).
- **"loudly fail"** — 비-2xx/스키마 불일치는 throw → 상태 컴포넌트. 401은 예외적으로 전역 인터셉트해 로그아웃(D-03).
- **UTC 데이터 + KST 표시** off-by-9h 가드 — D-08이 쓰기 입력까지 연장.

### Integration Points
- `frontend/src/main.tsx` 라우트 트리에 `/admin`(+ 로그인 가드) 추가(D-04/05). 신규 `frontend/src/features/admin/` 디렉터리(Phase 7 D-11 feature 구조).
- `frontend/src/lib/api.ts` — admin API 함수(events CRUD · items CRUD+list · `X-Admin-Secret` 첨부 래퍼). 공용 request에 admin 헤더 주입 지점.
- `frontend/src/lib/queries.ts` — admin query/mutation 훅.
- **백엔드 신규(유일):** `AdminItemController`에 `@GetMapping`(active+inactive 목록) + `AdminItemService` read 메서드 + `TrackedItemRepository`(전체 조회). 기존 `AdminSecretFilter`/`SecurityConfig`가 그대로 인증 커버(무변경). `TrackedItemResponse`(이미 `active` 포함) 재사용.

</code_context>

<specifics>
## Specific Ideas

- **포트폴리오 honesty 서사(D-01):** 관리자 시크릿을 브라우저 스토리지에 두는 트레이드오프(STATELESS 백엔드 · 단일 시크릿 게이트 한 겹 · sessionStorage로 노출 창 최소)를 정직하게 서술 — "풀 유저/권한 모델은 AUTH-V2"라는 명시적 경계와 함께. 면접 설명 포인트.
- **백엔드 무변경 원칙의 정직한 예외(D-13/14):** 마일스톤 "기존 소비" 원칙 하에서, ADMINUI-04 재활성화 UX만 소규모 read-only 엔드포인트로 충족하고 **인증·수집·캐시·event-impact는 0줄**임을 명확히 문서화. "요구 충실도와 Core Value 가드를 함께 지키는 최소 확장" 서사.
- **읽기(데모) ↔ 쓰기(관리자) 대칭:** 공개 3화면은 read-only, 관리자 콘솔은 시크릿 게이트 뒤 write — 같은 zod/Query 기반 위에서 mutation을 처음 도입하는 대비.

</specifics>

<deferred>
## Deferred Ideas

- **수집 이력 리스트(최근 N건)** — 이번엔 최신 상태 카드로 충족(D-11). 리스트형 이력이 필요해지면 `GET /api/admin/collection-runs?limit=N` 추가는 v2/후속 후보.
- **풀 유저/권한 모델(아이디·비번·역할)** — AUTH-V2. 이번은 시크릿 게이트 한 겹.
- **성공/오류 토스트 시스템 고도화** — 이번은 인라인/경량으로 충분(D-09 Claude 재량). 본격 토스트 라이브러리는 FE-V2 후보.
- **실시간 자동 갱신** — FE-V2-01. 관리자 콘솔도 fetch-on-mount + mutation 후 invalidate로 충분.
- **대시보드 카드 개선(고유번호 제거·딥링크)** — Phase 16. 실데이터 전환 — Phase 17. 배포/보안 게이트 — Phase 18.

None 외 모두 위에 보존됨 — 논의는 Phase 15 스코프(기존 admin API를 어떻게 소비/보호/표시) 안에 머물렀다.

</deferred>

---

*Phase: 15-관리자 콘솔 UI*
*Context gathered: 2026-07-01*
