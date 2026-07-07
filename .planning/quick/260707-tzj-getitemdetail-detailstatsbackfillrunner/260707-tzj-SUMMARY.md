---
quick_id: 260707-tzj
slug: getitemdetail-detailstatsbackfillrunner
status: complete
date: 2026-07-07
type: bugfix
tags: [backfill, restclient, engraving, detail-stats, testcontainers, core-value-guard]
commits:
  - 92dbf2e  # fix: getItemDetail selects traded detail-array element
  - 9e492b3  # fix: backfill all active items incl. engravings
duration: ~40min
---

# Quick Task 260707-tzj: 각인서 백필 버그 수정 Summary

**유물 각인서 상세 응답이 원소 2개 배열([0]=귀속 거래1회·Stats 0, [1]=자유거래·14일 실거래)인데 `getItemDetail`이 `details[0]`만 반환해 AvgPrice=0이던 것을 총 TradeCount 최대 원소 선택으로 수정하고, 소급 러너를 재료 한정→전 활성 품목으로 확대. 각인서도 재료처럼 상세 14일 소급됨. Core Value 경로 0줄.**

## Root Cause (실 API 증거)

사용자가 "각인서는 거래가 매우 활발한데 0은 말이 안 된다"고 지적 → 실 API 정밀 확인.

`GET /markets/items/{id}` 각인서 응답 = **원소 2개 배열**:
| 원소 | TradeRemainCount | Stats 14일 |
|---|---|---|
| `[0]` (귀속 후 거래 1회 가능) | 1 | 전부 **0** |
| `[1]` (자유거래) | 0 | 전부 **실거래** (유물 원한 각인서 07-07 145,743원·912건) |

재료는 원소 1개(`TradeRemainCount=null`). 기존 코드 `return details[0]`이 각인서에서 거래 0 껍데기 원소를 집었다. 5품목(재료 2·각인서 3)으로 패턴 일관 확인.

부차적으로 `DetailStatsBackfillRunner`가 스파이크 오판(D-01 "재료만 소급") 기반으로 `roleGroup=MATERIAL`만 소급 → 각인서는 상세 소급 대상에서 아예 제외돼 있었음.

## Changes

1. **`getItemDetail` 원소 선택** (`LostarkApiClient`) — `details[0]` 고정 → `selectTradedDetail`: Stats 총 TradeCount가 최대인 원소 선택(순서·TradeRemainCount 비의존, all-zero면 [0] 유지). 재료(원소1)는 no-op. `LostarkApiClientTest`에 2원소 배열 회귀 테스트 추가.
2. **소급 대상 확대** (`DetailStatsBackfillRunner`) — `findByActiveTrueAndRoleGroup("MATERIAL")` → `findByActiveTrue()`. 각인서 포함 전 활성 품목이 상세 14일 소급 대상. per-stat `AvgPrice>0` 가드·tryAcquire·fail-open·auth-break 무변경(≈22콜/회 ≪ 100/분). 미사용이 된 role-group finder 제거. IT의 `nonMaterialItemsAreNeverFetched`→`engravingItemsAreAlsoBackfilled`로 재작성(각인서 14행 소급 검증).
3. **문서 정정** — DTO javadoc·`SPIKE-FINDINGS`(2차 정정 블록)·`17.4-04-SUMMARY`(재정정 블록)의 "각인서 상세 Stats=0·소급 불가" 오판 정정.

## Task Commits
1. **getItemDetail 원소 선택 + 회귀테스트 + DTO 주석** — `92dbf2e`
2. **소급 전 품목 확대 + IT + repo 정리** — `9e492b3`
3. **스파이크/SUMMARY 문서 정정** — (본 docs 커밋)

## Verification
- `./gradlew test --tests LostarkApiClientTest --rerun-tasks` 그린 (2원소 배열 → [1] 선택 회귀 포함).
- `./gradlew test --tests DetailStatsBackfillRunnerIT` 그린 (재료 13행·멱등·**각인서 14행 소급**).
- **`./gradlew build` 그린** (전체 테스트, 2m34s).
- **Core Value 가드**: `PriceCollector`·`cache/`·`PriceSnapshot`·`db/migration/`·`WindowQueryService`·`DownsampleService`·`EventImpact*` diff **0줄**(`8128eea..HEAD`). `searchMarketItems`(수집) 무변경 — `getItemDetail`(백필)만 수정.

## Impact
- 각인서도 재료처럼 **최근 14일 일평균 거래가**가 `DETAIL_STATS`로 소급 적재 → 타임라인 백필 라인이 14일치로 채워짐(앱 재기동 시 러너가 소급). 사용자가 원한 "날짜별 각인서 시세"가 실제로 표시됨.
- 스파이크의 "각인서 소급 불가" 전제가 뒤집힘 → 지난 턴에 검토하던 범위 버튼 product 질문 대부분 무의미해짐(7일·30일 부분 유의미).

## Follow-ups (별건, 미포함)
- 범위 프리셋 90일 vs 실데이터 깊이(재료·각인서 최대 14일 소급) 정직성 — UX 결정 필요 시 별도 quick.
- 앱 재기동 후 dev DB에 각인서 `DETAIL_STATS` 14행 적재 육안 확인 권장.
