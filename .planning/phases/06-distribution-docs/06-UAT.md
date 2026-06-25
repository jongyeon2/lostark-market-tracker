---
status: diagnosed
phase: 06-distribution-docs
source: [06-01-SUMMARY.md, 06-02-SUMMARY.md]
started: 2026-06-25T01:53:22Z
updated: 2026-06-25T02:05:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Cold Start Smoke Test
expected: 실행 중인 앱/서비스를 모두 종료하고 임시 상태(temp DB·캐시)를 정리한 뒤, `LOSTARK_API_KEY` 없이 seed 프로파일로 앱을 처음부터 기동한다. `docker compose up -d`로 Postgres·Redis가 뜨고, `./gradlew bootRun --args='--spring.profiles.active=seed'`로 앱이 에러 없이 부팅되며, 시드가 완료되고, `GET /api/items/1/event-impact?window=24`가 비어있지 않은 실데이터를 반환한다.
result: pass

### 2. Full Build Green (CI 동등 명령)
expected: 리뷰어가 로컬에서 `./gradlew build`를 실행하면 CI(.github/workflows/ci.yml)가 돌리는 것과 동일한 전체 Testcontainers 스위트가 그린으로 끝난다 — 86 tests, 0 failures, 1 skipped(@Disabled Task-0 스파이크).
result: pass

### 3. Seed 타임라인 비어있지 않음
expected: seed 모드로 띄운 앱에서 `GET /api/items/1/prices`(타임라인 조회)가 비어있지 않은 스냅샷 배열을 반환한다 — 8일치 10분 간격 합성 시세(품목당 ~1152틱). API 키 없이도 데이터가 채워져 있다.
result: pass

### 4. event-impact 헤드라인 ("ok" + non-zero change_rate)
expected: `GET /api/items/1/event-impact?window=24`가 `events` 배열에 ≥1건의 `status: "ok"` 항목을 포함하고, 그 항목의 `changeRate`가 null이 아니며 0이 아닌 실제 값이다(데모 이벤트가 10분 그리드에서 5분 오프셋 배치되어 pre/post 앵커가 서로 다른 신선 스냅샷이 됨).
result: pass

### 5. README 3단계 재현
expected: README.md 상단의 "3단계 재현" 블록만 그대로 따라가면(`cp .env.example .env` → `docker compose up -d` → `./gradlew bootRun --args='--spring.profiles.active=seed'`), 이어지는 `curl .../api/items/1/event-impact?window=24`가 비어있지 않은 `events` 배열(`status:"ok"` 포함)을 돌려준다. 별도 사전 지식 없이 README만으로 재현된다.
result: pass

### 6. README 데모 표면 내용 점검
expected: README.md를 열면 최상단 CI 배지, mermaid 아키텍처 다이어그램, 정직한 트레이드오프 설명(왜 Redis / 왜 레이트리밋 / 왜 @Async), 로컬 setup + seed 모드 안내, curl 예시 3개 이상 + 샘플 JSON 응답, 그리고 데이터 보존 정책 한 줄이 모두 보인다.
result: pass

### 7. GitHub CI 배지 그린
expected: 브랜치를 GitHub(jongyeon2/lostark-market-tracker)에 푸시한 뒤, Actions 탭에 최신 커밋에 대한 "CI" 워크플로 실행이 그린으로 뜨고, README 상단 배지가 자동으로 그린으로 렌더된다. (로컬에서만 작업했고 아직 푸시 전이면 테스트 불가 — blocked.)
result: issue
reported: "아까 push 했을떄 확인했음 그린으로 뜨긴 했는데 Annotations 부분에 다음과 같은 사항이 명시됨 build / Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/checkout@v4, actions/setup-java@v4. For more information see: https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/"
severity: minor

## Summary

total: 7
passed: 6
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "GitHub Actions CI가 deprecation 경고 없이 그린으로 실행된다 (배지 그린 + 깨끗한 Annotations)"
  status: failed
  reason: "User reported: 아까 push 했을떄 확인했음 그린으로 뜨긴 했는데 Annotations 부분에 다음과 같은 사항이 명시됨 build / Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/checkout@v4, actions/setup-java@v4."
  severity: minor
  test: 7
  root_cause: ".github/workflows/ci.yml이 actions/checkout@v4 + actions/setup-java@v4를 핀 — 둘 다 Node.js 20 런타임 번들. GitHub 러너가 Node 20을 deprecate(2025-09-19)하면서 강제로 Node 24로 실행하고 build job에 deprecation annotation을 남김. 빌드 자체는 그린(기능 영향 없음)."
  artifacts:
    - path: ".github/workflows/ci.yml"
      issue: "actions/checkout@v4, actions/setup-java@v4 (Node 20 번들) 사용"
  missing:
    - "actions/checkout@v4 → @v5, actions/setup-java@v4 → @v5 로 메이저 버전 상향 (둘 다 Node 24 런타임)"
  debug_session: ""
