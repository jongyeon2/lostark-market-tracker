---
task: 아바타·모험의 서 실시간 시세 검색 페이지 2개
quick_id: 260716-o5k
status: complete
date: 2026-07-16
---

# SUMMARY — 아바타·모험의 서 검색 페이지

거래소 카테고리 확장. 인게임 거래소를 본떠 **검색으로 시세를 찾는** 페이지 2개(/adventure·/avatar).
저장 없는 실시간 조회 — 시계열 미저장·새 테이블 0·마이그레이션 0. 커밋 3개(백엔드 → camelCase 수정 → 프론트).

## 왜 저장 안 하나 (사용자 결정: 실시간 조회)

이 둘은 이벤트 상관 대상이 아니고(헤드라인은 각인서·재료·보석), 직업당 ~9k 아바타를 저장하면 순수
부하만 는다. `GemService` 패턴(온디맨드 + 공유 토큰버킷 + Redis 5분 캐시)을 그대로 따랐다. 검색 1회 =
최대 1 API콜(캐시 미스 시).

## 산출물

**백엔드** (`market` 패키지 신규 + `LostarkApiClient`/`ApiExceptionHandler` 확장)
- `searchMarket()`/`getMarketOptions()` — 정렬·직업·페이지를 받는 새 메서드. body는 Map+Jackson으로
  직렬화(한글 CharacterClass UTF-8·이스케이프 자동, 기존 text-block 수동 이스케이프 취약점 회피).
  에러 택소노미는 `applyErrorTaxonomy` 헬퍼로 공유.
- `MarketSearchService` — 정렬 화이트리스트, 공유 버킷 `tryAcquire()`, Redis 캐시(fail-open).
- `MarketController` — `/api/market/{classes,adventure,avatar}`. avatar class 필수, 부위 화이트리스트.
- `ApiExceptionHandler` — RateLimitedApiException→429, LostarkApiException→502(온디맨드라 컨트롤러까지 올라옴).

**프론트** (`features/market/` 신규)
- `AdventurePage`/`AvatarPage` + `MarketResultList`/`SortSelect`/`Pagination`/`useDebouncedValue`.
- `schemas.ts`/`api.ts`/`queries.ts` 확장(zod 경계 + TanStack). `TopNav` 4항목.

## 🔑 정렬 화이트리스트가 핵심이었다

실측: 로스트아크 API는 **잘못된 Sort/SortCondition을 200으로 받고 조용히 무시**한다. 검증 안 하면
"정렬했는데 안 바뀐다"가 된다. 백엔드가 `min_price`/`recent_price` × `asc`/`desc`만 받고 나머지는
400. 부위 코드도 서버측 화이트리스트라 임의 카테고리로 못 돌린다.

## 🔑 라이브 검증에서 잡은 버그 2개 (안 돌려봤으면 못 잡음)

**① 응답 PascalCase 누출.** DTO의 `@JsonProperty("PageNo")`가 역직렬화(업스트림)와 직렬화(프론트)
둘 다에 PascalCase를 적용 → 프론트 zod(camelCase)가 `.parse` 거부 → 화면 에러. **`@JsonAlias`로
전환**(입력만 별칭, 출력은 camelCase 필드명). IT에 원본 JSON 키 단언 추가 — 기존 타입 IT는 같은
DTO로 왕복해 이 누출을 못 잡았다(라이브 zod가 잡음).

**② React key 충돌.** 같은 `id`가 한 페이지에 중복(같은 아이템을 다른 가격에 여러 명이 등록 — 실측).
`key`에 index를 섞어 해결.

## 검증

**백엔드**: MarketSearchServiceIT(8) + MarketControllerIT(9, camelCase 키 단언 포함) 전부 통과.
정렬 매핑·화이트리스트 400·캐시 히트 0콜·페이지별 캐시키·throttle 미호출·class 필수·부위 화이트리스트.
`./gradlew build` 그린(무관 IT 커넥션 소진은 max_connections=300으로 완화).

**프론트 라이브** (dev 프론트 + 로컬 새 백엔드 8095 프록시, 실제 로스트아크 API):
- 모험의서: 목록·정렬(높은순 36500→10999→7990)·검색("숨결"→1건)·페이지(1·2·3…14) 실동작
- 아바타: 직업선택(30)→부위전환(무기→머리)→결과·페이지(13) 실동작, 직업 미선택 시 안내
- 한글 정상, 375px 반응형(부위 칩 가로 스크롤, 페이지 가로 스크롤 없음), 콘솔 에러 0, build 그린

레이트리밋: 검증 중 여러 번 429를 받았다(dev 수집기와 예산 경쟁) — 이건 **공유 버킷이 설계대로 동작**한
증거이자, 429가 정직하게 429로 매핑됨을 확인한 것. 캐시+디바운스로 정상 사용 시 예산 잠식 없음.

## 미배포

배포 백엔드는 아직 옛 버전(이 API 없음)이라 검증은 **로컬 새 백엔드**로 했다. 배포하면 실서비스에서
동작한다. 도메인 이전 전 마지막 기능이므로, 배포 시 이 커밋들도 함께 나간다.

## 남은 일

도메인 이전(가비아) — 브랜딩·신규 페이지를 다 얹은 뒤 마지막 단계.
