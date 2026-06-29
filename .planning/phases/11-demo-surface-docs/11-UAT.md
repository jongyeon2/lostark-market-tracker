---
status: complete
phase: 11-demo-surface-docs
source: [11-01-SUMMARY.md, 11-02-SUMMARY.md, 11-03-SUMMARY.md]
started: 2026-06-27T04:11:49Z
updated: 2026-06-29T00:20:00Z
resolved_by: 11-04-PLAN.md
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Cold Start — 인프라 + seed 백엔드 기동
expected: 클린 상태에서 `docker compose up -d`로 Postgres 16 + Redis 7 컨테이너가 기동되고, 이어서 `./gradlew bootRun --args='--spring.profiles.active=seed'` 실행 시 백엔드가 외부 로스트아크 API 키 없이 부팅되며 seed 합성 데이터가 적재된다(에러 없이 기동 완료, health 응답 정상).
result: pass

### 2. 프론트엔드 dev 서버 기동
expected: `cd frontend && npm install && npm run dev` 실행 시 Vite dev 서버가 `http://localhost:5173`에서 기동되고, 터미널/브라우저 콘솔에 치명적 에러 없이 앱이 로드된다.
result: pass

### 3. Dashboard 화면 (실데이터로 채워짐)
expected: `http://localhost:5173/` 접속 시 헬스 카드 + 활성 품목 워치리스트 그리드가 seed 데이터로 채워져 보인다(무한 로딩/빈 화면/에러 아님).
result: issue
reported: "수집헬스 카드에 시도는 12번인데 12개가 다 실패했다고 뜨고 카드에 나온 원인은 인증오류 라는데?"
severity: major

### 4. Item Timeline 화면
expected: `/timeline?item=<품목ID>` 접속 시 가격 라인 차트 + 이벤트 마커가 렌더된다(품목 미지정 시 최근 30일 안내). 데스크톱 폭에서 가로 스크롤·요소 겹침 없음.
result: pass

### 5. Event Impact 화면
expected: `/impact?item=<품목ID>&window=24` 접속 시 변화율 결과 표 + '상관≠인과' 정직성 배너가 표시된다. 좁은 폭(~768px)에서는 표가 카드로 전환된다.
result: pass

### 6. 스크린샷 3장 렌더 / 경로 유효
expected: `frontend/docs/screenshots/`의 dashboard.png · item-timeline.png · event-impact.png가 존재하고, frontend/README와 루트 README의 이미지 임베드가 깨지지 않고 렌더된다(상대경로 추적 자산, 외부 CDN 아님).
result: pass

### 7. 루트 README — 프론트 데모 포인터 + curl 섹션 보존
expected: 루트 README에 '프론트 데모' 포인터 섹션(3단계 요약 + 대표 스크린샷 + frontend/README 위임 링크)이 추가되어 있고, 기존 'curl + 샘플 JSON' 데모 섹션과 그 외 엔드포인트 섹션이 회귀 없이 보존된다. 인트로 문장이 프론트 데모 존재와 모순되지 않는다.
result: pass

### 8. frontend/README — 재현 단일 출처 + 정직성 카피
expected: frontend/README가 사전조건(seed 기동) → 실행(`npm install && npm run dev`) → 3화면 라우트 표 → Vite 프록시 설명 → 정직성(KST 표시/UTC 계산, seed 합성 데이터, 상관≠인과, 정적 서빙 v2 연기)을 한 곳에 담은 단일 출처로 동작한다.
result: pass

## Summary

total: 8
passed: 7
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "seed 모드 Dashboard 헬스 카드가 seed 합성 수집 성공 상태(시도12·성공12·실패0·SUCCESS)를 보여준다 — API 키 없이도 정상으로 보인다"
  status: resolved
  resolved_by: "11-04-PLAN.md — seed()가 합성 SUCCESS collection_run을 멱등 적재(startedAt=gridNow가 영속 볼륨의 과거 AUTH_ERROR run을 덮음). SyntheticDemoDataIT 회귀 단언으로 고정. 커밋 6c7cf0f·d03a9bb·4c13f44·9b38507"
  reason: "User reported: 수집헬스 카드에 시도는 12번인데 12개가 다 실패했다고 뜨고 카드에 나온 원인은 인증오류 라는데?"
  severity: major
  test: 3
  root_cause: "SyntheticDemoData.seed()는 PriceSnapshot·GameEvent만 심고 collection_run을 심지 않는다. 헬스 카드(CollectionHealthService.latestHealth() → CollectionRunRepository.findTopByOrderByStartedAtDesc())는 프로파일·나이 무관 '최신 run 1건'만 반영한다. 라이브 @Scheduled 수집기는 dev/기본 프로파일에서 collection.initial-delay-ms 기본값 0으로 부팅 즉시 키 없이 발화 → run(itemsAttempted=12[=WATCHLIST 12품목], succeeded=0, failed=12, status=FAILED, summaryMessage=AUTH_ERROR). docker-compose.yml의 명명 볼륨 pgdata가 Postgres를 영속시켜 그 실패 run이 docker compose down/up 후에도 살아남아 seed 대시보드의 '최신 run'으로 재등장. HealthCard.tsx가 '시도12·성공0·실패12' + SummaryMarker(AUTH_ERROR)를 그대로 렌더 → 사용자 보고와 정확히 일치. 정상 seed 부팅(깨끗한 볼륨)이라도 헬스 카드는 NO_RUNS('아직 수집 실행 기록이 없어요')로 떠 timeline/impact는 가득 찬데 헤드라인 수집 헬스 위젯만 비어 데모 내적 모순."
  artifacts:
    - path: "src/main/java/com/lostark/tracker/seed/SyntheticDemoData.java"
      issue: "seed()가 collection_run을 심지 않아 헬스 카드가 seed 데이터의 수집 성공을 표현할 근거가 없음"
    - path: "src/main/java/com/lostark/tracker/health/CollectionHealthService.java"
      issue: "findTopByOrderByStartedAtDesc()로 프로파일·나이 무관 최신 run만 반영 — 영속 볼륨의 과거 키리스 AUTH_ERROR run이 seed 데모의 얼굴이 됨"
    - path: "docker-compose.yml"
      issue: "pgdata 명명 볼륨으로 collection_run 영속 → 과거 dev/기본 키리스 tick의 실패 run이 재시작 후 잔존"
    - path: "src/main/resources/application-dev.yml"
      issue: "collection.initial-delay-ms 미설정 → 기본 0 → dev/기본 프로파일 부팅 즉시 키 없이 tick 발화(seed 전 1회만 실행해도 실패 run 적재)"
  missing:
    - "SyntheticDemoData.seed()가 합성 SUCCESS collection_run(attempted=12·succeeded=12·failed=0·status=SUCCESS·summaryMessage=null·startedAt/finishedAt=gridNow)을 멱등하게 적재 → startedAt=gridNow가 최신이라 영속 볼륨의 과거 실패 run을 덮고 헬스 카드가 녹색 12/12로 표시"
    - "CollectionRunRepository에 멱등 가드용 existsByStartedAtAndStatus(OffsetDateTime, String) 추가"
    - "SyntheticDemoDataIT에 seed 후 latest run = SUCCESS(12/12, AUTH_ERROR 아님) 단언 + 과거 실패 run 선적재 시에도 seed가 최신 SUCCESS로 덮는지 회귀 테스트"
    - "(선택적 하드닝) seed 프로파일에서 라이브 스케줄러 완전 비활성화 검토 — 1h initial-delay는 일반 데모만 커버, 장시간 데모/볼륨 잔존은 미커버"
  debug_session: ".planning/debug/seed-health-card-auth-error.md"
