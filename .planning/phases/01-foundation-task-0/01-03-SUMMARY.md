---
phase: 01-foundation-task-0
plan: 03
subsystem: api
tags: [lostark-api, restclient, spike, rate-limit, jwt, data-model-lock]

# Dependency graph
requires:
  - phase: 01-01
    provides: Spring Boot 스켈레톤, RestClient 의존성, 스파이크 프로파일 배선, .env.example
  - phase: 01-02
    provides: 스파이크가 현실 대비 확인하는 잠긴 4테이블 스키마
provides:
  - 실제 markets API의 Task 0 검증 (필드, 매칭 규칙, 레이트리밋)
  - 비준된 데이터 모델 잠금 (D-05 매칭 규칙, D-06 avg_price/trade_count 처리)
  - LostarkSpikeClient (RestClient) + @Disabled 스파이크 테스트 + 스파이크 프로파일
  - TASK0-FINDINGS.md (필드 매트릭스, 종료 게이트 판정) + 설계 문서 잠금
affects: [02-collection-pipeline, 03-read-api-cache, 05-event-impact]

# Tech tracking
tech-stack:
  added: []
  patterns: [restclient-spike, env-sourced-jwt-with-whitespace-hardening, disabled-manual-spike]

key-files:
  created:
    - src/main/java/com/lostark/tracker/spike/LostarkSpikeClient.java
    - src/test/java/com/lostark/tracker/spike/MarketsApiSpikeTest.java
    - src/test/resources/application-spike.yml
    - .planning/phases/01-foundation-task-0/TASK0-FINDINGS.md
  modified:
    - docs/design/yeonjong-unknown-design-20260619-221517.md

key-decisions:
  - "D-06: avg_price + trade_count는 제공되나 일단위(상세 Stats[]); 목록은 YDayAvgPrice만 제공"
  - "모델 잠금(비준): min_price 유지; avg_price를 V2로 추가(YDayAvgPrice); trade_count는 per-tick 제외(v2 daily-stats)"
  - "D-05/DATA-03: 안정적 API Id -> external_item_id + display_name; 퍼지 조합 없음"
  - "레이트리밋 100/min 확인; x-ratelimit-* 헤더 존재"

patterns-established:
  - "스파이크 = RestClient 클라이언트 + 스파이크 프로파일 하의 @Disabled @SpringBootTest; CI에서 절대 실행 안 함"
  - "JWT 키: env 전용; 클라이언트가 선행 bearer 접두 AND 모든 공백 제거(붙여넣기 래핑 방어)"

requirements-completed: [DATA-03]

# Metrics
duration: ~스파이크 (다단계, 401 근본원인 분석 포함)
completed: 2026-06-20
---

# Phase 01 / Plan 03: Task 0 API 검증 + 모델 잠금 요약

**실제 markets API를 한 번 검증: avg_price/trade_count는 일단위 전용(상세 Stats[]), 안정적 Id가 매칭 규칙을 확정, 레이트리밋은 100/min — 모델 잠금 비준, 종료 게이트 PASS.**

## 성능

- **완료:** 2026-06-20
- **태스크:** 3개 (자동 2 + 블로킹 사람 체크포인트 1, 비준됨)
- **파일:** 생성 3, 수정 1

## 주요 성과
- `LostarkSpikeClient`(RestClient, D-03)로 실제 `markets/items`(목록) + `markets/items/{id}`(상세)를 한 번 호출
- **D-06 해소:** `avg_price`(목록의 `YDayAvgPrice`; 상세의 일단위 `Stats[].AvgPrice`)와 `trade_count`(`Stats[].TradeCount`, 일단위, 상세 전용) — 둘 다 가용, 둘 다 일단위; 목록에 인트라데이 거래량 없음
- **D-05/DATA-03 해소:** 안정적 정수 `Id` → `external_item_id` + `display_name`; 퍼지 조합 불필요
- **레이트리밋 확인:** 키당 100/min; `x-ratelimit-{limit,remaining,reset}` 헤더 반환
- **종료 게이트 판정: PASS**; 블로킹 체크포인트에서 모델 잠금 비준
- `TASK0-FINDINGS.md` 작성; 설계 문서 Data Model Decisions / Task 0 Exit Criteria를 LOCKED 표시

## 태스크 커밋

1. **Task 1: 스파이크 클라이언트 + @Disabled 스파이크 테스트(스파이크 프로파일)** — `8b9b2ba` (feat)
2. **Task 2: 스파이크 실행, 발견 사항 기록, 모델 + 설계 문서 잠금** — `5a7bfbb` (feat)
3. **Task 3: 모델 잠금 비준(블로킹 사람 체크포인트)** — "권장안대로 잠금" 승인 (코드 커밋 없음)

## 생성/수정 파일
- `spike/LostarkSpikeClient.java` — markets/items + 상세 RestClient 호출; 키 정규화
- `spike/MarketsApiSpikeTest.java` — @Disabled 스파이크; 검색 + 상세 Stats 캡처
- `application-spike.yml` — base-url + ${LOSTARK_API_KEY}
- `TASK0-FINDINGS.md` — 필드 매트릭스, 매칭 규칙, 레이트리밋 현실, 종료 게이트 판정, V2 계획
- 설계 문서 — LOCKED Task 0 결과

## 결정 사항(비준됨)
- `price_snapshot.min_price` 유지 (= `CurrentMinPrice`).
- `avg_price`를 **`V2__add_price_metrics.sql`**로 추가 (= `YDayAvgPrice`, 목록 호출에 무료 동반) — Phase 2.
- `trade_count`는 per-tick `price_snapshot`에서 제외(일단위 전용; per-tick fetch는 레이트 비용 2배) → 선택적 v2 daily-stats 테이블.
- 매칭: `external_item_id` = API `Id`, `display_name` = `Name`.

## 계획 대비 이탈

### 자동 수정 이슈

**1. [블로킹] 붙여넣기로 손상된 JWT의 401**
- **발견 시점:** Task 2 — 키를 재발급해도 모든 호출이 `401 Authorization has been denied` 반환.
- **근본 원인(로깅 TCP 프록시 + 문자 단위 검사로 증명):** `.env`에 붙여넣은 키가 ~160자 래핑 지점에 리터럴 공백 3개가 주입되어 JWT 서명 손상.
- **수정:** `LostarkSpikeClient`가 선행 `bearer ` 접두와 **모든 공백**을 키에서 제거.
- **검증:** 정리 후 모든 엔드포인트 200 반환; 실제 클라이언트 경유 스파이크 테스트 그린.
- **커밋:** `5a7bfbb`

**2. [조정] 스파이크 쿼리에 리프 CategoryCode 필요**
- 부모 코드 `50000`이 0개 반환; 스파이크를 리프 `50010`(재련 재료)로 전환하고 일단위 `Stats[]` 캡처를 위해 `getItemDetail()` 추가.
- **커밋:** `5a7bfbb`

---
**총 이탈:** 2건(둘 다 실제 응답 확보에 필요). 스코프 확장 없음.

## 마주친 이슈
- 초기 401들이 추가 진단을 소모; 해결됨(상기). 01-01의 로컬 Docker/Testcontainers API 버전 이슈(api.version=1.44)는 계속 고정 유지.

## 사용자 셋업 필요
- `.env`(gitignore)의 `LOSTARK_API_KEY` — 스파이크 로컬 재실행에만 필요. CI나 일반 테스트 스위트에는 불필요(스파이크는 `@Disabled`).

## 다음 페이즈 준비도
- **Phase 1 완료.** 데이터 모델이 측정된 현실 위에 잠김. **Phase 2(Collection Pipeline)** 준비 완료.
- Phase 2 이월: `avg_price` 추가용 `V2__add_price_metrics.sql`; per-tick `CurrentMinPrice`→`min_price` 수집; `x-ratelimit-*` + `Retry-After`로 토큰버킷이 100/min 준수.

---
*Phase: 01-foundation-task-0*
*완료: 2026-06-20*
