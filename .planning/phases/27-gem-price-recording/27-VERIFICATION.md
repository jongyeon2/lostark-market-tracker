---
phase: 27-gem-price-recording
status: passed
verified: 2026-07-15
method: IT 16/16 + build + 라이브 실측(기록·멱등·캐시 무오염)
requirements: [GEM-03, GEM-04]
---

# Phase 27 VERIFICATION — 보석 시세 기록 시작 + 라벨 정정

**결과: ✅ PASSED**

## 성공조건 대조 (ROADMAP Phase 27)

| # | 성공조건 | 검증 | 결과 |
|---|---|---|---|
| 1 | 6종이 1시간마다 독립 테이블에 적재, 공유 버킷 사용, 재기동 멱등, 폴러 실패가 화면 무영향 | 라이브 부팅 기록 → `6 rows inserted, 0 unanswered, slot=2026-07-15T12:00Z`, DB 6행 실측 | ✅ |
| 2 | 답받은 것만 기록 (매물없음=null 행 / 실패·레이트리밋=행 없음) | IT `noBuyoutIsRecordedAsNullPriceNotAsMissingRow` · `throttledRunLeavesNoRowsAndNeverCallsAuctionApi` · `fetchFailureLeavesNoRowSoAbsenceMeansWeCouldNotAsk` | ✅ |
| 3 | 딜러/서포터 라벨 제거 (findings 문서 포함) | `grep` → 코드·화면 잔존 **0**(정정 각주의 인용 2건 제외). findings 표 정정 + 각주 | ✅ |
| 4 | ~~레벨 중심 표~~ | **폐기** — Phase 28이 `/gems`를 페이지째 삭제(사용자 재설계 2026-07-15) | ⏭️ |
| 5 | 헤드라인 현재가 회귀 없음 (≤5분, 폴러가 캐시 미오염) | 기록 12:52:37 / API `updatedAt` **12:54:00** = 캐시 미오염 실증. 2회 호출 `updatedAt` 동일 | ✅ |
| 6 | Core Value 0줄, additive V9만 | `git status` — `collect/`·`cache/`·`read/`·`ratelimit/` **0건**, V1–V8 변경 **0건** | ✅ |

## 라이브 증거

```
[1차 부팅] gem price record: 6 rows inserted, 0 unanswered (skipped), slot=2026-07-15T12:00Z

 series | level | min_buy_price |          recorded_at          |       hour_slot
--------+-------+---------------+-------------------------------+------------------------
 겁화   |     8 |        354333 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00
 겁화   |     9 |       1040000 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00
 겁화   |    10 |       3090000 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00
 작열   |     8 |        356000 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00
 작열   |     9 |       1040000 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00
 작열   |    10 |       3080000 | 2026-07-15 12:52:37.357551+00 | 2026-07-15 12:00:00+00

[2차 부팅·같은 시간대] gem price record: 0 rows inserted → DB 여전히 6행, 값도 12:52:37 첫 표본 그대로
[Flyway] 9 | add gem price snapshot | success=t
[429] 0건
```

**설계 3개가 라이브로 확인됨:**
- `recorded_at`=12:52:37(실제 측정 순간) vs `hour_slot`=12:00(절삭) — **측정 시각 미왜곡**
- 재기동 `0 rows inserted` — **멱등**(Phase 19가 main push마다 재배포하므로 필수)
- 기록 12:52:37 → API `updatedAt` 12:54:00 — **폴러가 서빙 캐시를 안 건드림**(안 그랬으면 화면이 1시간 묵은 값을 서빙)

## 빌드/테스트

- `GemPriceRecorderIT` **7/7** · `GemServiceIT` **9/9** (전부 skipped 0, XML 리포트 확인)
- 🔑 **`GemServiceIT` 9/9는 무수정 통과** — `GemPriceFetcher` 추출이 서빙 동작을 안 바꿨다는 증거
- `./gradlew build` **BUILD SUCCESSFUL** · `npm run build` **그린**(tsc 포함)

## 🔑 검증이 잡아낸 것 (수정 완료)

**계획의 파일 목록이 불완전했다.** `grep` 검증에서 `GemDtos.java:39`의 javadoc
(`@param series 계열 — 겁화(딜러) / 작열(서포터)`)이 살아있는 게 드러났다 — 27-02 PLAN의 artifacts에
그 파일이 없었다. 화면·상수만 고치고 끝냈으면 **같은 추측이 DTO 계약 문서에 남았다.**
`grep` 0건을 검증 기준으로 잡아둔 게 잡아냈다.

**테스트 상수도 같은 추측을 담고 있었다** — `LV8_DEALER`/`GemCatalog.SERIES_DEALER` 참조.
상수 rename 후 컴파일이 깨져 드러났고 함께 정정(`LV8_GEOPHWA`/`SERIES_GEOPHWA`).

## 비고

- ⚠️ **공유 리미터 미수정 보고사항 유지** — 버킷 용량 90 + 리필 90/분 → 최대 180콜/분 vs 서버 100/분.
  Phase 2부터의 성질, Core Value 코드라 미수정. 보석 기록은 시간당 6콜(0.1/분)로 이 문제에 기여하지 않는다.
- 🚀 **배포**: **V9 마이그레이션 있음**(v1.7까지는 없었다). 신규 테이블만 추가하는 additive라 기존 데이터
  무영향이고, 롤백 시 구버전은 이 테이블을 무시한다.
- ⏭️ **Phase 28 대기**: `/gems`·`이벤트 영향` 페이지 삭제 + 대시보드 통합(UI-SPEC 선행).
