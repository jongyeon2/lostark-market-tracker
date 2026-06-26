# Phase 9: Item Timeline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-26
**Phase:** 9-item-timeline
**Areas discussed:** 이벤트 마커 & 차트 서사, 조회 기간 컨트롤 & 기본 윈도우, 품목 셀렉터 & 진입 동작, 다운샘플·에러 정직성 표현

---

## 이벤트 마커 & 차트 서사

### Q1 — eventType 4종 시각 구분

| Option | Description | Selected |
|--------|-------------|----------|
| 4색 + 범례 | eventType마다 고유 색 ReferenceLine + 범례. 한눈 구분·면접 서사 명확 | ✓ |
| 단색 + 아이콘/라벨 구분 | 마커 한 색, lucide 아이콘/라벨로 구분. 색맹 안전하나 한눈 구분 약함 | |
| You decide | 색/아이콘 구분을 UI-SPEC/planner 재량 | |

**User's choice:** 4색 + 범례 (추천)
**Notes:** 의미→색 매핑만 잠금, 정확한 hex·범례 위치는 09-UI-SPEC 소관.

### Q2 — 마커 정보(title) 노출

| Option | Description | Selected |
|--------|-------------|----------|
| 호버 툴팁 | 마커 호버 시 title 표시(ROADMAP 완료조건 #4). 차트 깔끔, 정보 온디맨드 | ✓ |
| 상시 라벨 | title 항상 표시. 마커 적을 땐 명확하나 많으면 겹침 | |
| 호버 툴팁 + 상시 축약 라벨 | 약자/점 상시 + title 호버. 정보량↑·구현 복잡도↑ | |

**User's choice:** 호버 툴팁 (추천)
**Notes:** Recharts 차트당 Tooltip 1개 — ReferenceLine 호버와 가격선 Tooltip 공존 방식은 research/구현 영역.

---

## 조회 기간 컨트롤 & 기본 윈도우

### Q1 — 진입 시 기본 조회 범위

| Option | Description | Selected |
|--------|-------------|----------|
| 최근 30일 | seed 8일 윈도우 항상 포함, 마커 2개 다 보임, 빈 화면 방지 | ✓ |
| 최근 7일 | 더 촘촘하나 seed 8일 일부 잘려 누락 위험 | |
| 최근 90일 | 다운샘플 배지를 데모에서 확실히 보여줌, 단 raw 점 성김 | |

**User's choice:** 최근 30일 (추천)
**Notes:** URL에 from/to 없을 때의 기본값. 다운샘플 트리거는 백엔드 임계값에 따름.

### Q2 — 기간(from/to) 컨트롤 형태

| Option | Description | Selected |
|--------|-------------|----------|
| 프리셋 버튼 + 날짜 입력 | 7/30/90일 프리셋 + from/to 날짜 입력(커스텀·400 검증 도달). 둘 다 URL searchParams | ✓ |
| 프리셋 버튼만 | 간결하나 to≤from(400)을 화면에서 못 만들어 검증은 URL 의존 | |
| from/to 날짜 입력만 | 유연하나 데모 클릭 흐름 한 단계 더 | |

**User's choice:** 프리셋 버튼 + 날짜 입력 (추천)
**Notes:** 필터 상태→URL searchParams는 Phase 7 D-04 잠금 결정의 실현.

---

## 품목 셀렉터 & 진입 동작

### Q1 — 셀렉터 UI 형태

| Option | Description | Selected |
|--------|-------------|----------|
| 드롭다운 select | 컴팩트, 차트·카드에 공간. 단일 선택 집중 화면에 적합 | ✓ |
| Dashboard처럼 카드/목록 | 시각적이나 단일집중과 충돌·공간 차지 | |
| 검색 가능 콤보박스 | 품목 많을 때 유리하나 seed 소수엔 과잉 | |

**User's choice:** 드롭다운 select (추천)
**Notes:** seed 품목 소수.

### Q2 — 진입 시 기본 선택 동작

| Option | Description | Selected |
|--------|-------------|----------|
| 첫 품목 자동 선택 | URL item 없으면 첫 품목 자동 → 진입 즉시 비어있지 않음. 선택은 URL 기록 | ✓ |
| 선택 프롬프트 표시 | "품목을 선택하세요" 후 대기. 명시적이나 데모 첫 화면 비어 보임 | |

**User's choice:** 첫 품목 자동 선택 (추천)
**Notes:** URL `?item=`이 있으면 그것 우선.

### Q3 — 셀렉터·최신가 카드 재사용 계약 (Phase 10 공유)

| Option | Description | Selected |
|--------|-------------|----------|
| 공용 위치에 추출 | 공용 폴더에 두고 Phase 9·10 동일 import. 크로스페이즈 중복 없음. 최신가 카드는 useLatestPrice 재사용 + 타임라인용 경량 신규 | ✓ |
| timeline에 두고 Phase 10이 import | 지금은 단순하나 계층 의존 역향(impact→timeline) | |
| Dashboard ItemCard 그대로 재사용 | ItemCard는 그리드용 통합 카드라 단일 선택 화면과 형태 안 맞을 수 있음 | |

**User's choice:** 공용 위치에 추출 (추천)
**Notes:** 정확한 경로·명명은 planner 재량.

---

## 다운샘플·에러 정직성 표현

### Q1 — raw vs 다운샘플 시각 구분

| Option | Description | Selected |
|--------|-------------|----------|
| 배지 + 선 스타일 구분 | "버킷 평균" 배지 + raw=개별 점, downsampled=점 없는 평균선. 시각으로도 정직 전달 | ✓ |
| 배지만 + 동일 선 | 배지만, 선 동일. 단순하나 시각 신호가 배지 하나에 의존 | |
| You decide | 선 스타일·점 세부는 UI-SPEC/planner, 배지 노출만 필수 | |

**User's choice:** 배지 + 선 스타일 구분 (추천)
**Notes:** 선 스타일·점 표시 세부는 09-UI-SPEC/planner가 다듬되 배지+raw/평균 구분 의도는 잠금.

### Q2 — 400 / 404 / 200-empty 분기 수준

| Option | Description | Selected |
|--------|-------------|----------|
| 셋 각각 구분 안내 | 400=기간 입력 안내, 404=없는 품목, 200-empty=데이터 없음 각각 구분 카피. ROADMAP #5 명시·honest-data | ✓ |
| empty만 분리, 400/404 통합 | 빈 기간 EmptyState, 에러는 일반 ErrorState 하나 | |
| 일반 에러 하나 | 모든 비정상을 ErrorState 하나로. ROADMAP 요구·정직성과 충돌 | |

**User's choice:** 셋 각각 구분 안내 (추천)
**Notes:** ApiError가 status 보유 → AsyncBoundary + status 분기로 구현.

---

## Claude's Discretion

- 마커 호버 툴팁 ↔ 가격선 Tooltip Recharts 공존 구현, 마커 밀집/겹침 처리
- y축 골드 단위 포맷, x축 KST 라벨 밀도/포맷, 차트 높이·반응형
- 프리셋 정확 라벨/개수, 날짜 입력 위젯 형태(native vs 커스텀)
- 공용 컴포넌트 폴더 경로·명명, 최신가 카드 내부 필드 배열
- raw 점 표시 정밀 임계 등 선 스타일 세부

## Deferred Ideas

- 이벤트별 전후 변화율·insufficient_data·"상관≠인과" 고지 — Phase 10
- 배치 latest 엔드포인트 — v2 (Phase 8 Deferred 유지)
- 실시간 자동 갱신(폴링/SSE) — FE-V2-02
- 차트 줌/팬·브러시 범위 선택 — 잠재 v2 후보
- 마커 클릭→Event Impact 딥링크 — Phase 10 도입 후 자연 연결
