---
phase: 14-frontend-icons-fallback-docs
plan: 03
subsystem: docs
tags: [readme, documentation, portfolio]

requires:
  - phase: 14-frontend-icons-fallback-docs
    provides: 14-01/14-02 산출물 — <ItemIcon>·RoleBadge·역할군 셀렉터·역할 정렬(README가 정확히 기술)
  - phase: 12-api-spike-data-lock
    provides: 12-SPIKE-FINDINGS.md — iconUrl CDN·각인서 11종 동일 글리프·융화재료 4종 구별(요약+링크 대상)
provides:
  - 루트 README 시각 enrichment 섹션(데이터 출처·API 실측 요약·fallback 전략·자산 섹터 서사 + 3화면 스크린샷 참조)
  - frontend/README 아이콘+역할 배지 섹션(소비 방식·<ItemIcon> 구현·역할 배지/셀렉터·fallback 확인법)
affects: []

tech-stack:
  added: []
  patterns:
    - "findings 노출은 요약+링크+서사 — 단일 출처는 12-SPIKE-FINDINGS.md 유지(전재 금지, D-10)"
    - "docs만 수정 — 코드(src/·frontend/src/) diff 0줄"

key-files:
  created: []
  modified:
    - README.md
    - frontend/README.md

key-decisions:
  - "스크린샷 실 캡처는 수동 사용자 액션(앱 실행+브라우저)으로 위임(D-11) — README는 경로 참조만, 자동 증명 불가"
  - "findings는 요약+상대경로 링크만, 단일 출처는 12-SPIKE-FINDINGS.md 유지(D-10)"
  - "기존 curl/JSON 예시·CI 배지·3단계 재현 보존 — 시각 enrichment 섹션을 additive로 추가(전체 재작성 금지, D-09)"

patterns-established:
  - "루트 README=포트폴리오 진입점(출처·실측·서사), frontend/README=구현·실행 안내 — 역할 분담 중복 최소(D-09)"

requirements-completed: [ICON-08]

duration: 12min
completed: 2026-06-30
---

# Phase 14 Plan 03: README 문서(출처·실측·fallback·자산 섹터 서사) Summary

**루트 README에 아이콘 데이터 출처(API Icon URL→DB→4 read 패스스루)·실측 요약(각인서 동일/융화재료 구별)·fallback 전략·role_group=자산 섹터 서사를, frontend/README에 <ItemIcon> 소비·구현·fallback 확인법을 기록 — 코드 diff 0, 실 키/계정 0건**

## Performance

- **Duration:** 약 12 min
- **Completed:** 2026-06-30
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- **루트 README(D-09/D-10/D-11):** 시각 enrichment 섹션 추가 — (1) 데이터 출처(CDN `efui_iconatlas` → enrichment 컬럼 → 4 read 패스스루), (2) API 실측 요약(유물 각인서 11종 동일 `use_9_25.png` / 융화재료 4종 구별 → 각인서는 라벨·배지로 식별, 12-SPIKE-FINDINGS 링크), (3) fallback 전략(고정 슬롯 + 역할색 글리프, 시프트 0), (4) `role_group`=자산 섹터 서사(고변동 융화재료 오레하·아비도스), (5) 3화면 스크린샷 참조 갱신.
- **frontend/README(D-09):** 아이콘+역할 배지 섹션 — 소비 방식(zod `.nullable()` 검증, 프론트=백엔드 DTO만), `<ItemIcon>` 구현(고정 슬롯 md 32/sm 20, null/onError 역할색 글리프, lucide-react만), RoleBadge·역할군 셀렉터(그룹 헤더·정렬, 필터 v2), fallback 직접 확인법(devtools offline).
- **상시 가드:** 실 API 키·계정 식별자·가격 원문 0건(grep 게이트), 공개 메타데이터·키 없는 seed 재현만.

## Task Commits

1. **Task 1: 루트 README 시각 enrichment 섹션** — `bd1d368` (docs)
2. **Task 2: frontend/README 아이콘·fallback 안내** — `8eb49ff` (docs)

## Files Created/Modified

- `README.md` — 시각 enrichment 섹션(출처·실측·fallback·자산 섹터 서사) + 데모 섹션 스크린샷 캡션 갱신 + item-timeline/event-impact 스크린샷 참조
- `frontend/README.md` — 아이콘+역할 배지 섹션(소비·구현·fallback)

## Decisions Made

- **스크린샷 수동 위임(D-11):** 실 캡처는 사용자가 앱 실행+브라우저로 교체(자동 증명 불가) — README는 경로 참조만.
- **findings 요약+링크(D-10):** 단일 출처는 12-SPIKE-FINDINGS.md, README는 요약+상대경로 링크.
- **additive 보강(D-09):** 기존 구조·curl·CI 배지 보존, 시각 enrichment 섹션만 추가.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

**수동 사용자 액션(D-11 스크린샷):** 아이콘·역할 배지가 반영된 새 3화면 캡처는 직접 교체가 필요합니다:
```bash
docker compose up -d postgres redis
./gradlew bootRun --args='--spring.profiles.active=seed'
cd frontend && npm run dev   # http://localhost:5173
```
→ 브라우저에서 Dashboard / Timeline / Event Impact 캡처해 `frontend/docs/screenshots/{dashboard,item-timeline,event-impact}.png` 교체.

## Next Phase Readiness

- Phase 14 완료(3/3 plan) — 시각 enrichment(아이콘·역할 배지·fallback)가 코드+문서로 잠김. v1.2 마일스톤(Phase 12·13·14) 마지막 phase.
- 코드 diff 0(docs만), 실 키/계정/가격 원문 0건.

---
*Phase: 14-frontend-icons-fallback-docs*
*Completed: 2026-06-30*
