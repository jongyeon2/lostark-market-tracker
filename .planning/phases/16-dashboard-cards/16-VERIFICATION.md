---
phase: 16-dashboard-cards
verified_at: 2026-07-03
verdict: PASS
plans_verified: [16-01]
requirements_verified: [CARD-01, CARD-02]
---

# Phase 16 Verification — 대시보드 카드 개선

**Verdict: ✅ PASS** — Phase 16이 약속한 산출물(카드에서 의미 없는 고유 번호 제거 + 카드 클릭 시 해당 품목 선택 상태로 타임라인 딥링크)이 `ItemCard.tsx` 코드에 실재하며, v1.2 enrichment(아이콘·역할 배지)와 가격/수집시각은 무회귀 유지, 변경 파일 1개(백엔드 `src/` 0줄), `npm run build`(tsc -b && vite build) 그린.

## Goal-Backward 분석

**Phase 목표:** 대시보드 품목 카드에서 물품 고유 번호를 제거하고(아이콘·이름·골드·수집시각만), 카드 클릭 시 해당 품목이 선택된 상태로 품목 타임라인으로 딥링크 이동한다.

| 요구사항 | 약속 | 코드 증거 | 판정 |
|----------|------|-----------|------|
| CARD-01 | 카드에서 물품 고유 번호(`item.category` 코드) 제거 — 아이콘·이름·골드·수집시각만 | `ItemCard.tsx`에 `{item.category}` 부재(`grep -c`=0); `CardHeader`는 `<CardTitle>`(ItemIcon+displayName+RoleBadge)만, 대체 줄(itemGroup 등) 미추가(D-01) | ✅ |
| CARD-02 | 카드 클릭 시 해당 품목 선택 상태로 타임라인 딥링크 이동 | 반환 최상위가 `<Link to={\`/timeline?item=${item.id}\`}>`로 `<Card>` 래핑; `useNavigate` 부재(D-03); URL은 `?item=` 하나만(from/to 미전달)이고 `useTimelineParams`가 `?item=`을 `Number(...)`로 읽어 자동 선택(D-04, 타임라인 0줄) | ✅ |

## must_haves(PLAN 결정) 검증

| ID | 결정 | 코드 증거 | 판정 |
|----|------|-----------|------|
| D-01 | category 코드 줄 완전 제거, 다른 코드/숫자로 미대체 | `{item.category}` 렌더 `<p>` 삭제 확인 | ✅ |
| D-02 | ItemIcon·RoleBadge·minPrice.toLocaleString·formatKst·useLatestPrice 팬아웃·pending/error/success 인라인 분기 무회귀 | 6개 심볼 전부 잔존(grep pass), CardContent 3-상태 분기 원형 유지 | ✅ |
| D-03 | `<Card>` 전체를 react-router `<Link>`로 감싸 시맨틱 `<a>`(새 탭/Enter/우클릭), onClick+useNavigate 미채택 | `import { Link } from 'react-router-dom'` + `<Link to=...>` 래핑, `useNavigate` import 없음 | ✅ |
| D-04 | 딥링크 URL은 `/timeline?item={id}` 하나뿐, 타임라인 기본 30일 위임, 화면/선택 메커니즘 변경 0 | `to` 값이 `/timeline?item=${item.id}`; `useTimelineParams`(수정 없음)가 `?item=` 단일 출처로 처리 | ✅ |
| D-05 | hover는 elevation(shadow)+border 강조(accent fill 아님), accent(`--ring`/blue-600)는 focus-visible 링에만, `cursor-pointer` | `<Card className="cursor-pointer transition-shadow hover:border-foreground/20 hover:shadow-md">` — hover에 accent bg 클래스 없음; accent는 `<Link>`의 `focus-visible:ring-2 focus-visible:ring-ring`에만 | ✅ |
| D-06 | `<Link>` 래핑 후 focus-visible 링이 카드 외곽(rounded-xl 정합)에, 내부 자식은 비인터랙티브(단일 `<a>`) | `<Link>`에 `rounded-xl focus-visible:ring-*`; 내부 자식은 ItemIcon(img)·RoleBadge(span)·텍스트뿐 — 중첩 `<a>`/`<button>` 0 | ✅ |
| 파일 한정 | ItemCard.tsx 1파일, 백엔드 `src/` 0줄, 타임라인/그리드 무변경, build 그린 | `git diff --name-only` = `frontend/src/features/dashboard/ItemCard.tsx` 1개 | ✅ |

## 성공 기준(ROADMAP) 검증

1. 대시보드 카드에 물품 고유 번호가 없고 아이콘·품목명·골드 가격·수집 시각만 표시 — ✅ (CARD-01/D-01)
2. 카드 클릭 시 해당 품목 선택 상태로 타임라인 이동 — ✅ (CARD-02/D-03·D-04, `?item={id}` 딥링크)
3. v1.2 enrichment(아이콘·역할 배지)와 기존 카드 정보 무회귀 — ✅ (D-02, 6개 심볼 잔존)

## 빌드·게이트 검증

- **frontend build:** `npm run build`(tsc -b && vite build) **그린** — 2616 modules transformed, ✓ built. (청크 사이즈 경고는 기존 사항, 실패 아님)
- **grep 게이트:** `{item.category}` 부재 · `from 'react-router-dom'`·`/timeline?item=`·`focus-visible:ring`·`ItemIcon`·`RoleBadge`·`formatKst` 존재 · `useNavigate` 부재 — 전부 통과.
- **파일 한정 불변:** `git diff --name-only` = `frontend/src/features/dashboard/ItemCard.tsx` 1개 — 백엔드 `src/` 0줄(수집/캐시/event-impact 코어 경로 불변, T-16-BE 완화 확인).
- **위협 모델:** T-16-01(조작 `?item=`)은 `useTimelineParams`의 `Number(...)`→NaN→`itemId=null` 폴백으로 accept; T-16-02(중첩 인터랙티브)는 단일 `<a>` 내 비인터랙티브 자식만으로 mitigate.

## 미해결 항목 / 수동 액션

- **시각·기능 UAT(권장, 비차단):** `npm run dev`로 대시보드 카드 클릭 → `/timeline?item={id}` 이동해 해당 품목 선택된 차트 확인, Ctrl+클릭 새 탭·Tab 포커스 링(카드 외곽)·우클릭 메뉴, hover elevation(accent fill 없음) 직접 확인 권장(`/gsd-verify-work 16`).
- **code-review 게이트:** 이 환경에서 GSD 서브에이전트(gsd-code-reviewer)는 permission-denied이므로 subagent 자동 리뷰 미실행 — 변경 diff(1파일)를 인라인으로 직접 리뷰(grep + build + 접근성/중첩링크 점검)하며 진행(비차단, 15-VERIFICATION 선례 동일).

## 결론

코드 산출물이 phase 목표와 CARD-01·CARD-02, must_haves D-01..D-06, 성공 기준 1..3을 충족. frontend 빌드 그린, 변경 파일 1개(백엔드 0줄), 딥링크는 기존 타임라인 계약을 그대로 소비. **PASS** — 남은 것은 선택적 시각/기능 UAT뿐.
