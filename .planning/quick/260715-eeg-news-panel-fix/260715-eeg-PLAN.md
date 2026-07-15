---
quick_id: 260715-eeg
slug: news-panel-fix
date: 2026-07-15
mode: quick-full (--validate)
description: 소식 패널 교정 — 만료 이벤트 필터 + 라벨/쿠폰 표시 정리 (요청 3번·4번)
must_haves:
  truths:
    - "종료된 이벤트(endDate < now KST)는 진행중인 이벤트 목록에 절대 노출되지 않는다."
    - "만료 필터는 MAX_ITEMS cap보다 먼저 적용된다 — 만료분이 6칸을 잠식하면 진행중 이벤트가 밀려난다."
    - "endDate가 없거나 파싱 불가한 이벤트는 남긴다(임의 제거 금지 — 상시 이벤트 보호)."
    - "만료 판정은 KST 벽시계 기준이다 — endDate는 오프셋 없는 KST 로컬 문자열이라 UTC로 읽으면 9시간 어긋난다."
    - "쿠폰의 reward는 화면에서만 사라진다 — DB 컬럼·어드민 입력·API 응답은 그대로다."
  artifacts:
    - src/main/java/com/lostark/tracker/news/NewsDtos.java
    - src/main/java/com/lostark/tracker/news/LostarkNewsClient.java
    - src/main/java/com/lostark/tracker/news/NewsService.java
    - src/test/java/com/lostark/tracker/news/NewsEventOngoingTest.java
    - frontend/src/features/dashboard/NewsPanel.tsx
  key_links:
    - src/main/java/com/lostark/tracker/web/CouponController.java  # KST 만료 필터 선례(ZoneId.of("Asia/Seoul"))
    - src/test/java/com/lostark/tracker/news/NewsServiceIT.java
---

# Quick 260715-eeg — 소식 패널 교정 (요청 3번 + 4번)

## 배경 (실측)

운영 `GET /api/news`를 2026-07-15에 조회한 결과, **7/8에 종료된 이벤트 2건이 목록 최상단**에 있었다:

```
2026-06-24T06:00 ~ 2026-07-08T06:00  (종료됨)   ← 최상단
2026-04-29T06:00 ~ 2026-07-08T06:00  (종료됨)   ← 2번째
2026-06-24T06:00 ~ 2026-07-22T05:59  (진행중)
```

원인: `LostarkNewsClient.fetchEvents()`가 `endDate` **오름차순(종료임박순) 정렬 + 6건 cap**만 하고
만료 이벤트를 거르지 않는다. 정렬이 종료임박순이라 **만료된 이벤트가 오히려 앞자리를 차지**해
진행중 이벤트를 cap 밖으로 밀어낸다. 폴링 자체는 정상(7/8 시작 이벤트가 들어와 있음).

## 결정

**필터를 수집 시점과 응답 시점 양쪽에 건다** — 단일 술어를 공유한다.

- **수집 시점(`fetchEvents`)은 필수**: cap이 여기 있어서, 필터가 cap보다 먼저 오지 않으면
  만료분이 6칸을 먹고 진행중 이벤트가 애초에 캐시에 들어오지 못한다.
- **응답 시점(`getLatest`)은 안전망**: `news:latest`는 TTL 12h · 폴링 6h라, 캐시에 담길 당시엔
  진행중이던 이벤트가 다음 폴링 전에 종료될 수 있다. 그 구간(최대 6h) 동안 종료된 이벤트가
  "진행중인 이벤트"로 노출되는 걸 막는다.

**KST 함정**: `endDate`는 오프셋 없는 KST 벽시계 문자열(`"2026-07-08T06:00:00"`)이다.
`Instant.now()`와 비교하면 9시간 어긋난다 → `LocalDateTime.now(ZoneId.of("Asia/Seoul"))` 기준으로 판정한다.
`CouponController`(KST 만료 필터)의 선례를 따른다.

## Tasks

### Task 1 — 만료 이벤트 필터 (백엔드, TDD)

**files:**
- `src/main/java/com/lostark/tracker/news/NewsDtos.java` (수정)
- `src/main/java/com/lostark/tracker/news/LostarkNewsClient.java` (수정)
- `src/main/java/com/lostark/tracker/news/NewsService.java` (수정)
- `src/test/java/com/lostark/tracker/news/NewsEventOngoingTest.java` (신규)

**action:**
1. **실패 테스트 먼저** — `NewsEventOngoingTest`: 종료된 이벤트 제외 / 진행중 유지 /
   `endDate` 경계(정확히 now = 진행중) / `endDate` null·blank 유지 / 파싱 불가 유지.
2. `NewsDtos.NewsEvent`에 `isOngoingAt(LocalDateTime nowKst)` 추가 — `endDate`가 null/blank/파싱불가면
   `true`(남김), 아니면 `!parse(endDate).isBefore(nowKst)`.
3. `LostarkNewsClient.fetchEvents()` — `.filter(e -> e.isOngoingAt(LocalDateTime.now(KST)))`를
   **`.sorted(...)` · `.limit(MAX_ITEMS)`보다 먼저** 삽입. `KST` 상수 추가.
4. `NewsService.getLatest()` — 캐시에서 읽은 스냅샷의 events에 같은 술어 재적용.
5. 클래스 javadoc 갱신(만료 제외 계약 명시).

**verify:** `./gradlew test --tests 'com.lostark.tracker.news.*'` 그린
**done:** 만료 이벤트가 cap 이전에 제거되고, 캐시된 만료분도 응답에서 빠진다.

### Task 2 — 라벨 + 쿠폰 보상 표시 정리 (프론트)

**files:**
- `frontend/src/features/dashboard/NewsPanel.tsx` (수정)

**action:**
1. `진행중 이벤트` → `진행중인 이벤트` (`SectionHeading`), 빈 상태 문구
   `진행중 이벤트가 없어요` → `진행중인 이벤트가 없어요`.
2. 쿠폰 행에서 `{coupon.reward} · ` 제거 → 만료일만 남긴다. `Coupon` 타입·API·어드민은 무변경.
3. 주석의 쿠폰 행 계약 설명(`code(강조)·reward·만료일`)을 실제와 일치시킨다.

**verify:** `cd frontend && npm run build` 그린
**done:** 화면에 "진행중인 이벤트", 쿠폰 행은 코드 + 만료일만.

### Task 3 — 검증 체크포인트

**name:** 빌드 + 라이브 렌더 검증
**action:** `./gradlew build` · `npm run build` 그린 확인 후, 로컬 풀스택(postgres/redis + backend 8080 + vite 5173)에
Playwright로 접속해 소식 패널의 한글 렌더(깨짐 없음)·"진행중인 이벤트" 라벨·쿠폰 행(코드+기한만)·
만료 이벤트 부재를 육안 확인. 빌드 오류 시 로그 분석 후 수정하고 재시도.
**verify:** 스크린샷 + Playwright 스냅샷
**done:** 의도대로 렌더되고 빌드가 그린.

## Core Value 가드

수집(`PriceCollector`)·캐시(`cache/`)·event-impact·스키마 **0줄**. 뉴스는 Redis 전용 표시 데이터로
가격 파이프라인과 독립(D-06).
