---
phase: 13-backend-enrichment-seed
plan: 02
subsystem: api
tags: [spring-web, dto, redis-cache, read-path, testcontainers]

requires:
  - phase: 13-backend-enrichment-seed
    provides: "tracked_item enrichment(icon_url/item_group/role_group) + TrackedItem getter, 큐레이션 15개 seed"
provides:
  - "4개 read 응답이 iconUrl/itemGroup/roleGroup 노출 — item list / latest / timeline / event-impact"
  - "LatestPriceService MISS 분기가 enrichment를 캐시에 베이크(HIT zero-DB 보존)"
  - "EnrichedEventImpactResponse wrapper — EventImpactService/Response 0줄로 enrichment 부착"
  - "ItemEnrichmentReadIT — 4응답 노출 + HIT 보존 + ITEM-04 잠금 회귀"
affects: [14-frontend-icon]

tech-stack:
  added: []
  patterns:
    - "잠긴 DTO는 wrapper로 감싸고(EnrichedEventImpactResponse), 자유로운 DTO는 제자리 확장 — read-path additive 한 겹"
    - "MISS 시 enrichment를 캐시 값에 베이크 → HIT는 추가 DB 조회 0(API-02 zero-DB 보존)"
    - "existsById→findById 치환(읽기 1회 동일 비용)으로 검증 순서(400-우선-404) 보존하며 엔티티 확보"

key-files:
  created:
    - src/main/java/com/lostark/tracker/web/dto/EnrichedEventImpactResponse.java
    - src/test/java/com/lostark/tracker/web/ItemEnrichmentReadIT.java
  modified:
    - src/main/java/com/lostark/tracker/web/dto/TrackedItemResponse.java
    - src/main/java/com/lostark/tracker/web/dto/LatestPriceResponse.java
    - src/main/java/com/lostark/tracker/read/LatestPriceService.java
    - src/main/java/com/lostark/tracker/web/dto/TimelineResponse.java
    - src/main/java/com/lostark/tracker/web/PricesController.java
    - src/main/java/com/lostark/tracker/web/EventImpactController.java

key-decisions:
  - "latest enrichment는 MISS 시 캐시에 베이크 — HIT 분기 무변경으로 zero-DB(API-02) 보존"
  - "event-impact는 신규 EnrichedEventImpactResponse 컨트롤러 wrapper로 부착 — EventImpactService/EventImpactResponse 0줄(ITEM-04)"
  - "ITEM-04 잠금: PriceCollector/RedisTokenBucket/LatestPriceCache/CacheConfig/EventImpactService/WindowQueryService/DownsampleService/EventImpactResponse 8파일 0줄 — git diff 게이트 + 회귀 그린으로 강제"

patterns-established:
  - "DTO 컴포넌트 추가는 Jackson record 직렬화·CacheConfig 자동 처리 — 캐시 인프라 0줄"

requirements-completed: [ITEM-03, ITEM-04, SEED-03]

duration: ~25 min
completed: 2026-06-29
---

# Phase 13 Plan 02: Read-path Enrichment Passthrough Summary

**4개 read 응답(item list/latest/timeline/event-impact)이 iconUrl/itemGroup/roleGroup을 노출하되, latest는 enrichment를 캐시에 베이크해 HIT zero-DB를 보존하고 event-impact는 컨트롤러 wrapper로 감싸 — 수집/캐시/event-impact 계산 8파일이 0줄(ITEM-04), 전체 회귀 그린.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-06-29
- **Tasks:** 3
- **Files modified:** 8 (2 created, 6 modified)

## Accomplishments
- item list + latest — TrackedItemResponse.from()과 LatestPriceResponse에 enrichment 3필드 추가. LatestPriceService MISS 분기를 existsById→findById로 바꿔(읽기 1회 동일) enrichment를 채운 응답을 캐시에 베이크 → HIT 분기 무변경으로 zero-DB(API-02) 보존. LatestPriceCache/CacheConfig/ItemController 0줄.
- timeline 제자리 확장 — TimelineResponse에 enrichment 3필드, PricesController가 findById로 채움(400-우선-404 검증 순서 보존).
- event-impact wrapper — 신규 EnrichedEventImpactResponse를 EventImpactController가 조립(서비스의 EventImpactResponse를 호출 무변경으로 감쌈) → EventImpactService/EventImpactResponse 0줄.
- ItemEnrichmentReadIT — Testcontainers(키 없음)에서 4응답 enrichment 노출 + latest 2회차 HIT가 동일 enrichment 반환 단언. ITEM-04 잠금 8파일 0줄은 git diff 게이트 + 전체 회귀 그린으로 강제.

## Task Commits

1. **Task 1: item list + latest enrichment (캐시 베이크)** - `f7a73a0` (feat)
2. **Task 2: timeline 제자리 확장 + event-impact wrapper** - `22f8947` (feat)
3. **Task 3: ItemEnrichmentReadIT + ITEM-04 회귀 단언** - `6c8b676` (test)

## Files Created/Modified
- `web/dto/EnrichedEventImpactResponse.java` (신규) - event-impact 컨트롤러 wrapper(itemId/window/enrichment/events)
- `web/dto/TrackedItemResponse.java` - enrichment 3필드 + from() 매핑
- `web/dto/LatestPriceResponse.java` - enrichment 3필드(캐시 베이크 대상)
- `read/LatestPriceService.java` - MISS 분기 findById + enrichment 캐시 베이크(HIT 무변경)
- `web/dto/TimelineResponse.java` - enrichment 3필드(최상위 메타데이터)
- `web/PricesController.java` - findById로 timeline enrich(검증 순서 보존)
- `web/EventImpactController.java` - EnrichedEventImpactResponse로 wrap(서비스 무변경)
- `web/ItemEnrichmentReadIT.java` (신규) - 4응답 노출 + HIT 보존 IT

## Decisions Made
- latest enrichment를 MISS 시 캐시에 베이크 — HIT 분기 절대 무변경으로 API-02 zero-DB 보존.
- event-impact는 wrapper DTO로 부착 — EventImpactService/EventImpactResponse 생성자 호출 무파손(ITEM-04 잠금).
- existsById→findById 치환으로 검증 순서(400-우선-404) 보존하며 엔티티(enrichment) 확보, 읽기 비용 동일.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required (키 불필요).

**SEED-03 라이브 확인(선택, 수동):** 자동 증명은 WatchlistSeederIT(키 없이 seed가 enrichment를 DB에 기록) + ItemEnrichmentReadIT(read 응답이 enrichment 패스스루)로 실제 Postgres(Testcontainers)에서 이미 완료됨. 추가로 라이브 end-to-end를 직접 보려면(선택):
`docker compose up -d postgres redis` → `./gradlew bootRun --args='--spring.profiles.active=seed'` → `curl -s localhost:8080/api/items | jq '.[0]'` 의 `iconUrl`이 `https://cdn-lostark.game.onstove.com/efui_iconatlas/use/`로 시작하는지 확인. (LOSTARK_API_KEY 불필요.)

## Next Phase Readiness
- Phase 14(프론트 아이콘): iconUrl/itemGroup/roleGroup DTO 계약이 4응답에 확정됨. roleGroup ∈ {MATERIAL,DEALER,SUPPORT} 배지·동일 아이콘 라벨 병기·fallback을 zod 스키마와 정렬해 소비.
- Core Value 보존 증명: 수집/캐시/event-impact 8파일 0줄 + 전체 회귀 그린 — enrichment는 안전한 read-path additive 한 겹.

---
*Phase: 13-backend-enrichment-seed*
*Completed: 2026-06-29*
