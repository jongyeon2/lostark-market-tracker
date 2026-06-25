---
phase: 8
slug: dashboard
status: approved
shadcn_initialized: true
preset: "slate base color / new-york style (Phase 7에서 init 완료)"
created: 2026-06-25
reviewed_at: 2026-06-25
---

# Phase 8 — UI Design Contract

> Dashboard(DASH-01~04)의 시각·인터랙션 계약. gsd-ui-researcher 생성, gsd-ui-checker 검증.
> **이 계약은 Phase 7의 [07-UI-SPEC](../07-frontend-foundation/07-UI-SPEC.md)에서 확정된 v1.1 디자인 시스템(토큰·타이포·색·시맨틱)을 상속한다.** 본 문서는 그 토큰을 Dashboard 위젯(health 카드 · 품목 카드 그리드 · status 배지)에 적용하는 *위젯 계약*만 새로 정의하며, 토큰은 재정의하지 않는다.

상위 산출물에서 이미 확정된 사실(재정의 안 함):

- 디자인 시스템·스페이싱·타이포·색·시맨틱 색: **07-UI-SPEC 상속** (아래 표는 상속값 재기재)
- 데이터 결정: **08-CONTEXT.md D-01~D-08** (카드 그리드 / 카드별 useLatestPrice / status 4등급 / 카드별 빈·대기)
- 실측 API: `/api/health/collection`, `/api/items`, `/api/items/{id}/latest` (read 전용, UTC→KST 표시)
- 프론트 토큰 실제 구현: `frontend/src/index.css`에 `--up`/`--down`/`--warning`/`--neutral` + Tailwind `text-up`/`bg-warning`/… 유틸 이미 존재

---

## Design System

(07-UI-SPEC 상속 — Phase 7 실행 시 init 완료, `frontend/components.json` 존재)

| Property | Value |
|----------|-------|
| Tool | **shadcn/ui** (Radix primitives + Tailwind) |
| Preset | base color `slate`, style `new-york` (init 완료) |
| Component library | radix (shadcn/ui 경유) |
| Icon library | **lucide-react** |
| Font | **Inter** (variable), 폴백 `system-ui, -apple-system, sans-serif` |

> 가격·카운트 등 숫자는 `font-variant-numeric: tabular-nums`로 렌더해 자릿수 정렬 유지(07-UI-SPEC 규약).

---

## Spacing Scale

(07-UI-SPEC 상속 — 8-point 스케일, 모두 4의 배수)

| Token | Value | Phase 8 Usage |
|-------|-------|---------------|
| xs | 4px | 배지 내부 패딩, 아이콘-라벨 간격 |
| sm | 8px | 라벨-값 간격, 카드 내 조밀 요소 |
| md | 16px | 카드 내부 패딩, 카드 내 행 간격 |
| lg | 24px | 카드 그리드 갭, health 카드 ↔ 품목 그리드 구분 |
| xl | 32px | 콘텐츠 영역 좌우 여백 |
| 2xl | 48px | (미사용 — 단일 스크롤 대시보드) |
| 3xl | 64px | 페이지 상/하단 여백 |

Exceptions: none.

---

## Typography

(07-UI-SPEC 상속 — 4 사이즈 / 2 웨이트 고정. **새 사이즈·웨이트 추가 금지**)

| Role | Size | Weight | Line Height | Phase 8 Usage |
|------|------|--------|-------------|---------------|
| Display | 28px | 600 | 1.2 | 페이지 제목 "대시보드" |
| Heading | 20px | 600 | 1.3 | health 카드 제목, 품목 카드 displayName |
| Body | 16px | 400 | 1.5 | minPrice 값, 본문·상태 메시지 |
| Label | 14px | 600 | 1.4 | status 배지, category, 카운트 라벨, collectedAt 캡션 |

- minPrice 숫자는 **Body 사이즈 + `tabular-nums`** (강조 시 Heading 가능, 단 새 사이즈 도입 아님).
- status 배지 텍스트는 Label(14/600).

---

## Color

(07-UI-SPEC 상속 — 라이트 단일 테마, 60/30/10 분할)

| Role | Value | Phase 8 Usage |
|------|-------|---------------|
| Dominant (60%) | `#F8FAFC` (slate-50) | 앱·대시보드 배경 |
| Secondary (30%) | `#FFFFFF` 카드 / `#E2E8F0`(slate-200) 보더 / `#0F172A`(slate-900)·`#64748B`(slate-500) 텍스트 | health 카드·품목 카드 표면, 보더, 텍스트 |
| Accent (10%) | `#2563EB` (blue-600) | 아래 reserved 목록만 |
| Destructive | `#DC2626` (red-600) | 에러 상태 시각(ErrorState) |

**Accent reserved for** (상속 — "모든 인터랙티브 요소" 금지):

1. 현재 활성 내비게이션 항목
2. 주요 CTA ("다시 불러오기")
3. 포커스 링 (`focus-visible`)
4. (Phase 9~) item selector 선택 표시, 차트 주 가격 라인

> Phase 8은 read-only라 accent 사용 표면은 내비 활성·에러 CTA·포커스 링뿐. **status 배지는 accent가 아니라 아래 Semantic 색을 쓴다** — 의미 색과 장식 액센트는 역할 분리.

### Semantic Colors (status 전용 — accent 아님)

(07-UI-SPEC 상속, `frontend/src/index.css`에 토큰·유틸 구현 완료)

| Semantic | Token | Value | Phase 8 의미 |
|----------|-------|-------|-------------|
| Up / 양호 | `--up` (`text-up`/`bg-up`) | `#16A34A` green-600 | status **SUCCESS = 정상** |
| Down / 위험 | `--down` (`text-down`/`bg-down`) | `#DC2626` red-600 | status **FAILED = 위험**, summaryMessage **AUTH_ERROR** |
| Warning / 부분 | `--warning` (`text-warning`/`bg-warning`) | `#D97706` amber-600 | status **PARTIAL_SUCCESS = 경고**, summaryMessage **RATE_LIMITED** |
| Neutral / 대기 | `--neutral` (`text-neutral`/`bg-neutral`) | `#64748B` slate-500 | status **NO_RUNS = 대기**, 빈/없음 상태 |

---

## Status Badge Mapping (Phase 8 핵심 계약 — DASH-02)

health `status` 4값(백엔드 실측: `PriceCollector.java`·`CollectionHealthResponse.java`)을 **배지 1개**로 표현한다. 배지 = shadcn `badge`(아래 Registry), 시맨틱 색의 **연한 배경 + 진한 텍스트**(예: `bg-warning/10 text-warning`), Label(14/600), 좌측 lucide 아이콘 동반(아이콘-only 금지 — 접근성).

| status 값 | 등급 | 한국어 라벨 | 색(시맨틱) | lucide 아이콘 |
|-----------|------|------------|-----------|--------------|
| `SUCCESS` | 정상 | **정상** | up (green) | `CircleCheck` |
| `PARTIAL_SUCCESS` | 경고 | **일부 실패** | warning (amber) | `TriangleAlert` |
| `FAILED` | 위험 | **전체 실패** | down (red) | `CircleX` |
| `NO_RUNS` | 대기 | **수집 대기** | neutral (slate) | `Clock` |
| (그 외 미지값) | 대기 | status 원문 그대로 표기 | neutral (slate) | `Clock` |

**summaryMessage 마커** (DASH-02 — 진단 보조, null이면 미표시):

| summaryMessage | 한국어 라벨 | 색 | 위치 |
|----------------|------------|-----|------|
| `AUTH_ERROR` | **인증 오류** | down (red) | health 카드 안, status 배지 아래 보조 라인 |
| `RATE_LIMITED` | **레이트리밋** | warning (amber) | 〃 |
| `null` | (표시 안 함) | — | — |

> 시크릿 필드는 응답에 없으므로 화면에도 없다(DASH-02). 마커는 카테고리컬 라벨만.

---

## Screen Contract — Dashboard

수직 1열 스크롤. 상단 health 카드(전폭) → 그 아래 품목 카드 그리드. 섹션 간 `lg(24px)`.

### 1) Collection Health 카드 (DASH-01/02) — 전폭, focal point

- **데이터:** `useCollectionHealth()` → `{ lastRunAt, startedAt, itemsAttempted, itemsSucceeded, itemsFailed, status, summaryMessage }`.
- **앵커리 (`<AsyncBoundary>` 자체 래핑):**
  - pending → `LoadingState`(스켈레톤)
  - error → `ErrorState` + "다시 불러오기"
  - success → 아래 내용
- **레이아웃(success):**
  - 제목 Heading "수집 헬스" + 우측 **status 배지**(위 매핑)
  - 카운트 행(Body/Label, tabular-nums): **시도 N · 성공 N · 실패 N**. 실패 N>0이면 실패 수치를 `text-down`으로.
  - 마지막 실행: **lastRunAt → formatKst** (Label 캡션). `null`이면 "—".
  - summaryMessage 마커 라인(있을 때만).
- **NO_RUNS(대기) 분기:** status가 `NO_RUNS`(카운트 0·lastRunAt null)면 카운트/실행시각 대신 카드 안에 대기 카피(아래 Copywriting). 배지는 "수집 대기"(neutral). **에러 아님 — 깨지지 않음**(완료조건 #5).

### 2) 품목 카드 그리드 (DASH-03/04)

- **목록 데이터:** `useItems()` → `[{ id, displayName, category, active, … }]`. 활성만, 백엔드 정렬 순서 유지(D-02).
- **그리드:** 반응형 카드 그리드, 갭 `lg(24px)`. 컬럼 수는 실행 재량(권장: `grid` 1열(모바일)→2열(sm)→3열(lg)). 데스크톱 우선.
- **그리드 레벨 상태:**
  - `useItems` pending/error → 그리드 영역 `<AsyncBoundary>`(LoadingState/ErrorState)
  - 빈 배열(품목 0개) → `EmptyState`(Phase 7 빈 카피)
- **품목 카드 1개 (= 1 품목, DASH-03+04 통합, D-01):**
  - 상단: **displayName**(Heading 20/600) + **category**(Label, `text-muted-foreground`)
  - 하단: **minPrice**(Body + tabular-nums) + **collectedAt → formatKst**(Label 캡션)
  - 최신가 영역은 **카드별 `useLatestPrice(id)` + 자체 `<AsyncBoundary>`**(D-03/D-07):
    - pending → 그 카드 가격 영역만 스켈레톤(목록 이름/카테고리는 이미 보임)
    - error/404/미수집 → 그 카드 가격 영역만 "최신가 아직 없음"(card-level empty, neutral). **다른 카드·health 영향 없음**(완료조건 #5).
    - success → minPrice·collectedAt 표시

---

## Copywriting Contract

언어: **한국어**. 코드 식별자/경로/엔드포인트는 영어 유지. (07-UI-SPEC 상속 + Phase 8 추가)

| Element | Copy |
|---------|------|
| Primary CTA | **"다시 불러오기"** (상속) |
| Empty state heading (품목 0개) | **"표시할 데이터가 아직 없어요"** (상속) |
| Empty state body (품목 0개) | **"seed 프로파일 백엔드를 기동하면 시세가 채워집니다 (`SPRING_PROFILES_ACTIVE=seed`)."** (상속) |
| Error state | **"백엔드에 연결하지 못했어요. seed 백엔드(`:8080`)가 켜져 있는지 확인하고 다시 불러오세요."** (상속) |
| Destructive confirmation | 해당 없음 — read-only 데모 |

### Phase 8 추가 카피

| Element | Copy |
|---------|------|
| Health NO_RUNS heading | **"아직 수집 실행 기록이 없어요"** |
| Health NO_RUNS body | **"스케줄러가 첫 수집을 마치면 시도·성공·실패와 마지막 실행 시각이 여기 표시됩니다."** |
| 품목 카드 최신가 없음 | **"최신가 아직 없음"** (card-level, 짧게 — 최초 수집 전/캐시 미스) |
| status 배지 라벨 | 정상 / 일부 실패 / 전체 실패 / 수집 대기 (위 매핑 표) |
| summaryMessage 라벨 | 인증 오류 (AUTH_ERROR) / 레이트리밋 (RATE_LIMITED) |
| 카운트 라벨 | **시도 · 성공 · 실패** / 마지막 실행 캡션 **"마지막 실행"** |

> 일반 문구("데이터 없음", "오류 발생") 금지(07-UI-SPEC 규약). 카드별 짧은 빈 문구는 예외적으로 "최신가 아직 없음" 허용(공간 제약 + 문맥 명확).

---

## Layout & Visual Hierarchy

- **레이아웃:** 07-UI-SPEC 앱 셸(상단 고정 내비 ~56px) 안, 콘텐츠 영역 `max-w-screen-xl` + 좌우 `xl(32px)`. 페이지 제목 Display "대시보드".
- **시각 위계(ROADMAP 확인 포인트):** ① **health 카드**(전폭, 최상단) = "파이프라인이 살아있다"의 1차 신호 → ② **품목 카드 그리드** = "무엇을 추적하며 지금 얼마인가". 위→아래로 읽으면 서사가 완성.
- **status 배지**가 health 카드 내 2차 초점 — 색(시맨틱)으로 정상/경고/위험/대기를 즉시 구분(오해 없는 배지 = ROADMAP 확인 포인트).
- **카드별 독립 상태**: 한 위젯의 로딩/에러/빈이 화면 전체를 가리지 않음 — 부분 실패에도 "살아있는 부분"이 계속 보임.
- 내비 항목·아이콘은 항상 텍스트 라벨 동반(접근성). status/summaryMessage 배지도 색만으로 의미 전달 금지 — 라벨 텍스트 필수.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | card, **badge**, skeleton, alert, button | not required (공식 레지스트리) |

서드파티 레지스트리: **없음** (none). → Registry Safety PASS.

> **`badge`는 아직 `frontend/src/components/ui/`에 없음** — Phase 8 실행 시 `npx shadcn add badge`로 추가 필요(07-UI-SPEC가 "badge는 Phase 8~10에서 사용" 예고). card/skeleton/alert/button은 Phase 7에서 추가 완료.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS

**Approval:** approved 2026-06-25
