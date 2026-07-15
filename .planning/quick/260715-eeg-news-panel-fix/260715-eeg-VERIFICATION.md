---
quick_id: 260715-eeg
status: passed
verified: 2026-07-15
method: TDD + build + 로컬 API 실측 + Playwright 라이브 렌더
---

# Quick 260715-eeg VERIFICATION

**결과: ✅ PASSED**

## must_haves 대조

| truth | 검증 | 결과 |
|---|---|---|
| 종료된 이벤트는 진행중인 이벤트 목록에 노출되지 않는다 | 로컬 `/api/news` 실측 — 6건 전부 진행중(7/22·7/22·7/31·8/19·8/19·8/19), 만료 0건. Playwright 스냅샷 동일 | ✅ |
| 만료 필터가 MAX_ITEMS cap보다 먼저 적용된다 | `fetchEvents()` 코드 순서 `filter → sorted → limit`. **실측 증거**: 만료 2건 제거 후 밀려나 있던 진행중 이벤트 2건이 새로 등장 | ✅ 실증 |
| endDate 없거나 파싱 불가한 이벤트는 남긴다 | `NewsEventOngoingTest.nullOrBlankEndDateIsKept` · `unparseableEndDateIsKept` 그린 | ✅ |
| 만료 판정은 KST 벽시계 기준 | `isOngoingAt(nowKst)` 파라미터 + `ZoneId.of("Asia/Seoul")`. `endDateIsReadAsKstWallClockNotUtc` — UTC로 읽으면 뒤집히는 케이스를 고정 | ✅ |
| 쿠폰 reward는 화면에서만 사라진다 | `git diff` — `Coupon.java`·`CouponRequest/Response`·`AdminCouponService`·어드민 폼·스키마 **0줄**. `NewsPanel.tsx` 표시만 제거 | ✅ |

## 빌드/테스트

- `./gradlew build` → **BUILD SUCCESSFUL** (전체 스위트, 실패 0).
- `cd frontend && npm run build` → tsc -b + vite **그린**.
- TDD 순서 준수: 테스트 선작성 → `cannot find symbol` 레드 확인 → 구현 → 그린.

## 라이브 렌더 (Playwright, 로컬 풀스택 1280px)

- 한글 렌더 **깨짐 없음** (헤더·nav·카드·소식 전부).
- "진행중인 이벤트" 헤딩 확인.
- 쿠폰 행 = `2026로아온썸머감사선물` + `2026.09.16` + `복사` — reward 부재 확인.
- 이벤트 6건 전부 진행중, 만료 0건.
- 증거: `quickB-news-panel.png`

## 비고

- `NewsServiceIT` fixture를 far-future로 교체(2026-07-20 시한폭탄 제거) — SUMMARY 편차 참조.
- 대시보드 좌측 nav의 구분선 부재·"각인" 라벨·강화재료 분리는 **Quick A**의 범위(이 태스크 밖).

**Quick B 완료 → Quick A 착수 가능.**
