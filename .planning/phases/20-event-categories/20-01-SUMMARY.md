---
phase: 20-event-categories
plan: 01
status: complete
requirements: [EVT-01]
commits: []
---

# 20-01 SUMMARY — 이벤트 카테고리 +3 (additive)

## 무엇을 했나

관리자 이벤트 유형을 3종 확장했다(순수 additive, Core Value 0줄):
- **백엔드** `EventType.java`: `NEW_CLASS`·`NEW_RAID`·`GENERAL_PATCH` 추가(BALANCE_PATCH 뒤). **DB 마이그레이션 불필요** — `event_type VARCHAR(40)`에 CHECK 제약 없고 `@Enumerated(STRING)`+`@NotNull EventType` 바인딩이 유효화(최장명 GENERAL_PATCH=13자<40).
- **프론트** `schemas.ts` `eventTypeSchema` z.enum +3 → 파생 `EventType` 타입이 전 소비처(타임라인 마커·범례·이벤트영향·관리자 폼)로 락스텝 확장.
- **마커 색** `eventMarkers.ts` `EVENT_MARKERS` +3 — **dataviz 검증기 통과 팔레트**: NEW_CLASS=`#3B82F6`(blue)·NEW_RAID=`#DC2626`(red)·GENERAL_PATCH=`#16A34A`(green). 기존 4색과 합친 **7색이 light 모드 CVD/대비 전 항목 PASS**(worst adjacent ΔE 15.1). 앱은 다크모드 미구현이라 light만 필요. 마커는 대시 수직선+범례 한글 라벨로 색-only 의존 아님(2차 인코딩).
- **관리자 폼** `EventSection.tsx` `EVENT_TYPE_LABELS`·`EVENT_TYPE_ORDER` +3 — 셀렉트·목록에 7종 노출(라벨: 신규 캐릭터 출시/신규 레이드 출시/일반 패치).
- **테스트** `AdminEventControllerIT`에 NEW_CLASS 이벤트 create 201 + 영속 eventType 검증 1케이스.

**동기:** 차원술사(신규 직업, 2026-07-08 출시) 출시 기점 "타격의 대가" 각인서 급등→정상화를 `NEW_CLASS`로 상관 기록.

## 검증

- **백엔드**: `./gradlew test --tests "*AdminEventControllerIT*"` → **BUILD SUCCESSFUL**(Testcontainers Postgres+Redis, NEW_CLASS 왕복 케이스 포함 그린). 알 수 없는 값은 여전히 400(바인딩 유효화 불변).
- **프론트**: `npm run build`(tsc -b && vite build) → **그린**. `Record<EventType>`(EVENT_MARKERS·EVENT_TYPE_LABELS)가 3종 누락 없이 컴파일(타입 락스텝). 번들 크기 경고는 기존 것(무관).
- **마커 색**: dataviz `validate_palette.js`로 7색 light 모드 검증 — Lightness/Chroma/CVD/Contrast 전 항목 PASS. 위험쌍(blue↔violet, red↔orange/pink, green↔teal) 스트레스 순서로 재확인.
- **Core Value 가드**: 수집/캐시/event-impact 로직 0줄. event-impact는 eventType 무관(occurredAt 상관)이라 회귀 없음.

## 커밋

- (code) EventType.java · schemas.ts · eventMarkers.ts · EventSection.tsx · AdminEventControllerIT.java
- (planning) 20-01-PLAN·SUMMARY · ROADMAP · STATE