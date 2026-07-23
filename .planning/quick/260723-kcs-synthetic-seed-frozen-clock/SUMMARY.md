---
quick_id: 260723-kcs
slug: synthetic-seed-frozen-clock
description: SyntheticDemoDataIT의 10분 격자 경계 flaky 2건을 고정 시계로 제거
date: 2026-07-23
status: complete
commits: [0d680cc]
---

# Quick Task 260723-kcs — SyntheticDemoDataIT 격자 경계 flaky 제거

## 무엇을 바꿨나

`SyntheticDemoDataIT` 한 파일. 앱의 `Clock.systemUTC()` 빈 대신 **테스트 시작 시각에 동결한 시계**로 `SyntheticDemoData`를 직접 만들어 쓴다. 운영 코드 0줄.

## 어떻게 발견했나

PR #2(프론트 전용, 백엔드 0줄) CI에서 **같은 커밋의 `backend` 잡 2개 중 1개만** 실패했다. 코드가 아니라 **시점**이 원인이라는 뜻이다.

```
SyntheticDemoDataIT > seedIsIdempotent() FAILED — SyntheticDemoDataIT.java:118
실패 시각: 2026-07-23T05:30:00.785Z   ← 10분 격자 경계 그 순간
```

## 원인

`SyntheticDemoData.seed()`는 호출마다 벽시계에서 격자를 다시 계산한다(`currentTickFloor`, 10분 단위). `seed()`를 두 번 부르는 테스트에서 두 호출이 경계를 사이에 두면 두 번째가 **새 틱**을 쓴다.

## 🔑 재현 실험 — 추정이 아니라 측정

임시 IT를 만들어 두 방향을 모두 재현하고 삭제했다.

| 시나리오 | 스냅샷 | `collection_run` |
|---|---|---|
| **경계 넘김** (`12:09:59` → `12:10:01` 시계) | 1152 → **1153** ⬆ | 1 → **2** ⬆ |
| **동결 시계** (같은 instant 2회) | 1152 → **1152** ✓ | 1 → **1** ✓ |

`collection_run`도 늘어난 것이 중요하다 — **flaky가 1건이 아니라 2건이었음이 증명됐다:**

| 테스트 | 단정 | 상태 |
|---|---|---|
| `seedIsIdempotent()` | 스냅샷 수 불변 | CI에서 **실제로 터짐** |
| `seedWritesSuccessRunThatBeatsAStalePersistedAuthErrorRun()` | `collection_run == 2` | **아직 안 터졌을 뿐** — 실험으로 취약함 확인 |

두 번째는 주석에 *"Idempotent on the same 10-minute grid"*라고 전제를 적어 놓고 그 전제를 보장하지 않고 있었다.

## 핵심 결정 · 근거

- **과거 고정 날짜가 아니라 "현재 시각 동결".** `WatchlistSeederIT`는 `2026-07-01`로 못박았지만(quick-260713-mur) 이 클래스엔 위험하다. 여기 테스트들은 데이터가 **현재 시각 근처**라는 전제에 의존한다 — 조회 창이 `now-9d ~ now+1h`이고, 낡은 FAILED 런을 `now-1h`에 심고 seed의 런이 더 최신이길 요구한다. `Clock.fixed(Instant.now(appClock), ZoneOffset.UTC)`는 데이터 위치를 그대로 두고 **경계 레이스만** 없앤다.
- **`SyntheticDemoData` 본체는 건드리지 않았다.** 나중 틱에서 새 행이 생기는 건 "격자를 채우는" 시더의 **올바른 동작**이다. 버그는 그걸 시각 무관하게 단정한 테스트 쪽에 있었다.
- **`Clock` 빈을 test 프로파일에서 고정하지 않았다.** 다른 IT들이 실시간 전제를 쓰고 있어 범위가 통제 불가능하게 커진다.
- **javadoc에 실패 시각과 전례를 남겼다.** 다음 사람이 "왜 빈을 안 쓰지?"에서 멈추지 않도록.

## 검증

| 항목 | 결과 |
|---|---|
| 재현 실험 (임시 IT, 실행 후 삭제) | 위 표 — 양방향 재현 |
| `SyntheticDemoDataIT` (cleanTest) | ✅ 통과 |
| `WatchlistSeederIT` (cleanTest, 같은 시더 사용) | ✅ 통과 |
| 임시 파일 잔존 | 0 (`ls`로 확인) |

⚠️ 첫 시도는 `./gradlew test`만 써서 Gradle이 `:test`를 UP-TO-DATE로 **스킵**했고, 통과처럼 보였지만 실제로는 아무것도 안 돌았다. `cleanTest`를 붙여 다시 측정했다.

## 범위 밖

- 운영 코드 · 프론트 0줄.
- `SyntheticDemoData`의 격자 재계산 동작 자체(의도된 동작).
