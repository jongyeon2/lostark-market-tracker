# Phase 16: 대시보드 카드 개선 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-03
**Phase:** 16-대시보드 카드 개선
**Areas discussed:** 고유번호 자리 처리(CARD-01), 카드 클릭 구현(CARD-02), hover/포커스 피드백

---

## 고유번호 자리 (CARD-01)

| Option | Description | Selected |
|--------|-------------|----------|
| 완전 제거 | category 줄을 지워 카드가 아이콘·이름·역할배지·골드·수집시각만 남김. Goal 문구에 정확히 부합 | ✓ |
| itemGroup으로 대체 | "50010" 코드 대신 사람이 읽는 itemGroup("강화재료") 표시 | |

**User's choice:** 완전 제거
**Notes:** 제거 대상은 카드에 렌더되는 `item.category` 코드(예 "50010"). 아이콘·역할 배지는 유지(회귀 금지).

---

## 카드 클릭 구현 (CARD-02)

| Option | Description | Selected |
|--------|-------------|----------|
| `<Link>`로 카드 감싸기 | react-router `<Link to="/timeline?item={id}">`. 시맨틱 a태그 → Ctrl/중클릭 새 탭·키보드·우클릭 지원, 접근성 우수 | ✓ |
| onClick + useNavigate | onClick 핸들러로 프로그래매틱 이동. 간단하지만 새 탭·키보드 접근은 수동 추가 필요 | |

**User's choice:** `<Link>`로 카드 감싸기
**Notes:** 딥링크 대상 `/timeline?item={id}`는 타임라인이 이미 `?item=`을 단일 출처로 읽어(Phase 7 D-04) 그대로 동작 — 타임라인 변경 불필요.

---

## hover/포커스 피드백

| Option | Description | Selected |
|--------|-------------|----------|
| 은은한 강조 | cursor pointer + hover 살짝 강조 + focus-visible 링 | |
| 뚜렷한 강조 | hover 시 보더·그림자 뚜렷 + 살짝 리프트 | |
| Claude에게 위임 | UI-SPEC 토큰(accent 예약·기존 카드 선례)에 맞춰 planner/executor가 결정 | ✓ |

**User's choice:** Claude에게 위임
**Notes:** accent blue-600은 focus-visible 링에만 예약, hover는 elevation/보더로(accent fill 아님). 기존 카드 미감과 일관.

---

## Claude's Discretion

- hover/포커스 시각 피드백 수준(위 3번) — UI-SPEC 토큰·기존 카드 선례 준수 하에 결정.
- `<Link>` 래핑 시 접근성 마감(포커스 링 위치, 중첩 인터랙티브 요소 회피).

## Deferred Ideas

None — 논의는 페이즈 범위 내에서 유지됨.
