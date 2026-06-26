---
phase: 10
slug: event-impact
status: approved
shadcn_initialized: true
preset: "base color: slate, style: new-york (Phase 7 init 완료 — frontend/components.json)"
created: 2026-06-26
reviewed_at: 2026-06-26
---

# Phase 10 — UI Design Contract (Event Impact)

> 이벤트별 전후 변화율 표/카드 화면(IMPCT-01~04)의 시각·인터랙션 계약. gsd-ui-researcher 생성, gsd-ui-checker 검증.
> 이 계약은 **Phase 7 디자인 시스템(`07-UI-SPEC.md`)** 과 **Phase 9 시각 델타(`09-UI-SPEC.md`)** 를 상속하며, 재정의하지 않는다. 본 문서는 그 위에 **window 컨트롤·결과 표/카드·상승/하락 색·status 배지·insufficient 이유 카피·상관≠인과 배너** 의 phase 전용 시각 델타만 추가한다.

## 상속 (Phase 7 / Phase 9 — 재정의 금지)

상위 계약에서 확정돼 그대로 적용되는 토큰(`frontend/src/index.css`·`frontend/components.json` 구현 확인):

- **디자인 시스템:** shadcn/ui (slate / new-york), lucide-react, Inter (variable, 숫자는 `tabular-nums`)
- **간격:** 4 / 8 / 16 / 24 / 32 / 48 / 64 (8-point)
- **타이포:** Display 28/600 · Heading 20/600 · Body 16/400 · Label 14/600 (사이즈 4종 / 웨이트 2종 — **고정, 본 phase 신규 사이즈 없음**)
- **색 60/30/10:** Dominant `#F8FAFC`(slate-50) · Secondary(카드 `#FFFFFF`·보더 `#E2E8F0`·텍스트 `#0F172A`/`#64748B`) · Accent `#2563EB`(blue-600)
- **상속 의미색:** up `#16A34A`(green-600) · down/destructive `#DC2626`(red-600) · warning `#D97706`(amber-600) · neutral `#64748B`(slate-500)
- **eventType 4색(Phase 9, `frontend/src/features/timeline/eventMarkers.ts`):** `LOA_ON #7C3AED` · `MAJOR_UPDATE #EA580C` · `SEASON_END #DB2777` · `BALANCE_PATCH #0D9488` — Phase 10이 그대로 import해 행/카드 eventType 배지에 재사용(D-07).
- **공용 자산(Phase 9 D-07):** `_shared/ItemSelect`(controlled 드롭다운)·`_shared/LatestPriceCard`(자기 `useLatestPrice`+자기 `AsyncBoundary`) 그대로 import(D-12).
- **카피 언어:** 한국어(코드 식별자/경로/엔드포인트/hex/enum은 영어 원형 유지).
- **Accent 예약 목록(상속)** 에 이미 포함: 활성 내비 항목 · 주요 CTA · 포커스 링 · 선택된 item · 선택된 프리셋 버튼. 본 계약은 **새 accent 사용처를 추가하지 않는다.**

상위에서 확정된 사실(재논의 안 함): Vite+React+TS+Tailwind 스택, 백엔드 무변경·read 전용, UTC 원본 → KST 표시(off-by-9h 가드: 표시만 KST·정렬은 백엔드 occurred_at desc 유지), 라이트 단일 테마, URL searchParams 단일 상태 소스, 컴포넌트별 `<AsyncBoundary>` 격리. **차트(Recharts) 불필요 — 표/카드 화면.**

---

## Design System

| Property | Value |
|----------|-------|
| Tool | **shadcn/ui** (slate / new-york) — Phase 7 init 완료 |
| Preset | base color `slate`, style `new-york` (`frontend/components.json`) |
| Component library | radix (shadcn/ui 경유) |
| Icon library | **lucide-react** |
| Font | **Inter** (variable), 가격·변화율·시각 숫자는 `tabular-nums` |

> **신규 shadcn 블록:** `table`(반응형 표, D-04) 추가 필요. **현재 미보유** — 보유 블록은 alert·badge·button·card·navigation-menu·select·skeleton뿐. `table`은 공식 레지스트리 블록(Radix 의존 없는 styled `<table>` 래퍼).
> ⚠ **Windows 설치 주의(planner/executor):** `npx shadcn add table`은 이 환경에서 블록을 literal `@/` 디렉터리에 잘못 배치하고 불필요한 radix 의존을 추가하는 알려진 버그가 있다 → **`frontend/src/components/ui/table.tsx`에 공식 소스를 손으로 작성**한다. 기타 신규 블록 불요(전부 기존 보유).

---

## Spacing Scale

상속(재선언 안 함). Phase 10 화면 적용처:

| Token | Value | Phase 10 Usage |
|-------|-------|----------------|
| xs | 4px | 배지 내부 간격, 부호+숫자 간격, 색 스와치-라벨 |
| sm | 8px | 프리셋 버튼 간격, 셀렉터-컨트롤 간격, 표 셀 세로 패딩(컴팩트 행) |
| md | 16px | 표 셀 가로 패딩, 카드 내부 패딩, 컨트롤 바 ↔ 결과 영역, Alert 내부 패딩 |
| lg | 24px | 섹션 구분(배너 ↔ 컨트롤, 최신가 카드 ↔ 결과), 카드 리스트 항목 간격 |
| xl | 32px | 콘텐츠 영역 좌우 여백(상속) |

Exceptions: none. (표 셀 패딩은 세로 `sm`(8) / 가로 `md`(16) 조합으로 8-point 정렬 유지.)

---

## Typography

상속(신규 사이즈·웨이트 없음). Phase 10 표면의 역할 매핑:

| Surface | Role | Size/Weight |
|---------|------|-------------|
| 라우트 제목("이벤트 영향") | Display | 28/600 |
| 최신가 카드 품목명/최신가(상속) | Heading | 20/600 (+`tabular-nums`) |
| 상관≠인과 Alert 제목 | Label(semibold) | 14/600 |
| 표 컬럼 헤더 · status 배지 · eventType 배지 · 프리셋 버튼 · window 입력 라벨 | Label | 14/600 |
| 표 셀 본문 · 카드 필드 · changeRate · prePrice/postPrice · 앵커 시각 | Body | 16/400 (숫자/가격/시각 `tabular-nums`) |
| Alert 본문 · insufficient 이유 카피 · 빈/에러 상태 본문 | Body | 16/400 |

> 배지 텍스트는 shadcn 기본 `text-xs`(12px)를 쓰지 않고 **Label 14/600** 으로 고정(사이즈 4종 유지·Phase 9와 동일). changeRate·가격·앵커 시각은 자릿수 정렬을 위해 `tabular-nums`.

---

## Color

상속한 60/30/10 + 상속 의미색은 그대로. **본 phase가 신규로 잠그는 것은 (1) changeRate 상승/하락 방향색, (2) status(ok/insufficient) 배지색 매핑** 이다. 둘 다 *의미 인코딩 색* 이며 단일 장식 accent(blue-600)와 역할이 분리된다.

### Accent (상속 — 신규 사용처 없음)

`#2563EB`(blue-600)는 상속 예약 목록(선택된 프리셋 버튼 · 포커스 링 · 활성 내비 · 선택된 item)에만. **본 phase는 새 accent 사용처를 추가하지 않는다.**

### changeRate 방향색 (신규 — 한국 관례, D-05)

> **분리 근거(Dimension 3 대비):** 이 색쌍은 changeRate의 **부호(방향)를 인코딩** 하는 *의미 전용* 색으로, up/down·warning·eventType 4색과 같은 의미색 계열이다. 단일 장식 accent와 역할이 다르며 "두 번째 accent"가 아니다. **국내 거래소/주식 MTS 관례(상승=빨강·하락=파랑)** 를 따른다 — 대상 청중(로아 유저·국내 면접관)에게 자연스러움. 서구 관례(상승=초록)는 기각. 색은 **부호(+/−) + 선택적 ▲/▼ 글리프** 와 항상 함께 제시해 색 단독 의존을 피한다(접근성).

| changeRate | 색 | Hex | 의미 |
|-----------|-----|-----|------|
| `> 0` (상승) | red-600 | `#DC2626` | 가격 상승 (한국 관례 = 빨강). 부호 `+`, 선택 글리프 `▲` |
| `< 0` (하락) | blue-700 | `#1D4ED8` | 가격 하락 (한국 관례 = 파랑). 부호 `−`, 선택 글리프 `▼` |
| `= 0` (보합) | slate-500 | `#64748B` | 변화 없음(`0.0%`), 중립 |
| `null` (insufficient) | — | — | changeRate 미표시 → 이유 카피로 대체(아래 Copywriting) |

- **하락색은 의도적으로 blue-700(`#1D4ED8`)** 으로, accent blue-600(`#2563EB`)과 **구분** 한다: accent-blue는 *구조*(버튼/포커스/선택)에, 하락-blue는 *인라인 숫자 텍스트* 에만 쓰여 시각 역할이 겹치지 않는다.
- **상승색(red-600)과 에러색(red-600)은 동일 hex** 지만 동시 노출되지 않는다 — 에러(400/404)는 `<AsyncBoundary>`가 결과 표 *대신* 에러 패널을 보이므로 changeRate 행과 같은 화면에 공존하지 않는다.

### status 배지색 (신규 — 데이터 충분성 인코딩, D-07)

> changeRate 방향색과 **다른 의미 축** 이다(방향=가격 등락, 배지=데이터 충분성). 같은 글리프에 두 축이 겹치지 않는다.

| status | 색 | Hex | 배지 라벨 |
|--------|-----|-----|-----------|
| `ok` | green-600 | `#16A34A` | **"비교 가능"** |
| `insufficient_data` | amber-600 | `#D97706` | **"데이터 부족"** |

- `insufficient_data` = **amber(warning)** 매핑은 Phase 7 의미색(`insufficient_data → warning #D97706`)의 직접 연장.
- 배지는 색 + **텍스트 라벨** 을 항상 동반(색 단독 의미 금지).

### eventType 배지색 (상속 — Phase 9 4색)

`EVENT_MARKERS`(`eventMarkers.ts`) 그대로 import. 행/카드 eventType 배지 = **outline 배지**(border + 텍스트색 = 해당 카테고리 hex) + `koLabel`. 색 + 라벨 동반.

| eventType | Hex | 라벨 |
|-----------|-----|------|
| `LOA_ON` | `#7C3AED` | 로아ON |
| `MAJOR_UPDATE` | `#EA580C` | 대규모 업데이트 |
| `SEASON_END` | `#DB2777` | 시즌 종료 |
| `BALANCE_PATCH` | `#0D9488` | 밸런스 패치 |

---

## 상관 ≠ 인과 배너 (IMPCT-04, D-10 — 신규)

이 프로젝트 신뢰성의 얼굴. 표를 보기 전에 맥락을 먼저 읽게 하고, 항상 노출한다.

| Property | Value | 근거 |
|----------|-------|------|
| 컴포넌트 | shadcn `alert`(기존 보유) — `AlertTitle` + `AlertDescription` | D-10 "alert.tsx 재사용" |
| variant | **`default`**(중립 slate 보더) — `destructive`(빨강) 아님 | 에러가 아니라 *해석 주의 고지* 이므로 빨강 오신호 회피 |
| 아이콘 | lucide **`Info`** | 정보/안내 의미. 경고 삼각형(빨강 연상) 대신 중립 정보 톤 |
| 배치 | 콘텐츠 영역 **최상단·전체 폭**, 컨트롤/표보다 위 | "표 보기 전 맥락 먼저"(D-10) |
| 닫기 | **불가**(닫기 버튼 없음, 상시 노출) | 과대해석 차단이 목적(D-10) |
| 제목 | **"상관 ≠ 인과"** (Label 14/600) | 한눈에 들어오는 짧은 제목 |
| 본문 | **"이 수치는 이벤트와 가격의 시점 상관일 뿐, 인과(이벤트가 가격을 올렸다)를 의미하지 않습니다."** (한 문장 간결, Body 16/400) | D-10 톤 + IMPCT-04 문구 |

---

## Results Table / Card Contract (IMPCT-02, IMPCT-03 — 신규)

`useEventImpact(id, window)` → `events[]`(백엔드 `occurred_at` 내림차순 유지, **프론트 추가 정렬 없음**). 넓은 화면 = 표, 좁은 화면 = 카드 리스트(D-04). `ok`·`insufficient_data`를 **같은 목록에 혼합**(배지로 구분, D-09).

### 반응형 전환 (D-04)

| 뷰포트 | 형태 | 근거 |
|--------|------|------|
| `md` 이상(≥768px) | shadcn `table` — 컬럼 정렬 비교 | 넓은 화면 비교성 |
| `md` 미만(<768px) | 카드 리스트(행 1개 = 카드 1장) | 좁은 화면 가로 스크롤 회피 |

(정확 브레이크포인트 미세조정은 planner 재량이나 기본 분기 = Tailwind `md`.)

### 컬럼/필드 (표·카드 공통 데이터, 우선순위 순)

| 필드 | 소스 | 표시 | 좁은 화면 우선순위 |
|------|------|------|--------------------|
| eventType 배지 | `eventType` | outline 배지 + koLabel(위 색) | 높음(항상) |
| 이벤트명 | `title` | Body 16/400 | 높음(항상) |
| 발생 시각 | `occurredAt` | `formatKst` "YYYY-MM-DD HH:mm KST", `tabular-nums` | 높음(항상) |
| status 배지 | `status` | "비교 가능"(green) / "데이터 부족"(amber) | 높음(항상) |
| 변화율 | `changeRate` | `ok`: 부호+%+소수1자리(방향색·`tabular-nums`) / `insufficient`: **이유 카피로 대체** | 높음(focal — 이 화면의 핵심 수치) |
| 이전가 / 이후가 | `prePrice` / `postPrice` | `ok`: `toLocaleString('ko-KR')` + " G", `tabular-nums` / `insufficient`: "—" | 낮음(좁은 화면 생략 또는 접기 가능) |
| 기준 시각(앵커) | `preAnchorAt` / `postAnchorAt` | `formatKst` KST(없으면 **"없음"**) — insufficient 이유의 근거 | insufficient 행에서만 강조 노출 |

> 좁은 화면 카드는 위 "높음" 필드를 1차 노출하고 가격/앵커는 보조 영역에 배치. 정확 컬럼 생략 순서·카드 필드 배열은 planner/구현 재량(본 계약은 우선순위만 잠금).

### changeRate 포맷 (IMPCT-02, D-06)

- 형식: **부호 + 퍼센트 + 소수 1자리** — 예 `+12.3%` · `-4.0%` · `0.0%`(보합).
- 색: 위 changeRate 방향색(상승 red-600 · 하락 blue-700 · 보합 slate-500).
- `null`(insufficient): changeRate 숫자 대신 **이유 카피**(아래)로 대체 — null을 `0`이나 빈칸으로 위장하지 않는다(honest-data, zod nullable loud-fail).
- 정확 반올림 규칙·`-0.0%` 방지·`toLocaleString` 옵션은 데이터 분포 보고 planner 재량.

### insufficient 이유: 희소 vs stale (IMPCT-03, D-08 — honest-data 정점)

도출 규칙(프론트, **30분 임계 하드코딩 금지** — 백엔드 결합 회피):
- `status==='insufficient_data'` && (`preAnchorAt==null` ∥ `postAnchorAt==null`) → **희소**(해당 앵커 윈도우 내 스냅샷 0개)
- `status==='insufficient_data'` && 양쪽 앵커 모두 non-null → **stale**(앵커는 있으나 이벤트에서 너무 멂 — 백엔드 판정)

| 케이스 | 인라인 이유(status 배지 옆) | 본문 카피 + 근거 |
|--------|------------------------------|------------------|
| 희소 | **"데이터 부족 · 희소"** | **"이벤트 전후 윈도우에 수집된 시세가 없어 비교 기준을 잡지 못했어요."** + 앵커: `이전 기준: {KST 또는 없음} · 이후 기준: {KST 또는 없음}` |
| stale | **"데이터 부족 · 오래됨"** | **"기준 시각이 이벤트에서 너무 떨어져 비교를 신뢰할 수 없어요."** + 앵커: `이전 기준: {preAnchorAt KST} · 이후 기준: {postAnchorAt KST}` |

> 앵커 시각(KST)을 그대로 노출하는 것이 "왜 부족한지"의 정직한 증거다(IMPCT-03 "왜" 충족). `ok` 행은 앵커를 강조 노출하지 않아도 무방(보조).

---

## Window Controls Contract (IMPCT-01 — 신규)

Phase 9 `RangeControls`(프리셋 + 입력) 패턴을 미러. window 정수 1개를 URL `?window=`에 인코딩(`useImpactParams`가 `useTimelineParams` 미러).

| 컨트롤 | 형태 | 동작 |
|--------|------|------|
| 프리셋 버튼 | **6h / 24h / 72h**(가로 배열, `sm` 간격) — 선택된 버튼 = accent(blue-600) outline, 비선택 = secondary/outline | 클릭 → `?window=` 기록·재조회 (데모 원터치) |
| 숫자 입력 | native `<input type="number">` + 라벨 "윈도우(시간)" | 임의 정수 입력 → `?window=` 기록. **클라이언트 클램프 안 함**(D-03) |
| 기본값 | URL에 `window` 없으면 **24h** 반환(D-02). bare `/impact`는 eager write 안 함(명시 변경만 URL 기록) | 딥링크·스크린샷 재현성 |
| 범위밖 | `≤0` 또는 `>168` 전송 허용 → 백엔드 400 → `ApiError(400)` → 400 UI 노출(D-03) | 400 처리 시연(ROADMAP 검증) |

> 정확 프리셋 개수/라벨(6/24/72h는 출발점)·입력 위젯 형태·"입력 즉시 vs 제출 버튼" 검증 UX는 planner 재량.

---

## Copywriting Contract

언어: 한국어. 상속 공용 로딩/빈/에러 문구 위에 phase 전용 카피를 얹는다. 결과 영역의 `<AsyncBoundary>`가 `ApiError.status`로 분기(**일반 "오류 발생/데이터 없음" 금지**, Phase 9 D-09 상속).

### 상태 카피 — 200-empty / 400 / 404 구분

| 케이스 | Heading | Body (문제 + 다음 행동) | 비고 |
|--------|---------|------------------------|------|
| 200 + `events:[]`(이벤트 0개, D-11) | **"등록된 이벤트가 없어요"** | **"이 품목에 연결된 게임 이벤트가 아직 없습니다. 다른 품목을 선택하거나 관리자가 이벤트를 등록하면 표시됩니다."** | Phase 7 `EmptyState`. per-row insufficient와 별개(관리자가 0개로 둔 경우) |
| 400 (`window`≤0 또는 >168) | **"윈도우 값을 다시 확인해 주세요"** | **"윈도우는 1~168시간 사이의 정수여야 합니다. 프리셋 버튼을 누르거나 범위 안의 값을 입력해 다시 불러오세요."** | 입력 안내, destructive 아이콘 |
| 404 (없는 품목) | **"존재하지 않는 품목이에요"** | **"선택한 품목을 찾을 수 없습니다. 위 목록에서 다른 품목을 선택해 주세요."** | 셀렉터로 유도(Phase 9 상속) |

> 로딩 = 결과 표 자리 스켈레톤 + "불러오는 중…"(상속). 백엔드 연결 실패(네트워크) = 상속 에러 카피("백엔드에 연결하지 못했어요…") 재사용.

### Phase 10 신규 라벨/카피

| Element | Copy |
|---------|------|
| 라우트 제목 | **"이벤트 영향"** (내비 라벨과 동일, 상속) |
| Primary CTA (400/빈 상태 복구) | **"24시간으로 보기"** (기본 window=24h 프리셋으로 리셋 — 동사구) |
| 프리셋 버튼 | **"6시간" / "24시간" / "72시간"** |
| window 입력 라벨 | **"윈도우(시간)"** |
| status 배지 | **"비교 가능"**(ok) / **"데이터 부족"**(insufficient) |
| insufficient 인라인 이유 | **"데이터 부족 · 희소"** / **"데이터 부족 · 오래됨"** |
| 앵커 라벨 | **"이전 기준" / "이후 기준"**, 없으면 **"없음"** |
| eventType 배지 | 로아ON / 대규모 업데이트 / 시즌 종료 / 밸런스 패치 |
| 상관≠인과 배너 | 제목 "상관 ≠ 인과" + 본문(위 배너 계약) |

Destructive 확인: 해당 없음 — read-only 데모, 파괴적 동작 없음.

---

## Layout & Visual Hierarchy

위→아래 단일 컬럼(데스크톱, 콘텐츠 영역 `max-w-screen-xl` 상속):

1. **상관 ≠ 인과 배너**(최상단·전체 폭, 상시): 표를 보기 전 맥락 먼저(D-10).
2. **컨트롤 바**: `[품목 셀렉터]  [6시간][24시간][72시간]  [윈도우(시간) 입력]` — 한 줄(좁으면 wrap), 요소 간 `sm`~`md`.
3. **최신가 카드**(D-12): 컨트롤 바 아래, 공용 `LatestPriceCard` 재사용, 자기 `<AsyncBoundary>` 격리(최신가 실패가 결과 표를 가리지 않음).
4. **결과 영역**(주 시각 초점): 반응형 표↔카드. **focal point** — 각 이벤트의 `changeRate`(방향색)가 1차로 눈을 끈다.

- **Focal point:** 결과 표의 changeRate 컬럼(상승=red·하락=blue-700 방향색이 가장 강한 시각 신호). 상관≠인과 배너는 그 위 상시 컨텍스트.
- 각 비동기 단위(결과 표 / 최신가 카드)는 **자기 `<AsyncBoundary>`** 로 격리(Phase 7 D-08 상속).
- 아이콘 단독 액션 없음(프리셋·입력·셀렉터 모두 텍스트 라벨). 색 단독 의미 전달 금지(changeRate=색+부호+선택 글리프, 배지=색+라벨).

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | **table**(신규), alert, badge, button, card, select, skeleton | not required (공식 레지스트리) |

- 서드파티 레지스트리: **없음(none)** → view+diff 벳팅 대상 없음.
- `table`은 공식 shadcn 블록(Radix 의존 없는 styled `<table>` 래퍼) → 레지스트리 안전 게이트 비대상. **단 Windows에서 `npx shadcn add table` 금지(@ dir 버그·불필요 radix 추가) — `frontend/src/components/ui/table.tsx`에 공식 소스 손수 작성.**

따라서 Registry Safety **PASS**.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS

**Approval:** approved 2026-06-26
