---
phase: 26
slug: gem-current-price
status: draft
shadcn_initialized: true
preset: none
created: 2026-07-15
---

# Phase 26 — UI Design Contract (보석 현재가 둘러보기 `/gems`)

> GEM-02. 티어4 보석 6종의 **최저 즉시구매가**를 신규 `/gems` 페이지에 표시.
> **신규 디자인 토큰 0 · 신규 의존성 0 · 신규 shadcn 블록 0** — 기존 검증된 시스템 재사용.
> 신규 컴포넌트는 `GemPage` + `GemGroup`(내부). `TopNav`에 4번째 항목 1줄 추가 외 기존 화면 **무변경**.

**설계 근거:** `24-SPIKE-FINDINGS.md`(실측 잠금) + 사용자 비준(2026-07-15): 보석 6종 · 현재가=`min(BuyPrice)` ·
배치=별도 페이지 `/gems`. 승인 목업: 상단 "티어4 보석 현재가" + 부제, 겁화 3행 / 작열 3행.

---

## 🔑 이 화면을 지배하는 제약 (Phase 24 실측에서 옴)

이 세 가지가 나머지 모든 결정을 강제한다. 디자인 취향이 아니라 **데이터의 성질**이다.

| 실측 사실 | UI 귀결 |
|---|---|
| 보석은 응답에 **`Id`가 없다** + **시계열을 기록하지 않는다** | **행이 링크가 아니다.** `ItemCard`는 카드 전체가 `/timeline?item={id}` 링크지만(CARD-02), 보석은 갈 곳이 없다 → **hover elevation·cursor-pointer·`<Link>` 금지.** 눌리게 생겼는데 안 눌리는 가짜 어포던스를 만들지 않는다 |
| **`BuyPrice`가 null일 수 있다**(입찰 전용 매물) | 전 매물 null이면 **"즉시구매 매물 없음"**으로 표기. 0원·`-`·마지막 값으로 때우지 않는다 |
| **레이트리밋 버킷을 거래소 수집과 공유** | 가격은 **캐시된 스냅샷**이다 → **갱신 시각을 반드시 노출**해 "언제 기준 값인지" 정직하게 알린다 |

---

## Design System

| Property | Value |
|----------|-------|
| Tool | shadcn (기존 설정 — 프로젝트 자체 토큰, `frontend/src/index.css`) |
| Preset | none (slate/blue-600 자체 팔레트, 라이트 모드 전용) |
| Component library | Radix UI (기존). **이 phase는 신규 Radix 원시요소 0** — 순수 `<div>`/`<img>` + 기존 `Card` |
| Icon library | lucide-react (기존 dep). **신규 아이콘 0** — 보석은 로아 CDN 실아이콘 사용 |
| Font | Inter Variable (기존 `--font-sans`) |

**신규 의존성 0.** 기존 `card`/`skeleton` 블록 재사용 — shadcn CLI는 돌리지 않는다([[shadcn-cli-windows-at-dir-bug]]: Windows에서 `@/` 리터럴 디렉터리 생성 + radix-ui dep 오염).

---

## Spacing Scale

기존 토큰 재사용(전부 4의 배수). **신규 spacing 토큰 없음.**

| Token | Value | Usage (이 phase) |
|-------|-------|------------------|
| xs | 4px | 가격 라벨·🪙 인라인 gap(`gap-1`) |
| sm | 8px | 아이콘–이름 gap(`gap-2`), 행 간 세로 리듬(`space-y-0.5`=2px는 예외 아래 참조) |
| md | 16px | 카드 내부 좌우 패딩(`CardContent` 기존), 행 좌우 여백 |
| lg | 24px | **그룹(겁화/작열) 간 세로 간격(`space-y-6`)** |
| xl | 32px | 페이지 컨테이너 좌우 패딩(`AppLayout` 기존 `px-8`) |

예외(기존 관례 승계): 행 세로 패딩 `py-3`(12px — `ItemCard`와 동일), 그룹 헤더 하단 `pb-1.5`(6px — `CategoryNav`
헤더와 동일), 보조 텍스트 `space-y-0.5`(2px — `ItemCard` 가격 블록과 동일). 전부 **기존 화면에서 이미 쓰는 값**.

---

## Typography

기존 스크립트 재사용. **신규 타이포 토큰 없음.**

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Page heading ("티어4 보석 현재가") | 20px (`text-xl`) | 600 | 1.4 — `NewsPanel` `CardTitle`과 동일 |
| Page subtitle (부제) | 14px (`text-sm`) | 400, `text-muted-foreground` | 1.5 |
| Group header (겁화/작열) | 14px (`text-sm`) | 600, `tracking-wide`, `text-muted-foreground` | 1.4 — `CategoryNav` 그룹 헤더와 동일 |
| Row label (보석 이름) | 16px (`text-base`) | 500 | 1.5 — `ItemCard` 이름과 동일 |
| Price (골드) | 16px (`text-base`) | 500, **`tabular-nums`** | 1.5 — `ItemCard` 가격과 동일 |
| Price 보조 라벨 ("최저 즉시구매가") | 12px (`text-xs`) | 400, `text-muted-foreground` | 1 |
| 갱신 시각 | 12px (`text-xs`) | 600, `text-muted-foreground`, `tabular-nums` | 1 — `ItemCard` "수집 시각"과 동일 |

`tabular-nums`는 **필수**: 6행의 골드 자릿수(347,000 → 3,090,000)가 세로로 정렬돼야 비교가 된다.

---

## Color

기존 토큰(`index.css`)만 사용. **신규 색 0.**

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `--background` #f8fafc | 페이지 배경 |
| Secondary (30%) | `--card` #ffffff · `--border` | 카드 표면, 그룹 헤더 구분선 |
| Accent (10%) | `--primary` #2563eb (blue-600) | **활성 nav 링크("보석")** + focus-visible ring **전용** |
| Destructive | `--destructive` | 기존 `AsyncBoundary` 에러 상태만(재사용) |

**Accent 예약 유지(D-01):** blue-600은 이 화면에서 **상단내비 활성 표시와 focus ring에만** 쓴다.
가격·등급·계열을 accent로 칠하지 않는다. **색만으로 정보를 전달하지 않는다** — 계열은 헤더 텍스트로,
레벨은 행 라벨로, 가격 없음은 문구로 전달한다(등급별 색은 `GRADE-V2-01`로 이미 v2 연기됨 — 여기서 앞당기지 않는다).

---

## Layout & Interaction Contract *(이 phase의 핵심)*

### 페이지 골격

```
AppLayout (기존: TopNav + max-w-7xl px-8 py-8)
└─ GemPage
   └─ <Card>                          ← NewsPanel과 동일한 단일 카드 표면
      ├─ CardHeader
      │   ├─ CardTitle  "티어4 보석 현재가"
      │   └─ 부제       "최저 즉시구매가 · 기준 시각 {formatKst}"   ← ⚠️ 아래 §갱신 문구 정정
      └─ CardContent (space-y-6)
         └─ <AsyncBoundary>           ← 자체 경계(로딩/빈/에러)
            ├─ GemGroup "겁화 (딜러)"  ← 헤더 + border-b, 3행
            └─ GemGroup "작열 (서포터)" ← 헤더 + border-b, 3행
```

**폭:** 카드는 `max-w-3xl` — 6행짜리 목록이 `max-w-7xl` 전폭으로 늘어나면 이름과 가격 사이가 허허벌판이 된다
(대시보드 카드가 `max-w-3xl`로 제한된 quick-260706-ohg 선례와 동일한 이유).

### 행(row) 계약 — **링크 아님**

`ItemCard`의 가로 배치(아이콘+이름 좌 / 가격+시각 우)를 **시각적으로만** 승계하고 **상호작용은 승계하지 않는다**:

| 속성 | ItemCard(기존) | GemRow(이 phase) |
|---|---|---|
| 래퍼 | `<Link to="/timeline?item={id}">` | **`<div>`** — 링크 없음 |
| hover | `hover:shadow-md hover:border-foreground/20` | **없음** |
| cursor | `cursor-pointer` | **없음**(기본) |
| focus ring | 링크 포커스 | **없음**(포커스 대상 아님) |

> **왜:** 보석은 `Id`도 시계열도 없어 이동할 목적지가 존재하지 않는다. 눌리게 생긴 것은 눌려야 한다.

행 내용: `[아이콘 24px] [이름]` … `[최저 즉시구매가 라벨] [🪙] [가격]`
이름은 **"8레벨"** 처럼 레벨만(계열은 그룹 헤더가 이미 말한다 — 중복 제거).

### 아이콘

- 로아 CDN 실아이콘 6종 전부 **distinct**(겁화 `use_12_103/104/105`, 작열 `use_12_113/114/115`) →
  각인서처럼 라벨 병기로 보완할 필요 **없음**(12-SPIKE D-06 문제 미발생).
- `ItemIcon` 선례대로 **고정 슬롯**(`size-6 shrink-0`)에 렌더해 로딩 중/실패 시 **레이아웃 시프트 0**.
- `onError` 시 슬롯을 유지한 채 조용히 비운다(`alt=""` — 옆의 이름이 접근성 라벨을 담당).
- `loading="lazy"` — 6장뿐이라 큰 이득은 없으나 기존 관례 일관.
- **`img-src`에 이미 `onstove` CDN이 허용돼 있다**(quick-260713-e1o CSP) → CSP 변경 불요.

### 상태 (AsyncBoundary)

| 상태 | 표시 |
|---|---|
| pending | `AsyncBoundary` 기본 `LoadingState` |
| error | `AsyncBoundary` 기본 ErrorState + 다시 불러오기 |
| empty(6종 전부 조회 실패) | "보석 시세를 불러오지 못했어요" |
| success | 그룹 2개 × 3행 |

> **🔧 정정(구현 중 발견):** 초안은 pending에 6행 `Skeleton`을 규정했으나, `AsyncBoundary`에는
> pending 슬롯을 주입할 prop이 **없다**(`status`/`isEmpty`/`onRetry`/`emptyHeading`/`emptyBody`/
> `errorMessage`/`children`뿐). 넣으려면 **모든 화면이 의존하는 공용 컴포넌트를 수정**해야 하는데, 그건
> 이 UI-SPEC이 스스로 약속한 "`AsyncBoundary` 0줄 수정"과 모순이다. 가장 가까운 선례인 `NewsPanel`
> (Card + 자체 AsyncBoundary + 섹션 구조)도 **기본 `LoadingState`를 쓴다** → 그 관례를 따른다.
> (`ItemCard`의 `Skeleton`은 AsyncBoundary를 **거치지 않는** 카드 내부 인라인 분기라 사정이 다르다.)

### 🔑 가격 없음 (부분 실패) — 행 단위 정직 표기

**전체가 아니라 개별 보석 단위로 실패할 수 있다.** 그 행만 가격 자리에 문구를 넣고 나머지 5행은 정상 표시한다:

| 상황 | 가격 자리 문구 |
|---|---|
| 즉시구매 매물 0(전 매물 `BuyPrice`=null) | **"즉시구매 매물 없음"** |
| 레이트리밋 양보(`RATE_LIMITED`) | **"잠시 후 다시"** |
| 해당 보석 조회 실패 | **"불러오지 못함"** |

> **🔧 추가(구현 중 발견):** 초안엔 `RATE_LIMITED`가 없었다. 첫 라이브 실행에서 **겁화 3 OK / 작열 3 ×
> HTTP 429**가 실제로 났다 — 경매장이 수집과 서버 측 100/min 쿼터를 공유하는데(24 §H2) 보석 클라이언트가
> 앱의 공유 토큰 버킷을 우회했기 때문. 이건 **고장이 아니라 수집(Core Value)에 예산을 양보한 것**이므로
> "불러오지 못함"으로 뭉뚱그리면 거짓말이다 → 상태를 분리하고 재시도를 안내한다.

`ItemCard`가 404를 화면 전체 ErrorState가 아니라 **카드 수준 한 줄**로 처리한 것과 같은 규율(D-04/D-07/D-08).
**금지:** 0골드 표시 · `-` · 마지막 성공값 재사용 · 행 숨기기(6종은 항상 6행 — 없으면 없다고 말한다).

### 🔧 갱신 시각 — 승인 목업의 "5분마다 갱신" 문구 **정정**

사용자 승인 목업의 부제는 **"최저 즉시구매가 · 5분마다 갱신"**이었으나, **이 문구는 거짓이 된다.**

보석은 **온디맨드 + 캐시**다(로드맵/설계 스펙 확정) — **아무도 페이지를 안 보면 호출이 0**이고, 캐시는
누군가 볼 때 최대 5분까지 재사용된다. 즉 "5분마다 갱신"이 아니라 **"볼 때, 최대 5분 지난 값일 수 있음"**이다.
뉴스 패널은 **6시간 스케줄 폴러**라 "N시간마다 갱신"이 참이지만 보석은 구조가 다르다.

문구를 참으로 만들려면 5분 폴러를 돌려야 하는데, 그건 **아무도 보지 않는 시간에도 분당 1.2콜을 수집 버킷
(공유·§H2)에서 영구히 빼는 것**이다. Core Value를 위해 **동작을 유지하고 문구를 사실에 맞춘다.**

**확정 문구:**
- 부제: **"최저 즉시구매가 · 기준 시각 {formatKst}"** — 로딩 전엔 기준 시각 생략.
- 필요 시 보조: **"최대 5분까지 지난 값일 수 있어요"**(부제 아래 `text-xs muted`). "5분마다 갱신" 금지.

`formatKst` 재사용(UTC ISO `...Z` → KST) — 뉴스/쿠폰 날짜처럼 오프셋 없는 로컬 문자열이 **아니라** 서버가
`Instant.now().toString()`으로 주는 UTC 순간이므로 `formatKst`가 정답이다(D-11 함정의 반대편).

### 반응형

6종·2그룹이라 **데스크톱/모바일 모두 동일한 세로 스택**. 브레이크포인트 분기 **없음**.
행은 `flex items-center justify-between` — 좁은 폭에서 이름이 `truncate`, 가격은 `shrink-0`
(`ItemCard`와 동일). 가로 스크롤 0.

---

## Component Contract

| 컴포넌트 | 신규/기존 | 계약 |
|---|---|---|
| `GemPage` | **신규** `features/gems/GemPage.tsx` | `useGems()` 소비, 자체 `AsyncBoundary`, 그룹 2개 렌더 |
| `GemGroup` | **신규**(GemPage 내부 또는 동일 폴더) | 그룹 헤더(border-b) + 행 리스트. **무상태** |
| `TopNav` | **기존 수정(1줄)** | `navItems`에 `{ to: '/gems', label: '보석' }` 추가 — 4번째 |
| `main.tsx` | **기존 수정(1줄)** | `{ path: 'gems', element: <GemPage /> }` 라우트 추가 |
| `Card`/`CardContent`/`CardHeader`/`CardTitle` | 기존 재사용 | 0줄 수정 |
| `AsyncBoundary` | 기존 재사용 | 0줄 수정 |
| `Skeleton` | 기존 재사용 | 0줄 수정 |
| `ItemCard`·`NewsPanel`·`CategoryNav`·`DashboardPage` | **무변경** | 보석은 대시보드에 개입하지 않는다 |

> `ItemIcon`은 **재사용하지 않는다**: 시그니처가 `roleGroup` 기반 폴백 글리프(ScrollText/FlaskConical/Package)에
> 묶여 있는데 보석엔 `roleGroup`이 없다. 억지로 끼우면 `ItemIcon`에 보석 분기를 추가해야 해서 **기존 3화면의
> 공용 컴포넌트를 오염**시킨다. `GemRow`가 `<img>`를 고정 슬롯에 직접 렌더한다(폴백 글리프 불요 — 아이콘 6종 실재).

---

## Copywriting Contract

| 위치 | 문구 | 근거 |
|---|---|---|
| 상단내비 | **보석** | 기존 3개(대시보드/품목 타임라인/이벤트 영향)와 같은 명사형·짧게 |
| 페이지 제목 | **티어4 보석 현재가** | 사용자 승인 목업 그대로 |
| 부제 | **최저 즉시구매가 · 기준 시각 2026.07.15 15:23** | 무엇을 재는 값인지 + 신선도를 한 줄에. **"5분마다 갱신" 금지** — 온디맨드 캐시라 거짓(§갱신 시각 정정) |
| 그룹 헤더 | **겁화 (딜러)** / **작열 (서포터)** | 로아 유저 어휘. 역할 병기는 비유저 면접관도 읽히게 |
| 행 라벨 | **8레벨** / **9레벨** / **10레벨** | 계열은 헤더가 말하므로 중복 제거 |
| 가격 | **🪙 346,888** (라벨 없이 골드만) | 무엇을 재는 값인지는 **부제가 이미 말한다**. 🔧 정정: 초안은 행마다 "최저 즉시구매가" 라벨을 붙였는데, 라이브 렌더에서 6행 반복이 정작 비교할 숫자를 가렸다 — 레벨 행에서 계열을 뺀 것과 같은 중복 제거 원칙. (`ItemCard`가 행마다 "최저가"를 다는 건 카드 단위 부제가 없어서다.) "최저 즉시구매가"라는 어휘 자체는 부제에서 유지 — 거래소의 "최저가"와 **구별**되어야 한다 |
| 가격 없음 | **즉시구매 매물 없음** | 값을 지어내지 않음. "0골드" 금지 |
| 개별 실패 | **불러오지 못함** | 행 수준 한 줄(화면 전체 에러 아님) |
| 기준 시각 | **기준 시각 2026.07.15 15:23** | 캐시 스냅샷임을 명시 |
| 빈/에러 | **보석 시세를 불러오지 못했어요** | 기존 `AsyncBoundary` 한국어 톤(존댓말·부드러운 종결) 유지 |

**금지 어휘:** "현재가"를 단독으로 가격 라벨에 쓰지 않는다(즉시구매가임을 명시). 개발자 용어("BuyPrice",
"경매장 API", "캐시 미스") 노출 금지 — quick-260707-uly가 "백필·일평균(거래가)"을 "평균 거래가"로 순화한 선례.

---

## Registry Safety

- shadcn CLI **미실행**. 신규 블록 0 — 기존 `card`/`skeleton`만 import.
- 신규 npm 의존성 **0**.
- CSP 변경 **0** — `img-src`에 onstove CDN 기허용(quick-260713-e1o).
- `frontend/src/index.css` 토큰 **0줄 수정**.

---

## Checker Sign-Off *(오케스트레이터 인라인 검증 — 서브에이전트 권한 거부 환경 [[gsd-subagents-inline]])*

| # | Dimension | 판정 | 근거 |
|---|---|---|---|
| 1 | **Design system integrity** | ✅ PASS | 신규 토큰·의존성·shadcn 블록 전부 0. 기존 `Card`/`AsyncBoundary`/`Skeleton`/`formatKst` 재사용 |
| 2 | **Spacing scale** | ✅ PASS | 전 값 4의 배수 + 예외(`py-3`·`pb-1.5`·`space-y-0.5`)는 **기존 화면에서 이미 쓰는 값**을 승계 |
| 3 | **Typography hierarchy** | ✅ PASS | 5단계 명확(20/16/14/12), 기존 스크립트와 1:1. 가격 `tabular-nums` 강제 |
| 4 | **Color / 60-30-10** | ✅ PASS | accent(blue-600)를 **활성 nav + focus ring**으로만 한정. 색 단독 정보 전달 없음(계열=텍스트, 가격없음=문구) |
| 5 | **Copywriting** | ✅ PASS | 전 상태 문구 확정. "최저 즉시구매가"로 거래소 "최저가"와 구별. 개발자 용어 금지 명문화 |
| 6 | **Registry safety** | ✅ PASS | CLI 미실행·CSP 무변경·index.css 무변경 |

**FLAG(비차단):** 없음.

**설계 판단 기록:** (a) 행을 링크로 만들지 않은 것은 스타일이 아니라 **데이터 제약**(Id·시계열 부재)의 귀결이다.
(b) `ItemIcon` 재사용을 **거부**했다 — `roleGroup` 폴백에 묶인 공용 컴포넌트에 보석 분기를 넣으면 기존 3화면이
오염된다. (c) 등급별 색은 `GRADE-V2-01`(v2 연기)이라 여기서 앞당기지 않는다.

**Verdict: ✅ APPROVED — 6/6 dimensions passed.**
