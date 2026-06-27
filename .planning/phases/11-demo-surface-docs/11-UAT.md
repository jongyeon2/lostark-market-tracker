---
status: complete
phase: 11-demo-surface-docs
source: [11-01-SUMMARY.md, 11-02-SUMMARY.md, 11-03-SUMMARY.md]
started: 2026-06-27T04:11:49Z
updated: 2026-06-27T04:11:49Z
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

- truth: "seed 모드 Dashboard 헬스 카드가 seed 합성 수집 성공 상태(또는 라이브 수집 비활성)를 보여준다 — API 키 없이도 정상으로 보인다"
  status: failed
  reason: "User reported: 수집헬스 카드에 시도는 12번인데 12개가 다 실패했다고 뜨고 카드에 나온 원인은 인증오류 라는데?"
  severity: major
  test: 3
  root_cause: ""     # Filled by diagnosis
  artifacts: []      # Filled by diagnosis
  missing: []        # Filled by diagnosis
  debug_session: ""  # Filled by diagnosis
