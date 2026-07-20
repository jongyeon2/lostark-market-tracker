---
quick_id: 260720-lk6
slug: event-impact-filter
description: 이벤트 영향 — 종류 필터·날짜 정렬·개수 제한(서버) + 대시보드 중앙 폭 확대
date: 2026-07-20
---

# Quick Task 260720-lk6 — 이벤트 영향 필터·정렬·개수 제한

## 문제

대시보드에서 물품을 고르면 아래에 **등록된 모든 이벤트**의 영향 카드가 뜬다. 지금은 이벤트가 몇
개뿐이라 티가 안 나지만, 이벤트가 쌓이면 끝없이 스크롤해야 한다.

### 스크롤은 증상이고 병목은 페이로드다

`EventImpactService.eventImpact()`는 `findAllByOrderByOccurredAtDesc()`로 **전체 이벤트**를 가져와
전부 계산하고 **전부 반환**한다. limit도 필터도 없다. 프론트는 받은 걸 다 그리는데, 게다가
`EventImpactTable`(md+)과 `EventImpactCards`(<md)가 **둘 다 DOM에 들어간다**(CSS로 하나만 숨김).

이벤트 1만 건 기준:
- `EventImpactItem`이 11필드(타임스탬프 3 + 제목) ≈ 250~400B → 응답 **3~4MB**
- DOM 서브트리 **2만 개**(테이블 1만 + 카드 1만)

→ 클라이언트에서만 거르면 3~4MB를 받아놓고 거르는 셈이라 절반만 해결된다. **서버에서 거른다**
(사용자 결정). 부수 효과로 타입 필터를 계산 **전에** 적용하므로 스냅샷 범위 조회도 좁아진다.

## 계약 변경 — `GET /api/items/{id}/event-impact`

기존 `window`(필수)에 더해:

| 파라미터 | 기본값 | 검증 |
|---|---|---|
| `types` | 없음(= 전체) | 각 값이 `EventType`이어야 함, 아니면 400 |
| `sort` | `occurred_desc` | `occurred_desc` \| `occurred_asc` 화이트리스트, 아니면 400 |
| `limit` | 50 | 1 이상 `MAX_LIMIT`(200) 이하, 아니면 400 |

응답에 **`totalCount`** 추가 — 필터 적용 후·limit 적용 **전**의 전체 건수. "전체 N건 중 M건"을
정직하게 말하고 "더 보기" 종료 시점을 판단하는 데 쓴다.

🔑 **화이트리스트로 400을 낸다**(조용히 무시 금지). `MarketSearchService`가 정렬 값에 대해 이미
세운 규율이다 — 거기선 업스트림 API가 잘못된 Sort를 200으로 무시해서 "정렬했는데 안 바뀐다"가
생길 수 있었다. 여기도 같다: 모르는 `sort`를 기본값으로 처리하면 화면은 정렬했다고 말하는데
실제로는 안 바뀐다.

🔑 **400 검증은 404보다 먼저**(기존 D-08/D-13 유지). 새 파라미터 검증도 `window` 검증과 같은
자리에 둔다.

## 작업

### 1. 백엔드 — 필터·정렬·limit

- `GameEventRepository`: `findByEventTypeIn(Collection<EventType>, Pageable)` (Page) 추가.
  `findAllByOrderByOccurredAtDesc()`는 **제거**한다 — 유일한 호출자가 이 서비스이고, 남겨두면
  "전량 조회" 경로가 살아있어 같은 실수를 다시 부른다.
  - 타입 필터가 없을 땐 **전체 7종을 넘긴다**. `null` 분기를 만들지 않아 쿼리 경로가 하나로 유지된다.
  - `Page.getTotalElements()`가 `totalCount`를 공짜로 준다.
- `EventImpactService.eventImpact(itemId, windowHours, types, direction, limit)`:
  - 반환에 `totalCount` 포함.
  - **N+1 회피 성질은 그대로**다. 오히려 limit 덕에 `min/max` 범위가 좁아져 스냅샷 단일 범위
    조회가 더 작아진다.
- `EventImpactResponse` / `EnrichedEventImpactResponse`에 `totalCount` 추가.
- `EventImpactController`: 파라미터 3개 + 검증.

### 2. 프론트 — 필터 UI

- `schemas.ts`에 `totalCount` 추가, `api.ts`/`queries.ts`가 파라미터를 실어 보냄.
- `ItemImpactSection`:
  - **종류 필터**: 7종 칩(다중 선택). 색·라벨은 기존 `EVENT_MARKERS`를 그대로 쓴다 — 범례·배지·
    차트 마커와 같은 색이어야 같은 이벤트로 읽힌다. 아무것도 안 고르면 전체.
  - **정렬**: 최신순 / 오래된순 (`SortSelect`는 거래소 전용 vocabulary라 재사용하지 않고 별도).
  - **개수**: 기본 50, `더 보기`로 +50. `전체 N건 중 M건` 항상 표기.
  - 필터·정렬이 바뀌면 limit을 50으로 되돌린다(거래소 페이지가 검색·정렬 변경 시 1페이지로
    돌아가는 것과 같은 이유 — 좁아진 결과에서 늘어난 limit은 의미가 없다).
- `useEventImpact`에 `placeholderData: (prev) => prev` — `더 보기`·필터 전환 시 깜빡임 방지.

### 3. 중앙 컬럼 폭 확대

사용자 결정: 좌우를 좁히고 페이지 폭도 넓힌다.
- `DashboardPage` 그리드 `20rem → 18rem`(좌우 대칭 유지)
- `AppLayout` + `TopNav` `max-w-[100rem] → [110rem]` (**둘은 항상 같아야 한다** — 헤더가 본문과 정렬)
- 결과: 중앙 800px → **1088px**(1760px 화면 기준)

가로 스크롤은 그리드가 `minmax(0,1fr)`이라 화면이 좁아지면 중앙이 줄어들 뿐 페이지가 넘치지 않는다.
실제 위험은 **이벤트 영향 테이블의 최소 폭**(~754px)이라 중앙이 그보다 좁아지는 구간에서 테이블이
자체 스크롤된다 — 폭을 넓히는 이번 변경은 그 여유를 키우는 방향이다.

## 검증

- `./gradlew build` 통과 + 신규 IT: 타입 필터 / 정렬 양방향 / limit 경계 / 잘못된 값 400 /
  `totalCount`가 limit과 무관하게 필터 전체 건수 / 400이 404보다 먼저.
- `npm run build` 통과.
- 라이브: 필터·정렬·더보기 동작, 중앙 폭 확대 후 가로 스크롤 없음(1920·1440·375).

## 범위 밖

- 이벤트 영향의 시간 창(`window`)은 24h 고정 그대로(사용자 결정 2026-07-15).
- 테이블/카드 이중 렌더는 이번에 건드리지 않는다 — limit 50이면 100 서브트리라 문제가 아니다.
