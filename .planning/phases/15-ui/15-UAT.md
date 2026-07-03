---
status: complete
phase: 15-ui
source: [15-01-SUMMARY.md, 15-02-SUMMARY.md, 15-03-SUMMARY.md, 15-04-SUMMARY.md]
mode: real-data (dev 프로파일 실수집 — seed 아님)
started: 2026-07-03T12:45:00Z
updated: 2026-07-03T13:20:00Z
---

## Current Test

[testing complete]

## Tests

### 1. dev 실데이터 기동 (Cold Start)
expected: |
  docker-compose로 postgres+redis 기동, .env에 유효한 LOSTARK_API_KEY·ADMIN_API_SECRET 설정 후
  `./gradlew bootRun --args='--spring.profiles.active=dev'` → 백엔드 부팅 에러 없음. 기동 ~10초 뒤 첫
  실수집 틱이 실행되어 실 API로 시세를 적재(콘솔 로그에 수집 결과, AUTH_ERROR/키 무효 아님). `npm run dev`
  프론트 기동 후 http://localhost:5173/admin 접근 가능.
result: pass
note: |
  최초 blocked(env-config). Root cause(경험 증거로 확정): ./gradlew bootRun이 .env를 로드하지 않음
  (build.gradle에 dotenv 메커니즘 없음). 실행 중 dev 백엔드 확인 결과 /api/health/collection = AUTH_ERROR
  (itemsFailed 15/15 → LOSTARK_API_KEY 빈 값), /api/admin/events(임의 시크릿) = 401(AdminSecretFilter는
  configuredSecret 빈 값이면 fail-closed → ADMIN_API_SECRET 빈 값). Phase 15 admin 콘솔 코드 결함 아님
  (빈 서버 시크릿에 401 응답하는 정상 동작) — 환경/온보딩 결함.
  FIX 적용(commit): build.gradle bootRun이 .env를 앱 JVM으로 자동 로드(셸 env 우선, 없으면 no-op),
  .env.example/README 문구 정정. 재기동 후 재검증: /api/health/collection = SUCCESS(15/15, AUTH_ERROR
  사라짐) → 실수집·.env 로딩 확인. PASS.

### 2. 로그인 게이트 (ADMINUI-01/02/06)
expected: |
  /admin 진입 시 화면 중앙에 로그인 카드만 보이고 이벤트/워치리스트/수집 폼·버튼은 화면에 전혀 없다.
  틀린 시크릿 입력 → "시크릿이 올바르지 않습니다. 다시 확인해 주세요." 인라인 오류. 백엔드 미기동 상태면
  "백엔드에 연결하지 못했어요…(:8080)…" 메시지. .env의 ADMIN_API_SECRET 값을 입력 → 콘솔 진입
  (게임 이벤트·워치리스트·수집 상태 3개 섹션 표시). 입력창은 password로 마스킹되어 시크릿 값이 안 보인다.
result: pass

### 3. 세션 지속·소멸 (ADMINUI-01)
expected: |
  로그인 상태에서 F5 새로고침 → 재로그인 없이 콘솔 유지(sessionStorage). 탭을 완전히 닫고 새 탭에서
  http://localhost:5173/admin 재접속 → 로그인 화면(세션 소멸, localStorage 아님).
result: pass

### 4. 수집 상태 — 실데이터 (ADMINUI-05)
expected: |
  "수집 상태" 카드가 dev 실수집 결과를 표시한다: 시도 카운트가 실 워치리스트 규모(약 15)와 일치하고,
  "마지막 실행 {KST}"가 방금 수집 시각. 합성값이 아니라 실 API 응답 기반이며, 상태 배지가 정상(또는
  일부 실패면 그 카운트가 실제 값). AUTH_ERROR/RATE_LIMITED 마커가 없다(키 유효 시).
result: pass
note: "백엔드 실측 /api/health/collection = itemsSucceeded 15/15, status SUCCESS와 카드 일치 확인."

### 5. 이벤트 CRUD + KST→UTC (ADMINUI-03)
expected: |
  "게임 이벤트"에서 유형(예 로아ON)/제목/발생 시각(KST)/설명을 입력하고 "이벤트 등록" → 목록 최신순
  맨 위에 나타나고 "이벤트를 등록했어요." 인라인 메시지. 표시된 발생 시각이 입력한 KST와 정확히 일치
  (9시간 밀리지 않음). "수정" → 폼이 그 값으로 프리필(발생 시각도 KST) → 값 바꿔 "변경 저장" → 목록 반영.
  "삭제" → "이 이벤트를 삭제할까요? 되돌릴 수 없습니다." 확인 → "삭제" → 목록에서 제거. (첫 "삭제"는
  확인 단계일 뿐, "취소"로 무를 수 있다.)
result: pass
note: "등록·수정·삭제(2단계 확인) 반영 + 발생 시각 KST 정확(off-by-9h 없음) 확인."

### 6. 워치리스트 — 실데이터 추가·비활성·재활성 (ADMINUI-04)
expected: |
  "워치리스트"가 활성·비활성 품목을 함께 보여주고 각 행에 활성(초록)/비활성(회색) 배지가 있다. 실제
  로스트아크 아이템 번호로 품목을 추가 → 활성 목록에 편입, "품목을 추가했어요.". 같은 번호를 다시 추가 →
  "이미 활성 상태인 품목이에요." 경고(중복). 활성 품목 "비활성" → "이 품목을 비활성할까요? 수집 대상에서
  제외됩니다." 확인 → 비활성 배지로 전환. 그 품목 "재활성" → 활성 복귀.
  [실데이터 핵심] 새로 추가한 품목이 다음 수집 틱(최대 ~10분) 뒤 실제로 시세가 수집된다 — 수집 상태의
  시도 카운트 증가 또는 대시보드/타임라인에서 그 품목의 최신가가 실 데이터로 표시.
result: pass
note: |
  기존 실 번호로 409(중복)·비활성·재활성 확인. 새 품목 6861010 추가 → /api/items에 #16 active로 편입,
  다음 틱 itemsAttempted 15→16(수집 루프 실 편입 증명). 단 6861010은 실 거래소 미존재 품목이라 그 1건만
  수집 실패(PARTIAL_SUCCESS, itemsFailed:1) — 번호 추천 오류일 뿐 Phase 15 결함 아님(콘솔은 정확히 추가,
  수집기는 실패를 정직하게 기록). 정리: 6861010 비활성 권장 → 15/15 복귀.

### 7. 로그아웃 (ADMINUI-06)
expected: |
  콘솔 헤더 우측 "로그아웃" 클릭 → 로그인 화면으로 복귀. 이후 새로고침해도 로그인 화면(세션 종료됨).
result: pass

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]
