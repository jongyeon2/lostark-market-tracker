---
phase: 25-event-impact-backfill-anchor
plan: 01
status: complete
requirements: [IMPACT-V2-01]
commits: [089ce68, 7d03b7b]
---

# Phase 25-01 SUMMARY — event-impact 백필 폴백 앵커

## 무엇을 했나

이벤트 영향 페이지가 **이미 가진 데이터를 쓰게** 했다. 타임라인은 `price_snapshot`(10분 min 호가)과
`item_daily_stats`(백필 일평균)를 둘 다 읽지만 `EventImpactService`는 전자만 읽었다 — Phase 17.4가
BACKFILL-04로 "event-impact 0줄"을 의도적으로 가드했기 때문. 그래서 **수집 시작(2026-07-10) 이전
이벤트는 앵커 0건이라 영원히 "데이터 부족"**이었다. 사용자가 화면에서 본 스파이크를 영향 페이지만
못 본 것.

- **스냅샷 우선**: 스냅샷 앵커를 먼저 판정하고 통과하면 지금과 100% 동일하게 min 기준 계산 →
  기존 `ok` 행의 숫자 불변. 자를 몰래 바꾸지 않는다.
- **폴백 규칙**: 스냅샷이 없을 때만 `pre` = 이벤트 KST **전일**, `post` = KST **당일** 일평균.
  일평균은 그날 전체 체결을 이미 담으므로 이 해상도에서 당일이 곧 첫 사후 관측이다.
- **혼합 금지**: min(호가)과 avg(체결)를 한 `changeRate` 안에서 절대 섞지 않는다.
- **자료원 우선순위**: `DETAIL_STATS` > `YDAY_AVG` — 수집 이전까지 소급되는 자료원이라(정확히 이
  폴백이 존재하는 구간) pre/post를 같은 기준으로 덮는다.
- **`AnchorSource`(SNAPSHOT_MIN/DAILY_AVG)** 신설 + 프론트 `'일별 평균 기준'` 표기.
- **앵커 시각 null**: 일평균은 순간이 아니라 날짜에 속한다. 자정 같은 시각을 지어내면 측정 시점을
  오도하므로 비운다.
- **N+1 회피 유지**: 일별 스탯도 이벤트 전 구간 **단일 범위 조회 1회** 후 메모리 판정(D-09).

## 결정 근거 (계획 중 발견)

- **DETAIL_STATS는 전 품목 커버** — 17.4 문서엔 "재료만·각인서 미호출"이라 적혀 있으나, 러너 코드는
  `findByActiveTrue()`로 모든 활성 품목을 돈다("Covers materials AND engraving books"). 문서가 낡았고
  코드가 진화했다. 각인서인 타격의 대가에 DETAIL_STATS가 있는 이유이자, 이 폴백이 전 품목에서
  동작하는 근거.
- **`prePrice`/`postPrice`는 `Long` 유지** — 일평균은 소수지만 반올림해 담고 자료원 필드로 구분했다.
  `BigDecimal` 승격은 프론트 계약까지 흔들어 스코프 밖.

## 검증

- **TDD** — `EventImpactBackfillAnchorIT` 7케이스: 폴백 계산(실제 수치 7/7 32628.5 → 7/8 45877.9) ·
  스냅샷 우선(일별이 전혀 다른 답이어도 스냅샷 채택) · DETAIL_STATS 우선 · YDAY_AVG 대체 ·
  한쪽 일자 결측 시 여전히 insufficient · 양쪽 다 없을 때 insufficient · **KST 일자 판정**
  (16:00Z = KST 익일이라 UTC로 읽으면 틀리는 케이스).
- `./gradlew build` **BUILD SUCCESSFUL** · `npm run build` **그린**.
- **라이브 API 실측**: `/api/items/10/event-impact?window=24` →
  `status: ok`, `changeRate: 0.4061`, `anchorSource: DAILY_AVG`, `prePrice 32629`, `postPrice 45878`.
- **Playwright 라이브 렌더**: 유물 타격의 대가 각인서 × 차원술사 출시 →
  **"비교 가능 / 일별 평균 기준 / +40.6% / 32,629 G → 45,878 G / 2026.07.08 10:00 KST"**.
  한글 깨짐 없음. 재료(빙하의 숨결)도 폴백 동작 확인(−1.4%). 스크린샷 `phase25-impact-tagyeok.png`,
  `phase25-impact-daily-avg.png`.

## 편차

- 테스트 기댓값 산술 오류 1건(`0.4060` → 실제 `0.4061`) — 코드가 아니라 내 단언이 틀렸고, 주석엔
  이미 0.4061로 적혀 있었다. 수정 후 그린.
- `GameEvent` 생성자 인자 순서 오해로 컴파일 실패 1회 — `(EventType, title, occurredAt, description)`.
- **dev DB에 차원술사 출시 이벤트 1건 삽입** — dev는 이벤트 0건이었다(합성 seed 삭제됨). 운영과 동일한
  실제 이벤트(`NEW_CLASS`, `2026-07-08T01:00:00Z`)라 합성 데이터가 아니며, 라이브 재현에 필요했다.

## Core Value 가드

수집(`PriceCollector`)·캐시(`cache/`)·`price_snapshot`·V1 스키마 **0줄**. 읽기 경로에서 기존 테이블
(`item_daily_stats`)을 **추가로 읽기만** 한다 — 신규 테이블·신규 수집 호출 없음.

## 커밋

- `089ce68` — EventImpactService · EventImpactItem · AnchorSource · EventImpactBackfillAnchorIT
- `7d03b7b` — schemas.ts · impactFormat.ts · EventImpactTable · EventImpactCards
