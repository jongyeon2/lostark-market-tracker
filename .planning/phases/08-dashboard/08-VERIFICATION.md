---
status: passed
phase: 08-dashboard
verified: 2026-06-25
requirements: [DASH-01, DASH-02, DASH-03, DASH-04]
---

# Phase 8: Dashboard — Verification

**Verified:** 2026-06-25 (inline goal-backward verification by the execute-phase orchestrator)
**Verdict:** ✅ PASS — ROADMAP 5개 완료조건 구조적 충족, DASH-01~04 만족, `npm run build` green, Java `src/` 무변경.

> Method note: 이 환경에서는 GSD verifier 서브에이전트가 permission-denied라, 오케스트레이터가
> build + grep + 구조 증거로 인라인 검증했다. 런타임 데이터 일치(curl 대조)는 seed 백엔드 기동이
> 필요한 수동 확인 포인트로 분리(아래 User Confirmation).

## 목표 (ROADMAP §Phase 8)

> 한 화면에서 수집 파이프라인이 살아있음(health) + 추적 품목 목록 + 품목별 최신가 요약을 보여줘,
> 면접관이 "데이터가 실제로 흐른다"를 즉시 읽게 한다.

→ health 카드(전폭) + 품목 카드 그리드(품목별 최신가)를 'health → 무엇을 추적 → 지금 얼마'의
단일 세로 스크롤로 조립 완료(D-01). 목표를 코드 레벨에서 달성.

## Success Criteria (ROADMAP §Phase 8)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| 1 | health(lastRunAt KST·시도/성공/실패·status)가 카드로 표시 | ✅ PASS | `HealthCard.tsx`: `useCollectionHealth()` 성공 분기에서 `시도 N · 성공 N · 실패 N`(tabular-nums, 실패>0 `text-down`) + '마지막 실행' `formatKst(lastRunAt)`('—' if null) + `<StatusBadge status={data.status}/>`. grep: useCollectionHealth/formatKst/시도/tabular-nums 매치 |
| 2 | status NO_RUNS/PARTIAL_SUCCESS/정상 시각 구분 + summaryMessage 노출, 시크릿 비노출 | ✅ PASS | `StatusBadge.tsx`: 4등급(SUCCESS→정상/up, PARTIAL_SUCCESS→일부 실패/warning, FAILED→전체 실패/down, NO_RUNS→수집 대기/neutral) + unknown→원문 라벨 neutral. `SummaryMarker.tsx`: AUTH_ERROR→'인증 오류'(down)/RATE_LIMITED→'레이트리밋'(warning)/그 외 null→`return null`(원문 echo 없음). 응답 스키마(collectionHealthSchema)에 시크릿 필드 없음 |
| 3 | /api/items 활성 품목이 displayName·category로 목록 표시 | ✅ PASS | `DashboardPage.tsx`: `useItems()` → `<ItemCard item={item}/>` 그리드(응답 순서, `.sort` 없음 — D-02). `ItemCard.tsx`: `displayName`(Heading) + `category`(Label) 상단 항상 노출. (활성 전용은 `/api/items` 백엔드 계약 — Phase 7/백엔드) |
| 4 | 각 품목 /latest(minPrice, collectedAt KST) 요약 표시 | ✅ PASS | `ItemCard.tsx`: 카드별 `useLatestPrice(item.id)` fan-out(D-03) → success 시 `minPrice`(tabular-nums, toLocaleString) + '수집 시각' `formatKst(collectedAt)`. grep: useLatestPrice/formatKst/minPrice 매치 |
| 5 | 품목 0개 / health NO_RUNS 시 빈·대기 상태가 깨지지 않음 | ✅ PASS | 그리드: `<AsyncBoundary isEmpty={length===0}>` → Phase-7 `EmptyState`. health: NO_RUNS는 success 분기의 '아직 수집 실행 기록이 없어요' 대기 카피(ErrorState 아님). 카드 latest 404 → 카드 레벨 '최신가 아직 없음'. 3중 경계 격리(D-07/D-08)로 한 곳 실패가 화면을 비우지 않음 |

## 코드 리뷰 (인라인 — gsd-code-review 서브에이전트 대체)

5개 변경 파일(badge/StatusBadge/SummaryMarker/HealthCard/ItemCard/DashboardPage) 자체 리뷰:
- **XSS/Tampering:** 모든 API 문자열은 JSX 텍스트 자식으로 렌더(React-escape), `dangerouslySetInnerHTML` 없음; 값은 07-02 zod 스키마로 경계 검증됨 (T-0801-02/T-0802-04/T-0803-01 완화).
- **시크릿 누출:** `SummaryMarker`는 알려진 카테고리만 고정 라벨로 매핑, 그 외/null은 `return null` — 원문 `summaryMessage`를 DOM에 echo하지 않음 (T-0801-01 완화).
- **격리/DoS:** health·그리드·카드별 latest가 각자 경계를 가져 단일 실패가 화면을 비우지 않음 (T-0802-02/T-0803-02 완화).
- **의존성:** 신규 런타임 의존성 0(shadcn CLI의 `radix-ui` 추가는 원복, 기존 `@radix-ui/react-slot` 사용). 서드파티 레지스트리 없음.
- **결과:** Blocking/High 이슈 없음. 타입 체크(tsc) 통과.

## User Confirmation Points (manual — seed 백엔드 :8080 + `npm run dev`)

- [ ] health 카드 카운트가 `curl /api/health/collection`의 itemsAttempted/Succeeded/Failed와 일치, '마지막 실행'이 KST로 표시
- [ ] 품목 목록 개수·이름이 `curl /api/items`와 일치(활성만, 백엔드 순서), 각 카드 minPrice가 `curl /api/items/{id}/latest`와 일치
- [ ] status 배지(특히 PARTIAL_SUCCESS·NO_RUNS)와 정보 위계('수집 살아있음 → 무엇을 추적 → 지금 얼마')가 오해 없이 읽히는지 시각 확인
- [ ] 한 품목 /latest 404(미수집) 시 그 카드만 '최신가 아직 없음', 이웃 카드·health는 정상; 품목 0개 시 그리드 EmptyState + health는 그대로
- [x] **Backend unchanged** — `git status --short -- src/` empty; 모든 변경은 `frontend/` 하위

## Requirements

| Req | Status |
|-----|--------|
| DASH-01 (health: lastRunAt KST + 시도/성공/실패 + status) | ✅ Complete |
| DASH-02 (status 배지 4등급 + summaryMessage 마커, 시크릿 비노출) | ✅ Complete |
| DASH-03 (활성 품목 카드 목록, displayName/category, 백엔드 순서) | ✅ Complete |
| DASH-04 (품목별 minPrice + KST collectedAt, 카드별 fan-out) | ✅ Complete |

## Notes
- 범위 유지(설계대로 제외): 차트/시계열(Phase 9), 이벤트 영향(Phase 10), 품목 selector·딥링크(Phase 9~), 자동 갱신·폴링(FE-V2-02), 관리자 쓰기 UI(FE-V2-01), 배치 latest 엔드포인트(백엔드 변경→v2).
- Deviation: 08-01에서 shadcn 4.11 CLI의 Windows `@` 경로 버그 + 불필요한 `radix-ui` 의존성 추가를 교정(파일 정위치 작성 + 의존성 원복) — 08-01-SUMMARY 참고. 동작·계약 영향 없음.
- D-04 fan-out 비용(품목 N개 → latest N요청)은 의도된 트레이드오프로 수용(배치 엔드포인트 없음, seed 워치리스트 소규모).

---
*Phase: 08-dashboard — verified complete 2026-06-25*
