# Phase 10: Event Impact - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-26
**Phase:** 10-event-impact
**Areas discussed:** window 컨트롤 & 기본값, 결과 표시 & 변화율 포맷, insufficient 이유 구분, 상관≠인과 고지

---

## window 컨트롤 & 기본값

### Q1. window(시간) 입력 컨트롤 형태

| Option | Description | Selected |
|--------|-------------|----------|
| 프리셋 + 숫자 입력 | 프리셋 버튼(예: 6/24/72h) 데모 클릭 한 번 + 직접 입력으로 커스텀·범위밖(400) 도달. Phase 9 RangeControls와 동일 패턴 | ✓ |
| 프리셋 버튼만 | 6/24/72/168h 고정 프리셋만. 가장 간단하나 임의 window·범위밖 400 시연 어려움 | |
| 숫자 입력만 | 숫자 입력 필드 하나. 유연·400 도달 쉬우나 데모 클릭 원터치 약함 | |

### Q2. 기본 window 값

| Option | Description | Selected |
|--------|-------------|----------|
| 24h | ROADMAP 완료조건 예시값. 수집 10분 주기 기준 이벤트 전후 하루 충분 | ✓ |
| 48h | 더 넓은 전후 창. 희소 이벤트도 ok 가능성↑, 단 먼 앵커 stale 위험↑ | |
| 12h | 더 타이트한 창. insufficient_data가 더 잘 드러남(가드 시연 유리) | |

### Q3. window 범위밖(≤0 또는 >168) → 400 UI 처리

| Option | Description | Selected |
|--------|-------------|----------|
| 범위밖 허용 → 400 시연 | 0·169 등 범위밖 값 전송 허용해 400 안내 UI 노출. ROADMAP 검증·Phase 9 D-09 정직성 충족 | ✓ |
| 클라이언트 클램프 | 1..168로 입력 강제 제한해 400 안 띄움. 간편하나 400 처리 UI 검증 불가 | |

**User's choice:** 프리셋 + 숫자 입력 / 24h / 범위밖 허용 → 400 시연
**Notes:** Phase 9 RangeControls·D-09 패턴을 그대로 연장. window는 URL `?window=`에 인코딩(useImpactParams).

---

## 결과 표시 & 변화율 포맷

### Q1. 이벤트별 결과 표시 형태

| Option | Description | Selected |
|--------|-------------|----------|
| 반응형(표↔카드) | 넓은 화면=표, 좁은 화면=카드 리스트. ROADMAP "표로" + Phase 11 반응형 마감 충족 | ✓ |
| 표(table) 고정 | 항상 표. 컬럼 정렬 명확·구현 단순, 좁은 화면 가로 스크롤/압축 필요 | |
| 카드 리스트 고정 | 항상 카드. insufficient 설명에 유리, 넓은 화면 비교성↓ | |

### Q2. changeRate 상승·하락 색 방향

| Option | Description | Selected |
|--------|-------------|----------|
| 한국 관례(상승=빨강) | 상승=빨강/하락=파랑. 국내 거래소·주식 MTS 관례 — 로아 유저·국내 면접관에게 자연스러움 | ✓ |
| 서구 관례(상승=초록) | 상승=초록/하락=빨강. 글로벌/서구 금융 UI 관례 | |
| 색 없이 부호만 | +/- 부호·화살표만, 색 중립. 색각 접근성 안전하나 시각 서사 약함 | |

### Q3. changeRate 숫자 포맷

| Option | Description | Selected |
|--------|-------------|----------|
| 부호+% 소수 1자리 | 예: +12.3% / -4.0%. 부호 명시 + 읽히는 정밀도 | ✓ |
| 부호+% 정수 | 예: +12% / -4%. 더 간결, 작은 변화 해상도↓ | |
| Claude 재량 | 소수 자릿수·반올림을 UI-SPEC·데이터 분포 보고 결정 | |

**User's choice:** 반응형(표↔카드) / 한국 관례(상승=빨강) / 부호+% 소수 1자리
**Notes:** 의미→색 방향만 잠금(정확 hex는 10-UI-SPEC). prePrice/postPrice는 G 단위 천단위 구분(LatestPriceCard 관례).

---

## insufficient 이유 구분

### Q1. ok vs insufficient_data 구분 방식

| Option | Description | Selected |
|--------|-------------|----------|
| 배지 + 이유 카피 | ok/insufficient 상태 배지(색 구분) + insufficient 행에 이유 문구. eventType 배지는 Phase 9 4색 토큰 재사용 | ✓ |
| 배지만(이유 생략) | 상태 배지만, 세부 이유 없음. 간결하나 IMPCT-03 "왜" 미충족 위험 | |

### Q2. 희소 vs stale 이유 깊이

| Option | Description | Selected |
|--------|-------------|----------|
| 앵커 시각(KST) 노출 | preAnchorAt/postAnchorAt를 KST로 표시(없으면 "없음"). 희소=앵커 없음, stale=앵커 시각이 멀다는 게 자명. 30분 상수 하드코딩 없음 — 가장 정직 | ✓ |
| 이유 문구만 | "윈도우 내 데이터 없음(희소)" / "앵커가 너무 오래됨(stale)" 문구만, 시각 숫자 미노출 | |
| 시각 + 경과분 | 앵커 시각 + "이벤트로부터 N분" 계산. 가장 친절하나 30분 임계를 프론트가 하드코딩 → 백엔드 결합↑ | |

### Q3. insufficient_data 행 배치

| Option | Description | Selected |
|--------|-------------|----------|
| 같은 목록에 섞어 | occurred_at 내림차순 한 목록에 ok·insufficient 함께(배지 구분). "가드가 작동함" 그대로 시연 — 핵심 학습 신호 | ✓ |
| 별도 섹션 분리 | ok 테이블 / insufficient 테이블 분리. 깔끔하나 occurred_at 단일 정렬 서사 끊김 | |

**User's choice:** 배지 + 이유 카피 / 앵커 시각(KST) 노출 / 같은 목록에 섞어
**Notes:** 백엔드 의미(Phase 5 D-02~05) — anchor null=희소, 둘 다 non-null인데 insufficient=stale. 프론트는 30분 임계 하드코딩 안 함.

---

## 상관≠인과 고지

### Q1. "상관 ≠ 인과" 고지 배치

| Option | Description | Selected |
|--------|-------------|----------|
| 상단 상시 배너(Alert) | 화면 상단 Alert 상시 노출. 표를 보기 전 맥락 먼저. shadcn alert.tsx 재사용 | ✓ |
| 표 캡션/제목 근처 | 결과 표 바로 위·아래 캡션. 컴팩트하나 상단 시선보다 약하게 노출될 수 있음 | |
| 행별 각주 | 각 changeRate 근처 툴팁/각주. 맥락 밀착하나 전체 고지가 한눈에 안 들어올 수 있음 | |

### Q2. 고지 닫기 가능 여부

| Option | Description | Selected |
|--------|-------------|----------|
| 상시 노출(비닫힘) | 닫기 불가, 항상 보임. 과대해석 차단이 목적이라 숨기지 않는 게 정직 | ✓ |
| 닫기 가능 | X로 닫을 수 있게. 덜 거슬리나 핵심 고지가 사라질 수 있음 | |

### Q3. 고지 카피 톤

| Option | Description | Selected |
|--------|-------------|----------|
| 한 문장 간결 | 예: "이 수치는 이벤트와 가격의 시점 상관일 뿐, 인과를 의미하지 않습니다." 명확·간결 | ✓ |
| 한 문장 + 부연 | 고지 + 의도 설명(다른 요인·계절성도 작용). 더 교육적이나 길어짐 | |

**User's choice:** 상단 상시 배너(Alert) / 상시 노출(비닫힘) / 한 문장 간결
**Notes:** 의미·배치·상시성만 잠금. 정확 카피·아이콘·variant는 10-UI-SPEC 계약.

---

## 최종 확인 (엣지 케이스 — 합리적 기본값 잠금)

마무리 단계에서 두 후보를 논의 없이 합리적 기본값으로 잠그기로 선택("CONTEXT로 잠그"):

| 케이스 | 잠근 기본값 |
|--------|------------|
| 이벤트 0개(200 + 빈 events 배열) | Phase 7 `EmptyState`로 "등록된 이벤트 없음"(per-row insufficient와 별개) — CONTEXT D-11 |
| `LatestPriceCard` 표시 여부 | 타임라인과 일관되게 impact 화면에도 표시(공용 컴포넌트 재사용·자기 AsyncBoundary) — CONTEXT D-12 |

## Claude's Discretion

- 정확 프리셋 라벨/개수(6/24/72h는 출발점), 숫자 입력 위젯 형태, window 입력 검증 UX(입력 즉시 vs 제출 버튼).
- 반응형 브레이크포인트·표 컬럼 우선순위/생략 순서, 카드 레이아웃 필드 배열.
- changeRate 정확 반올림·소수 자릿수·0%/null 표시 방식.
- impact feature 폴더 하위 분할, `useImpactParams` 시그니처, 결과 표/카드 컴포넌트 분리 단위.
- 상관≠인과 Alert 아이콘·variant·정확 카피(10-UI-SPEC 계약 따름).

## Deferred Ideas

- 카테고리 베이스라인 대비 초과상승률 시각화 — FE-V2-05(백엔드 IMPACT-V2-01 선행).
- window·staleness 임계 `@ConfigurationProperties` 외부화 — 백엔드 CFG-V2-01(프론트는 30분 하드코딩 안 함으로 디커플).
- 스코프 좁힘 파라미터(from/to·event id 필터) — 백엔드 v2.
- 마커 클릭 → Event Impact 딥링크 — Phase 9 deferred, 본 phase 범위 밖.
- 실시간 자동 갱신(폴링/SSE) — FE-V2-02.
