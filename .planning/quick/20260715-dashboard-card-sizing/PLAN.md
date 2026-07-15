---
task: 대시보드 카드 크기 조정 3건
date: 2026-07-15
type: quick
requirements: [DASH-02, DASH-03]
---

# 대시보드 카드 크기 조정 (사용자 피드백 2026-07-15)

Phase 28 배포 전 실사용 확인에서 나온 3건. **크기 조정만** — 동작·데이터·백엔드 무변경.

## 실측 (1280px, dev)

| 항목 | 값 |
|---|---|
| 물품 카드 높이 / 간격 | **68px / 12px** |
| **6장 정확히** = 6×68 + 5×12 | **468px** (현재 416px → 5.2장에서 잘림) |
| 표가 원하는 폭 | **754px** (이벤트 104 · 종류 139 · 발생시각 182 · 상태 110 · 변화율 61 · 이전가 79 · 이후가 79) |
| 현재 중앙 칸 | **641px** → 113px 부족 → 가로 스크롤 |
| 주의 배너 본문 | 579px에 2줄. **1줄엔 658px 필요** |

## 🔑 3번을 고치면 2번(배너)이 저절로 풀린다

배너 본문은 `중앙폭 − 62px`(아이콘+패딩)을 받는다. 중앙이 816px가 되면 본문은 **754px** → 658px을
넘으므로 **한 줄이 된다.** 글자 크기를 줄이거나 배너를 손댈 필요가 없다 — 원인이 폭이었으므로
폭을 고치면 증상이 사라진다.

## 폭을 어디서 얻나 (사용자 확인: 모니터 1920px 이상)

**페이지 최대폭만 넓힌다. 소식 패널·카테고리는 그대로.** 화면이 1920px이라 넓힐 여지가 있으므로
무언가를 희생할 이유가 없다.

```
max-w-7xl(1280) → max-w-[90rem](1440)
  main 1440 − px-8(64) = 1376
  − gap(32×2=64) − nav(176) − news(320) = 중앙 816px ≥ 754 ✅
```

⚠️ **묶는 건 `AppLayout`의 `max-w-7xl`이다.** `DashboardPage` 그리드에도 자체 `max-w-7xl`이 있어
**둘 다** 풀지 않으면 안 먹는다(그리드가 1280에서 다시 잘림). `TopNav`도 같이 넓혀야 브랜드·메뉴가
본문과 계속 정렬된다.

⚠️ **`AppLayout`을 넓히면 `/timeline`도 넓어진다.** 차트 페이지라 이득이고, 그 외 화면은 `/admin`
(AppLayout 밖 형제 라우트)이라 무영향.

⚠️ **1280px 화면에선 표가 여전히 가로 스크롤된다** — 페이지를 넓혀도 화면이 1280이면 소용없다.
지금과 같은 동작이고(래퍼가 `overflow-x:auto`) 악화는 아니다. 사용자 화면 기준으로 해결한다.

## Tasks

### Task 1 — 박스 높이 416 → 468px (6장 온전히)

**files:** `frontend/src/features/dashboard/DashboardPage.tsx`

`lg:h-[26rem]`(416) → `lg:h-[468px]`. 실측 조합(6×68 + 5×12)이라 rem 환산(29.25rem)보다
`px`가 근거를 그대로 보여준다. 주석에 계산식을 남긴다.

**verify:** 1280px에서 박스 높이 468 · 6번째 카드 하단이 박스 하단과 일치 · 7번째는 안 보임.

### Task 2 — 이벤트 영향 제목에서 물품 이름 제거

**files:** `frontend/src/features/dashboard/DashboardPage.tsx` · `ItemImpactSection.tsx`

`이벤트 영향 · 유물 결투의 대가 각인서` → `이벤트 영향`. `displayName` prop 제거
(DashboardPage의 `selectedItem` 조회도 함께 제거 — 다른 용도가 없다).
어차피 사용자가 방금 누른 카드가 바로 위에 선택 표시된 채로 있으므로 이름은 중복이다.

**verify:** 제목이 `이벤트 영향`만.

### Task 3 — 기간 버튼 제거 + 페이지 폭

**files:** `ItemImpactSection.tsx` · `DashboardPage.tsx` · `AppLayout.tsx` · `TopNav.tsx`
· `frontend/src/features/impact/WindowControls.tsx` (**삭제**)

- `WindowControls` 렌더 제거 → 쓰는 곳이 여기뿐이라 **파일 삭제**(고아 방지).
- `window`를 상수 24로 고정. `DashboardPage`의 `impactWindow` state·`onWindowChange` 제거.
- **400 분기도 제거한다** — window가 항상 24면 백엔드 검증(1~168)을 절대 못 넘으므로 그 분기는
  도달 불가다. 남겨두면 "일어날 수 있는 일"이라고 거짓말하는 죽은 코드가 되고, 그 안의
  "24시간으로 보기" 버튼은 이미 24인 값을 24로 되돌리는 무의미한 동작이 된다.
  404·네트워크·빈 이벤트 분기는 **그대로 유지**(전부 여전히 도달 가능).
- `max-w-7xl` → `max-w-[90rem]`: `AppLayout`·`TopNav`. `DashboardPage` 그리드의 `max-w-7xl`은
  **제거**(껍데기가 폭을 소유 — 같은 매직넘버를 세 곳에 두지 않는다).

**verify:** 표에 가로 스크롤 **없음**(wrapper scrollWidth == clientWidth) · 배너 **1줄** ·
기간 버튼 없음 · 타임라인 회귀 없음 · `npm run build` 그린.

## 가드

백엔드 **0줄** · 마이그레이션 **0** · `EventImpactTable`/`Cards`/`impactFormat`/`CorrelationBanner`
**무변경** · 보석·소식·카테고리 **무변경**.
