---
phase: 9
slug: item-timeline
status: approved
shadcn_initialized: true
preset: "base color: slate, style: new-york (Phase 7에서 init 완료 — frontend/components.json)"
created: 2026-06-26
reviewed_at: 2026-06-26
---

# Phase 9 — UI Design Contract (Item Timeline)

> 품목 가격 시계열 차트 + 이벤트 마커 화면(TIME-01~05)의 시각·인터랙션 계약. gsd-ui-researcher 생성, gsd-ui-checker 검증.
> 이 계약은 **Phase 7 디자인 시스템(`07-UI-SPEC.md`)을 상속**하며, 재정의하지 않는다. 본 문서는 그 위에 **차트·이벤트 마커·다운샘플·상태 카피**의 phase 전용 시각 델타만 추가한다.

## 상속 (Phase 7 — 재정의 금지)

상위 계약에서 이미 확정돼 그대로 적용되는 토큰 (`frontend/src/index.css` 구현 확인):

- **디자인 시스템:** shadcn/ui (slate / new-york), lucide-react, Inter (variable, `tabular-nums`)
- **간격:** 4 / 8 / 16 / 24 / 32 / 48 / 64 (8-point)
- **타이포:** Display 28/600 · Heading 20/600 · Body 16/400 · Label 14/600 (사이즈 4 / 웨이트 2)
- **색 60/30/10:** Dominant `#F8FAFC` · Secondary(카드 `#FFFFFF`·보더 `#E2E8F0`·텍스트 `#0F172A`/`#64748B`) · Accent `#2563EB`(blue-600)
- **의미색:** up `#16A34A` · down `#DC2626` · warning `#D97706` · neutral `#64748B`
- **카피 언어:** 한국어(코드 식별자/경로/엔드포인트는 영어 유지)
- **Accent 예약 목록**에 이미 포함된 Phase 9 항목: `item selector 선택 항목` · `차트의 주 가격 라인`. 본 계약은 여기에 **선택된 프리셋 버튼**을 추가 사용처로 명시한다(아래 Color 참조).

상위 산출물에서 확정된 사실(재논의 안 함): Vite+React+TS+Tailwind+Recharts 스택, 백엔드 무변경·read 전용, UTC 데이터 원본 → KST 표시(off-by-9h 가드: 차트 위치는 raw UTC epoch, 라벨만 KST), 라이트 단일 테마.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | **shadcn/ui** (slate / new-york) — Phase 7 init 완료 |
| Preset | base color `slate`, style `new-york` (`frontend/components.json`) |
| Component library | radix (shadcn/ui 경유) |
| Icon library | **lucide-react** |
| Font | **Inter** (variable), 숫자/가격은 `tabular-nums` |
| Charting | **Recharts** (직접 npm 의존 — 레지스트리 블록 아님). `npm install recharts` 필요(미설치), React 19 호환 버전 |

> **신규 shadcn 블록:** `select`(드롭다운 셀렉터, D-05) 추가 필요. 기존 보유: button, card, badge, skeleton, alert, navigation-menu. 전부 공식 레지스트리.

---

## Spacing Scale

상속(재선언 안 함). Phase 9 화면에서의 적용처:

| Token | Value | Phase 9 Usage |
|-------|-------|---------------|
| xs | 4px | 범례 색 스와치-라벨 간격, 배지 내부 |
| sm | 8px | 프리셋 버튼 간격, 셀렉터-컨트롤 간격 |
| md | 16px | 컨트롤 바 ↔ 차트, 최신가 카드 내부 패딩 |
| lg | 24px | 차트 영역 패딩, 섹션 구분 |
| xl | 32px | 콘텐츠 영역 좌우 여백(상속) |

Exceptions: none. (차트 내부 마진은 Recharts `margin` prop으로 처리하되 시각 간격은 위 스케일에 정렬: 예 `{ top: 16, right: 16, bottom: 8, left: 8 }`.)

---

## Typography

상속. Phase 9 신규 표면의 역할 매핑:

| Surface | Role | Size/Weight |
|---------|------|-------------|
| 라우트 제목("품목 타임라인") | Display | 28/600 |
| 최신가 카드 품목명 | Heading | 20/600 |
| 최신가(골드 숫자) | Heading 20/600 + `tabular-nums` | 자릿수 정렬 |
| 차트 축 눈금·범례 라벨·배지·날짜 입력 라벨 | Label | 14/600 |
| 상태(빈/에러) 본문, 호버 툴팁 본문 | Body | 16/400 |

차트 SVG 텍스트(Recharts `tick`/`Tooltip`)도 위 사이즈·`var(--font-sans)`·muted(`#64748B`) 색을 따른다.

---

## Color

상속한 60/30/10 + 의미색은 그대로. **본 phase가 신규로 잠그는 것은 이벤트 마커 카테고리 색뿐이다.**

### Accent (상속 — Phase 9 사용처 확정)

`#2563EB`(blue-600)는 아래에만:
1. 차트의 **주(主) 가격 라인** (가장 중요한 신규 사용처)
2. **선택된** 프리셋 버튼(7/30/90일) — 비선택은 secondary/outline
3. 드롭다운에서 **선택된** 품목 표시
4. 포커스 링(`focus-visible`)

### 이벤트 마커 팔레트 (신규 — 카테고리 인코딩, 장식 액센트 아님)

> **분리 근거(Dimension 3 대비):** 이 4색은 `eventType`이라는 **범주형 데이터를 인코딩**하는 색으로, 의미색(up/down/warning/neutral)과 동일하게 *의미 전용*이다. 단일 장식 액센트(blue-600)와 역할이 다르며 "두 번째 액센트"가 아니다. 가격선(blue)·의미색(green/red/amber)과 **모두 다른 색상환 구역**을 골라 차트 위에서 충돌하지 않도록 했다(상호 색상 ≥ 55° 이격). 사용자 확정: 선명한 정성 4색.

| eventType | 색 | Hex | 범례 라벨 |
|-----------|-----|-----|-----------|
| `LOA_ON` | violet-600 | `#7C3AED` | 로아ON |
| `MAJOR_UPDATE` | orange-600 | `#EA580C` | 대규모 업데이트 |
| `SEASON_END` | pink-600 | `#DB2777` | 시즌 종료 |
| `BALANCE_PATCH` | teal-600 | `#0D9488` | 밸런스 패치 |

- 마커(`ReferenceLine`)는 **점선 세로선**(`strokeDasharray "4 4"`, `strokeWidth 1.5`)으로 그려 **실선 가격선과 형태로도 구분**한다(색 의존 최소화·접근성).
- 색만으로 의미를 전달하지 않는다: 항상 **범례 라벨(텍스트)** + 호버 툴팁 제목과 함께 제시.

### 의미색·중립색 (상속 — Phase 9 사용처)

| Semantic | Hex | Phase 9 Usage |
|----------|-----|---------------|
| Neutral | `#64748B` (slate-500) | **다운샘플 배지** 표면/텍스트, 빈 상태 |
| Destructive/Down | `#DC2626` (red-600) | 에러 상태(400/404) 아이콘·강조 |

가격 등락 up/down 색은 본 phase 차트 라인엔 쓰지 않는다(라인은 accent blue 단색). 등락 색은 Phase 10 변화율 표 소관.

---

## Chart Visual Contract (TIME-02 — 신규)

| Property | Value | 근거 |
|----------|-------|------|
| 컨테이너 | Recharts `ResponsiveContainer` width 100%, height **360px**(데스크톱), 최소 280px | 데스크톱 우선 |
| 가격 라인 | `minPrice`, stroke `#2563EB`(accent), `strokeWidth 2`, `type="monotone"` | accent=주 가격선 |
| x축 | raw **UTC epoch(ms)** 기준 위치, **라벨만 KST**(`formatKst`) | off-by-9h 가드(Phase 7 D-09) |
| x축 라벨 포맷 | 범위 > 2일: `M/D`(KST) · 툴팁 안에선 `M/D HH:mm`(KST) | 밀도 과밀 방지 |
| y축 | 골드, 눈금은 **compact ko-KR**(예 `1.2만`, `125만`), `tabular-nums` | 큰 골드 단위 축약 |
| 그리드 | 수평선만, `#E2E8F0`(slate-200), `strokeDasharray "3 3"`, 수직 그리드 없음 | 깔끔·마커 가시성 |
| 가격선 데이터 툴팁 | 호버 지점: KST 시각 + 골드 **전체 자릿수**(그룹 구분 + " G") + (다운샘플 시 `sampleCount` "n개 평균") | honest-data |

> **다운샘플 라인 구분(D-08, TIME-03):** `downsampled=true`면 가격선을 **점(dot) 없는 평균선**(`dot={false}`)으로, raw면 **개별 점이 있는 원점선**(`dot={{ r: 2.5 }}`, 단 점 개수 > 60이면 dot 생략·`activeDot`만)으로 그린다. 선 색·두께는 동일(accent blue 단색), **점 유무 + 배지**가 "원점 vs 버킷 평균"의 이중 시각 신호다.

---

## Event Marker Contract (TIME-04 — 신규)

- 기간과 겹치는 각 `event`를 `occurredAt`(UTC epoch) 위치에 **세로 `ReferenceLine`**(점선, 위 팔레트 색)으로 그린다.
- **호버 툴팁(D-02):** 마커에 호버 시 이벤트 `title` + KST 일시를 표시. 상시 라벨은 두지 않는다(밀집 시 겹침). seed는 마커 2개라 단순.
  - *구현 주의(planner/researcher 영역):* Recharts는 차트당 Tooltip 1개라 `ReferenceLine` 호버와 가격선 Tooltip 공존이 까다롭다 — 커스텀 Tooltip / `ReferenceLine label` / 별도 호버 타깃 중 택1을 planner가 Recharts 최신 API로 확정. 본 계약은 **동작(호버→title+KST)**만 잠근다.
- **범례:** 차트 바로 위 또는 아래에 4색 스와치 + Label(14/600) 텍스트의 가로 범례. 색-라벨 간격 `xs(4px)`, 항목 간 `md(16px)`. 현재 기간에 해당 eventType 마커가 없어도 4종 범례는 항상 표기(색-의미 학습 안정성).
- **서사 정직성:** 마커는 "이벤트 시점이 가격 변동과 *겹친다(상관)*"는 시각이지 인과가 아니다 — 라벨·툴팁에 인과 단정 카피 금지(상관≠인과, Phase 10에서 명시 고지).

---

## Selector & Controls Contract (TIME-01, TIME-05 — 신규)

- **품목 셀렉터(D-05/D-06):** shadcn `select` 드롭다운. 트리거는 선택된 `displayName` 표시. 진입 시 URL `?item=` 우선, 없으면 `/api/items` 첫 품목 자동 선택(빈 프롬프트 없음). 선택 변경 → URL `?item=` 기록.
- **기간 컨트롤(D-04):** 프리셋 버튼 **7일 / 30일 / 90일**(가로 배열, `sm` 간격) + 그 옆에 **시작일/종료일** native `<input type="date">` 쌍. 선택된 프리셋은 accent(blue-600) outline, 비선택은 secondary. from/to 변경 → URL `?from=&to=` 기록.
- **기본 윈도우(D-03):** URL에 from/to 없으면 **최근 30일**. (seed 8일 윈도우 포함돼 마커 2개·차트 비어있지 않음.)
- **배치 위치:** 셀렉터 + 최신가 카드는 **공용 위치**에 추출(D-07) — Phase 10이 동일 import 재사용. 정확한 경로/명명은 planner 재량.

## Latest Price Card Contract (TIME-01 — 신규)

타임라인 전용 **경량** 카드(Dashboard `ItemCard` 재사용 아님 — D-07). shadcn `card`. 내용 수직 배치(요소 간 `sm`):

| 필드 | 소스 | 표시 |
|------|------|------|
| 품목명 | 선택 item `displayName` | Heading 20/600 |
| 최신가 | `useLatestPrice(id)` → `minPrice` | Heading 20/600 + `tabular-nums`, 전체 자릿수 + " G" |
| 수집 시각 | `collectedAt` | Label 14/600 muted, `formatKst` "YYYY-MM-DD HH:mm KST" |

카드는 자기 `<AsyncBoundary>`로 격리(로딩=스켈레톤, 빈/에러=상속 상태 컴포넌트).

---

## Copywriting Contract

언어: 한국어. 상속한 공용 로딩/빈/에러 문구 위에 phase 전용 카피를 얹는다.

### 상태 카피 — 200-empty / 400 / 404 각각 구분 (D-09, TIME-05)

차트 영역의 `<AsyncBoundary>`가 `ApiError.status`로 분기. **일반 "오류 발생/데이터 없음" 금지.**

| 케이스 | Heading | Body (문제 + 다음 행동) | 비고 |
|--------|---------|------------------------|------|
| 200-empty (빈 범위) | **"이 기간에는 시세 데이터가 없어요"** | **"선택한 기간에 수집된 가격이 없습니다. 기간을 넓히거나 아래 '최근 30일'을 눌러보세요."** | 빈 상태 + "최근 30일 보기" 버튼(accent CTA, 30일 프리셋으로 리셋) |
| 400 (to≤from) | **"조회 기간을 다시 확인해 주세요"** | **"종료일이 시작일보다 같거나 빠릅니다. 시작일 이후의 종료일을 선택하고 다시 불러오세요."** | 입력 안내, destructive 아이콘 |
| 404 (없는 품목) | **"존재하지 않는 품목이에요"** | **"선택한 품목을 찾을 수 없습니다. 위 목록에서 다른 품목을 선택해 주세요."** | 셀렉터로 유도 |

> 로딩 = 차트 자리 스켈레톤 + "불러오는 중…"(상속). 백엔드 연결 실패(네트워크) = 상속 에러 카피("백엔드에 연결하지 못했어요…") 재사용.

### Phase 9 신규 라벨/카피

| Element | Copy |
|---------|------|
| Primary CTA (빈 상태) | **"최근 30일 보기"** (30일 프리셋 리셋 — 동사구) |
| 라우트 제목 | **"품목 타임라인"** (내비 라벨과 동일, 상속) |
| 프리셋 버튼 | **"7일" / "30일" / "90일"** |
| 날짜 입력 라벨 | **"시작일" / "종료일"** |
| 다운샘플 배지 | **"버킷 평균 · {bucketWidth}"** (예: "버킷 평균 · 1h") — neutral(slate) 배지 |
| 배지 호버 툴팁 | **"이 구간은 원본이 아니라 {bucketWidth} 단위 버킷 평균입니다."** (정직성) |
| 마커 호버 툴팁 | `{이벤트 title}` + KST 일시 (예: "로아ON 윈터 페스타 · 2026-06-20 21:00 KST") |
| 범례 라벨 | 로아ON / 대규모 업데이트 / 시즌 종료 / 밸런스 패치 |
| 셀렉터 placeholder | **"품목 선택"** (자동 선택으로 거의 미노출) |

Destructive 확인: 해당 없음 — read-only 데모, 파괴적 동작 없음.

---

## Layout & Visual Hierarchy

위→아래 단일 컬럼(데스크톱, 콘텐츠 영역 `max-w-screen-xl` 상속):

1. **컨트롤 바**(상단): `[품목 셀렉터]  [7일][30일][90일]  [시작일][종료일]` — 한 줄(좁으면 wrap), 요소 간 `sm`~`md`.
2. **최신가 카드**: 컨트롤 바 아래, 좌측 정렬 경량 카드.
3. **차트 영역**(주 시각 초점): 범례 + 360px 차트 + (다운샘플 시) 우상단 배지. **이 화면의 focal point** — 가격선(accent) + 이벤트 마커가 1차로 눈을 끈다.

- **Focal point:** 차트. 가격선(accent blue)이 주 데이터, 마커(점선 카테고리 색)가 보조 오버레이.
- 각 비동기 단위(차트 / 최신가 카드)는 **자기 `<AsyncBoundary>`**로 격리(Phase 7 D-08 / Phase 8 D-07 상속) — 한쪽 실패가 다른 쪽을 가리지 않음.
- 아이콘 단독 액션 없음(프리셋·셀렉터 모두 텍스트 라벨). 색 단독 의미 전달 금지(마커=색+범례 텍스트+형태).

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | select(신규), card, badge, button, alert, skeleton | not required (공식 레지스트리) |

- 서드파티 레지스트리: **없음(none)** → view+diff 벳팅 대상 없음.
- **Recharts**는 npm 차트 라이브러리(직접 의존)로 shadcn 레지스트리 블록이 아니다 → 레지스트리 안전 게이트 비대상.

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
