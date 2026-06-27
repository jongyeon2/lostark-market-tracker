---
phase: 11-demo-surface-docs
plan: 02
subsystem: ui
tags: [screenshots, docs, demo, deep-link, git-tracked-assets]

# Dependency graph
requires:
  - phase: 11-demo-surface-docs
    provides: 11-01 시각 마감 audit(잘 나온 최종 화면 전제)
  - phase: 09-item-timeline
    provides: useTimelineParams(?item=&from=&to=) URL-as-state 딥링크
  - phase: 10-event-impact
    provides: useImpactParams(?item=&window=) URL-as-state 딥링크
provides:
  - frontend/docs/screenshots/ 디렉터리 + 캡처 프로토콜 README(확정 파일명·alt·재현 딥링크·seed 전제)
  - 데모 스크린샷 3장(dashboard.png / item-timeline.png / event-impact.png) git 추적 커밋
affects: [11-03-docs]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "스크린샷 = 레포 추적 상대경로 PNG(외부 CDN 금지, D-03) → GitHub 렌더·클론 오프라인 재현"
    - "재현 딥링크 = URL searchParams 단일 소스(Phase 9/10) 재사용 → 캡처 장면을 결정적으로 고정"

key-files:
  created:
    - frontend/docs/screenshots/README.md
    - frontend/docs/screenshots/.gitkeep
    - frontend/docs/screenshots/dashboard.png
    - frontend/docs/screenshots/item-timeline.png
    - frontend/docs/screenshots/event-impact.png
    - .planning/phases/11-demo-surface-docs/11-02-SUMMARY.md
  modified: []

key-decisions:
  - "확정 파일명 3장(dashboard/item-timeline/event-impact.png) — 11-03 README가 상대경로로 그대로 소비"
  - "수동 캡처(D-02): 스캐폴드·파일명·딥링크는 plan이 마련, 바이너리 PNG는 사용자가 캡처. 이번엔 사용자가 즉시 캡처 완료(슬립 아님)"
  - ".gitignore 무변경 — docs/는 dist/dist-ssr 무시 규칙과 별개 추적 경로(git check-ignore로 검증)"

patterns-established:
  - "캡처 프로토콜 단일 안내서: frontend/docs/screenshots/README.md가 seed 전제·뷰포트·파일명·alt·딥링크를 한 곳에 모음"

requirements-completed: [DEMO-02]

# Metrics
duration: ~user-paced (capture checkpoint)
completed: 2026-06-27
---

# Phase 11 Plan 02: 데모 스크린샷 표면 Summary

**스크린샷 캡처 프로토콜(디렉터리·확정 파일명·alt·재현 딥링크·seed 전제)을 README로 잠그고, 사용자가 seed 백엔드 + `npm run dev`로 3화면을 캡처한 PNG 3장을 git 추적 상대경로로 커밋.**

## Performance

- **Duration:** 사용자 페이스(캡처 체크포인트)
- **Tasks:** 2 (Task 1 자동 스캐폴드 / Task 2 사용자 캡처 — 즉시 완료)
- **Files created:** 5 (README + .gitkeep + PNG 3장)

## Accomplishments
- `frontend/docs/screenshots/` 디렉터리 + `.gitkeep`(빈 폴더 git 유지) + 캡처 프로토콜 `README.md`
- README: 3장 확정 파일명·alt 텍스트·재현 딥링크(`/`, `/timeline?item=`, `/impact?item=&window=24`)·seed 기동 전제·데스크톱 뷰포트 가이드·커밋 안내(외부 CDN 금지)
- 사용자 캡처 완료 — `dashboard.png`(98KB)·`item-timeline.png`(56KB)·`event-impact.png`(59KB) 유효 PNG 시그니처 검증 후 커밋
- `git check-ignore`로 3장 모두 추적 가능 확인(.gitignore 무변경)

## Task Commits

1. **Task 1: 스크린샷 디렉터리 + 캡처 프로토콜 README** — `90e4008` (docs)
2. **Task 2: 사용자 스크린샷 캡처(체크포인트)** — `48b3ca8` (docs) — 사용자 캡처 PNG 3장 커밋

**Plan metadata:** SUMMARY + 추적 파일 커밋 (docs: complete plan)

## Files Created/Modified
- `frontend/docs/screenshots/README.md` — 캡처 프로토콜(파일명·alt·딥링크·seed 전제·뷰포트)
- `frontend/docs/screenshots/.gitkeep` — 빈 폴더 placeholder
- `frontend/docs/screenshots/{dashboard,item-timeline,event-impact}.png` — 사용자 캡처 데모 스크린샷 3장

## Decisions Made
- **사용자 즉시 캡처(슬립 아님):** 체크포인트에서 사용자가 "지금 캡처"를 선택해 seed 백엔드 + dev 서버로 3장 캡처 → PNG 커밋 완료. 슬롯만 남기는 이월 경로는 사용하지 않음.
- **확정 파일명 고정:** 11-03 README가 재정의 없이 `docs/screenshots/{dashboard,item-timeline,event-impact}.png`를 상대경로로 참조.

## Deviations from Plan

None - plan executed exactly as written. (Task 2의 두 허용 경로 — "캡처" 또는 "슬립" — 중 사용자가 캡처를 선택해 완료.)

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required. (스크린샷 캡처는 체크포인트로 처리·완료됨.)

## Next Phase Readiness
- 3장 확정 파일명·상대경로 PNG가 추적 커밋됨 → 11-03 README가 `![alt](docs/screenshots/*.png)` / 루트 `![](frontend/docs/screenshots/dashboard.png)`로 결정적으로 참조 가능.
- 소스/빌드/.gitignore 무변경 — 앱 동작·기존 데모 표면 무영향.

---
*Phase: 11-demo-surface-docs*
*Completed: 2026-06-27*
