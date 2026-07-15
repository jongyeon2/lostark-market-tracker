---
phase: 25-event-impact-backfill-anchor
status: passed
verified: 2026-07-15
requirements: [IMPACT-V2-01]
method: TDD + build + 라이브 API 실측 + Playwright 렌더
---

# Phase 25 VERIFICATION — event-impact 백필 폴백 앵커

**결과: ✅ PASSED**

## 목표 대조

목표: 이벤트 영향 페이지가 이미 가진 데이터(백필 일평균)를 쓰게 해, 수집 시작 이전 이벤트가
구조적으로 "데이터 부족"이던 문제를 없앤다. 단, 지표 semantics를 섞지 않고 정직하게 표기한다.

| must_have | 검증 | 결과 |
|---|---|---|
| 스냅샷 앵커가 신선하면 기존과 100% 동일(min 기준, 값 불변) | `snapshotAnchorsWinWhenPresentAndKeepMinSemantics` — 일별이 전혀 다른 답(32k/45k)을 줘도 스냅샷 답(1000→1100, +10%)이 나온다. `EventImpactGuardIT` 전 케이스 무변경 통과 | ✅ |
| 스냅샷이 없을 때만 폴백, min/avg 혼합 금지 | 폴백은 pre·post가 **둘 다 일별**일 때만 계산(`dailyFallback`이 두 날짜 모두 없으면 null). 한쪽만 스냅샷인 혼합 경로 자체가 존재하지 않음 | ✅ |
| 폴백 행은 "일별 평균 기준"으로 표기 | Playwright — 상태 뱃지 아래 "일별 평균 기준" 노출. `SNAPSHOT_MIN`은 라벨 없음(기본 의미) | ✅ |
| 차원술사(7/8) × 타격의 대가가 데이터 부족 → +40.6% | 라이브 API: `status ok` · `changeRate 0.4061` · `DAILY_AVG` · 32,629 → 45,878. 화면: **+40.6%** | ✅ 실증 |
| N+1 회피 유지 | 일별 스탯도 `findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc` **1회**(min일−1 ~ max일) 후 메모리 맵. 이벤트 수와 무관 | ✅ |
| 수집·캐시 0줄 | `git diff 95ad414..HEAD` — `collect/`·`cache/`·`PriceSnapshot`·V1~V7 스키마 **0줄**. 읽기 경로만 | ✅ |

## 빌드/테스트

- `./gradlew build` → **BUILD SUCCESSFUL** (전체 스위트).
- `cd frontend && npm run build` → tsc -b + vite **그린**.
- `EventImpactBackfillAnchorIT` **7/7**: 폴백 · 스냅샷 우선 · DETAIL_STATS 우선 · YDAY_AVG 대체 ·
  한쪽 결측 insufficient · 양쪽 없음 insufficient · **KST 일자 판정**(16:00Z=KST 익일).

## 라이브 증거

```
GET /api/items/10/event-impact?window=24
{ "status": "ok", "prePrice": 32629, "postPrice": 45878,
  "changeRate": 0.4061, "anchorSource": "DAILY_AVG",
  "preAnchorAt": null, "postAnchorAt": null }
```

화면(Playwright 1280px): `차원술사 출시 | 신규 캐릭터 출시 | 2026.07.08 10:00 KST |
✓ 비교 가능 · 일별 평균 기준 | +40.6% | 32,629 G | 45,878 G`. 한글 깨짐 없음.
스크린샷: `phase25-impact-tagyeok.png` · `phase25-impact-daily-avg.png`(재료 −1.4%).

## 비고

- **정직성 보존**: 일별 앵커의 `preAnchorAt`/`postAnchorAt`은 null이다. 일평균은 순간이 아니라 날짜에
  속하므로 자정 같은 시각을 지어내지 않는다. `insufficientReason`(희소/stale 판정)은 `ok` 행에
  관여하지 않으므로 D-08 계약 무변경.
- **상관≠인과**는 그대로다 — 폴백은 더 많은 이벤트에 수치를 붙일 뿐, 인과 주장을 하지 않는다.
  페이지 상단 "해석에 주의하세요" 배너 유지.
- dev DB에 운영과 동일한 실제 차원술사 이벤트 1건을 넣어 재현했다(합성 데이터 아님).

**Phase 25 완료.**
