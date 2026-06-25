# Phase 7: Frontend Foundation - Context

**Gathered:** 2026-06-25
**Status:** Ready for planning

<domain>
## Phase Boundary

`frontend/`에 Vite + React + TypeScript + Tailwind 앱을 세우고 — Vite 프록시(`/api`→:8080, **백엔드 무변경**)로 read API를 호출하며, 실측 5개 read DTO와 일치하는 타입드 API 클라이언트, 3화면(대시보드/품목 타임라인/이벤트 영향) 상단 내비게이션, 공용 로딩/빈/에러 상태 컴포넌트, UTC→KST 표시 헬퍼를 마련한다. (FND-01~05)

이 페이즈는 **앱 셸·내비게이션·타입드 클라이언트·공용 상태 기반**까지만 — 각 화면의 실제 데이터 위젯(헬스 카드·차트·임팩트 표)은 Phase 8~10에서 채운다. 빈 라우트 골격이라도 3탭 네비게이션과 공용 상태 컴포넌트가 동작하면 완료.

**범위 밖(다른 페이즈/마일스톤):** 실시간 자동 갱신, 관리자 쓰기 UI, 다크모드·i18n, 정적 서빙 단일 출처, 백엔드 도메인 로직 변경.
</domain>

<decisions>
## Implementation Decisions

### 데이터 패칭 (서버 상태)
- **D-01:** 서버 상태 관리는 **TanStack Query (React Query)**. 5개 read 엔드포인트 각각 query 훅으로 노출. 캐싱·로딩/에러/재시도/중복요청 제거를 기본 제공받아 FND-04 공용 상태와 직결. (백엔드 v1.0 Redis 서빙 캐시와 "서버 상태 캐시" 서사 대칭 — 포트폴리오 설명 포인트.)
- **D-02:** **fetch-on-mount / route-entry만** 수행. `refetchOnWindowFocus: false`, 높은 `staleTime`(사실상 정적 seed 데이터 가정), 폴링/인터벌 없음 — 실시간 자동 갱신은 FE-V2-02로 명시 연기. 사용자 재시도는 UI-SPEC의 "다시 불러오기" CTA가 query refetch를 호출.

### 라우팅·URL
- **D-03:** **React Router(실제 경로)**. 라우트 `/dashboard` · `/timeline` · `/impact`, `/`→`/dashboard` 리다이렉트. 새로고침 시 현 화면 유지·딥링크·스크린샷 안정성·브라우저 뒤로가기 동작(데모 표면 체감). 인메모리 탭 전환은 기각.
- **D-04 (포워드 노트, Phase 7 산출물 아님):** Phase 9~10의 필터 상태(item id, from/to, window)는 **URL 쿼리 파라미터(searchParams)**로 인코딩 — 딥링크·스크린샷 재현용. Phase 7은 라우터 셸 + 빈 라우트 골격까지만 세팅(필터 UI는 후속 페이즈). 라우터를 이 방향으로 구성.

### API 경계 검증·타입
- **D-05:** **zod를 단일 소스**로. 5개 DTO를 zod 스키마로 정의하고 `z.infer`로 TS 타입 파생 → 타입과 검증을 따로 유지하지 않음. (백엔드 무변경·OpenAPI 코드젠 없음이라 스키마는 REQUIREMENTS의 실측 API 계약 표와 1:1로 손수 작성.)
- **D-06:** API 클라이언트 **경계에서 `.parse`로 응답 런타임 검증**. 실패 시 throw → React Query 에러 → 공용 에러 상태 컴포넌트로 표면화("loudly fail"). zod 기본 동작으로 unknown 키는 strip(백엔드가 필드 추가해도 전방호환). null 가능 필드(insufficient_data의 anchor/changeRate)를 경계에서 명시적으로 다뤄 차트로 원단 silent 전파 차단 — off-by-9h·"honest data" 에토스의 프론트 연장.

### 비동기 상태 아키텍처 (FND-04)
- **D-07:** 공용 **프레젠테이셔널 컴포넌트** — `LoadingState`(스켈레톤 + 보조 텍스트 "불러오는 중…"), `EmptyState`(UI-SPEC 빈 상태 카피), `ErrorState`(에러 카피 + "다시 불러오기" CTA). UI-SPEC의 시각·카피 계약을 이 컴포넌트들이 담당.
- **D-08:** 얇은 **`<AsyncBoundary>` 래퍼**가 React Query 결과(status: pending/error/success)와 `isEmpty` 술어를 받아 알맞은 상태 컴포넌트를 렌더. 각 화면은 **성공 경로만** 작성 → 분기 로직 중앙화(DRY)·테스트 용이. D-01·D-06과 자연 결합.

### 빌더 기본값 (사용자 위임 — 게이트에서 이의 없음)
- **D-09 (FND-05 KST 헬퍼):** native **`Intl.DateTimeFormat({ timeZone: 'Asia/Seoul' })`** 기반 `formatKst()` 공용 헬퍼로 *표시*. 차트 x축 위치·정렬 계산은 **raw UTC epoch(ms)** 기준으로 하고 라벨만 KST 포맷 → off-by-9h 가드. tz 라이브러리(date-fns-tz/dayjs/Luxon) 미사용.
- **D-10 (FND-02 프록시):** `vite.config.ts` `server.proxy` `/api`→`http://localhost:8080`. `VITE_API_TARGET` env 오버라이드 허용(기본 localhost:8080). 백엔드 CORS 설정 추가 안 함(Vite 프록시로 dev 동일 출처).
- **D-11 (폴더 구조):** feature 기반 — `src/features/{dashboard,timeline,impact}/`; 공용은 `src/lib/`(api 클라이언트·zod 스키마·queries·formatKst), `src/components/ui/`(shadcn), `src/components/state/`(LoadingState/EmptyState/ErrorState/AsyncBoundary).
- **D-12 (API 클라이언트):** `src/lib/api.ts` 단일 모듈 — 엔드포인트별 타입드 함수 + zod 스키마. React Query 훅은 `src/lib/queries.ts`(또는 feature별) 정의.

### Claude's Discretion
- shadcn `init` 실행 시점(Vite 스캐폴딩 직후, UI-SPEC `shadcn_initialized: false` 따름)과 정확한 블록 추가 목록(UI-SPEC Registry: button/card/skeleton/alert/navigation-menu 우선).
- `staleTime` 정확값, `isEmpty` 술어 구현 방식(엔드포인트별 length/null 체크), 최상위 React `ErrorBoundary`(예기치 못한 렌더 크래시 대비) 포함 여부, 디렉터리 세부 명명.
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 요구사항·범위·API 계약
- `.planning/REQUIREMENTS.md` — FND-01..05 정의 + **"실제 API 계약(코드 실측)" 표(5개 read DTO의 정확한 필드/상태값)** + 확정 결정(Recharts / Vite 프록시 / seed 우선). **MUST read** — zod 스키마와 TS 타입은 이 표와 1:1로 일치해야 함.
- `.planning/ROADMAP.md` §"Phase 7: Frontend Foundation" — Goal · 완료 조건(5) · 검증 방법 · 사용자 확인 포인트.

### 디자인 계약 (시각·카피·레이아웃 — 재정의 금지)
- `.planning/phases/07-frontend-foundation/07-UI-SPEC.md` — **디자인 계약(MUST read)**. 디자인 시스템(shadcn/ui slate·new-york, lucide-react, Inter), spacing/typography/color/semantic 토큰, Copywriting 계약(내비 라벨·빈/에러/로딩 카피·브랜드 타이틀), 상단 내비 레이아웃·시각 위계, Registry 안전(blocks 목록). 본 CONTEXT의 D-07 상태 컴포넌트는 이 파일의 카피·시각을 그대로 구현.

### 프로젝트 원칙
- `.planning/PROJECT.md` §"Key Decisions" · §"Context" — UTC→KST off-by-9h 가드, **상관 ≠ 인과** 고지 원칙(Phase 10에서 핵심), seed 프로파일 우선, **백엔드 무변경**(사용자 제약).
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **프론트엔드 재사용 자산 없음 (greenfield).** `frontend/` 디렉터리 미존재, codebase 맵 없음. shadcn/ui 컴포넌트는 Phase 7 실행 중 `init`하며 추가(UI-SPEC `shadcn_initialized: false`).

### Established Patterns
- 백엔드 read API 5개가 **유일한 통합 표면**: `GET /api/health/collection`, `/api/items`, `/api/items/{id}/latest`, `/api/items/{id}/prices?from=&to=`, `/api/items/{id}/event-impact?window=N`. 전부 `permitAll`(인증 불필요), 모든 시각은 UTC ISO-8601(`...Z`).
- **백엔드 `src/` 변경 금지** — 이 마일스톤은 기존 read API 소비만(사용자 제약). 사용자 확인 포인트에 `git status`에 `src/` 변경 없음 확인 포함.
- v1.0 백엔드의 Redis 서빙 캐시 패턴 → 프론트 React Query "서버 상태 캐시"와 서사 대칭(포트폴리오 설명 포인트, 강제 아님).

### Integration Points
- **Vite 프록시** `/api`→`http://localhost:8080` (dev 동일 출처 → CORS 회피, 백엔드 코드 0줄 변경).
- **seed 프로파일 백엔드**(`SPRING_PROFILES_ACTIVE=seed`)가 로컬에 떠 있어야 3화면이 비어있지 않게 재현됨 — Phase 7은 이를 전제로 검증.
</code_context>

<specifics>
## Specific Ideas

- **포트폴리오 서사:** "백엔드 Redis 서빙 캐시 ↔ 프론트 React Query 서버 상태 캐시" 대칭, "zod 경계 검증 = 프로젝트의 honest-data·off-by-9h 가드의 프론트 연장" — 면접 설명 포인트로 의식하되 구현 강제는 아님.
- **read-only 데모:** 파괴적 버튼·쓰기 UI 없음(UI-SPEC Copywriting "Destructive confirmation: 해당 없음"과 일치). 에러 상태의 "다시 불러오기"가 유일한 주요 CTA.
</specifics>

<deferred>
## Deferred Ideas

- **필터 상태 URL 쿼리 인코딩 상세 구현** — Phase 9~10 (포워드 노트 D-04에서 라우터 방향만 선반영).
- **실시간 자동 갱신(폴링/SSE/WebSocket)** — FE-V2-02 (v2). D-02에서 명시 비활성.
- **다크모드·테마 토글, i18n** — FE-V2-03 (v2). UI-SPEC도 라이트 단일 테마로 잠금.
- **관리자 쓰기 UI(CRUD)** — FE-V2-01 (v2). read-only 시각화 경계.
- **정적 서빙 단일 출처** — DEMO-03 (Phase 11, 선택/stretch).

None other — 논의는 Phase 7 범위 내에 머물렀다.
</deferred>

---

*Phase: 7-frontend-foundation*
*Context gathered: 2026-06-25*
