---
quick_id: 260707-tzj
slug: getitemdetail-detailstatsbackfillrunner
date: 2026-07-07
type: bugfix
tags: [backfill, restclient, engraving, detail-stats, testcontainers]
core_value_guard: [collect, cache, price_snapshot, WindowQueryService, DownsampleService, EventImpact]
---

# Quick Task 260707-tzj: 각인서 백필 버그 수정 (상세 배열 원소 선택 + 소급 대상 확대)

## Problem

유물 각인서는 인게임에서 활발히 거래되는데(하루 900~2000건, 평균 ~145k) 타임라인에 백필 일평균 거래가가 안 보였다. 실 API로 근본 원인 확인:

- `GET /markets/items/{id}` 각인서 응답은 **원소 2개 배열**: `[0]` = "귀속 후 거래 1회 가능" 변형(`TradeRemainCount=1`, Stats 14일 전부 0), `[1]` = 자유거래 변형(`TradeRemainCount=0`, Stats 14일 전부 실거래).
- `LostarkApiClient.getItemDetail`이 무조건 `details[0]`(거래 0 껍데기)을 반환 → 각인서 AvgPrice=0.
- 재료는 원소 1개(`TradeRemainCount=null`, 실거래)라 영향 없었음.
- 추가로 `DetailStatsBackfillRunner`가 `roleGroup=MATERIAL`만 소급 → 각인서는 상세 소급 대상에서 아예 제외(스파이크 오판 D-01 기반).

실측 근거(5품목): 재료 66102007/6861012 = 배열 1, 각인서 65200505/65203405/65201005 = 배열 2([1]에만 실거래).

## Fix

1. **`getItemDetail` 원소 선택** — `details[0]` 고정 대신 배열에서 **Stats 총 TradeCount가 최대인 원소** 선택(순서·TradeRemainCount 비의존, all-zero면 [0] 유지). 재료/각인서 모두 정확.
2. **`DetailStatsBackfillRunner` 소급 확대** — `findByActiveTrueAndRoleGroup("MATERIAL")` → `findByActiveTrue()`. 각인서 포함 전 활성 품목이 14일 상세 소급 대상. 기존 `avgPrice>0` per-stat 가드가 all-zero 방어. tryAcquire·fail-open·auth-break 로직 무변경(≈22콜/회 ≪ 100/분).
3. **문서·주석 정정** — 스파이크/DTO/러너 javadoc의 "각인서 상세 Stats=0·소급 불가" 오판 정정(배열 원소 구분으로 수정).

## Tasks

### Task 1 — getItemDetail 배열 원소 선택 + 회귀 테스트 + DTO 주석 정정
- **files:** `src/main/java/com/lostark/tracker/collect/LostarkApiClient.java`, `src/test/java/com/lostark/tracker/collect/LostarkApiClientTest.java`, `src/main/java/com/lostark/tracker/collect/dto/ItemDetailResponse.java`, `src/main/java/com/lostark/tracker/collect/dto/MarketStat.java`
- **action:** getItemDetail이 총 TradeCount 최대 원소를 고르도록 수정(private helper `totalTrades`). LostarkApiClientTest에 2원소 배열([0] all-zero, [1] 실거래) → [1] 선택 회귀 테스트 추가. DTO javadoc의 "engravings return 0 (API limitation)" 문구를 배열-원소 설명으로 정정.
- **verify:** `./gradlew test --tests LostarkApiClientTest` 그린(신규 포함).
- **done:** 2원소 배열에서 실거래 원소가 선택됨을 테스트가 증명.

### Task 2 — 소급 대상 전 품목 확대 + IT 갱신 + 러너/repo 주석 정정
- **files:** `src/main/java/com/lostark/tracker/backfill/DetailStatsBackfillRunner.java`, `src/test/java/com/lostark/tracker/backfill/DetailStatsBackfillRunnerIT.java`, `src/main/java/com/lostark/tracker/repository/TrackedItemRepository.java`
- **action:** 러너를 `findByActiveTrue()`로 변경(MATERIAL 상수 제거), javadoc 정정. IT의 `nonMaterialItemsAreNeverFetched`를 `engravingItemsAreAlsoBackfilled`로 재작성(각인서도 fetch·backfill 검증), 기타 활성 아이템 NPE 방지용 `getItemDetail(anyLong())` 기본 empty 스텁 추가. repo `findByActiveTrueAndRoleGroup` javadoc 정정(현재 미사용이면 유지하되 설명 수정).
- **verify:** `./gradlew test --tests DetailStatsBackfillRunnerIT` 그린.
- **done:** 각인서가 소급되고 재료 멱등성이 유지됨을 IT가 증명.

### Task 3 — 스파이크/SUMMARY 문서 정정
- **files:** `.planning/phases/17.4-timeline-gap-backfill/17.4-SPIKE-FINDINGS.md`, `.planning/phases/17.4-timeline-gap-backfill/17.4-04-SUMMARY.md`
- **action:** "각인서 상세 Stats=0·소급 불가" 결론에 dated 정정 블록 추가(배열 원소 [1]에 실거래 존재, 본 quick에서 수정).
- **verify:** 문서 정정 반영 확인.
- **done:** 오판이 문서에 정정 기록됨.

## Core Value Guard
수집(`PriceCollector`)·캐시(`cache/`)·`price_snapshot`·`WindowQueryService`·`DownsampleService`·`EventImpact*` 무변경. 이 quick은 backfill 경로(read-only 상세 API + item_daily_stats 적재)만 손댐.

## Full-suite verification
`./gradlew build` 그린(전체 테스트) 후 커밋.
