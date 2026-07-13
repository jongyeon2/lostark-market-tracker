---
quick_id: 260713-mur
slug: watchlistseederit-flaky-clock
date: 2026-07-13
description: WatchlistSeederIT 시점 경계 flaky 수정 — 테스트에서 SyntheticDemoData를 고정 Clock으로 생성
---

# Quick Task 260713-mur: WatchlistSeederIT flaky 수정 (고정 Clock)

## 근본 원인 (CI 로그 + 코드 분석)

CI backend job 실패: `WatchlistSeederIT.syntheticDemoDataSnapshotsNewItemsKeylessAndIdempotent()`
(WatchlistSeederIT.java:135) — `assertThat(count()).isEqualTo(afterFirst)` 실패.

- `SyntheticDemoData.seed()`는 호출마다 `currentTickFloor()`로 **벽시계 `now(clock)`을 10분 그리드로 내림**해 `gridNow`를 새로 계산하고, 스냅샷을 `gridNow.minus(TICK*i)`에 적재하며 `existsByTrackedItem_IdAndCollectedAt`로 멱등 가드.
- 첫 `seed()`는 22품목 × 1152틱 ≈ **25k 인서트라 수십 초** 소요. 첫 호출과 둘째 호출의 `gridNow` 계산 시각이 **10분 경계를 넘으면** `gridNow`가 10분 밀려 타임스탬프 집합이 달라지고, 가드가 안 맞아 둘째 seed가 새 스냅샷을 삽입 → 멱등성 깨짐 → 135행 실패.
- 실행 시각 의존 = **시점 경계 flaky**. 증거: glz(03:06) 성공 → h3s(03:27) 실패 → 07:17 실패(백엔드 코드 무변경 구간).
- **prod 무해**: `SeedDataRunner`가 부팅 시 `seed()`를 1회만 호출.

## Task 1 — WatchlistSeederIT에서 SyntheticDemoData를 고정 Clock으로 생성

**File:** `src/test/java/com/lostark/tracker/collect/WatchlistSeederIT.java`

**Action:**
- `SyntheticDemoData`를 `@Autowired` 대신 `@BeforeEach`에서 **`Clock.fixed(Instant, ZoneOffset.UTC)`로 수동 생성**(기존 `seeder = new WatchlistSeeder(...)` 패턴과 동일). 필요한 `CollectionRunRepository`를 `@Autowired` 추가.
- 두 `seed()` 호출이 **같은 고정 순간**을 보므로 `gridNow` 동일 → 둘째 seed는 진짜 no-op → 135행 결정적 통과.
- SEED-02 증명(실 repo의 `findByActiveTrue()`로 신규 품목 흡수)은 실 repo 사용으로 그대로 유지. **프로덕션 코드 0줄**.

**Verify:** `./gradlew test --tests "*WatchlistSeederIT*"` 로컬 그린(반복 실행에도 안정).

**Done:** 시점과 무관하게 항상 통과. CI backend job 그린 복구.

## Core Value 가드
테스트만. 수집/캐시/서빙/seed 프로덕션 로직 0줄.
