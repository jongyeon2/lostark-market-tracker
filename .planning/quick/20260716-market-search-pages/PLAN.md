---
task: 아바타·모험의 서 실시간 시세 검색 페이지 2개
quick_id: 260716-o5k
date: 2026-07-16
status: in-progress
requirements: [신규 — 거래소 카테고리 확장]
---

# PLAN — 아바타·모험의 서 실시간 검색 페이지

## 핵심 결정: 저장 안 함 (실시간 프록시)

시계열 미저장·새 테이블 0·마이그레이션 0. 이유: (1) UI가 "검색"이라 실시간과 맞음, (2) 아바타·모험의서는
이벤트 상관 대상이 아님(헤드라인은 각인서·재료·보석), (3) 수집기 부하 0 → Core Value 안전.
설계 원형 = **`GemService` 패턴**(온디맨드 + 공유 토큰버킷 + Redis 5분 캐시).

## 실측 확정 사실 (전부 라이브 API 검증)

- 모험의서 `CategoryCode=100000` (140개). 아바타 상위 `20000`, 부위 10개:
  20005무기 20010머리 20020얼굴1 20030얼굴2 20050상의 20060하의 20070상하의세트 21400악기 21500아바타상자 21600이동효과
- 직업 30개 = `GET /markets/options`의 `Classes` 배열 (버서커…가디언나이트)
- `POST /markets/items`: `CategoryCode` `ItemName` `CharacterClass` `PageNo` `Sort` `SortCondition`,
  PageSize=10 고정. 응답 = PageNo/PageSize/TotalCount/Items[]. Item = CurrentMinPrice/RecentPrice/
  YDayAvgPrice/Name/Grade/Icon/Id/BundleCount.
- 정렬: `Sort` ∈ {CURRENT_MIN_PRICE, RECENT_PRICE}, `SortCondition` ∈ {ASC(낮은순), DESC(높은순)}.
  ⚠️ **잘못된 값은 API가 200 주고 조용히 무시** → 백엔드 화이트리스트 필수.
- CharacterClass 한글은 **파일로 UTF-8 전달**해야 API가 받음(셸에서 깨지면 필터 실패).
- `20000`(상위)+CharacterClass면 전 부위 한 번에(바드 1279). 부위별은 20005 등 + CharacterClass.

## 사용자 결정

- 정렬 기준 **2개만**: 최저가·최근거래가 × 높은/낮은순.
- 아바타 **직업 선택 필수**(미선택 시 안내). 부위 사이드바는 직업 후 좁히기(전체 빼고 10개).
- 페이지네이션 = **이전/다음 + 페이지 번호**(TotalCount로 총페이지).

## 기존 코드 사실 (재사용/주의)

- `LostarkApiClient.searchMarketItems(cat, name)` 존재하나 **Sort/SortCondition/CharacterClass/PageNo
  하드코딩**(CURRENT_MIN_PRICE/ASC/PageNo1) + 반환 DTO `MarketItem`이 **RecentPrice/Grade/Icon 무시**.
  → 이 메서드 재사용 불가. 새 메서드 `searchMarket(params)` + 새 DTO 필요.
- `RedisTokenBucket.tryAcquire()` — 공유 버킷(CAPACITY=90, REFILL=90/60). GemPriceFetcher가 쓰는 그것.
- `GemService`: Redis cache-aside, TTL 5분, fail-open(Redis 죽어도 페이지 안 죽음).
- 컨트롤러는 `web` 패키지(GemController.java = web/). RestController + `/api/...`.
- `ApiExceptionHandler` 존재 — 400 등 공통 처리.
- 프론트: `lib/queries.ts`(TanStack), `lib/schemas.ts`(zod), `lib/api.ts`(fetch),
  `components/ui/{select,input,button,card,badge,skeleton}.tsx`, `AsyncBoundary`, `formatKst`.

## 백엔드 산출물 (`market` 패키지 신규)

1. `LostarkApiClient`에 **오버로드 추가** `searchMarket(String categoryCode, String characterClass,
   String itemName, int pageNo, String sort, String sortCondition)` → 새 응답 타입. 기존 메서드·에러
   택소노미 그대로. body는 기존 text-block 방식(CharacterClass는 빈 값이면 필드 생략 or "").
   ⚠️ 한글 CharacterClass가 RestClient body에 UTF-8로 실리는지 IT로 확인.
2. `market/dto/MarketSearchItem.java` — Id/Name/Grade/Icon/CurrentMinPrice/RecentPrice/YDayAvgPrice.
   `market/dto/MarketSearchResponse.java` — PageNo/PageSize/TotalCount/Items.
3. `market/MarketSearchService.java` (@Service) — GemService 패턴:
   - `rateLimiter.tryAcquire()` 먼저(실패 시 429 성격 → 예외 or 빈+상태). Gem처럼 조용히 다루되
     컨트롤러가 503/재시도 안내. **공유 버킷 반드시 통과**(D-03).
   - 정렬 화이트리스트: `min_price`→CURRENT_MIN_PRICE, `recent_price`→RECENT_PRICE, `asc/desc`→ASC/DESC.
     그 외 → IllegalArgumentException → 컨트롤러 400.
   - Redis 캐시 키 = `market:{cat}:{class}:{part}:{q}:{sort}:{dir}:{page}`, TTL 5분, fail-open.
   - 응답 DTO로 매핑. CurrentMinPrice null이면 그대로 null(값 안 지어냄, GemPrice 선례).
4. `market/ClassCatalogService.java` (또는 서비스 내 메서드) — `GET /markets/options`의 Classes를
   Redis 장기 캐시(예: 6h). 직업 목록은 거의 안 변함.
5. `web/MarketController.java`:
   - `GET /api/market/classes` → 직업 30개
   - `GET /api/market/adventure?q=&sort=&dir=&page=` → cat 100000 고정
   - `GET /api/market/avatar?class=&part=&q=&sort=&dir=&page=` → class **필수(없으면 400)**,
     part 부위코드 화이트리스트(없으면 20000 상위=전 부위), cat=part or 20000
   - sort/dir 기본값(min_price/asc), page 기본 1. 잘못된 값 400.

## 프론트 산출물

6. `lib/schemas.ts` — `marketSearchResponseSchema`, `marketClassesSchema` (zod).
7. `lib/queries.ts` — `useMarketSearch(kind, params)`, `useMarketClasses()`.
8. `features/market/` 신규:
   - `MarketResultList.tsx` — 아이콘·이름·등급배지·최저가🪙·최근거래가. 공용.
   - `SortSelect.tsx` — 최저가↑↓·최근거래가↑↓ (select 2기준 + 방향, 또는 4옵션).
   - `Pagination.tsx` — 이전/다음 + 번호(현재/총).
   - `AdventurePage.tsx` — 검색바(디바운스 300ms)+정렬+목록+페이지.
   - `AvatarPage.tsx` — 직업 select(30)+부위 사이드바(10)+검색+정렬+목록+페이지. 직업 미선택 안내.
   - `useDebouncedValue.ts` — 검색 디바운스 훅.
9. `main.tsx` — `/adventure`·`/avatar` 라우트. `TopNav` navItems 4개로.

## 레이트리밋 안전

캐시(5분)+디바운스(300ms)+공유버킷 → 수집기 예산 잠식 없음. 검색 1회 = 최대 1 API콜(캐시 미스 시).

## 검증

**백엔드**: IT(Testcontainers, LostarkApiClient @MockitoBean) — 정렬 화이트리스트, 잘못된 sort/dir/
class 400, 캐시 히트 시 API 미호출, tryAcquire 호출됨. 한글 CharacterClass가 body에 실림. `./gradlew build` 그린.
**프론트**: `npm run build` 그린. Playwright 라이브(dev+VITE_API_TARGET=배포백엔드):
직업선택→부위→검색→정렬방향바뀜→페이지이동 실동작, 한글 OK, 375px, 디바운스 확인.

## 진행 순서 (원자적 커밋)

1. 백엔드 DTO+client 메서드+service+controller → IT → `./gradlew build` → 커밋
2. 프론트 schemas+queries+컴포넌트+페이지+라우트 → build → Playwright → 커밋
3. STATE 등록

## 하지 않는 것

- 시계열 저장·테이블·마이그레이션 — 0
- 기존 `searchMarketItems`·`MarketItem`·수집 경로 — 무변경
- 아바타 전수 캐시/수집 — 안 함(온디맨드만)
- 푸시 — 사용자
