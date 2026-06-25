---
phase: 7
slug: frontend-foundation
status: approved
shadcn_initialized: false
preset: "slate base color / new-york style — init during Phase 7 execution (no frontend scaffold exists yet)"
created: 2026-06-25
reviewed_at: 2026-06-25
---

# Phase 7 — UI Design Contract

> Frontend Foundation (FND-01~05)의 시각·인터랙션 계약. gsd-ui-researcher 생성, gsd-ui-checker 검증.
> 이 계약은 v1.1 마일스톤(Phase 7~11) 전체에 적용되는 디자인 토큰을 정의하며, Phase 7은 그중 **앱 셸·내비게이션·공용 상태 컴포넌트**를 구현한다.

상위 산출물에서 이미 확정된 사실(재정의 안 함):

- 스택: **Vite + React + TypeScript + Tailwind + Recharts** (REQUIREMENTS 확정 결정)
- 백엔드 무변경, Vite 프록시(`/api`→:8080), read 전용
- 3개 화면: 대시보드 / 품목 타임라인 / 이벤트 영향 (FND-03)
- 시각: UTC 데이터 원본 유지 → 화면 KST 표시 (FND-05). 다크모드·테마 토글은 v2(out of scope)

---

## Design System

| Property | Value |
|----------|-------|
| Tool | **shadcn/ui** (Radix primitives + Tailwind) |
| Preset | base color: `slate`, style: `new-york` — `npx shadcn init`은 Phase 7 실행 시 Vite 스캐폴딩 직후 수행 (현재 frontend 스캐폴드 없음) |
| Component library | radix (shadcn/ui 경유) |
| Icon library | **lucide-react** (shadcn 기본 아이콘셋) |
| Font | **Inter** (variable), 폴백 `system-ui, -apple-system, sans-serif` |

> `shadcn_initialized: false` — 초기화는 Phase 7 실행 태스크(앱 스캐폴딩 후)에서 수행. 본 계약은 init 시 적용할 base color/style/토큰을 사전 확정한다.
> 가격 등 숫자 표시에는 `font-variant-numeric: tabular-nums`를 적용해 자릿수 정렬을 유지한다.

---

## Spacing Scale

선언값 (모두 4의 배수, 표준 8-point 스케일):

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | 아이콘-라벨 간격, 인라인 패딩 |
| sm | 8px | 조밀한 요소 간격(배지 내부, 칩) |
| md | 16px | 기본 요소 간격, 카드 내부 패딩 |
| lg | 24px | 카드 간격, 섹션 패딩 |
| xl | 32px | 레이아웃 갭, 콘텐츠 영역 좌우 여백 |
| 2xl | 48px | 주요 섹션 구분 |
| 3xl | 64px | 페이지 상단/하단 여백 |

Exceptions: none — 데스크톱 우선 대시보드. (모바일 탭 타깃은 v2 반응형 마감(Phase 11)에서 다루며, 그 경우에도 표준 스케일 내에서 최소 44px 높이를 패딩 조합으로 확보한다.)

---

## Typography

폰트: Inter. 사이즈 4종 / 웨이트 2종(400 regular, 600 semibold) 고정.

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Display | 28px | 600 | 1.2 |
| Heading | 20px | 600 | 1.3 |
| Body | 16px | 400 | 1.5 |
| Label | 14px | 600 | 1.4 |

- **Display(28/600)**: 라우트 페이지 제목(예: "대시보드")
- **Heading(20/600)**: 카드·섹션 제목
- **Body(16/400)**: 본문, 설명, 상태 메시지
- **Label(14/600)**: 내비 항목, 배지, 테이블 헤더, 캡션, 보조 라벨

숫자/가격은 Body 또는 Label 사이즈에 `tabular-nums`로 렌더한다.

---

## Color

라이트 모드 단일 테마(다크모드 v2). 60/30/10 분할 명시.

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `#F8FAFC` (slate-50) | 앱 배경, 기본 표면 |
| Secondary (30%) | `#FFFFFF` 카드 표면 / `#E2E8F0` (slate-200) 보더·구분선 / 텍스트 `#0F172A`(slate-900)·`#64748B`(slate-500) | 카드, 상단 내비 바, 보더, 텍스트 |
| Accent (10%) | `#2563EB` (blue-600) | 아래 "Accent reserved for" 목록에만 |
| Destructive | `#DC2626` (red-600) | 에러 상태 표면/아이콘 (read-only 데모라 파괴적 버튼은 없음 — 에러 시각화에 사용) |

**Accent reserved for** (명시 목록 — 절대 "모든 인터랙티브 요소" 금지):

1. 현재 활성(선택된) 내비게이션 항목
2. 주요 버튼/CTA (예: 에러 상태의 "다시 불러오기")
3. 포커스 링 (`focus-visible` 아웃라인)
4. item selector에서 선택된 항목 표시 (Phase 9~)
5. 차트의 주(主) 가격 라인 (Phase 9~)

### Semantic Colors (액센트 아님 — 의미 전용, 장식 아님)

가격 등락·수집 status는 **의미 색**으로, 단일 장식 액센트(blue)와 역할이 분리된다. 이 분리가 의도된 이유: 액센트가 green이면 "가격 상승" 의미색과 충돌하므로 blue를 액센트로 택했다.

| Semantic | Value | Usage |
|----------|-------|-------|
| Up / 양호 | `#16A34A` (green-600) | 가격 상승, status 정상 |
| Down / 위험 | `#DC2626` (red-600) | 가격 하락, AUTH_ERROR 등 |
| Warning / 부분 | `#D97706` (amber-600) | PARTIAL_SUCCESS, RATE_LIMITED, insufficient_data |
| Neutral / 대기 | `#64748B` (slate-500) | NO_RUNS, 비어있음, downsampled 배지 |

> Phase 7은 앱 셸·공용 상태 컴포넌트만 구현하므로 의미 색은 주로 에러/대기 상태에 쓰이고, 등락 색은 Phase 8~10에서 활용된다. 토큰은 Phase 7에서 정의(테마 변수/Tailwind 확장)한다.

---

## Copywriting Contract

언어: **한국어** (UI 카피). 코드 식별자/경로/엔드포인트는 영어 유지.

| Element | Copy |
|---------|------|
| Primary CTA | **"다시 불러오기"** (에러 상태의 재시도 버튼 — 동사+명사) |
| Empty state heading | **"표시할 데이터가 아직 없어요"** |
| Empty state body | **"seed 프로파일 백엔드를 기동하면 시세가 채워집니다 (`SPRING_PROFILES_ACTIVE=seed`)."** |
| Error state | **"백엔드에 연결하지 못했어요. seed 백엔드(`:8080`)가 켜져 있는지 확인하고 다시 불러오세요."** (문제 + 해결 경로) |
| Destructive confirmation | 해당 없음 — read-only 데모, 파괴적 동작 없음 (v2의 관리자 쓰기 UI에서 다룸) |

### 추가 카피 (Phase 7 셸)

- 내비게이션 라벨: **대시보드** / **품목 타임라인** / **이벤트 영향**
- 로딩 상태: 스켈레톤(시각) + 보조 텍스트 **"불러오는 중…"**
- 앱 타이틀/브랜드: **"로스트아크 시세 트래커"** (상단 내비 좌측)

> 빈 상태·에러는 화면(컴포넌트)별로 동일 문구를 재사용하되, 데이터 종류에 맞춰 본문 한 줄만 교체할 수 있다. 일반 문구("데이터 없음", "오류 발생") 사용 금지.

---

## Layout & Visual Hierarchy

Phase 7 앱 셸의 시각 위계 — 실행자가 우선순위를 추측하지 않도록 명시:

- **레이아웃**: 상단 고정 내비 바(secondary 표면, 높이 ~56px) + 그 아래 콘텐츠 영역. 콘텐츠 영역 최대 폭 제한(예: `max-w-screen-xl`) + 좌우 `xl(32px)` 여백.
- **Focal point (각 라우트)**: 현재 라우트의 **콘텐츠 영역**이 1차 시각 초점. 상단 내비는 지속적 secondary 요소다.
- **활성 표시**: 내비에서 현재 라우트 항목은 **accent(blue-600) 텍스트/언더라인**으로, 나머지는 slate-500 Label로 구분 → 사용자가 위치를 즉시 인지.
- **내비 항목은 텍스트 라벨 우선**(아이콘-only 아님). 아이콘을 곁들일 경우 항상 텍스트 라벨과 동반(접근성).
- **공용 상태 컴포넌트(로딩/빈/에러)**: 콘텐츠 영역 중앙 정렬, 카드 또는 패널 안에 heading + body + (에러 시) CTA 수직 배치, 요소 간 `md(16px)`.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | button, card, badge, skeleton, alert, select, table, navigation-menu (또는 tabs) | not required (공식 레지스트리) |

서드파티 레지스트리: **없음** (none). 따라서 view+diff 벳팅 대상 없음 → Registry Safety PASS.

> Phase 7에서 우선 사용하는 블록: button, card, skeleton, alert, navigation-menu(상단 내비). badge/select/table은 Phase 8~10에서 사용하나 init 시 함께 추가 가능.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS
- [x] Dimension 2 Visuals: PASS
- [x] Dimension 3 Color: PASS
- [x] Dimension 4 Typography: PASS
- [x] Dimension 5 Spacing: PASS
- [x] Dimension 6 Registry Safety: PASS

**Approval:** approved 2026-06-25
