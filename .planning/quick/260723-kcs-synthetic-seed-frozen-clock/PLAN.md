---
quick_id: 260723-kcs
slug: synthetic-seed-frozen-clock
description: SyntheticDemoDataIT의 10분 격자 경계 flaky 2건을 고정 시계로 제거
date: 2026-07-23
status: in-progress
---

# Quick Task 260723-kcs — SyntheticDemoDataIT 격자 경계 flaky 제거

## 어떻게 발견했나

PR #2(프론트 전용, **백엔드 0줄**) CI에서 **같은 커밋의 `backend` 잡 2개 중 1개만** 실패했다.

```
SyntheticDemoDataIT > seedIsIdempotent() FAILED
  org.opentest4j.AssertionFailedError at SyntheticDemoDataIT.java:118
실패 시각: 2026-07-23T05:30:00.785Z
```

같은 코드가 한 번은 통과하고 한 번은 실패했으므로 코드 변경이 아니라 **시점**이 원인이다.

## 원인 (확정)

`SyntheticDemoData.seed()`는 호출할 때마다 **벽시계에서 10분 격자를 다시 계산**한다:

```java
// SyntheticDemoData.java:144
private OffsetDateTime currentTickFloor() {
    OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    return now.withMinute((now.getMinute() / 10) * 10);
}
```

그리고 앱이 주입하는 `Clock` 빈은 `Clock.systemUTC()`다(`CollectionConfig.java:39`).

`seed()`를 두 번 부르는 테스트에서 두 호출이 **10분 경계를 사이에 두면** 두 번째가 다른 격자를 계산해 새 틱을 쓴다 → 스냅샷/실행 기록이 늘어난다 → 단정 실패.

🔑 **실패 시각이 `05:30:00.785`, 격자 경계 그 순간이다.** 추정이 아니라 확인이다.

## 영향 범위 — flaky는 1건이 아니라 2건

| 테스트 | `seed()` 호출 | 단정 |
|---|---|---|
| `seedIsIdempotent()` | 2회 | 스냅샷 수 불변 ← **실제로 터진 것** |
| `seedWritesSuccessRunThatBeatsAStalePersistedAuthErrorRun()` | 2회 | `collection_run` 수 == 2 ← **아직 안 터졌을 뿐** |

두 번째는 주석에 *"Idempotent on the same 10-minute grid"*라고 **전제를 스스로 적어 놓고 그 전제를 보장하지 않았다.**

## 전례 — 같은 버그를 이미 한 번 고쳤다

`WatchlistSeederIT.java:69-72` (quick-260713-mur):

```java
// so a second seed() is a true no-op regardless of wall-clock time.
new SyntheticDemoData(..., Clock.fixed(Instant.parse("2026-07-01T12:05:00Z"), ZoneOffset.UTC));
```

**`SyntheticDemoDataIT`만 그때 같이 안 고쳐졌다.**

## 결정 — 과거 고정 날짜가 아니라 "테스트 시작 시각에 동결"

`WatchlistSeederIT`처럼 `2026-07-01`로 못박는 방법도 있지만 이 클래스에는 위험하다. 여기 세 테스트는 데이터가 **현재 시각 근처**에 있다는 전제에 의존한다:

- `seedYieldsNonEmptyTimelineAndAnEventImpactOk()` — 조회 창을 `now-9d ~ now+1h`로 잡는다.
- `seedWritesSuccessRunThatBeats...()` — 낡은 FAILED 런을 `now-1h`에 심고 seed의 런이 **더 최신**이길 요구한다.

시계와 테스트의 시간 계산이 갈라지면 이 전제들이 깨진다. 그래서 **`Clock.fixed(Instant.now(appClock), ZoneOffset.UTC)`** — 실제 현재 시각을 그대로 얼린다. 데이터 위치는 지금과 동일하고, **경계 레이스만 사라진다.**

## 작업

`src/test/java/com/lostark/tracker/seed/SyntheticDemoDataIT.java` 한 파일.

1. `@Autowired SyntheticDemoData` 제거 → `@BeforeEach`에서 `frozenClock`으로 직접 생성.
2. `@Autowired Clock clock` → `appClock`(동결 instant의 출처로만 사용)으로 개명.
3. 테스트 내 `OffsetDateTime.now(clock)` → `now(frozenClock)`.
4. 클래스 javadoc에 이유·실패 시각·전례를 남긴다. 다음 사람이 "왜 빈을 안 쓰지?"에서 멈추지 않도록.

## 검증

- `./gradlew test --tests SyntheticDemoDataIT` 통과
- **10분 경계를 실제로 넘겨가며 반복 실행** — 한 번 통과는 증거가 못 된다(전에도 대부분 통과했다)
- CI가 PR에서 2회(push + pull_request) 실행

## 하지 않는 것

- **`SyntheticDemoData` 본체 무변경.** 나중 틱에서 새 행이 생기는 건 "격자를 채우는" 시더의 **올바른 동작**이다. 버그는 그걸 시각 무관하게 단정한 테스트 쪽에 있다.
- `Clock` 빈을 테스트 프로파일에서 고정 시계로 바꾸는 것 — 다른 IT들이 실시간 전제를 쓰고 있어 범위가 통제 불가능하게 커진다.
- 프론트·운영 코드 0줄.
