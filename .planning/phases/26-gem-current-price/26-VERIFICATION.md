---
phase: 26-gem-current-price
status: passed
verified: 2026-07-15
method: IT 9/9 + build + 라이브 API 실측 + Playwright 렌더
requirements: [GEM-02]
---

# Phase 26 VERIFICATION — 보석 현재가 둘러보기 `/gems`

**결과: ✅ PASSED**

## 성공조건 대조 (ROADMAP Phase 26)

| # | 성공조건 | 검증 | 결과 |
|---|---|---|---|
| 1 | `GET /api/gems`가 6종 최저 즉시구매가 서빙(계열·레벨·아이콘·가격·갱신시각), 프론트는 백엔드만 소비 | 라이브 200 · 6/6 `OK` · `updatedAt` 포함. `grep "auctions" frontend/src` → **0건** | ✅ |
| 2 | Redis 캐시로 서빙 — 페이지뷰마다 경매장 미호출 | 2회 연속 호출 `updatedAt` **동일**(0.70s → 0.04s). `cacheHitIssuesZeroAuctionCalls` IT가 호출 0 단언 | ✅ |
| 3 | 즉시구매 매물 없으면 가격 없음으로 정직 표기 | `NO_BUYOUT` → `minBuyPrice=null` + "즉시구매 매물 없음"(IT 고정). 0골드·마지막값 재사용 없음 | ✅ |
| 4 | 계열별 6종 표시 + 로딩/빈/에러 정직 처리 | Playwright: 겁화(딜러) 3행 / 작열(서포터) 3행. 자체 `AsyncBoundary` | ✅ |
| 5 | (Core Value 가드) 수집·캐시·event-impact·`price_snapshot` 0줄, 마이그레이션 0 | `git status` — `collect/`·`cache/`·`read/EventImpact`·`db/migration`·`ratelimit/` **전부 무변경** | ✅ |

## must_haves 대조 (26-01-PLAN)

| truth | 검증 | 결과 |
|---|---|---|
| 6종 최저 즉시구매가가 /gems에 보인다 | Playwright 1280px 스크린샷 6행 | ✅ |
| 캐시로 서빙 — 페이지뷰마다 경매장 미호출 | `updatedAt` 동일 + IT 호출 0 | ✅ |
| 무방문 시 경매장 호출 0(폴러 없음) | 스케줄러 0 — `@Scheduled` 없음, 캐시-어사이드 | ✅ |
| 없는 가격은 행 단위 정직 표기, 나머지는 정상 | `oneGemFailureIsIsolatedToItsOwnRow`·`gemWithNoBuyoutListing…` IT | ✅ |
| 화면 문구가 동작과 일치("5분마다 갱신" 아님) | 부제 = **"최저 즉시구매가 · 기준 시각 2026. 07. 15. 16:12"** | ✅ |
| 행은 링크가 아니다 | Playwright 스냅샷: 행에 `cursor=pointer` **없음**(nav 링크에만 있음). `grep "<Link" GemPage.tsx` → 0건 | ✅ 실증 |
| Core Value 0줄 | 위 참조 | ✅ |

## 라이브 증거

```
GET /api/gems  (2026-07-15 16:12 KST)
  겁화 8레벨  | OK |   346,888      작열 8레벨  | OK |   348,798
  겁화 9레벨  | OK | 1,038,888      작열 9레벨  | OK | 1,069,997
  겁화 10레벨 | OK | 3,060,000      작열 10레벨 | OK | 3,050,000
1차 0.70s (캐시 미스·6콜) → 2차 0.04s (캐시 히트·0콜), updatedAt 동일
```

## 빌드/테스트

- `GemServiceIT` **9/9** (failures 0 · errors 0 · **skipped 0** — XML 리포트로 확인)
- `./gradlew build` **BUILD SUCCESSFUL** · `npm run build` **그린**(tsc 포함)

## 🔑 중간에 잡은 실버그 (수정 완료)

**첫 라이브 실행: 겁화 3 OK / 작열 3 × HTTP 429.** 보석 클라이언트가 프로젝트의 공유
`RedisTokenBucket`(단일 전역 버킷, "one API key, one bucket" D-03)을 **우회**했다 — Phase 24가
"경매장은 거래소와 서버 쿼터를 공유한다"를 측정해놨는데 코드가 그걸 안 썼다. `GemService`가 같은
버킷에서 토큰을 얻도록 수정 → **재검증 6/6 OK · 429 0건**. IT 2건으로 고정
(`throttledGemYieldsWithoutCallingAuctionApi`, `serverSideTooManyRequestsIsReportedAsRateLimitedNotFailure`).

## 비고 / 후속 (사용자 판단 필요)

- ⚠️ **공유 리미터가 서버 한도를 넘길 수 있다(미수정·보고만).** 버킷 = 용량 90 + 리필 90/분 →
  가득 찬 상태면 1분에 최대 **180콜** 가능한데 서버 한도는 **100/분**. 리미터를 지켜도 429가 날 수 있다.
  **Phase 2부터의 성질**이고 수집기는 `Retry-After`로 흡수한다. 수정하려면 Core Value 코드를 만져야 해
  GEM-02의 "수집 0줄" 가드를 지켜 **손대지 않았다**.
- 🚀 **배포 시**: 마이그레이션 **없음**(보석은 DB 미사용). 프론트 새 라우트 `/gems` 추가 —
  Caddy SPA fallback이 이미 있어 별도 설정 불요. CSP도 무변경(onstove CDN 기허용).
