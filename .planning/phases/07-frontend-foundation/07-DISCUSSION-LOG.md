# Phase 7: Frontend Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-25
**Phase:** 7-frontend-foundation
**Areas discussed:** 데이터 패칭 전략, 라우팅·URL 전략, API 경계 런타임 검증, 비동기 상태 아키텍처

---

## 데이터 패칭 전략

| Option | Description | Selected |
|--------|-------------|----------|
| TanStack Query (추천) | React Query로 서버 상태 캐시·로딩/에러/재시도/중복요청 제거 기본 제공. FND-04와 직결, 업계 표준. 백엔드 Redis 캐시와 서사 대칭. | ✓ |
| 직접 fetch 훅 | useEffect/useState 손수 작성 useFetch. 의존성 최소·번들 가볍지만 로딩/에러/중복요청 직접 구현, 보일러플레이트↑. | |
| 직접 결정해줘 (You decide) | Claude 판단 위임. | |

**User's choice:** TanStack Query (추천)
**Notes:** 후속 정책은 빌더 기본값으로 합의 — fetch-on-mount만, `refetchOnWindowFocus: false`, 높은 `staleTime`, 재시도는 UI-SPEC "다시 불러오기". 실시간은 FE-V2-02로 명시 연기.

---

## 라우팅·URL 전략

| Option | Description | Selected |
|--------|-------------|----------|
| React Router 실경로 (추천) | `/dashboard`·`/timeline`·`/impact` 실제 URL. 새로고침·딥링크·스크린샷·뒤로가기 안정. 데모 표면에 적합. | ✓ |
| 인메모리 탭 전환 | useState로 현재 탭만 추적. 가볍지만 새로고침 리셋·딥링크 불가·뒤로가기 안 됨 → 데모 체감 저하. | |
| 직접 결정해줘 (You decide) | Claude 판단 위임. | |

**User's choice:** React Router 실경로 (추천)
**Notes:** `/`→`/dashboard` 리다이렉트. 포워드 노트: Phase 9~10 필터 상태(item id, from/to, window)는 URL 쿼리 파라미터로 두어 딥링크·스크린샷 재현 — 라우터를 그 방향으로 세팅. Phase 7은 셸+빈 라우트 골격까지만.

---

## API 경계 런타임 검증

| Option | Description | Selected |
|--------|-------------|----------|
| zod 단일 소스 (추천) | zod 스키마 단일 소스 → z.infer로 TS 타입 + 경계 런타임 검증. 드리프트·null 필드를 경계에서 잡아 silent 버그 차단. honest-data 에토스·포트폴리오 방어 시그널. | ✓ |
| 손수 TS 타입만 신뢰 | interface로 5개 DTO 작성, 런타임 검증 없이 계약 신뢰. 가볍지만 드리프트에 침묵·타입과 검증 분리. | |
| 직접 결정해줘 (You decide) | Claude 판단 위임. | |

**User's choice:** zod 단일 소스 (추천)
**Notes:** 검증 실패 동작 기본값 — 경계 `.parse` → 실패 throw → React Query 에러 → 공용 에러 상태. unknown 키 strip(전방호환).

---

## 비동기 상태 아키텍처

| Option | Description | Selected |
|--------|-------------|----------|
| AsyncBoundary 래퍼 (추천) | 프레젠테이셔널 LoadingState/EmptyState/ErrorState(UI-SPEC 시각) + 얇은 `<AsyncBoundary>`가 React Query 상태+isEmpty 매핑. 화면은 성공 경로만 작성, DRY·테스트 용이. | ✓ |
| 화면별 조합 | 공용 프레젠테이셔널만 두고 각 화면이 if 분기 직접 작성. 유연하나 분기 반복·일관성은 규율 의존. | |
| 직접 결정해줘 (You decide) | Claude 판단 위임. | |

**User's choice:** AsyncBoundary 래퍼 (추천)
**Notes:** A(React Query)·C(zod 에러)와 자연 결합. 최상위 React ErrorBoundary 포함 여부는 Claude 재량.

---

## Claude's Discretion

- shadcn `init` 실행 시점·블록 추가 목록(UI-SPEC Registry 따름).
- `staleTime` 정확값, `isEmpty` 술어 구현, 최상위 ErrorBoundary 포함 여부, 디렉터리 세부 명명.
- 빌더 기본값으로 합의된 항목: FND-05 KST 헬퍼(native Intl/Asia/Seoul), FND-02 프록시 설정, 폴더 구조, API 클라이언트 모듈 구성.

## Deferred Ideas

- 필터 상태 URL 쿼리 인코딩 상세 — Phase 9~10.
- 실시간 자동 갱신(폴링/SSE) — FE-V2-02 (v2).
- 다크모드·테마 토글, i18n — FE-V2-03 (v2).
- 관리자 쓰기 UI — FE-V2-01 (v2).
- 정적 서빙 단일 출처 — DEMO-03 (Phase 11, 선택).
