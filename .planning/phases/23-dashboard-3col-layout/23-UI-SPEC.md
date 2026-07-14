---
phase: 23
slug: dashboard-3col-layout
status: draft
shadcn_initialized: true
preset: none
created: 2026-07-14
---

# Phase 23 — UI Design Contract (대시보드 3열 카테고리 레이아웃)

> UX-01 / UX-02. maplanet식 3열(좌 카테고리 필터 / 중앙 물품 / 우 소식). **순수 프론트 read-path — Core Value 0줄.**
> 이 phase는 **레이아웃 재구성**이다: 기존 검증된 디자인 시스템·카드·소식 패널을 재사용하고 **신규 디자인 토큰 0개**.
> 신규 컴포넌트는 `CategoryNav` 하나. `ItemCard`·`NewsPanel`은 **무변경**(내용 계약 유지).

**설계 근거:** `docs/superpowers/specs/2026-07-14-v1.5-market-scope-ux-design.md` 기능 A. 사용자 확정(2026-07-14): 카테고리 nav = **2단계 그룹(maplanet식)**, 필터 상호작용, 로컬 useState, 모바일 상단 칩.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | shadcn (기존 설정 — 프로젝트 자체 토큰, `frontend/src/index.css`) |
| Preset | none (slate/blue-600 자체 팔레트, 라이트 모드 전용 — `.dark` 블록 없음) |
| Component library | Radix UI (기존: navigation-menu·select·slot). **CategoryNav는 신규 Radix 원시요소 없이 순수 `<button>`으로 손수 작성** (shadcn CLI Windows 버그 회피) |
| Icon library | lucide-react (기존 dep) — **이 phase는 신규 아이콘 불필요** |
| Font | Inter Variable (기존 `--font-sans`) |

**신규 의존성 0.** 신규 shadcn 블록 0(기존 `card`/`button`/`skeleton` 재사용).

---

## Spacing Scale

기존 사용 토큰 재사용(전부 4의 배수). 신규 spacing 토큰 없음.

| Token | Value | Usage (이 phase) |
|-------|-------|------------------|
| xs | 4px | — |
| sm | 8px | nav leaf 간 세로 간격(`space-y-2`), 칩 간 gap |
| md | 16px | 카드 내부 gap(기존), nav 좌우 패딩 |
| lg | 24px | 섹션 세로 간격(`space-y-6`) |
| xl | 32px | **3열 grid 컬럼 gap(`gap-8`)** — 기존 2열과 동일 |
| 2xl | 48px | — |
| 3xl | 64px | — |

예외: nav leaf 세로 리듬 `space-y-1`/`space-y-3`(4px/12px — 4의 배수), 카드 `py-3`(12px, 기존 유지).

---

## Typography

기존 스크립트 재사용(신규 타이포 토큰 없음).

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Body (카드 이름·가격) | 16px (`text-base`) | 500 | 1.5 |
| Label (섹션/그룹 헤더) | 14px (`text-sm`) | 600 | 1.4 (`tracking-wide`) |
| Nav leaf (카테고리명) | 14px (`text-sm`) | 500(비활성)/600(활성) | 1.4 |
| Count badge | 12px (`text-xs`) | 600, `tabular-nums` | 1 |
| Heading (소식 패널 타이틀) | 20px (`text-xl`) | 600 | 1.4 |

---

## Color

기존 토큰(`index.css`)만 사용. **accent(blue-600)는 이미 "active nav / primary CTA / focus"로 예약** — 활성 카테고리에 그대로 적용(신규 색 도입 아님).

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `--background` #f8fafc (slate-50) | 페이지 배경 |
| Secondary (30%) | `--card` #ffffff · `--muted` #f1f5f9 | 카드 표면, 카테고리 nav 표면/호버, 그룹 헤더 |
| Accent (10%) | `--primary` #2563eb (blue-600) | **활성 카테고리 leaf(텍스트+좌측 인디케이터)**, focus-visible ring |
| Destructive | `--destructive` (기존 red) | 기존 에러 상태만(재사용) |

Accent(blue-600) 예약 대상: **활성 카테고리 leaf 1개 · focus-visible ring**. 비활성 leaf·헤더·칩은 `foreground`/`muted-foreground`만(색만으로 상태 구분 금지 — 활성은 색 + 배경 + 굵기 + `aria-current`로 다중 인코딩).

---

## Layout & Interaction Contract  *(이 phase의 핵심)*

### 데스크톱 (lg 이상) — 3열 grid
- 컨테이너: `mx-auto max-w-7xl`(기존 `6xl`→`7xl`로 확장), `grid gap-8`.
- 컬럼: `lg:grid-cols-[11rem_minmax(0,1fr)_20rem]` = **좌 CategoryNav(11rem) · 중앙 물품(가변) · 우 NewsPanel(20rem≈320px)**.
- 좌 nav는 `lg:sticky lg:top-6 self-start`(스크롤 시 카테고리 고정 — 선택). 중앙/우는 기존 세로 흐름.

### CategoryNav — 2단계 그룹 (사용자 확정)
```
┌ 카테고리 ────────┐
│ 각인             │  ← 그룹 헤더(비선택 라벨, muted-foreground text-sm)
│  · 딜러 각인   11 │  ← leaf 버튼(라벨 + count badge)
│  · 서포터 각인  7 │
│ 재료             │
│  · 강화재료     2 │
│  · 재련재료     9 │
│  · 상급재련     8 │
│  · 재련보조     6 │
│  · 아크그리드젬 6 │
└──────────────────┘
```
- **그룹**: `각인`(딜러 각인·서포터 각인) / `재료`(강화재료·재련재료·상급재련·재련보조·아크그리드젬). 헤더는 클릭 불가 라벨.
- **leaf(선택 단위)** 7종. 각 leaf = label + 우측 count badge(`tabular-nums`, muted).
- **활성 표시**: 활성 leaf는 `text-primary font-semibold` + `bg-muted` + 좌측 2px primary 인디케이터 + `aria-current="true"`. 비활성은 `text-foreground hover:bg-muted/60`.
- **빈 카테고리 숨김**: count 0 leaf는 렌더 안 함. 그룹의 leaf가 전부 비면 그룹 헤더도 숨김(A2 — 빈 탭 없음).

### 필터 상호작용
- leaf 클릭 → 중앙에 **그 카테고리 물품만**(기존 `ItemCard` 세로 스택, 카드 **내용 무변경**). 헤더 노출 안 함(선택된 카테고리 = 문맥).
- **상태**: `DashboardPage`의 로컬 `useState<selectedId>`(A4 — YAGNI, URL 파라미터화는 후속). 
- **기본 선택**: 정렬 순서상 **첫 번째 비어있지 않은 leaf**(현행 데이터 기준 `딜러 각인`).
- **자기치유**: 데이터 리페치로 `selectedId`가 사라지면(카테고리 비워짐) 첫 비어있지 않은 leaf로 폴백.

### 모바일 (<lg) — 단일 컬럼
- 순서: **상단 가로 스크롤 칩 탭(CategoryNav) → 중앙 물품 → NewsPanel(맨 아래)**.
- 칩: 같은 leaf를 가로 `overflow-x-auto` pill로. 활성 칩 = `bg-primary text-primary-foreground`, 비활성 = `bg-muted text-foreground`. 그룹 헤더는 칩 모드에서 생략(공간 절약) — leaf만 나열.
- **동일 컴포넌트** `CategoryNav`가 반응형으로 데스크톱 세로 nav ↔ 모바일 가로 칩 전환(Tailwind `lg:` 분기).

### 견고성 / 경계
- 중앙 물품 목록·우 NewsPanel은 **각자 독립 `AsyncBoundary`**(현행 D-07 유지 — 한쪽 실패가 다른 쪽 안 가림).
- `roleGroup=null` 또는 알 수 없는 `itemGroup` 품목은 어느 카테고리에도 안 들어감(현행처럼 미표시 — 큐레이션상 없음).
- 전체 물품 0건 → 중앙은 기존 목록 `AsyncBoundary`의 empty 상태(신규 위젯 없음).

---

## Component Contract

| 컴포넌트 | 상태 | 계약 |
|---|---|---|
| `features/dashboard/categories.ts` | **신규** | 카테고리 taxonomy 단일 출처(roleGroup.ts와 동형). 그룹·leaf 정의(id·label·group·predicate)·표시순서, `deriveCategories(items)` = 데이터→비어있지 않은 leaf 목록(count 포함). 순수 함수, 데이터 페칭 없음. |
| `features/dashboard/CategoryNav.tsx` | **신규** | props: `categories`(파생 결과) · `selectedId` · `onSelect`. 데스크톱 세로 그룹 nav + 모바일 가로 칩(반응형 단일 컴포넌트). `<nav aria-label="카테고리">`, leaf=`<button>`. 표현만 담당(무상태). |
| `features/dashboard/DashboardPage.tsx` | **수정** | `deriveCategories(useItems())` → 카테고리 파생, `useState` 선택 상태, 3열 grid, 선택 카테고리 predicate로 중앙 필터. 기존 `sortByRole`로 카드 내부 정렬 유지. |
| `features/dashboard/ItemCard.tsx` | **무변경** | 내용 계약 그대로(아이콘·이름·RoleBadge·최저가·수집시각·타임라인 Link). |
| `features/dashboard/NewsPanel.tsx` | **무변경** | 우 컬럼 소식(쿠폰/이벤트/공지) 그대로, 독립 boundary. |
| `features/_shared/roleGroup.ts` | **무변경** | 재사용(각인 leaf가 DEALER/SUPPORT 매핑에 사용). |

**카테고리 taxonomy(`categories.ts`에 잠금):**
| group | leaf id | label | predicate |
|---|---|---|---|
| 각인 | `dealer` | 딜러 각인 | `roleGroup==='DEALER'` |
| 각인 | `support` | 서포터 각인 | `roleGroup==='SUPPORT'` |
| 재료 | `mat-강화재료` | 강화재료 | `itemGroup==='강화재료'` |
| 재료 | `mat-재련재료` | 재련재료 | `itemGroup==='재련재료'` |
| 재료 | `mat-상급재련` | 상급재련 | `itemGroup==='상급재련'` |
| 재료 | `mat-재련보조` | 재련보조 | `itemGroup==='재련보조'` |
| 재료 | `mat-아크그리드젬` | 아크그리드젬 | `itemGroup==='아크그리드젬'` |

표시순서: 각인(딜러→서포터) → 재료(강화재료→재련재료→상급재련→재련보조→아크그리드젬).

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Primary CTA | 없음(read-only 대시보드 — leaf 클릭은 필터, 라벨=카테고리명) |
| 그룹 헤더 | `각인` / `재료` |
| Leaf 라벨 | 딜러 각인 · 서포터 각인 · 강화재료 · 재련재료 · 상급재련 · 재련보조 · 아크그리드젬 |
| nav aria-label | `카테고리` |
| Empty state (전체 0건) | 기존 목록 `AsyncBoundary` 재사용(신규 카피 없음) |
| Empty category | 해당 없음 — 빈 카테고리는 **숨김**(선택 불가) |
| Error state | 기존 `AsyncBoundary`/`NewsPanel` 재사용(문제+재시도 경로 그대로) |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | card·button·skeleton (기존, 재사용) | not required |
| (third-party) | 없음 | — |

CategoryNav는 레지스트리 블록이 아니라 **직접 작성**(순수 `<button>` + Tailwind). shadcn CLI 미사용(Windows `@` 디렉토리 버그 회피).

---

## Checker Sign-Off  *(오케스트레이터 인라인 검증 — 서브에이전트 권한 거부 환경)*

- [x] Dimension 1 Copywriting: **PASS** — 모든 라벨/카피 명시(카테고리명·헤더·aria), 신규 문안 없음(기존 재사용).
- [x] Dimension 2 Visuals: **PASS** — 레이아웃 grid·nav·칩 구조 명시, 카드/소식 무변경.
- [x] Dimension 3 Color: **PASS** — 기존 토큰만, accent(blue-600)는 예약 용도(활성 nav/focus)와 일치, 상태 다중 인코딩(색+배경+굵기+aria).
- [x] Dimension 4 Typography: **PASS** — 기존 text-sm/base/xl 스케일 재사용, 신규 없음.
- [x] Dimension 5 Spacing: **PASS** — gap-8/space-y-* 전부 4의 배수, 기존 리듬 유지.
- [x] Dimension 6 Registry Safety: **PASS** — 신규 레지스트리 0, CategoryNav 직접 작성.

**Approval:** approved 2026-07-14 (인라인 검증)
