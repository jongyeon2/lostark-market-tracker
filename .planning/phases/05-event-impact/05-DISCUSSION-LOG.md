# Phase 5: Event Impact (게이트 조건부) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-24
**Phase:** 5-Event Impact (게이트 조건부)
**Areas discussed:** v1 지표 정의, 충분성·staleness 가드, 이벤트 스코프·응답 형태, window 파라미터 계약

---

## v1 지표 정의

설계 §79는 앵커 단일 스냅샷 공식으로 v1을 "확정"으로 잠갔으나, ROADMAP 성공기준 1의 응답 필드명(`pre_avg/post_avg`)은 윈도우 평균을 암시 — 충돌을 사용자가 adjudicate.

| Option | Description | Selected |
|--------|-------------|----------|
| 앵커 단일 스냅샷 (설계 §79) | pre = 직전 마지막 스냅샷 min_price, post = 직후 첫 스냅샷 min_price, change_rate = post/pre−1. staleness가 "앵커↔이벤트 간격"으로 깔끔히 정의. ROADMAP pre_avg/post_avg는 느슨한 표현으로 간주 | ✓ |
| 윈도우 평균 (pre_avg/post_avg) | 구간 스냅샷 min_price 평균 비율. ROADMAP 필드명 일치, 단일 점 노이즈에 덜 민감. 단 "앵커 시각"·staleness 의미 모호 | |
| 하이브리드 (앵커+평균 둘 다) | 앵커로 change_rate 계산 + 평균도 함께 노출. 계산·응답 복잡 | |

**User's choice:** 앵커 단일 스냅샷 (설계 §79)
**Notes:** 설계 확정 레이어 우선 + staleness 의미 명확이 결정 근거. → CONTEXT D-01.

---

## 충분성·staleness 가드

설계는 충분성 "앞뒤≥1"(T7)을 잠갔으나 staleness "허용치"의 값을 미정으로 둠.

### staleness 허용치 기준

| Option | Description | Selected |
|--------|-------------|----------|
| 고정 절대값 (기본 30분) | 앵커가 이벤트로부터 30분(≈ 수집주기 10분×3틱) 이내여야 유효. 단순·설명 명확, @ConfigurationProperties 외부화(CFG-V2-01) 후보 | ✓ |
| 윈도우 Nh 비례 (예: ±Nh의 10%) | window 클수록 허용치 커짐. 단 큰 윈도우서 stale 앵커를 관대하게 봄 | |
| 하이브리드 (min(고정, 비례)) | 작은 윈도우엔 비례, 큰 윈도우엔 고정 지배. 정확하나 설명·테스트 복잡 | |

**User's choice:** 고정 절대값 (기본 30분)
**Notes:** → CONTEXT D-03.

### pre/post 앵커 적용 규칙

| Option | Description | Selected |
|--------|-------------|----------|
| 양쪽 모두 신선 필수 | change_rate = post/pre는 두 앵커의 비율 — 한쪽만 stale해도 비율 왜곡. 하나라도 없거나 >30분이면 insufficient_data + 발견 앵커 시각 반환 | ✓ |
| 한쪽만 신선해도 계산 | 신선한 쪽 기준으로라도 내보냄. 단 비율 한쪽이 오래돼 수치 오해 — 상관 신뢰도↓ | |

**User's choice:** 양쪽 모두 신선 필수
**Notes:** 지표 무결성 우선. → CONTEXT D-04, D-05.

---

## 이벤트 스코프·응답 형태

`game_event`는 글로벌(품목별 아님) → 엔드포인트는 모든 이벤트가 이 품목 가격에 준 영향을 계산. 어떤 이벤트를 평가 대상으로 삼을지가 열린 결정.

| Option | Description | Selected |
|--------|-------------|----------|
| 전체 이벤트 평가 | 모든 game_event를 per-event 결과로, 데이터 없는 건 insufficient_data. 단순·데모 친화, 가드 작동을 포트폴리오에서 시연. occurred_at desc | ✓ |
| 스냅샷 범위 겹치는 것만 | 이 품목 스냅샷 이력 범위와 겹치는 이벤트만 포함. 노이즈↓ 하지만 "왜 이 이벤트가 안 나오지?" 모호 | |
| 파라미터로 좁힘 (from/to·event id) | 호출자가 선택. 유연하나 엔드포인트 계약(window=Nh만) 확장 = 스코프 크리프 위험 (v2 후보) | |

**User's choice:** 전체 이벤트 평가
**Notes:** insufficient 항목을 status와 함께 포함해 가드 작동을 시연. → CONTEXT D-06, D-07.

---

## window 파라미터 계약

`window≤0→400`은 Phase 3 D-13 검증 계약 재사용 확정. 단위·기본값·상한이 열린 결정.

| Option | Description | Selected |
|--------|-------------|----------|
| 필수 정수시간 + 상한 캡 | window 필수 정수 시간(h), 생략 시 400. 상한 캡(예 ≤168h)으로 배치 범위 폭주 방지. Phase 3(from/to 필수)와 일관된 명시적 계약 | ✓ |
| 기본값 24h + 상한 캡 | 정수 시간, 생략 시 기본 24h. README curl 데모 마찰↓. 단 '기본값' 숨은 동작 추가 | |
| 실수 허용 (예: 1.5h) | 소수점 시간. 정밀하나 수집 10분 주기라 과함, 파싱·검증 복잡 (v2) | |

**User's choice:** 필수 정수시간 + 상한 캡
**Notes:** 명시적 계약 우선 — 리뷰어가 노브를 분명히 봄. → CONTEXT D-08.

---

## Claude's Discretion

- `change_rate` 표현(부호 %, 소수 자리, 반올림)·응답 정확 필드명/JSON 키·`status` 문자열
- `window` 상한 캡 정확값·캡 초과 시 400 메시지
- 패키지/레이어 위치(`EventImpactService`/`EventImpactController` 신설 vs `PricesController` 확장)
- N+1 배치 구현 형태(단일 범위 1쿼리 vs 청크, 4A 메서드 재사용 vs 신규 배치 메서드) — "2N 아님"은 검증 대상(설계 T7)
- 앵커 동률 처리(이벤트 `occurred_at` == 스냅샷 `collected_at`, 간격 0)

## Deferred Ideas

- 윈도우 평균(pre_avg/post_avg) 지표 — v1은 앵커 단일 스냅샷
- event-impact median / 스무딩 — v2 (IMPACT-V2-02)
- 카테고리 베이스라인 대비 초과 상승률 — v2 (IMPACT-V2-01)
- staleness 허용치·충분성 임계 `@ConfigurationProperties` 외부화 — v2 (CFG-V2-01)
- `window` 실수(소수 시간) 허용 — v2
- 스코프 좁힘 파라미터(`from/to`·`event id`) — v2
- 기간형 이벤트(start/end) 모델 — v2 (EVT-V2-01)
