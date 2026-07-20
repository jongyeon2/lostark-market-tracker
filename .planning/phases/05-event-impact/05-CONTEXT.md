# Phase 5: Event Impact (게이트 조건부) - Context

**Gathered:** 2026-06-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1/2가 쌓은 `price_snapshot`와 Phase 4가 관리하는 `game_event` 위에 **헤드라인 기능인 이벤트 상관(event-impact) 계층**을 올린다 — `GET /api/items/{id}/event-impact?window=Nh`가 각 game_event의 ±N시간 전후 가격 변화율을 **데이터 충분성·staleness 가드**와 함께 계산해 **이벤트별 목록**으로 반환한다. (IMPACT-01, IMPACT-02)

이 페이즈는 "리뷰어가 curl로 한 품목에 대해 등록된 이벤트들의 전후 변화율과, 데이터가 희소/오래된 경우 `insufficient_data`(발견 앵커 시각 포함)를 읽을 수 있다"까지 책임진다. **백엔드 전용 — 프론트엔드 UI 명시적 제외.** **상관 ≠ 인과** — 응답·문구는 "시점 상관"까지만, change_rate를 인과로 과대 서술하지 않는다(PROJECT 전제 5). 시드 데이터(Phase 6 DIST-03)·README 데모 표면은 후속.

**게이트:** 설계상 2주차 말 하드 게이트(수집·조회·시드 데모가 단단한가) 통과 조건부 — 슬립 시 v2 강등. Phase 1~4가 완료(verified)되어 게이트가 사실상 통과된 상태에서 진행한다.

**선행 페이즈에서 잠긴 것(planner 변경 금지):**
- **4A 공유 윈도우 쿼리** `read/WindowQueryService.fetchWindow(itemId, from, to)` 이미 존재(인클루시브 UTC 경계, 두 쿼리: 스냅샷 asc + 겹치는 이벤트) → **그대로 재사용/배치 확장**
- **`min_price`만 존재** (`avg_price`·`trade_count`는 Task 0에서 per-tick 제외) → 변화율은 `min_price` 기준
- **`game_event` = 단일 `occurred_at`(UTC), `event_type` 4종** (기간형 start/end는 v2 — 단일 시점이어야 ±Nh 윈도우 계산이 정의됨)
- 관리자 이벤트 PUT의 `occurred_at` 변경(04 D-06)이 **앵커 시각에 직접 반영**됨
- 모든 시간 `TIMESTAMPTZ`(UTC) + 응답 UTC ISO-8601(off-by-9h 차단, 03 D-11)
- 에러 계약 `{timestamp,status,error,message}` + `@RestControllerAdvice`; `window≤0→400`, 없는 item→404, 유효하나 빈 데이터→200 (03 D-10/D-13 재사용)
- 단일 인스턴스, 범위 쿼리는 **캐시 안 함**(latest만 캐시) → DB 직조회/배치

</domain>

<decisions>
## Implementation Decisions

### v1 변화율 지표 (IMPACT-01 · 설계 §79 / 확정 8결정 중 5A·8A)
- **D-01:** **앵커 단일 스냅샷 델타.** `change_rate = post / pre − 1` 에서 **pre = 이벤트 `occurred_at` 직전(≤ occurred_at) 윈도우 내 가장 가까운(마지막) 스냅샷 `min_price`**, **post = 직후(≥ occurred_at) 윈도우 내 가장 가까운(첫) 스냅샷 `min_price`**. 설계 §79 문자 그대로 — **윈도우 평균(pre_avg/post_avg)이 아니라 경계 앵커 단일 스냅샷**. `min_price` 기준(Task 0에서 avg_price/trade_count per-tick 제외). ROADMAP 성공기준의 `pre_avg/post_avg` 필드명은 느슨한 표현으로 간주 — 실제 값은 앵커 스냅샷 가격이며 평균이 아님. `change_rate` 표현(부호 %, 소수 자리, 반올림)·정확 응답 필드명은 Claude 재량.

### 충분성 + staleness 가드 (IMPACT-02 · 5A)
- **D-02:** **충분성 = 윈도우 ±Nh 안에 앞·뒤 각 ≥1 스냅샷**(설계 T7 "앞뒤≥1"). 한쪽이라도 0개면 `insufficient_data`, 그쪽 앵커 시각 = `null`. 앵커 = 윈도우 후보 중 이벤트에 **가장 가까운** 스냅샷(직전 마지막 / 직후 첫).
- **D-03:** **staleness = 고정 절대 허용치, 기본 30분**(≈ 수집주기 10분 × 3틱). 앵커↔이벤트 `occurred_at` 간격이 30분 초과면 stale. 건강한 파이프라인은 이벤트 ±10분에 스냅샷이 있으므로 "2틱 연속 구멍까지 허용"으로 설명 가능. `@ConfigurationProperties` 외부화(CFG-V2-01)는 v2 — MVP는 상수.
- **D-04:** **pre/post 양쪽 앵커 모두 신선(≤30분)해야 `change_rate` 계산.** 둘 중 하나라도 없거나(D-02) stale(D-03)이면 `insufficient_data`. `change_rate`는 두 앵커의 비율이라 한쪽만 stale해도 수치가 왜곡되므로 양쪽 모두 신선을 요구한다.
- **D-05:** **`insufficient_data` 응답엔 항상 발견한 앵커 시각을 포함**(pre/post 각각, 없으면 `null`). 호출자가 "윈도우 내 데이터 없음(앵커 null)"인지 "앵커는 있으나 stale(앵커 시각 보고)"인지 이유를 구분(ROADMAP 성공기준 2·3, 설계 failure-mode 표).

### 이벤트 스코프 + 응답 형태 (IMPACT-01..02)
- **D-06:** **전체 `game_event` 평가.** `game_event`는 글로벌(품목별 아님) → 이 엔드포인트는 **모든 이벤트가 이 품목 가격에 준 영향**을 per-event로 계산. `insufficient_data` 항목도 status와 함께 목록에 포함 → "충분성/staleness 가드가 작동함"을 포트폴리오에서 시연(가드가 핵심 학습 신호). `occurred_at` desc 정렬. 스코프 좁힘 파라미터(`from/to`·`event id`)는 v2 — 엔드포인트 계약은 `window=Nh`만 유지(스코프 크리프 방지).
- **D-07:** **응답 = per-event 목록.** 각 항목 = 이벤트 메타(`id`, `event_type`, `title`, `occurred_at`) + `status`(`"ok"` | `"insufficient_data"`) + (ok일 때) pre/post 앵커 가격·앵커 시각·`change_rate`, (insufficient일 때) 발견 앵커 시각(없으면 `null`). 없는 item → 404, 이벤트 0개 → 200 빈 `events` 배열(03 D-13 관례). 톱레벨 래퍼(`itemId`·`window` 에코)·정확 필드명·JSON 키 네이밍·status 표현은 Claude 재량.

### window 파라미터 계약 (IMPACT-01 · API-05 검증 재사용)
- **D-08:** **`window` = 필수 정수 시간(h), 생략 시 400.** 상한 캡(예: ≤168h = 7일)으로 배치 조회 범위 폭주 방지. `window≤0 → 400`은 Phase 3 D-13 검증 계약·기존 `ApiExceptionHandler`/`InvalidRequestException` 재사용. 실수(소수 시간) 허용은 v2(수집 10분 주기라 시간 단위로 충분). 정확 캡 값·캡 초과 시 400 메시지 문구는 Claude 재량.

### N+1 배치 (8A · 설계 T7 — planner 구현 영역)
- **D-09:** **이벤트 N개에 쿼리 2N 금지.** 4A `WindowQueryService.fetchWindow`를 per-event로 호출하면 2N 쿼리가 되므로, **`[min(occurred_at)−Nh, max(occurred_at)+Nh]` 범위 스냅샷을 1회(또는 소수)로 조회한 뒤 메모리에서 각 이벤트 앵커를 계산**하는 배치 전략. 정확 구현(단일 범위 1쿼리 vs 청크, 4A 메서드 확장 vs 신규 배치 메서드)은 planner/연구 재량 — 단 "이벤트 N개에 쿼리 2N 아님"은 IT 검증 대상(설계 T7).

### Claude's Discretion
- `change_rate` 표현(부호 %, 소수 자리, 반올림)·응답 정확 필드명/JSON 키·`status` 문자열
- `window` 상한 캡 정확값·캡 초과 시 400 메시지
- 패키지/레이어 위치(`read` 패키지 `EventImpactService` + `web` `EventImpactController` 신설 vs `PricesController` 확장)
- 배치 구현 형태(단일 범위 1쿼리 vs 청크), 4A 메서드 재사용 vs 신규 배치 메서드
- **앵커 동률 처리:** 이벤트 `occurred_at`과 정확히 같은 `collected_at` 스냅샷이 있을 때(간격 0) pre/post 어느 쪽에 귀속할지 — 한쪽 귀속 또는 양쪽 후보(planner 재량, IT로 경계 고정)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 설계 / 리뷰 (필수 — 구현 확정 레이어)
- `docs/specs/2026-06-19-project-design.md` — Phase 5 핵심: **§endpoints `event-impact` v1 지표 공식(§79: "이벤트~이벤트+Nh 첫 스냅샷 min_price / 이벤트−Nh~이벤트 마지막 스냅샷 min_price − 1")**, **확정 8결정 중 5A(event-impact 충분성+staleness)·8A(event-impact N+1 배치)·4A(공유 윈도우 쿼리 재사용)**, **§게이트(수집 신뢰성 1순위, event-impact는 게이트 통과 조건부 헤드라인)**, **§risk(분석 과투자=도금 / 희소·경계 이벤트 / stale 앵커 → `insufficient_data`)**, **§Implementation Tasks T7**(충분성 앞뒤≥1 + staleness 허용치 + `insufficient_data` + 앵커시각 응답 + 범위조회 배치 N+1 제거; Verify: 구멍/희소→insufficient, 이벤트 N개에 쿼리 2N 아님), **§failure-mode 표**(프로세스 다운→시계열 구멍→희소→insufficient_data / event-impact 오래된 앵커→staleness 허용치→insufficient_data).
- `docs/specs/2026-06-20-eng-review-test-plan.md` — 테스트 플랜: **event-impact v1 델타·충분성(앞/뒤 각 1 미만→insufficient_data)·staleness 허용치 밖(구멍)→insufficient_data·앵커시각·window≤0**; **E2E(이벤트 등록[관리자 인증]→타임라인 겹침→event-impact 델타 계산)**.

### 선행 데이터·코드 (필수 — 사실 근거)
- `src/main/java/com/lostark/tracker/read/WindowQueryService.java` — **4A 공유 윈도우 쿼리** `fetchWindow(itemId, from, to)` → `WindowResult{snapshots(asc), events}`(인클루시브 UTC 경계, per-window 2쿼리). Phase 5가 **재사용/배치 확장** — per-event 호출은 2N이므로 D-09 배치 적용.
- `.planning/phases/03-read-api-cache/03-CONTEXT.md` — D-04 타임라인 `{snapshots,events}`, D-05 이벤트 겹침 `from≤occurred_at≤to`, **D-06 4A `WindowQueryService`(Phase 5 재사용 명시)**, D-10 에러 advice, **D-11 UTC ISO-8601(off-by-9h)**, **D-13 입력검증(`window≤0→400`·없는 item→404·빈 범위→200)**.
- `.planning/phases/04-admin-events/04-CONTEXT.md` — **D-06 이벤트 PUT `occurred_at` 변경 → event-impact 앵커에 직접 반영**; `game_event` 단일 `occurred_at`·`event_type` 4종·`created_by` 없음.
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — `min_price`=`CurrentMinPrice`, `avg_price`/`trade_count` per-tick 제외, 모든 시간 `TIMESTAMPTZ`(UTC).
- `src/main/resources/db/migration/V1__init_schema.sql` — `price_snapshot(collected_at, min_price)`, `game_event(occurred_at, event_type, title)` — Phase 5는 읽기 전용(신규 테이블/컬럼 없음).

### 프로젝트 계획
- `.planning/PROJECT.md` — Core Value(수집·서빙 1순위, event-impact는 그 위 헤드라인), **상관 ≠ 인과(과대주장 금지)**, Key Decisions(2주차 말 하드 게이트 조건부 / 5A·8A).
- `.planning/REQUIREMENTS.md` — **IMPACT-01, IMPACT-02**.
- `.planning/ROADMAP.md` (§Phase 5) — 목표 + 3개 성공 기준(pre/post·change_rate%·앵커 시각 / 희소→insufficient_data+앵커 null / stale→insufficient_data+앵커 시각) + 2개 plan 분할(05-01 v1 지표 계산 + 범위조회 배치 / 05-02 충분성+staleness+insufficient_data+앵커시각).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`src/main/java/com/lostark/tracker/read/WindowQueryService.java`** — `fetchWindow(itemId, from, to)` 재사용. per-event 2쿼리면 N+1 → D-09 배치(범위 1회 조회 후 메모리 분할 또는 신규 배치 메서드). 내부적으로 `PriceSnapshotRepository.findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc` + `GameEventRepository.findByOccurredAtBetween`.
- **`src/main/java/com/lostark/tracker/web/PricesController.java`** — 컨트롤러 패턴(`@DateTimeFormat ISO`, `InvalidRequestException`→400, `ItemNotFoundException`→404, `existsById` 검사). `EventImpactController` 신설 또는 `/api/items` 매핑 확장.
- **`src/main/java/com/lostark/tracker/web/error/{ApiExceptionHandler, InvalidRequestException, ItemNotFoundException}`** — `window≤0→400`·없는 item→404 에러 계약 재사용(신규 예외 불요).
- **`repository/{PriceSnapshotRepository, GameEventRepository, TrackedItemRepository}`** — read 쿼리. `GameEventRepository`에 전체 조회(`findAll` + 정렬) 또는 배치 범위 조회 추가; `PriceSnapshotRepository`는 Phase 2 insert 경로 공유 → **read 메서드만**.
- **`domain/{PriceSnapshot(collected_at/min_price), GameEvent(occurred_at/event_type/title)}`** — 엔티티 그대로 사용.
- **`src/test/java/com/lostark/tracker/support/PostgresRedisContainers.java`** — Testcontainers 공유 베이스. Phase 5 IT(충분성·staleness·앵커시각·**배치 2N 아님 spy/카운트**·E2E) 재사용.

### Established Patterns
- **Flyway 포워드 + `ddl-auto=validate`** — Phase 5는 **읽기 전용, 신규 테이블/컬럼 없음** → 마이그레이션 불요.
- UTC `OffsetDateTime` ISO-8601 직렬화(03 D-11), 범위 경계 UTC. `occurred_at`·`collected_at` 모두 `TIMESTAMPTZ` UTC → **off-by-9h 차단**(이벤트가 KST 자정 근처여도 앵커 계산 정확).
- 범위 쿼리 결과 **캐시 안 함**(latest만 캐시) — event-impact는 DB 직조회/배치.
- 전역 `@RestControllerAdvice`(`ApiExceptionHandler`)가 4xx 계약 통일.

### Integration Points
- **소비:** Phase 1/2 `price_snapshot`(앵커 후보) + Phase 4 `game_event`(`occurred_at` 앵커 기준)을 Phase 5가 읽어 변화율 계산. 관리자 `occurred_at` 정정(04 D-06)이 앵커에 즉시 반영.
- **Phase 6 시드(DIST-03):** 합성 스냅샷 7일+ · 이벤트 2개+ → event-impact가 비어있지 않게 데모. Phase 5 응답이 시드 데이터로 검증/시연됨(헤드라인 기능 빈 화면 방지).
- **게이트:** 2주차 말 하드 게이트 통과 조건부 — Phase 1~4 완료로 사실상 통과, 진행 결정됨.

</code_context>

<specifics>
## Specific Ideas

- **v1 지표 = 설계 §79 문자 그대로:** `(이벤트 직후 첫 스냅샷 min_price) / (이벤트 직전 마지막 스냅샷 min_price) − 1`. **윈도우 평균이 아닌 앵커 단일 스냅샷**(사용자 명시 선택).
- **staleness 기본 30분 = 수집주기 10분 × 3틱** — "2틱 연속 구멍까지 허용"으로 면접에서 설명 가능(파이프라인 현실에 연동된 임계).
- **`insufficient_data` 두 경우 명확 구분:** (a) 윈도우 내 한쪽 스냅샷 0개 → 그쪽 앵커 `null`; (b) 앵커는 있으나 >30분 → 그 앵커 시각 보고. **둘 다 발견 앵커 시각을 응답에 포함**해 호출자가 이유를 안다.
- **상관 ≠ 인과:** 응답·README 문구는 "시점 상관"까지만, `change_rate`를 "이벤트가 가격을 올렸다"로 과대 서술 금지(PROJECT 전제 5).
- **배치 검증(8A·T7):** 이벤트 N개에 쿼리가 2N이 아님을 spy/쿼리 카운트로 단언하는 IT — 면접 신호(N+1 인지·제거).

</specifics>

<deferred>
## Deferred Ideas

- **윈도우 평균(pre_avg/post_avg) 지표** — v1은 앵커 단일 스냅샷으로 확정. (IMPACT-V2-02 스무딩과 인접)
- **event-impact median / 스무딩(노이즈 완화)** — v2 (IMPACT-V2-02). MVP는 staleness 가드 + 신중 서술로 대응.
- **카테고리 베이스라인 대비 초과 상승률 비교** — v2 (IMPACT-V2-01). MVP는 단일 품목 전후 변화율까지.
- **staleness 허용치·충분성 임계 `@ConfigurationProperties` 외부화** — v2 (CFG-V2-01). MVP는 상수.
- **`window` 실수(소수 시간) 허용** — v2 (수집 10분 주기라 시간 정수 단위로 충분).
- **스코프 좁힘 파라미터(`from/to`·`event id`)** — v2 (엔드포인트 계약 `window=Nh`만 유지).
- **기간형 이벤트(start/end) 모델** — v2 (EVT-V2-01). MVP는 단일 `occurred_at`(±Nh 윈도우 계산 전제).

None 외 위 항목은 모두 명시적 v2/설계 제외로 회귀 추적용.

</deferred>

---

*Phase: 5-Event Impact (게이트 조건부)*
*Context gathered: 2026-06-24*
