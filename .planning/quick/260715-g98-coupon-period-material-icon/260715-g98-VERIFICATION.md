---
quick_id: 260715-g98
status: passed
verified: 2026-07-15
method: build + IT + 라이브 API 실측 + Playwright 렌더
---

# Quick 260715-g98 VERIFICATION

**결과: ✅ PASSED**

## must_haves 대조

| truth | 검증 | 결과 |
|---|---|---|
| 재료 그룹 헤더에 망치 아이콘 | Playwright — "재료" 옆 Hammer 글리프. 각인서는 게임 아이콘 유지 | ✅ |
| 쿠폰이 "시작일 ~ 만료일"로 렌더 | `2026썸머페스타` → **2026.07.01 ~ 2026.08.19** | ✅ |
| 시작일 없는 기존 쿠폰은 지어내지 않고 "~ 만료일" | `2026로아온썸머감사선물` → **~ 2026.09.16** (API `startsAt: null`) | ✅ 실증 |
| starts_at 추가가 기존 행을 안 깬다 | Flyway `now at version v8`, 기존 쿠폰 그대로 조회·렌더. nullable·백필 없음 | ✅ |
| 역순 기간은 400 | `postWithStartsAtAfterExpiresAtReturns400Contract` 그린(@AssertTrue). 어드민 폼도 `max={expiresAt}`로 선차단 | ✅ |

## 빌드/테스트

- `./gradlew build` → **BUILD SUCCESSFUL** (전체 스위트).
- `cd frontend && npm run build` → tsc -b + vite **그린**.
- `AdminCouponControllerIT` +5 케이스 그린(왕복·null 허용·역순 400·경계 허용·replace 지우기).

## 라이브 증거

```
GET /api/coupons
{"code": "2026썸머페스타",        "startsAt": "2026-07-01", "expiresAt": "2026-08-19"}
{"code": "2026로아온썸머감사선물", "startsAt": null,         "expiresAt": "2026-09-16"}
```

화면(1280px): 쿠폰 섹션 2행 = `2026.07.01 ~ 2026.08.19` / `~ 2026.09.16`, 좌측 nav = `각인서 [게임
아이콘]` · `재료 [망치]`. 한글 깨짐 없음. 스크린샷 `quickC-hammer-coupon-period.png`.

## 비고

- **운영 반영 시**: V8이 배포와 함께 실행되어 `starts_at`이 추가된다. 기존 운영 쿠폰은 `~ 2026.09.16`로
  보이다가, 관리자가 어드민에서 시작일을 채우면 즉시 기간으로 바뀐다.
- 아이콘 출처가 그룹마다 다르다(각인서=CDN 이미지, 재료=lucide 글리프). 데이터가 대표 아이콘을
  공급할 수 있는지에 따른 것이며, `groupIconUrl`/`GROUP_GLYPH` 한 규칙으로 표현된다.
