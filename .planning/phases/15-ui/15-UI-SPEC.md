---
phase: 15
slug: ui
status: approved
shadcn_initialized: true
preset: "new-york style / slate base color (frontend/components.json — 이미 초기화됨)"
created: 2026-07-01
reviewed_at: 2026-07-01
---

# Phase 15 — UI Design Contract (관리자 콘솔)

> `/api/admin/*`(X-Admin-Secret 게이트) + `/api/health/collection`을 소비하는 프론트 관리자 콘솔의 시각·인터랙션 계약. gsd-ui-researcher 생성, gsd-ui-checker 검증.
> 이 계약은 **Phase 7 UI-SPEC(v1.1 디자인 토큰)을 상속**하며, 그 위에 관리자 **쓰기 표면**(로그인 게이트·CRUD 폼·파괴적 작업·인라인 피드백)만 신규로 정의한다. 토큰(spacing/typography/color)은 재정의하지 않고 Phase 7 계약과 `frontend/src/index.css`를 단일 출처로 삼는다.

상위 산출물에서 이미 확정된 사실(재정의 안 함):

- 디자인 시스템: **shadcn/ui new-york · slate · lucide · Inter** — `frontend/components.json` 실측으로 초기화 확인
- 구조 결정(CONTEXT.md D-01~D-14): sessionStorage 시크릿 · `GET /api/admin/events` probe 로그인 · 전역 401 자동 로그아웃 · 신규 `/admin` 라우트 · 전체 게이트(로그인 폼만 렌더) · 단일 페이지 섹션 · 인라인 폼 · KST→UTC 변환 · 확인 단계 · `invalidateQueries`
- 시각·카피 세부는 CONTEXT "Claude's Discretion"으로 **명시 위임** → 본 계약이 규범적으로 확정(재질문 없음)
- 백엔드 무변경 원칙: 신규 코드는 `GET /api/admin/items` read-only 1개(D-13), 수집/캐시/event-impact/인증 **0줄**

---

## Design System

| Property | Value |
|----------|-------|
| Tool | **shadcn/ui** (Radix primitives + Tailwind) — `frontend/components.json`에서 이미 초기화됨 |
| Preset | style: `new-york`, base color: `slate`, cssVariables: true (Phase 7 계약과 일치) |
| Component library | radix (shadcn/ui 경유) |
| Icon library | **lucide-react** |
| Font | **Inter** (variable, 번들), 폴백 `system-ui, -apple-system, sans-serif` |

**이 페이즈가 신규로 필요한 shadcn 블록**: `input`, `textarea` (폼 입력 — 현재 `src/components/ui/`에 **미설치**).
설치된 블록(`button·card·table·select·badge·alert·skeleton·navigation-menu`)은 재사용한다. `dialog`·`toast`는 **의도적으로 도입하지 않는다**(D-07 인라인 폼 · D-09 인라인 확인 · 인라인 상태 메시지로 회피).

> ⚠ shadcn CLI Windows `@` 디렉터리 버그 — `input`·`textarea` 블록은 CLI 대신 **`src/components/ui/`에 수기 작성**한다(기존 블록과 동일 방식). 폼 필드 라벨은 별도 `label` 블록 없이 네이티브 `<label>` + Label 타이포 토큰으로 처리한다.
> 숫자(수집 카운트 등)는 Phase 7 계약대로 `tabular-nums`로 렌더한다.

---

## Spacing Scale

Phase 7 계약(8-point 스케일) **상속** — 모두 4의 배수:

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | 아이콘-라벨 간격, 배지 내부 |
| sm | 8px | 폼 필드 라벨↔입력 간격, 조밀한 요소 |
| md | 16px | 기본 요소 간격, 카드 내부 패딩, 폼 필드 간 세로 간격 |
| lg | 24px | 섹션(카드) 내부 패딩, 폼↔목록 간격 |
| xl | 32px | 섹션(카드) 간 세로 간격, 콘텐츠 좌우 여백 |
| 2xl | 48px | 로그인 화면 카드 상하 여백(수직 중앙 정렬) |
| 3xl | 64px | 페이지 상단/하단 여백 |

Exceptions: none. 폼 입력·버튼은 shadcn 기본 높이(`h-9`=36px / `h-10`=40px, 모두 4의 배수)를 유지해 스케일을 벗어나지 않는다.

---

## Typography

Phase 7 계약(Inter · 사이즈 4종 · 웨이트 2종) **상속**:

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Display | 28px | 600 | 1.2 |
| Heading | 20px | 600 | 1.3 |
| Body | 16px | 400 | 1.5 |
| Label | 14px | 600 | 1.4 |

관리자 콘솔에서의 적용:
- **Display(28/600)**: 로그인 화면 제목 "관리자 콘솔", 인증 후 콘솔 헤더 제목
- **Heading(20/600)**: 각 섹션 카드 제목("게임 이벤트" / "워치리스트" / "수집 상태")
- **Body(16/400)**: 폼 입력 텍스트, 목록 셀 본문, 설명, 인라인 상태/오류 메시지
- **Label(14/600)**: 폼 필드 라벨, 테이블 헤더, 배지(활성/비활성), 확인 프롬프트 보조 텍스트

수집 카운트·아이템 번호 등 숫자는 Body/Label 사이즈에 `tabular-nums`.

---

## Color

Phase 7 계약(라이트 단일 테마 · 60/30/10 분할) **상속**. `frontend/src/index.css` 토큰이 단일 출처.

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `#F8FAFC` (slate-50) | 앱 배경, 기본 표면 |
| Secondary (30%) | `#FFFFFF` 카드 / `#E2E8F0`(slate-200) 보더 / 텍스트 `#0F172A`(slate-900)·`#64748B`(slate-500) | 섹션 카드, 콘솔 헤더 바, 폼 입력 보더, 텍스트 |
| Accent (10%) | `#2563EB` (blue-600) | 아래 "Accent reserved for" 목록에만 |
| Destructive | `#DC2626` (red-600) | 파괴적 작업 버튼·확인 프롬프트 (아래 참조) |

**Accent reserved for** (명시 목록 — "모든 인터랙티브 요소" 금지):

1. 폼의 **주요 제출(primary) 버튼**: 로그인 · 이벤트 등록/변경 저장 · 품목 추가 · **재활성**
2. 포커스 링 (`focus-visible` 아웃라인 — `--ring` = blue-600)
3. 활성(현재 선택된) 섹션 표시 — 섹션 내비를 둘 경우 활성 항목 텍스트/언더라인

> **Phase 7과의 차이(의도된 확장):** Phase 7은 read-only 데모라 파괴적 버튼이 없어 destructive를 에러 시각화에만 썼다. 관리자 콘솔은 **쓰기 표면**이므로 destructive(red-600)를 파괴적 **동작 버튼**과 그 확인 프롬프트에 확장한다:
> - **이벤트 삭제** 버튼, **품목 비활성** 버튼 = destructive(red-600) 스타일(shadcn `variant="destructive"` 또는 destructive 텍스트/보더)
> - 인라인 확인 프롬프트의 실행 버튼("삭제"/"비활성") = destructive
> - **재활성**은 파괴적이지 않으므로 accent(primary), destructive 아님

### Semantic Colors (액센트 아님 — 의미 전용)

Phase 7 의미색 토큰(`--up/--down/--warning/--neutral`) 재사용:

| Semantic | Value | Usage (Phase 15) |
|----------|-------|------------------|
| Up / 양호 | `#16A34A` (green-600) | 워치리스트 **활성** 배지, 수집 status 정상 |
| Down / 위험 | `#DC2626` (red-600) | 수집 실패 카운트, AUTH_ERROR |
| Warning / 부분 | `#D97706` (amber-600) | PARTIAL_SUCCESS, RATE_LIMITED |
| Neutral / 대기 | `#64748B` (slate-500) | 워치리스트 **비활성** 배지, NO_RUNS |

> 활성/비활성 배지는 기존 `StatusBadge`/`ImpactStatusBadge` 색 배지 선례를 따른다(활성=up green, 비활성=neutral slate). 수집 상태 카드는 기존 `HealthCard`를 재사용하므로 그 status 매핑을 그대로 상속한다(D-11).

---

## Copywriting Contract

언어: **한국어** (UI 카피). 코드 식별자/경로/엔드포인트/헤더명은 영어 유지. 일반 문구("오류 발생", "데이터 없음") 금지 — 문제 + 다음 행동을 항상 함께.

| Element | Copy |
|---------|------|
| Primary CTA | **"로그인"** (콘솔 진입 게이트 — 시크릿 제출) |
| Empty state heading | **"등록된 이벤트가 없어요"** (이벤트 섹션) / **"워치리스트가 비어 있어요"** (워치리스트 섹션) |
| Empty state body | 이벤트: **"위 폼에서 첫 게임 이벤트를 등록하면 목록에 표시됩니다."** · 워치리스트: **"위에서 로스트아크 아이템 번호로 품목을 추가하면 수집 대상이 됩니다."** |
| Error state | **"백엔드에 연결하지 못했어요. admin 백엔드(`:8080`)가 켜져 있는지 확인하고 다시 시도하세요."** (조회 실패 — 문제 + 해결 경로) |
| Destructive confirmation | 이벤트 삭제: **"이 이벤트를 삭제할까요? 되돌릴 수 없습니다."** [삭제] [취소] · 품목 비활성: **"이 품목을 비활성할까요? 수집 대상에서 제외됩니다."** [비활성] [취소] |

### 로그인 게이트 카피 (D-01/02/03, ADMINUI-01/02/06)

| 상황 | Copy |
|------|------|
| 화면 제목 (Display) | **"관리자 콘솔"** |
| 안내 (Body) | **"관리자 시크릿을 입력해 로그인하세요."** |
| 시크릿 입력 라벨 (Label) | **"관리자 시크릿"** — `type="password"`, placeholder **"관리자 시크릿 입력"** |
| 제출 버튼 (Primary CTA) | **"로그인"** / 검증 중 **"확인 중…"**(disabled) |
| 시크릿 거부 (401) | **"시크릿이 올바르지 않습니다. 다시 확인해 주세요."** (인라인, destructive 텍스트) |
| 연결 실패(네트워크/5xx) | **"백엔드에 연결하지 못했어요. admin 백엔드(`:8080`)가 켜져 있는지 확인하고 다시 시도하세요."** |
| 세션 중 401 자동 로그아웃 (D-03) | 로그인 화면 복귀 + **"세션이 만료되어 로그아웃되었습니다. 시크릿을 다시 입력해 주세요."** |
| 로그아웃 버튼 (콘솔 헤더) | **"로그아웃"** |

> Enter 키로 제출 가능(Claude 재량). 시크릿은 화면·에러 문구·로그 어디에도 값 자체를 노출하지 않는다(불변 제약).

### 이벤트 관리 카피 (ADMINUI-03)

| Element | Copy |
|---------|------|
| 섹션 제목 (Heading) | **"게임 이벤트"** |
| 등록 폼 제출 | **"이벤트 등록"** / 저장 중 **"등록 중…"** |
| 수정 폼 제출 | **"변경 저장"** / 저장 중 **"저장 중…"** · 취소 **"취소"** |
| 폼 필드 라벨 | 유형 **"이벤트 유형"** · 제목 **"제목"** · 발생 시각 **"발생 시각 (KST)"** · 설명 **"설명 (선택)"** |
| EventType 셀렉트 라벨 | `LOA_ON`→**"로아ON"** · `MAJOR_UPDATE`→**"대규모 업데이트"** · `SEASON_END`→**"시즌 종료"** · `BALANCE_PATCH`→**"밸런스 패치"** |
| 성공 피드백 (인라인) | 등록 **"이벤트를 등록했어요."** · 수정 **"변경을 저장했어요."** · 삭제 **"이벤트를 삭제했어요."** |
| mutation 실패 (인라인) | **"요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."** |

### 워치리스트 카피 (ADMINUI-04)

| Element | Copy |
|---------|------|
| 섹션 제목 (Heading) | **"워치리스트"** |
| 추가 폼 필드 라벨 | 아이템 번호 **"아이템 번호"**(placeholder **"로스트아크 아이템 번호"**) · 표시 이름 **"표시 이름"** · 분류 **"분류 (선택)"** |
| 추가 폼 제출 | **"품목 추가"** / 추가 중 **"추가 중…"** |
| 활성/비활성 배지 (Label) | **"활성"**(up green) / **"비활성"**(neutral slate) |
| 비활성 버튼 (destructive) | **"비활성"** (확인 단계 경유) |
| 재활성 버튼 (accent) | **"재활성"** / 처리 중 **"재활성 중…"** |
| 중복 추가 (409) | **"이미 활성 상태인 품목이에요."** (인라인, warning 톤) |
| 성공 피드백 (인라인) | 추가 **"품목을 추가했어요."** · 비활성 **"품목을 비활성했어요."** · 재활성 **"품목을 재활성했어요."** |

### 수집 상태 카피 (ADMINUI-05, D-11)

기존 `HealthCard`를 재사용하므로 Phase 7/대시보드 카피를 상속한다:
- NO_RUNS(대기): **"아직 수집 실행 기록이 없어요"** + **"스케줄러가 첫 수집을 마치면 시도·성공·실패와 마지막 실행 시각이 여기 표시됩니다."**
- 조회 실패 시 재시도 CTA: **"다시 불러오기"**
- 카운트: **"시도 N · 성공 N · 실패 N"** · **"마지막 실행 {KST}"**

### 공통 로딩

- 조회 로딩: 스켈레톤(시각) + 보조 텍스트 **"불러오는 중…"** (Phase 7 상속)
- mutation 진행 중: 해당 제출 버튼 disabled + 진행 문구("저장 중…" 등), 목록은 유지

---

## Layout & Visual Hierarchy

관리자 콘솔의 시각 위계 — 실행자가 우선순위를 추측하지 않도록 명시:

### 라우팅 & 게이트 (D-04/05)
- 신규 **최상위 `/admin` 라우트**. 공개 TopNav("대시보드/품목 타임라인/이벤트 영향")에는 **관리자 미노출** — 직접 URL 진입(필요 시 footer 소형 링크는 재량).
- **미로그인 = 로그인 폼만 렌더**(DOM 전체 게이트). 쓰기 UI(폼·목록·버튼)의 DOM은 미인증 상태에서 **생성하지 않는다** — ADMINUI-06 "노출되지 않는다" 문구에 정확 부합(버튼 disable 아님).

### 로그인 화면 (focal = 중앙 카드)
- 콘텐츠 영역 **수직·수평 중앙**에 단일 로그인 Card(폭 제한, 예 `max-w-sm`).
- 카드 내부: Display 제목 "관리자 콘솔" → Body 안내 → 시크릿 입력(Label + `password` input) → Primary CTA "로그인" → (오류 시) 인라인 에러. 요소 간 `md(16px)`, 라벨↔입력 `sm(8px)`.

### 인증 후 콘솔 (focal = 활성 섹션 콘텐츠)
- **관리자 셸 헤더**(secondary 표면 바): 좌측 Display/Heading "관리자 콘솔", 우측 **"로그아웃"** 버튼. 공개 셸의 라우트 내비는 재현하지 않는다(관리자 맥락 명시). 기존 `AppLayout` 재사용 여부는 재량이나, **공개 내비 링크는 관리자 화면에 노출하지 않는다**.
- **단일 페이지 · 스택 섹션**(D-06): 콘텐츠 영역에 3개 섹션 Card를 세로로 쌓는다(섹션 간 `xl(32px)`):
  1. **게임 이벤트** — 상단 인라인 등록/수정 폼(D-07), 하단 이벤트 목록(테이블/카드, `occurred_at desc`). 각 행에 수정·**삭제**(destructive) 액션.
  2. **워치리스트** — 상단 인라인 추가 폼, 하단 품목 목록(활성+비활성, `GET /api/admin/items` D-13). 각 행에 활성/비활성 배지 + **비활성**(destructive)/**재활성**(accent) 액션.
  3. **수집 상태** — 기존 `HealthCard` 재사용(최신 1건 카드, D-11).
  > 섹션 대신 탭 렌더도 CONTEXT 허용 범위지만, 본 계약은 **스택 섹션**을 규범으로 택한다(신규 `tabs` 블록 회피 + 기존 대시보드 카드-스택 패턴 일관). 탭 채택 시 활성 탭은 accent 언더라인.

### 폼 & 파괴적 확인 (D-07/09)
- **인라인 폼**: 모달(`dialog`) 미사용. 폼은 해당 섹션 Card 내부 상단에 배치, 필드는 세로 스택(라벨 위·입력 아래, 필드 간 `md`).
- **파괴적 확인 = 인라인 2단계**(D-09): 삭제/비활성 버튼 클릭 → 같은 자리(행/카드)에서 확인 프롬프트로 치환("…할까요?" + [실행(destructive)] [취소]). 별도 다이얼로그 없음.
- **mutation 후 갱신**: 성공 시 `invalidateQueries` → 목록 refetch(D-10), 인라인 성공 메시지 표시.

### 상태 컴포넌트 재사용
- 조회 로딩/빈/에러는 기존 `AsyncBoundary` + `LoadingState/EmptyState/ErrorState`(Phase 7 D-07/08) 재사용. 빈/에러 카피는 위 Copywriting 계약으로 교체.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | **신규**: input, textarea · **재사용**: button, card, table, select, badge, alert, skeleton | not required (공식 레지스트리) |

서드파티 레지스트리: **없음** (none). 따라서 view+diff 벳팅 대상 없음 → Registry Safety PASS.

> 신규 블록(`input`·`textarea`)은 shadcn 공식이나, Windows CLI `@` 디렉터리 버그로 CLI 대신 `src/components/ui/`에 **수기 작성**한다(기존 8개 블록과 동일). 이는 배치 방식의 문제일 뿐 레지스트리 신뢰성 문제가 아니다.

---

## Checker Sign-Off

- [x] Dimension 1 Copywriting: PASS — CTA 전부 동사+명사, 빈/에러/파괴적 카피 구체 선언
- [x] Dimension 2 Visuals: PASS — 포컬·위계 선언, 아이콘-only 없음
- [x] Dimension 3 Color: PASS — accent 명시 목록, 60/30/10, destructive 선언(쓰기 표면 확장)
- [x] Dimension 4 Typography: PASS — 사이즈 4종·웨이트 2종, body 1.5
- [x] Dimension 5 Spacing: PASS — 전부 4의 배수 표준 세트, 예외 없음
- [x] Dimension 6 Registry Safety: PASS — shadcn 공식만, 서드파티 없음

**Approval:** approved 2026-07-01
