---
phase: 14
slug: frontend-icons-fallback-docs
status: approved
shadcn_initialized: true
preset: new-york / slate / tailwind v4 (css variables, no tailwind.config)
created: 2026-06-29
reviewed_at: 2026-06-29
---

# Phase 14 — UI Design Contract

> Visual and interaction contract for the icon + role-badge + fallback enrichment layer.
> Phase 14 는 **순수 프론트 소비 한 겹**(백엔드 `src/` 0줄)이다. 이 계약서는 v1.1 디자인 시스템
> (`frontend/src/index.css` 토큰 — Phases 8–10이 이미 소비)을 **상속**하고, 신규로 추가되는
> **역할 시각 시스템(팔레트·배지·아이콘 슬롯·fallback)** 만 잠근다. 기존 토큰은 재정의하지 않는다.

---

## Design System

| Property | Value | Source |
|----------|-------|--------|
| Tool | shadcn (new-york) | `frontend/components.json` (detected) |
| Preset | new-york / baseColor `slate` / `cssVariables: true` / no `tailwind.config` (Tailwind v4 CSS-first) | detected |
| Component library | radix-ui (`@radix-ui/react-select`, `react-slot`, `react-navigation-menu`) | `package.json` (detected) |
| Icon library | **lucide-react** (이미 의존, 외부 아이콘 라이브러리 추가 금지 — D-02) | detected |
| Font | Inter Variable (`@fontsource-variable/inter`, 번들 — 런타임 네트워크 없음) | `index.css` (detected) |
| Theme | Light single theme only (다크모드 = FE-V2, 범위 밖) | `index.css` |

**상속 원칙:** 신규 컴포넌트(`<ItemIcon>`, 역할 배지)는 raw hex를 직접 쓰지 않고 토큰/유틸을 통해
색을 참조한다. 단, 역할색 3종은 index.css에 신규 토큰으로 추가한다(아래 Color §).

---

## Spacing Scale

8-point scale (Tailwind 기본 — 이미 전 화면이 `gap-2`/`space-y-4` 등으로 소비). 변경 없음.

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | 아이콘↔라벨 미세 간격, 배지 내부 아이콘 gap (`gap-1`) |
| sm | 8px | **아이콘 슬롯 ↔ 품목명 간격, 품목명 ↔ 역할 배지 inline 간격 (`gap-2`)** |
| md | 16px | 카드 내부 기본 간격 (`space-y-4`) |
| lg | 24px | 섹션 패딩 |
| xl | 32px | 레이아웃 갭 |
| 2xl | 48px | 주요 섹션 분리 |
| 3xl | 64px | 페이지 레벨 간격 |

**Phase 14 exceptions:** 없음. 아이콘 슬롯은 spacing 토큰이 아닌 **고정 px 슬롯**(아래 ItemIcon §)으로
레이아웃 시프트(ICON-01)를 방지한다.

---

## Typography

Inter Variable, 변경 없음. 신규 텍스트(역할 배지 라벨, 셀렉터 그룹 헤더)는 기존 배지/라벨 타이포에 정렬.

| Role | Size | Weight | Line Height | Usage |
|------|------|--------|-------------|-------|
| Display | 20px (`text-xl`) | 600 | 1.2 | 카드 타이틀 `CardTitle` (품목명) — 변경 없음 |
| Body | 16px (`text-base`) | 400 | 1.5 | 가격 본문 — 변경 없음 |
| Label | 14px (`text-sm`) | 600 | 1.4 | **역할 배지 라벨, 셀렉터 그룹 헤더, 셀렉터 옵션 텍스트** (기존 StatusBadge/ImpactStatusBadge `text-sm font-semibold` 선례에 정렬) |
| Caption | 12px (`text-xs`) | 400/500 | 1.4 | 서브타이틀·메타(category, KST) — 변경 없음 |

**Weights:** 400 (regular) + 600 (semibold) — 2단계, 변경 없음.
**역할 배지 라벨:** `text-sm font-semibold` (sibling 의미 배지와 동일). cva 기본 `text-xs` 를 className으로
오버라이드(StatusBadge 선례) 하거나 신규 variant 내부에 size 포함 — 둘 다 허용.

---

## Color

### 상속(변경 없음) — v1.1 토큰 (`index.css`)

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `#F8FAFC` slate-50 | app 배경 / dominant surface |
| Secondary (30%) | `#FFFFFF` (card) + `#F1F5F9` slate-100 | 카드 / subtle surface |
| **Accent (10%)** | `#2563EB` blue-600 | **active nav · primary CTA · focus ring 전용 — 변경/확장 금지** |
| Destructive | `#DC2626` red-600 | error 비주얼 |
| Semantic up/down/warning/neutral | `#16A34A` / `#DC2626` / `#D97706` / `#64748B` | 가격 상승·하락·경고·중립 (tinted 상태 배지 전용) |

**Accent reserved for:** active nav 항목 · primary CTA · `focus-visible` ring. **그 외 어떤 요소에도
blue-600 accent 를 쓰지 않는다.** 역할색은 accent 가 아니라 **semantic 색군**(up/down/warning 과 동급
계열)으로 신규 편입 — index.css 의 "Semantic colors — meaning, not decoration. Kept distinct from the
single blue accent" 선례를 그대로 따른다.

### 신규 — 역할 팔레트 (D-01) · index.css 에 추가할 토큰

역할 3군의 **색 배경 배지**(D-03)는 **solid 채움 + 흰 텍스트**. 가격 semantic 색군(up/down/warning)과
**hue 를 의도적으로 오프셋**해, 색이 비슷해도 헷갈리지 않게 한다(아래 Badge Taxonomy §의 처리 분리와 이중 가드).

| 토큰 | Value | 도메인 직관 | 대비(흰 텍스트) | 가격 semantic 과의 구별 |
|------|-------|-------------|----------------|------------------------|
| `--role-dealer` | `#E11D48` rose-600 | 딜러 = 공격(레드 계열) | **4.70:1 ✓ AA** | down/destructive red-600 `#DC2626` 와 다른 rose hue |
| `--role-support` | `#047857` emerald-700 | 서포터 = 회복·버프(그린 계열, 로아 서포터=초록 관습) | **5.50:1 ✓ AA** | up green-600 `#16A34A` 보다 진한 emerald |
| `--role-material` | `#B45309` amber-700 | 융화재료 = 재화(앰버/골드 계열) | **5.05:1 ✓ AA** | warning amber-600 `#D97706` 보다 진한 amber |

> **대비 근거:** 14px/600 라벨은 WCAG 기준 normal text → AA 4.5:1 필요. 세 hue 모두 흰 텍스트와 ≥4.7:1.
> 토큰은 `@theme inline` 에 `--color-role-dealer` 등으로 노출해 `bg-role-dealer text-white` 유틸로 소비.

### Light-theme dominance check (60/30/10)

역할 배지는 품목당 **단 1개**의 작은 pill(px-2 py-0.5)이라 화면 면적 점유는 미미 — 60/30/10 균형을
깨지 않는다. solid 색은 "스캔 가능한 자산 섹터 코드"로만 기능하고, 본문·표면은 그대로 slate/white.

---

## Component Spec — `<ItemIcon>` (ICON-01)

신규 공용 컴포넌트. `frontend/src/features/_shared/ItemIcon.tsx`. 4곳에서 소비.

### Props

| Prop | Type | Note |
|------|------|------|
| `iconUrl` | `string \| null` | zod `.nullable()`. null/undefined → 즉시 fallback |
| `roleGroup` | `'DEALER' \| 'SUPPORT' \| 'MATERIAL' \| null` | fallback 글리프·배경색 결정. null → neutral 슬롯 |
| `size` | `'md' \| 'sm'` (default `'md'`) | md=카드용, sm=셀렉터 옵션용 |
| `alt` | 내부 처리 | 아래 alt 규칙 참조 |

### 고정 슬롯 (레이아웃 시프트 0 — ICON-01)

| size | 슬롯 px | img w/h | fallback 글리프 px |
|------|---------|---------|-------------------|
| `md` | **32×32** (`size-8`) | `width={32} height={32}` | 20 (`size-5`) — 슬롯의 ~62%, 4-grid 정렬 |
| `sm` | **20×20** (`size-5`) | `width={20} height={20}` | 12 (`size-3`) — 슬롯의 60%, 4-grid 정렬 |

> 슬롯(32/20)·img(32/20)·글리프(20/12) px 전부 4의 배수 — 8-point grid 정렬 유지. 슬롯↔라벨 간격은
> spacing 토큰 `gap-2`(8px). 아이콘 치수는 spacing 스케일이 아닌 컴포넌트 고정 치수다.

- 슬롯은 항상 동일 px 점유(성공/실패/로딩 무관) → **시프트 없음**. `shrink-0`, `rounded-md`(`--radius-md`),
  `overflow-hidden`.
- `<img>`: `loading="lazy"`, `decoding="async"`, `width`/`height` 명시(intrinsic ratio 고정).
- **alt 규칙:** 아이콘은 항상 품목명 텍스트와 **인접**하므로 `alt=""`(decorative) — SR 중복 announce 방지.
  앱의 "icon + 항상 존재하는 text label, color/icon-alone 금지" ethos 와 일치. (단독 노출 지점이 생기면
  `alt={displayName}` 로 승격 — 현재 4곳 모두 라벨 인접이라 decorative.)

### Fallback (D-02, D-06)

**트리거(둘 다):** `iconUrl == null` **OR** `<img>` `onError`(로딩 실패/CDN 차단). 고정 슬롯 안에서 교체 →
시프트 없음.

| roleGroup | 글리프 (lucide) | 슬롯 배경 | 글리프 색 |
|-----------|----------------|-----------|-----------|
| `DEALER` / `SUPPORT` (각인서) | **`ScrollText`** | 역할색 solid (`bg-role-dealer` / `bg-role-support`) | white |
| `MATERIAL` (융화재료) | **`FlaskConical`** | `bg-role-material` solid | white |
| `null`/unknown | `Package` (또는 ScrollText) | `bg-muted` (slate-100) | `text-muted-foreground` |

- **"미완성처럼 보이지 않게"**(ROADMAP 사용자 확인 포인트): fallback 은 회색 플레이스홀더가 아니라
  **역할색으로 꽉 찬 글리프 타일** → "의도된 디자인"으로 읽힘. neutral 은 역할 미상일 때만.
- onError 는 한 번만 발생하도록 처리(에러 후 state 전환 → img 미렌더, 재시도 루프 방지).
- **각인서 실아이콘(11종 동일 글리프, Phase 12 (d) identical)은 fallback 으로 일부러 대체하지 않는다**
  (D-05): 실아이콘 그대로 렌더 + 한글 라벨 + 역할 배지로 식별. fallback 은 어디까지나 null/onError 방어.

---

## Badge Taxonomy (3-treatment system — 충돌 방지의 핵심)

앱에는 이미 **2가지 의미 배지 처리**가 존재. 역할 배지를 **3번째 처리(solid)** 로 두어, hue 가 겹쳐도
**처리 방식만으로 즉시 구별**된다(이중 가드: 처리 분리 + hue 오프셋 + 한글 라벨).

| 배지 군 | 처리(treatment) | 예시 | 의미 축 |
|---------|----------------|------|---------|
| **상태(Status)** | **tinted** `variant="secondary"` + `bg-{token}/10 text-{token}` | StatusBadge(정상/일부실패), ImpactStatusBadge(비교가능/데이터부족) | 측정·동적 |
| **이벤트 타입(Event)** | **outline** `variant="outline"` + 컬러 border/text | EventTypeBadge (로아ON 등 4색) | 분류·taxonomy |
| **역할(Role) — 신규** | **solid 색배경**(D-03) `bg-role-* text-white` | 딜러/서포터/융화재료 | 품목 정체성(intrinsic) |

> 예: Event Impact 화면에서 emerald solid "서포터" 역할 배지와 green-tint "비교 가능" 상태 배지가
> 가까워도, **solid vs tinted** 로 한눈에 다른 의미임이 읽힌다. (게다가 역할 배지는 품목 정체성 영역,
> 상태 배지는 이벤트/상태 영역으로 zone 도 분리.)

### 역할 배지 스펙 (D-03, D-04, ICON-06)

| 항목 | 값 |
|------|-----|
| 형태 | solid 색배경 pill (`bg-role-* text-white`, `rounded-full`, `px-2 py-0.5`) |
| 라벨(한글) | `DEALER`→**딜러** · `SUPPORT`→**서포터** · `MATERIAL`→**융화재료** |
| 타이포 | `text-sm font-semibold` (sibling 의미 배지 선례) |
| 배치 | **품목명 옆 inline** (`gap-2`) — 4곳 공통(D-04) |
| 구현 | `ui/badge.tsx` cva 에 `dealer`/`support`/`material` variant 신규 추가 **또는** className 오버라이드(StatusBadge 선례). 둘 다 허용 |
| 접근성 | 색 단독 금지 — 항상 한글 라벨 동반. 아이콘 글리프는 선택(배지엔 텍스트만으로 충분) |

---

## Per-Surface Placement (ICON-02..05)

enrichment 3필드(`iconUrl`/`itemGroup`/`roleGroup`)는 zod 4개 스키마(`trackedItemSchema`/`latestPriceSchema`/
`timelineSchema`/`eventImpactSchema`)에 `.nullable()` 로 추가(`roleGroup` = `z.enum([...]).nullable()`).
event-impact 는 **wrapper 형태**(`{itemId, window, enrichment, events}`)로 재구성(13-02 정렬).

| # | 화면/컴포넌트 | 아이콘 위치 | 역할 배지 | enrichment 출처 |
|---|--------------|-------------|-----------|----------------|
| ICON-02 | `dashboard/ItemCard.tsx` | `CardTitle`(품목명) 좌측 inline (md 32px) | 품목명 우측 inline | `useItems()` 항목(list enrichment) |
| ICON-03 | `_shared/ItemSelect.tsx` | 각 `SelectItem` 좌측 (sm 20px) | 옵션 텍스트 우측 inline | `useItems()` 항목 |
| ICON-04 | `_shared/LatestPriceCard.tsx` | `CardTitle` 좌측 inline (md 32px) | 품목명 우측 inline | **props 추가**(`iconUrl`/`roleGroup`) — 호출처 `TimelinePage`/`ImpactPage` 동반 수정 |
| ICON-05 | `impact/EventImpactCards.tsx` 컨텍스트 | **품목 정체성 영역(페이지 헤더/선택 품목)에 1회** — 이벤트 카드마다가 아님 | 동일 1회 | **wrapper `enrichment`**(품목당 1개) |

> ICON-05 주의: event-impact enrichment 은 응답당 1개(품목 단위)다. 따라서 아이콘·역할 배지는 이벤트
> 리스트의 각 카드가 아니라 **선택된 품목의 정체성이 표시되는 곳**(ImpactPage 헤더/셀렉터 영역)에 1회
> 렌더한다. 이벤트 카드의 `EventTypeBadge`/`ImpactStatusBadge` 와 역할 배지가 같은 카드에서 충돌하지 않음.

### 셀렉터·대시보드 조직 (D-07, D-08)

- **정렬(D-07):** 역할군 `DEALER → SUPPORT → MATERIAL` → 이름. 백엔드 0줄이므로 **프론트 클라 정렬**
  (`useItems()` 결과를 셀렉터/대시보드에서 정렬). roleGroup null 은 말미.
- **셀렉터 그룹 헤더(D-08):** `SelectGroup`/`SelectLabel`(이미 export 확인) 로 **딜러 / 서포터 / 융화재료**
  정적 섹션 헤더. (필터 컨트롤 아님 — 필터는 v2.) 헤더 라벨 = `text-muted-foreground text-xs`(SelectLabel 기본).

---

## Copywriting Contract

Phase 14 는 읽기 전용 enrichment 한 겹 — 신규 CTA·폼·destructive 액션 없음. 신규 카피는 **라벨·헤더** 위주.

| Element | Copy |
|---------|------|
| Primary CTA | 없음 (읽기 전용 enrichment — 신규 인터랙션 없음) |
| 역할 배지 라벨 | 딜러 / 서포터 / 융화재료 (D-03) |
| 셀렉터 그룹 헤더 | 딜러 / 서포터 / 융화재료 (D-08) |
| 아이콘 alt | `""`(decorative — 품목명 텍스트 인접). SR 중복 방지 |
| Fallback 상태 | **텍스트 없음** — 역할색 글리프 타일이 곧 상태 표현. "이미지 없음" 류 문구 미표기 |
| Error state | 아이콘 로딩 실패 → fallback 글리프(텍스트 에러 없음). 카드/화면 레벨 에러는 기존 inline 상태 패턴 유지(번지지 않음) |
| Empty state | Phase 14 신규 없음 — 최신가 없음 등 기존 카피(`최신가 아직 없음`) 유지 |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | 기존 `badge`/`select`/`card`/`skeleton` 재사용 + (필요 시) badge variant 수기 확장 | not required |
| third-party | **none** | not applicable |
| 외부 아이콘 라이브러리 | **none** — `lucide-react`(기존 의존)만 사용(D-02) | not applicable |

> 신규 npm 의존 추가 없음. `<ItemIcon>`·역할 배지·셀렉터 그룹화는 모두 기존 의존(react/lucide/radix-select/
> cva/tailwind-merge)으로 구현. 백엔드 `src/` 0줄(상시 가드).

---

## Inherited Invariants (상시 가드 — CONTEXT 불변 제약)

- 프론트에서 Lostark Open API **직접 호출 금지** — 백엔드 DTO만 소비.
- 백엔드 `src/` **0줄 변경** (`git status` 검증).
- 실 API 키·계정 식별자·가격 원문 미기재.
- `npm run build`(tsc 포함) 통과 + seed만으로(키 없이) 3화면 아이콘 표시 + CDN 차단/아이콘 부재에도 무파손.
- 그룹 **필터 컨트롤은 v2** — v1.2는 역할 배지 + 그룹 헤더로 대체.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS — 신규 카피(역할 라벨·셀렉터 헤더) 모두 구체적, generic CTA 없음(읽기 전용 phase, CTA 부재는 정당)
- [x] Dimension 2 Visuals: PASS — enrichment 레이어, 시각 위계(Display=품목명) + 4면 배치 명시, icon-only 없음(항상 라벨 인접)
- [x] Dimension 3 Color: PASS — accent(blue) reserved-for 3개 명시·확장 금지, 역할색은 semantic 색군으로 분리(accent 아님), 60/30/10 선언
- [x] Dimension 4 Typography: PASS — 4 sizes(12/14/16/20) · 2 weights(400/600), body line-height 1.5 선언
- [x] Dimension 5 Spacing: PASS — 스케일 {4,8,16,24,32,48,64} 전부 4의 배수, 아이콘 치수(32/20/20/12)도 4-grid 정렬
- [x] Dimension 6 Registry Safety: PASS — shadcn official 전용, third-party·외부 아이콘 라이브러리 none, 신규 npm 의존 0

**Approval:** approved 2026-06-29
