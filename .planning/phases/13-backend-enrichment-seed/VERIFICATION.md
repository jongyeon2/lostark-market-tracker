# Phase 13 Verification — Backend Enrichment + Seed Expansion

**Verdict:** ✅ PASS (goal-backward, orchestrator-inline)
**Date:** 2026-06-29
**Plans:** 13-01 (data layer + seed) · 13-02 (read-path passthrough) — both complete
**Build:** `./gradlew build` 그린 (Testcontainers Postgres+Redis, 전체 회귀 포함)

## Phase Goal

스파이크가 잠근 큐레이션 15개의 enrichment(아이콘 URL·품목군·역할군)를 nullable 컬럼·엔티티·시더에 베이크하고 4개 read 응답에 노출하되, 수집·캐시·event-impact 핵심 경로는 한 줄도 건드리지 않고 키 없이 동작한다.

## Truth-by-truth (must_haves)

| # | Truth | Req | Evidence | Status |
|---|-------|-----|----------|--------|
| 1 | V4가 icon_url/item_group/role_group을 전부 NULLABLE 추가, V1–V3 불변, ddl-auto=validate 통과 | ITEM-01 | V4 = 단일 ALTER TABLE 3 ADD COLUMN, NOT NULL/DEFAULT 토큰 0; V1–V3 diff 0줄; build 그린(부팅 validate) | ✅ |
| 2 | TrackedItem이 3필드 매핑 + getter, 3-arg 보존 + 6-arg 추가 | ITEM-02 | TrackedItem.java getIconUrl/getItemGroup/getRoleGroup, 3-arg→6-arg 위임, protected 기본 생성자 보존 | ✅ |
| 3 | WatchlistSeeder가 큐레이션 15개(4/9/2)를 enrichment 멱등 등록 | SEED-01 | WatchlistSeederIT: count 15, role 분포 4/9/2, 표본 enrichment 일치, 2회 멱등 | ✅ |
| 4 | SyntheticDemoData 0줄 변경으로 신규 품목 합성 스냅샷 키 없이 멱등 | SEED-02 | synthetic 소스 diff 0줄; IT가 신규 품목 스냅샷 present + 2회 멱등 단언 | ✅ |
| 5 | seed/watchlist 소스에 키·가격 0건 | SEED-04 | bearer/eyJ grep 0건 | ✅ |
| 6 | 4개 read 응답이 iconUrl/itemGroup/roleGroup 노출 | ITEM-03 | ItemEnrichmentReadIT: item list/latest/timeline/event-impact 4응답 단언 | ✅ |
| 7 | latest는 MISS 시 enrichment 캐시 베이크 → HIT zero-DB, LatestPriceCache/CacheConfig 0줄 | ITEM-03/04 | LatestPriceService MISS만 수정(HIT 무변경); cache/CacheConfig diff 0줄; IT가 2회차 HIT 동일 enrichment 단언 | ✅ |
| 8 | event-impact wrapper로 EventImpactService/EventImpactResponse 0줄 | ITEM-04 | EnrichedEventImpactResponse 신규; service/response diff 0줄 | ✅ |
| 9 | 수집/캐시/event-impact 8파일 0줄 + 회귀 그린 | ITEM-04 | PriceCollector/RedisTokenBucket/LatestPriceCache/CacheConfig/EventImpactService/WindowQueryService/DownsampleService/EventImpactResponse 전 구간(dfce5dd^..HEAD) diff 0줄; 전체 회귀 그린 | ✅ |
| 10 | seed 프로파일만으로 키 없이 응답 enrichment가 스파이크 상수와 일치 | SEED-03 | WatchlistSeederIT(키 없는 seed→enrichment DB 기록) + ItemEnrichmentReadIT(read 응답 패스스루)로 실 Postgres 자동 증명. 라이브 bootRun curl은 선택적 수동 확인(13-02-SUMMARY 기록) | ✅ |

## Requirements

ITEM-01, ITEM-02, ITEM-03, ITEM-04, SEED-01, SEED-02, SEED-03, SEED-04 — 전부 Complete (REQUIREMENTS.md).

## Invariants (Core Value 보존)

- 수집 신뢰성 핵심 경로(PriceCollector/RedisTokenBucket/LatestPriceCache/CacheConfig/EventImpactService) **0줄** — enrichment는 read-path additive 한 겹.
- V1–V3 Flyway 마이그레이션 **0줄** (체크섬 불변).
- 시더·합성기 소스 키·가격 **0건**.
- 전체 빌드(기존 수집/캐시/event-impact 회귀 + 신규 2 IT) 그린.

## Outstanding / Notes

- SEED-03 라이브 `seed` 프로파일 curl은 선택적 수동 확인(메커니즘은 자동 증명 완료). 운영자가 원하면 `docker compose up -d postgres redis` → `bootRun --args='--spring.profiles.active=seed'` → `curl /api/items`로 직접 확인 가능.
- Phase 14(프론트) 합의: DTO 필드명 iconUrl/itemGroup/roleGroup + roleGroup ∈ {MATERIAL,DEALER,SUPPORT} zod 정렬, 동일 아이콘(use_9_25) 라벨 병기 + fallback.

---
*Phase: 13-backend-enrichment-seed · Verified inline (orchestrator) 2026-06-29*
