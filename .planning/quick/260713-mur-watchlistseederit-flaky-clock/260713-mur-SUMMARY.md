---
quick_id: 260713-mur
slug: watchlistseederit-flaky-clock
date: 2026-07-13
status: complete
commit: a0ed5be
---

# Quick Task 260713-mur 완료 — WatchlistSeederIT 시점 경계 flaky 수정

## 증상
CI backend job(`./gradlew build`) 간헐 실패: `WatchlistSeederIT.syntheticDemoDataSnapshotsNewItemsKeylessAndIdempotent()` (`:135` `count()==afterFirst`). glz(03:06) 성공 → h3s(03:27) 실패 → 07:17 실패 — 그 구간 백엔드 코드 무변경.

## 근본 원인 (코드 분석)
`SyntheticDemoData.seed()`는 호출마다 `currentTickFloor()`로 **벽시계 `now(clock)`을 10분 그리드로 내림**해 `gridNow`를 새로 계산하고, 스냅샷을 `gridNow.minus(TICK*i)`에 `existsBy` 가드로 멱등 적재한다. 첫 `seed()`는 22품목 × 1152틱 ≈ **25k 인서트라 수십 초** 걸린다. 두 `seed()` 호출의 `gridNow` 계산 시각이 **10분 경계를 넘으면** `gridNow`가 밀려 타임스탬프 집합이 달라지고, 가드가 안 맞아 둘째 seed가 새 스냅샷을 삽입 → 멱등성 깨짐 → 135행 실패. **실행 시각 의존 flaky**. prod는 `SeedDataRunner`가 부팅 시 1회만 호출하므로 무해.

## 수정 (테스트만, 프로덕션 0줄)
`WatchlistSeederIT`에서 `SyntheticDemoData`를 `@Autowired` 대신 **`@BeforeEach`에서 `Clock.fixed(Instant.parse("2026-07-01T12:05:00Z"), ZoneOffset.UTC)`로 수동 생성**(기존 `seeder = new WatchlistSeeder(...)` 패턴과 동일). `CollectionRunRepository`를 `@Autowired`로 추가. 두 `seed()` 호출이 **같은 고정 순간**을 보므로 `gridNow` 동일 → 둘째 seed는 진짜 no-op → 결정적 통과. SEED-02 증명(실 repo `findByActiveTrue()`로 신규 품목 흡수)은 실 repo 사용으로 그대로 유지.

## 검증
- `./gradlew test --tests "*WatchlistSeederIT*" -PdockerApiVersion=1.44` → **BUILD SUCCESSFUL** (Testcontainers, 1m 2s). 고정 Clock이라 반복 실행에도 결정적.

## 남은 것
- push 후 CI backend job 그린 확인(그러면 frontend·images job까지 진행). 참고: 이 커밋도 아직 로컬 main.

## 커밋
- `a0ed5be` fix(test): WatchlistSeederIT 시점 경계 flaky 수정 — SyntheticDemoData 고정 Clock
