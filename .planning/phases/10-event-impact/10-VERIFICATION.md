---
status: passed
phase: 10-event-impact
verified_at: 2026-06-27
verifier: inline-orchestrator
requirements: [IMPCT-01, IMPCT-02, IMPCT-03, IMPCT-04]
plans_verified: [10-01, 10-02, 10-03, 10-04]
human_verified: true
---

# Phase 10 (Event Impact) — Verification

> Goal-backward 검증: 태스크 완료가 아니라 **페이즈 목표 달성**을 코드/빌드/실행 기준으로 확인. 빌드(tsc -b + vite)로 증명 가능한 부분은 자동 검증, 빌드로 증명 불가한 정직성·% 부합·KST 정합·반응형은 10-04 blocking human-verify 체크포인트에서 사용자 승인("approved")으로 확인. GSD 서브에이전트(gsd-verifier)는 본 환경에서 권한 거부되어 오케스트레이터 인라인 검증으로 대체.

## Phase Goal

이벤트별 전후 변화율을 표로 보여주되, `insufficient_data`의 이유(희소 vs stale)를 구분하고 "상관 ≠ 인과"를 분명히 고지해 — 데이터에 정직한 분석 화면을 만든다 (IMPCT-01~04). 백엔드 무변경(read-only, 기존 read API 소비만).

## Requirement Traceability (4/4)

| Req | 의미 | 구현 | 결과 |
|-----|------|------|------|
| IMPCT-01 | window 조회(프리셋+입력, URL 상태, 기본 24h) | `useImpactParams`(?item=&window=, 기본 24 non-eager), `WindowControls`(6/24/72h+숫자입력), `ImpactPage` 배선 | ✓ |
| IMPCT-02 | 전후 변화율 표시·포맷·방향색 | `formatChangeRate`(비율×100, 부호+%+소수1), `changeRateColorClass`(상승빨강/하락파랑), `EventImpactTable`/`Cards` | ✓ |
| IMPCT-03 | ok/insufficient 구분 + 희소/stale "왜" | `STATUS_BADGE_META`, `insufficientReason`(앵커 null로 희소/stale), 앵커 KST 노출 | ✓ |
| IMPCT-04 | "상관 ≠ 인과" 고지 | `CorrelationBanner`(상시·닫기불가·default+Info), `ImpactPage` 최상단 배치 | ✓ |

## Must-Haves Verification (20/20 verified — 자동 19 + 휴먼 1)

| # | Must-have (계약·D-NN) | 근거 | 결과 |
|---|------------------------|------|------|
| 1 | 손수 작성 shadcn new-york `table` 블록(Windows shadcn-add 버그 회피) | `ui/table.tsx` Table~TableCaption export, `data-slot`+`cn`, literal `@/` 없음 | ✓ |
| 2 | `useImpactParams` ?item=&window= 단일 상태(D-01) + 기본 24 non-eager(D-02) + 클램프 없음(D-03) | `useImpactParams.ts` useSearchParams, DEFAULT_WINDOW=24, setWindow 비클램프 | ✓ |
| 3 | changeRate 비율 ×100(D-06) — 단일 변환 지점 | `impactFormat.formatChangeRate` `Math.round(rate*1000)/10`, `-0.0%` 방지 | ✓ |
| 4 | 한국 관례 방향색(D-05) — 상승#DC2626/하락#1D4ED8/보합#64748B, index.css 토큰 미사용 | `changeRateColorClass` explicit hex + 사유 주석 | ✓ |
| 5 | 희소/stale = 앵커 null 여부만(D-08), 30분 임계 하드코딩 금지 | `insufficientReason` 앵커 null 분기, 본문에 숫자 임계 없음(플랜 게이트 `!STALENESS\|Duration\|minutes` 통과) | ✓ |
| 6 | `WindowControls` controlled(프리셋 6/24/72h + 숫자입력), router 상태 미보유, 비클램프(D-03) | `WindowControls.tsx` PRESETS=[6,24,72], variant active, Math.trunc만(범위 제한 없음) | ✓ |
| 7 | `CorrelationBanner` 상시·닫기불가·default+Info·축자 카피(D-10) | `CorrelationBanner.tsx` Alert default, Info, "상관 ≠ 인과", destructive/dismiss 없음 | ✓ |
| 8 | `ImpactStatusBadge`(ok=비교가능 green/insufficient=데이터부족 amber) + `EventTypeBadge`(EVENT_MARKERS outline)(D-07) | `ImpactStatusBadge.tsx` STATUS_BADGE_META, EVENT_MARKERS import, 색+라벨 동반 | ✓ |
| 9 | `EventImpactTable` ≥md 표, ok·insufficient 혼합(D-09) 정렬 유지, 앵커 노출(D-08) | `EventImpactTable.tsx` `hidden md:block`, sort() 부재, "이전 기준" 앵커 줄 | ✓ |
| 10 | `EventImpactCards` <md 카드(D-04), 동일 impactFormat/배지 소비 | `EventImpactCards.tsx` `md:hidden`, insufficientReason body+앵커 | ✓ |
| 11 | `ImpactPage` 조립: 배너+컨트롤+최신가+결과, useImpactParams 단일 소스, D-06 첫 품목 자동선택 | `ImpactPage.tsx` CorrelationBanner/ItemSelect/WindowControls/LatestPriceCard, items[0].id effect | ✓ |
| 12 | 결과 영역 독립 비동기 스코프(D-12) — 최신가 실패가 표를 가리지 않음 | `ImpactResults` 별도 컴포넌트, 자체 useEventImpact, LatestPrice(자체 AsyncBoundary)와 분리 | ✓ |
| 13 | ApiError.status 분기(D-03/D-09): 400("윈도우…"+24시간으로 보기 CTA)/404("존재하지 않는…")/네트워크 | `ImpactPage` ApiError 400/404/ErrorState 분기, 카피 10-UI-SPEC 축자 | ✓ |
| 14 | 200-empty(D-11) "등록된 이벤트가 없어요" + 본문 | `ImpactResults` events.length===0 블록(계약 제목+본문) | ✓ |
| 15 | 빌드 0 에러(tsc -b + vite) | `npm run build` ✓ (2593 modules) | ✓ |
| 16 | Java `src/` 무변경(read-only) | `git diff a3c16e1..HEAD` 에 `^src/` 0건 | ✓ |
| 17 | 라우트/AppLayout/내비 불변 | ImpactPage 본문만 교체, import-only 소비 | ✓ |
| 18 | 회귀 안전 — Phase 7/8/9 화면 무영향 | 신규 파일 + 단일 골격 교체, 전체 타입 그래프 빌드 통과 | ✓ |
| 19 | key_links 무결성(스키마·EVENT_MARKERS·ApiError·_shared) | 전 key_link grep ✓ | ✓ |
| 20 | (휴먼) 상관≠인과 가시성·희소/stale 납득성·% 부합·KST 9h 드리프트 없음·400/404/빈 카피·반응형 | 10-04 blocking checkpoint **"approved"** | ✓ |

## Self-Check Notes

- **자동 검증 스윕 1건 false positive 해명:** 오케스트레이터 사전 스윕에서 `30-min 하드코딩` 항목이 `-i` 패턴으로 주석 단어 "staleness"를 잡아 ✗로 표시됐으나, (a) 플랜의 실제 게이트(`! grep -qE "STALENESS|Duration|minutes"`)는 통과하고 (b) `insufficientReason` 본문에 숫자 임계 리터럴이 없음(앵커 null 도출만)을 확인 → D-08 충족. "30-minute staleness"는 디커플 사유를 설명하는 주석에만 존재.
- **편차 1건(10-04):** 200-empty 제목을 공용 `EmptyState`(고정 제목 "표시할 데이터가 아직 없어요")가 아닌 EmptyState 형태의 전용 블록으로 렌더해 10-UI-SPEC 계약 제목 "등록된 이벤트가 없어요"를 정확히 노출(중복 제목 회피). 사용자 카피는 계약과 축자 일치 — 정직성 강화, 스코프 크리프 없음.

## Verdict

**PASSED** — IMPCT-01~04 전부 코드/빌드/휴먼으로 충족. 백엔드 무변경. 상관≠인과·honest-data(희소/stale) 정직성이 화면으로 시연됨.

---
*Phase: 10-event-impact*
*Verified: 2026-06-27 (inline-orchestrator, human-verified)*
