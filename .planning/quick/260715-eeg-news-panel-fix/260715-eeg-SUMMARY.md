---
quick_id: 260715-eeg
status: complete
date: 2026-07-15
commits: [50136b1, 3871c16]
---

# Quick 260715-eeg SUMMARY — 소식 패널 교정 (요청 3번 + 4번)

## 무엇을 했나

**1. 만료 이벤트 필터 (요청 4번, 실버그)** — `LostarkNewsClient.fetchEvents()`가 `endDate`
오름차순(종료임박순) 정렬 + 6건 cap만 하고 만료 이벤트를 거르지 않았다. 정렬이 종료임박순이라
**만료된 이벤트가 오히려 앞자리를 차지**해 진행중 이벤트를 cap 밖으로 밀어내는 구조였다.

- `NewsDtos.NewsEvent.isOngoingAt(LocalDateTime nowKst)` — 진행중 판정을 한 곳에 잠금(양쪽 필터가
  서로 어긋날 수 없다). `nowKst`가 파라미터인 이유: `endDate`는 오프셋 없는 KST 벽시계 문자열이라
  `Instant` 비교였다면 9시간 어긋난다. 경계 INCLUSIVE. `endDate`가 null/blank/파싱불가면 **유지**
  (증명된 만료만 제거 — 상시 이벤트 보호).
- `fetchEvents()` — 필터를 `sorted`·`limit`**보다 먼저**. cap 이후 필터는 무의미하다.
- `NewsService.getLatest()` — 캐시 스냅샷에 같은 술어 재적용. `news:latest`는 TTL 12h·폴링 6h라
  캐시된 뒤 종료된 이벤트가 최대 6h 동안 "진행중"으로 노출되는 구멍이 있었다. 캐시 JSON 자체는
  건드리지 않아 keep-on-failure(D-07) 보장은 그대로.

**2. 라벨 + 쿠폰 행 (요청 3번·4번)** — `NewsPanel.tsx`: "진행중 이벤트" → "진행중인 이벤트"
(헤딩 + 빈 상태 문구), 쿠폰 행에서 `reward` 표시 제거 → 코드 + 만료일만. `Coupon` 타입·API 응답·
어드민 입력·DB 컬럼 전부 무변경(표시만).

## 편차 (계획에 없던 발견)

**`NewsServiceIT` 시한폭탄 제거** — fixture `EVENT`의 `endDate`가 `2026-07-20`이었다. 응답 시점
필터는 **실제 KST 클록**으로 판정하므로, 이 fixture는 2026-07-20이 지나면 저절로 레드가 된다.
`2099-12-31`(far-future)로 교체하고 far-past(`2020-02-01`) 만료 fixture로 read-time 필터 테스트를
추가했다. 내 변경이 5일 뒤 빌드를 깨뜨렸을 것이므로 스코프 확장이 아니라 필수 수반 수정.

## 검증

- **TDD** — `NewsEventOngoingTest` 6케이스를 먼저 작성해 `cannot find symbol`로 레드 확인 후 구현:
  종료 제외 / 미래 유지 / 경계(정확히 now = 진행중) / null·blank 유지 / 파싱불가 유지 /
  **KST를 UTC로 읽으면 뒤집히는 케이스**(06:00 KST는 10:00 KST에 이미 만료, UTC로 읽으면 15:00이 되어
  잘못 생존).
- `./gradlew build` **BUILD SUCCESSFUL** (전체 스위트).
- `npm run build` (tsc -b + vite) **그린**.
- **로컬 /api/news 실측** — 만료 2건 제거 확인. 그 자리에 밀려나 있던 진행중 이벤트 2건
  ("아크 패스 : 창공의 안내자", "와글와글 워터 페스티벌")이 **새로 들어옴** = cap 잠식이 실재했다는 직접 증거.
- **Playwright 라이브 렌더(1280px)** — 한글 깨짐 없음, "진행중인 이벤트" 헤딩, 쿠폰 행
  `2026로아온썸머감사선물 / 2026.09.16 / 복사`(reward 없음), 이벤트 6건 전부 진행중(7/22~8/19).
  스크린샷 `quickB-news-panel.png`.

## Core Value 가드

수집·캐시·event-impact·스키마 **0줄**. 뉴스는 Redis 전용 표시 데이터로 가격 파이프라인과 독립(D-06).

## 커밋

- `50136b1` — 만료 필터(NewsDtos·LostarkNewsClient·NewsService + NewsEventOngoingTest·NewsServiceIT)
- `3871c16` — NewsPanel 라벨 + 쿠폰 행
