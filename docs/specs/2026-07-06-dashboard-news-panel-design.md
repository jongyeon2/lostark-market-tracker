# 대시보드 뉴스 패널 (이벤트 + 공지) — 설계 스펙

**작성:** 2026-07-06
**상태:** 설계 확정 (구현 대기)
**대상 마일스톤:** v1.3 (관리자 콘솔 + 실데이터) — Phase 18 배포 전 삽입 예정 (Phase 17.2)

## 1. 개요 / 목표

대시보드 물품 카드(`max-w-3xl`)를 좁히면서 생긴 우측 여백에, 로스트아크 **진행중 이벤트**와 **공지사항**을 표 형식으로 보여주는 사이드바 패널을 추가한다. 데이터는 이미 사용 중인 **로아 공식 API**(`/news/events`, `/news/notices`)에서 가져오며, 프로젝트의 수집→캐시→서빙 파이프라인 패턴을 그대로 재사용한다.

**쿠폰코드는 이 스펙의 범위 밖**이다 — 공식 API에 없으므로 이후 관리자 콘솔 수동 입력으로 별도 처리(다음 단계).

## 2. 범위 / 비범위

**In scope**
- 로아 공식 `/news/events`(진행중 이벤트) + `/news/notices`(공지사항) 소싱
- 저빈도 스케줄 폴러 → Redis 캐시(TTL) → `GET /api/news` read 엔드포인트
- 대시보드 우측 사이드바 `NewsPanel`(이벤트 표 + 공지 표), lg 2-column·모바일 세로 스택
- 로딩/빈/에러 상태(기존 `AsyncBoundary` 재사용), 로아 공식 링크 새 탭

**Out of scope (비범위)**
- **쿠폰코드** — 공식 API 부재, 이후 관리자 수동 입력(별도 작업)
- 경매장/보석 등 다른 데이터 소스 (v2 유지)
- 뉴스의 PostgreSQL 시계열 적재 (뉴스는 휘발성 표시 데이터 — Redis 캐시로 충분)
- 수집(`PriceCollector`)/가격 캐시/event-impact 로직 변경 (Core Value 가드 — 0줄)
- 썸네일 이미지 표시(1차 텍스트 표 중심, 후속 개선 여지)

## 3. 아키텍처

Core Value 경로(수집/가격 캐시/event-impact)와 **완전히 분리된 별도 read-path**. 각 유닛은 단일 책임.

### 백엔드 — 신규 패키지 `com.lostark.tracker.news`

| 유닛 | 책임 | 인터페이스 | 의존 |
|------|------|-----------|------|
| `LostarkNewsClient` | 로아 `/news/events`·`/news/notices` GET 호출, 원시 응답 반환 | `List<RawEvent> fetchEvents()`, `List<RawNotice> fetchNotices()` | RestClient, `lostark.api.base-url`/`key` (기존 설정 재사용) |
| `NewsService` | 폴러가 채운 Redis 캐시 관리 + 조회 제공 | `NewsSnapshot getLatest()`, `void refresh()` | `LostarkNewsClient`, `StringRedisTemplate`/`RedisTemplate` |
| `NewsPoller` | `@Scheduled` 저빈도(기본 6h + 부팅 시 1회) `NewsService.refresh()` 호출 | (스케줄 트리거) | `NewsService` |
| `NewsController` | `GET /api/news` → 캐시 스냅샷 서빙 | `NewsResponse getNews()` | `NewsService` |

- **키 안전:** 키는 서버 env only(`LOSTARK_API_KEY`) — 기존 클라이언트와 동일. 응답/로그에 키 미기재.
- **레이트리밋:** 6h당 2요청 → 마켓 수집(분당 100회, 10분 케이던스) 예산에 사실상 무영향. 뉴스 폴러는 마켓 수집과 독립 스케줄이며 서로 블로킹하지 않는다.

### 프론트 — `frontend/src/features/dashboard/`

| 유닛 | 책임 |
|------|------|
| `useNews()` (`@/lib/queries`) | `GET /api/news` React Query 훅 (staleTime ~5분, 공격적 폴링 없음) |
| `NewsPanel.tsx` | 이벤트 표 + 공지 표 렌더, `AsyncBoundary`로 로딩/빈/에러 감쌈 |
| `DashboardPage.tsx` | lg에서 2-column(좌 물품 / 우 NewsPanel), 모바일 세로 스택 |
| `schemas.ts` | `newsResponseSchema`(zod) — boundary loud-fail(기존 패턴) |

## 4. 데이터 흐름

```
NewsPoller(@Scheduled 6h)
  → NewsService.refresh()
     → LostarkNewsClient.fetchEvents()/fetchNotices()  (로아 공식 API)
     → 정규화(RawEvent/RawNotice → EventDTO/NoticeDTO)
     → Redis SET news:latest = {events, notices, updatedAt} (TTL 12h)
GET /api/news
  → NewsService.getLatest() → Redis GET news:latest
  → NewsResponse{events, notices, updatedAt}
프론트 useNews() → NewsPanel 표 렌더
```

- 폴러 실패 시(외부 API 4xx/5xx/타임아웃): **마지막 캐시 유지**, `updatedAt` 갱신 안 함, 경고 로그. read 엔드포인트는 계속 마지막 스냅샷을 서빙(정직성 — 프론트는 `updatedAt`로 신선도 판단 가능).
- 캐시가 아직 비었으면(부팅 직후 첫 폴 전) 빈 배열 + `updatedAt=null` → 프론트 빈/로딩 상태.

## 5. 데이터 계약

**로아 API 응답 필드는 §11 필드 검증 스파이크로 실측·잠금**(spike-then-lock). 아래는 문서 기준 예상 형태.

- `/news/events` 항목: `{ Title, Thumbnail, Link, StartDate, EndDate, RewardDate }`
- `/news/notices` 항목: `{ Title, Date, Link, Type }` (Type 예: 공지/점검/상점/이벤트)

**정규화 DTO (프론트 계약):**
```
EventDTO   { title: string, link: string, startDate: string(ISO), endDate: string(ISO), thumbnail?: string }
NoticeDTO  { title: string, link: string, date: string(ISO), type: string }
NewsResponse { events: EventDTO[], notices: NoticeDTO[], updatedAt: string(ISO) | null }
```
- 이벤트 정렬: 종료 임박순(EndDate ASC), 최대 ~6개.
- 공지 정렬: 최신순(Date DESC), 최대 ~6개, 전 타입.

## 6. 레이아웃 / 상호작용

- **DashboardPage(lg+):** 컨텐츠 `max-w-6xl`, `lg:grid lg:grid-cols-[minmax(0,1fr)_20rem] lg:gap-8`. 좌 = 기존 물품 섹션(각인→재료), 우 = `NewsPanel`(≈320px). 기존 단독 `max-w-3xl`은 그리드가 폭을 제어하므로 제거(좌 컬럼이 ≈800px로 자연 제한 → 시선 이동 개선 유지).
- **모바일(< lg):** 단일 컬럼, 물품 아래 `NewsPanel` 세로 스택.
- **NewsPanel:** 상단 이벤트 섹션 + 하단 공지 섹션(각 경량 제목). 표 행: 이벤트=제목·기간, 공지=타입 뱃지·제목·날짜. 행 클릭 → 로아 공식 `Link` **새 탭**(`target="_blank" rel="noopener noreferrer"`).
- 스타일: 기존 `Card`/타이포/8pt 스페이싱 재사용. non-sticky(사용자 선택).

## 7. 상태 처리 (정직성)

- **로딩:** `AsyncBoundary` pending(스켈레톤).
- **에러:** `AsyncBoundary` error + '다시 불러오기'(read 엔드포인트 실패 시). 단, 캐시가 있으면 정상 데이터 + 필요 시 신선도 표기.
- **빈:** 이벤트/공지 0건 → 차분한 빈 카피('진행중 이벤트가 없어요' 등).
- **신선도(선택):** `updatedAt`이 오래되면(예: >12h) 작은 '업데이트 지연' 표시.

## 8. 캐싱 전략

- Redis 키 `news:latest`(JSON), TTL 12h(폴 주기 6h의 2배 — 폴 1회 실패해도 캐시 생존).
- 폴 성공 시 덮어쓰기 + `updatedAt=now`. 실패 시 유지.
- PostgreSQL 미사용(시계열 아님).

## 9. Core Value 가드

- `PriceCollector`·`CollectionConfig`·가격 캐시·`EventImpactService`·`WatchlistSeeder`·`SyntheticDemoData` **0줄**.
- 뉴스는 완전 독립 패키지·독립 스케줄·독립 Redis 키. 마켓 수집 틱과 자원 경쟁 없음(6h당 2요청).
- 프론트는 백엔드 `GET /api/news`만 소비 — 로아 API 직접 호출 금지 가드 유지.

## 10. 테스트 전략

- **`LostarkNewsClient`**: 스텁 서버(WireMock 또는 mock RestClient)로 events/notices 파싱 검증.
- **`NewsService`**: 캐시 set/get + 폴 실패 시 캐시 유지 로직 IT (Testcontainers Redis, 기존 `PostgresRedisContainers` 재사용).
- **`NewsController`**: `GET /api/news`가 캐시 스냅샷을 `NewsResponse`로 반환 IT.
- **레이트리밋/독립성**: 폴러가 마켓 수집과 독립임을 구조로 보장(리뷰 확인).
- **프론트**: `npm run build` 그린 + `NewsPanel` 상태 매핑(pending/empty/error/success) 수동 확인.

## 11. 필드 검증 스파이크 (spike-then-lock)

Phase 12/17.1 동형으로, 구현 전 `/news/events`·`/news/notices` 실 응답 필드명을 **라이브 1회 실측**해 DTO 매핑을 잠근다(기존 `MarketsApiSpikeTest` 하니스 확장, `@Disabled`·spike 프로파일·키 안전 계승, 공개 메타만 출력). 결과를 `NEWS-SPIKE-FINDINGS.md`(또는 스펙 부록)에 기록. 이벤트/공지 필드는 저위험이라 스파이크는 경량.

## 12. 결정 / 미해결

- **폴 주기 6h**(config로 조정 가능) — 확정.
- **표시 개수** 이벤트/공지 각 ~6 — 확정(조정 여지).
- **썸네일 미표시**(1차 텍스트 표) — 확정, 후속 개선 여지.
- **신선도 표기**는 선택 구현(우선순위 낮음).

## 13. GSD 편입

quick task가 아닌 백엔드+프론트 신규 기능이므로, 본 스펙을 근거로 **Phase 17.2 삽입**(ROADMAP, Phase 18 배포 앞) 후 `/gsd-plan-phase 17.2`로 정식 분해한다. 요구사항 신규 ID 예: `NEWS-01`(이벤트/공지 소싱·캐시·서빙), `NEWS-02`(대시보드 패널·레이아웃), `NEWS-03`(정직성 상태·Core Value 가드).
