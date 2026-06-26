---
status: passed
phase: 09-item-timeline
verified_at: 2026-06-26
verifier: inline-orchestrator
requirements: [TIME-01, TIME-02, TIME-03, TIME-04, TIME-05]
plans_verified: [09-01, 09-02, 09-03, 09-04, 09-05]
human_verified: true
---

# Phase 9 (Item Timeline) — Verification

> Goal-backward 검증: 태스크 완료가 아니라 **페이즈 목표 달성**을 코드/실행 기준으로 확인. 빌드로 증명 가능한 부분은 자동 검증, 빌드로 증명 불가한 서사·정직성·KST 정렬은 09-05 blocking human-verify 체크포인트에서 사용자 승인("approved")으로 확인.

## Phase Goal

품목 **하나**의 가격 시계열(min_price)을 Recharts 라인 차트로 그리고 그 위에 게임 이벤트를 `eventType`별 세로 마커로 겹쳐 "언제·무슨 이벤트 즈음 움직였나"를 보게 한다. item selector로 품목을 고르고, from/to 기간을 바꾸며, 다운샘플 응답을 정직하게 표시하고, 빈/잘못된/없는 케이스를 각각 처리한다 (TIME-01~05). 백엔드 무변경(read-only).

## Must-Haves Verification (8/8 verified)

| # | Must-have (계약) | 근거 | 결과 |
|---|------------------|------|------|
| 1 | recharts ^3 + @radix-ui/react-select ^2 설치(umbrella 없음), 손수 작성 select 블록 | package.json deps, `select.tsx`(SelectTrigger/data-slot), literal `@/` 디렉터리 없음 | ✓ |
| 2 | EVENT_MARKERS 4색 팔레트(D-01) enum 키 잠금 + bucketWidthLabel(D-08) | `eventMarkers.ts`: `#7C3AED/#EA580C/#DB2777/#0D9488`, hour→'1시간'/day→'1일' | ✓ |
| 3 | URL-as-state(D-04) + 최근 30일 기본(D-03) + 7/30/90 프리셋 + KST 날짜 입력(off-by-9h 입력 경계) | `useTimelineParams.ts`(useSearchParams, 30일), `RangeControls.tsx`(type=date, +09:00) | ✓ |
| 4 | 공용 ItemSelect(D-05) + 경량 LatestPriceCard(자체 AsyncBoundary, D-07) — `_shared` | `_shared/ItemSelect.tsx`(onValueChange), `_shared/LatestPriceCard.tsx`(useLatestPrice+AsyncBoundary) | ✓ |
| 5 | 차트: UTC epoch 위치 + KST 라벨(off-by-9h), compact 골드 y축, accent 라인, 다운샘플 점 이중신호 | `PriceTimelineChart.tsx`(toEpochMs/formatKst×5, scale="time", notation:'compact', dot 분기) | ✓ |
| 6 | eventType 점선 ReferenceLine 마커 + 호버 title+KST(D-02) + 항상-4종 범례 | `PriceTimelineChart.tsx`(ReferenceLine, SVG `<title>`), `EventMarkerLegend.tsx`(Object.entries 4종) | ✓ |
| 7 | 다운샘플 정직 배지(D-08) — '버킷 평균 · {ko}', raw일 때 null | `DownsampleBadge.tsx`(bucketWidthLabel, return null) | ✓ |
| 8 | 조립: D-06 진입 자동 선택 + D-09 status별 400/404/200-empty 구분 카피 + 독립 에러 스코프 | `TimelinePage.tsx`(items[0].id, ApiError.status 분기, 4종 카피 verbatim, ChartArea 독립) | ✓ |

## Requirements Traceability (5/5)

| Req | 정의 | 전달 플랜 | 상태 |
|-----|------|-----------|------|
| TIME-01 | 품목 선택 → 최신가 카드 + 차트 갱신 | 09-02, 09-05 | ✓ Complete |
| TIME-02 | 가격 라인 차트(min_price) | 09-01, 09-04 | ✓ Complete |
| TIME-03 | 다운샘플 정직 표시(배지+선 스타일) | 09-01, 09-04 | ✓ Complete |
| TIME-04 | 이벤트 세로 마커(eventType별 색+호버) | 09-01, 09-04 | ✓ Complete |
| TIME-05 | 기간 변경 + 빈/400/404 구분 처리 | 09-03, 09-05 | ✓ Complete |

REQUIREMENTS.md에서 TIME-01~05 모두 `mark-complete` 처리 확인.

## Automated Checks

- `cd frontend && npm run build` (tsc -b + vite build): **0 type errors**, 빌드 성공 (recharts로 인한 chunk-size 권고 경고만 — 에러 아님)
- 핵심 산출물 10개 파일 디스크 존재 확인 ✓
- Java `src/` 무변경 (`git diff aaeac29..HEAD -- src/` 빈 출력) ✓
- /timeline 라우트·AppLayout 불변(페이지 본문만 교체) ✓

## Human Verification (09-05 Task 3 — blocking, approved)

사용자가 시드 백엔드 + dev 서버로 실제 화면 검증 후 **approved**:
1. 진입 즉시 비어있지 않은 차트(첫 품목 자동 + 30일) + 시드 마커 2개 ✓
2. 마커 eventType별 색 구분 + 4색 범례 + 호버 title+KST, 서사 읽힘 ✓
3. 품목 변경 시 최신가 카드 + 차트 동시 갱신 ✓
4. 다운샘플 배지('버킷 평균 · 1시간/1일') + 점 사라짐 + 응답값 일치(정직성) ✓
5. 400/200-empty/404 각각 구분 카피 ✓
6. x축 KST 라벨 + 9시간 드리프트 없음 ✓

추가 UI 폴리시 1건(400/404 Alert 폭 `max-w-xl`→`max-w-md`) 요청·반영 후 재승인.

## Gaps

None — 모든 must-have·요구사항 충족, 자동 빌드 그린, 사람 검증 통과.

## Verdict

**PASSED** — Phase 9는 목표(단일 품목 가격 시계열 + 이벤트 마커 시각화 + 기간/상태 정직 처리)를 코드와 실행 양면에서 달성. Phase 10(Event Impact)이 `_shared` 셀렉터/카드 + useTimelineParams를 재사용할 준비 완료.
