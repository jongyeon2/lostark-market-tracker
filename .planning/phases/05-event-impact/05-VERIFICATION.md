---
phase: 05-event-impact
verified: 2026-06-24T13:10:00Z
status: passed
score: 3/3 성공 기준 검증됨
---

# Phase 5: Event Impact 검증 보고서

**페이즈 목표:** 이벤트별 ±N시간 전후 가격 변화율을 데이터 충분성·staleness 가드와 함께 계산한다.
**검증 시각:** 2026-06-24T13:10:00Z
**상태:** passed
**검증 방식:** 목표 역산(goal-backward) — ROADMAP Phase 5 목표 + 3개 성공 기준 + IMPACT-01/02 요구사항 + 두 PLAN의 must_haves. 오케스트레이터가 인라인으로 수행(이 환경은 백그라운드 서브에이전트가 권한 거부됨).

## 목표 달성

### 관측 가능한 진실 (ROADMAP 성공 기준)

| # | 진실 | 상태 | 증거 |
|---|------|------|------|
| 1 | `GET /api/items/{id}/event-impact?window=Nh`가 이벤트별 (pre/post 앵커 가격)·change_rate·앵커 시각 목록을 반환한다 | ✓ 검증됨 | `EventImpactController.eventImpact` → `EventImpactService.eventImpact`(EventImpactService.java) → `EventImpactResponse{itemId, window, events[]}`, `EventImpactItem{status, preAnchorAt, postAnchorAt, prePrice, postPrice, changeRate}`, occurred_at desc. 테스트 `EventImpactIT.happyPathComputesAnchorDeltaChangeRate`(prePrice=1000/postPrice=1200/changeRate=0.2/앵커 시각) + `eventsAreReturnedNewestFirst`. **명시적 정정(D-01/D-07):** ROADMAP의 "pre_avg·post_avg" 표현은 느슨함 — v1 지표는 **앵커 단일 스냅샷 가격(평균 아님)**이며 필드명 prePrice/postPrice·change_rate(델타 비율)로 정정됨. |
| 2 | 희소 윈도우는 발견한 앵커 시각(또는 null)과 함께 `"status":"insufficient_data"`를 반환한다 | ✓ 검증됨 | `EventImpactService.toImpactItem` insufficient 분기: present-side 앵커 시각 보고, absent-side null. 테스트 `EventImpactGuardIT.preOnlyWindowIsInsufficientWithNullPostAnchor`(postAnchorAt null) + `postOnlyWindowIsInsufficientWithNullPreAnchor`(preAnchorAt null). |
| 3 | staleness 허용치를 벗어난 앵커는 그 앵커 시각과 함께 `insufficient_data`를 반환해 호출자가 이유를 안다 | ✓ 검증됨 | `STALENESS_ALLOWANCE = Duration.ofMinutes(30)` + `isFreshAnchor`(EventImpactService.java:56,~135). stale 앵커는 시간 보고(non-null)·change_rate 미산출. 테스트 `EventImpactGuardIT.stalePreAnchorIsInsufficientButReportsTheAnchorTime`/`stalePostAnchor...`/`anchorOneSecondPastThirtyMinutesIsStale`. |

**점수:** 3/3 진실 검증됨

### 필수 산출물 (must_haves artifacts)

| 산출물 | 상태 | 상세 |
|--------|------|------|
| `web/EventImpactController.java` | ✓ 존재 + 실질적 | `@GetMapping("/{id}/event-impact")` int window; window ≤0/>168 → `InvalidRequestException`(400) BEFORE `existsById`(404). 새 예외 타입 0개(기존 재사용, grep 확인). |
| `read/EventImpactService.java` | ✓ 존재 + 실질적 | 단일 배치(이벤트 1회 + 스냅샷 범위 1회) anchor-delta; staleness/sufficiency 게이트; `fetchWindow` 호출 0회(grep 확인). |
| `web/dto/EventImpactResponse.java` | ✓ 존재 + 실질적 | `{long itemId, int window, List<EventImpactItem> events}` 요청 echo + 이벤트 리스트. |
| `web/dto/EventImpactItem.java` | ✓ 존재 + 실질적 | `status` + nullable preAnchorAt/postAnchorAt/prePrice/postPrice/changeRate(앵커 가격, 평균 아님). |
| `repository/GameEventRepository.java` | ✓ 존재 + 실질적 | `findAllByOrderByOccurredAtDesc()` 추가, `findByOccurredAtBetween` 미변경(grep 확인). |
| `read/EventImpactIT.java` | ✓ 존재 + 실질적 | 11 케이스: happy/desc/empty-200/404/window 400 매트릭스/**N+1 spy times(1)**/**admin E2E**. |
| `read/EventImpactGuardIT.java` | ✓ 존재 + 실질적 | 6 케이스: preOnly/postOnly/stalePre/stalePost/boundary=30min ok/30min+1s stale. |

### 핵심 진실 (must_haves truths) 스폿체크

- **D-01 앵커 단일 스냅샷 델타(평균 아님):** `changeRate = BigDecimal(post.minPrice)/BigDecimal(pre.minPrice) - 1`(min_price만). happy-path IT가 0.2(1200/1000-1) 단언. gap-0 타이 규칙은 양쪽 분기 코드로 충족.
- **D-06 전역 이벤트 + occurred_at desc:** `findAllByOrderByOccurredAtDesc()` 1회; 모든 이벤트가 리스트에 포함(insufficient도 포함). `eventsAreReturnedNewestFirst` 단언.
- **D-08 window 필수 int 검증 계약:** 0/음수/>cap → `InvalidRequestException`(400), 누락 → MissingServletRequestParameter(400), 비정수 → MethodArgumentTypeMismatch(400), 없는 아이템 → 404; 검증이 존재확인보다 먼저. 5종 400 매트릭스 + 404 IT 통과.
- **D-09 N+1 회피:** `findByTrackedItem_IdAndCollectedAtBetweenOrderByCollectedAtAsc` 서비스 내 1회·루프 없음(grep), `@MockitoSpyBean` `times(1)` IT 증명(N≥2 이벤트).
- **D-02/D-04 both-fresh 규칙:** ok는 양쪽 anchor present + priced + 둘 다 fresh일 때만. `isFreshAnchor` 헬퍼.
- **D-03 30분 MVP 상수·경계 포함:** `Duration.ofMinutes(30)`, `compareTo(...) <= 0`(=30min fresh). `@ConfigurationProperties`/`@Value` 0개(grep 확인). boundary IT 2건 단언.
- **D-05 sparse vs stale 구분:** insufficient에서 present-side 앵커 시간 보고, absent-side만 null. stale IT는 non-null, sparse IT는 null 단언.
- **권한/읽기전용:** /api/items/** permitAll(04-02) 재사용 → SecurityConfig 무변경; Flyway 마이그레이션/엔티티/의존성 변경 0(`git diff src/main/resources/db` 비어 있음).

## 요구사항 추적

| 요구사항 | 상태 | 근거 |
|----------|------|------|
| IMPACT-01 | ✓ 충족 | event-impact v1 지표 엔드포인트 + 배치 계산(05-01). EventImpactIT 11/11. |
| IMPACT-02 | ✓ 충족 | 충분성 + staleness 가드 + insufficient_data 앵커 시각(05-02). EventImpactGuardIT 6/6. |

## 테스트 증거

- `EventImpactIT` — 11 tests, 0 failures/skips (Testcontainers Postgres+Redis).
- `EventImpactGuardIT` — 6 tests, 0 failures/skips.
- 전체 스위트 `./gradlew test -PdockerApiVersion=1.44` — **84 tests, 0 failures, 0 errors, 1 skipped**(기존 phase의 사전 skip, 본 페이즈 무관). Phase 1–4 회귀 없음.

## 의도된 정정 / 비고

- **ROADMAP SC1 표현 정정:** "pre_avg·post_avg" → 설계 §79·D-01에 따라 **앵커 스냅샷 가격(prePrice/postPrice)**으로 구현. 평균이 아니라 단일 앵커 가격이며 이는 PLAN의 must_haves에 명시된 잠긴 결정(D-01). 목표의 실질(이벤트별 전후 가격 + 변화율 + 앵커 시각)은 완전 충족.
- **상관 ≠ 인과:** change_rate는 시간적 상관 수치로만 명명/문서화(PROJECT premise 5). 응답·필드명·테스트 모두 인과 주장 없음.

## 판정

**PASSED** — Phase 5 목표(이벤트별 전후 변화율 + 데이터 충분성·staleness 가드)를 코드와 통합 테스트로 충족. 3/3 성공 기준, IMPACT-01/02 모두 검증. 회귀 없음.

---
*Phase: 05-event-impact*
*Verified: 2026-06-24*
