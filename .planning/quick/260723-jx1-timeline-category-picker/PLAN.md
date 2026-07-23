---
quick_id: 260723-jx1
slug: timeline-category-picker
description: 타임라인 품목 선택을 49개 드롭다운에서 카테고리 칩 + 품목 칩 2단으로 교체
date: 2026-07-23
status: in-progress
---

# Quick Task 260723-jx1 — 타임라인 품목 선택: 드롭다운 → 2단 칩

## 왜 — 49개를 드롭다운 하나에 넣어 두고 스크롤로 찾게 하고 있다

사용자 지적: *"드롭다운을 눌러 스크롤을 내리고 원하는 품목을 찾아야 하는데 귀찮고 거슬린다."*

배포 사이트에서 확인한 결과 지적이 정확하고, **그보다 더 나쁜 결함이 하나 더 있다.**

### 결함 1 — 트리거가 품목명을 안 보여준다

```
[ 재료 ▾ ]  ← 실제 선택된 품목은 "빙하의 숨결"
```

`ItemSelect`는 트리거에 **역할군 라벨**(`딜러`/`서포터`/`재료`/`기타`)만 렌더한다. 지금 무엇을 보고 있는지 선택기만 봐서는 알 수 없고, 이름은 아래 `LatestPriceCard`에만 있다.

주석에 이유가 적혀 있다 — *"a long engraving name otherwise overflows the compact trigger"*. 진단은 맞지만 처방이 틀렸다. **넘치면 넓히거나 말줄임할 일이지, 이름을 지울 일이 아니다.**

### 결함 2 — 49개를 4개 그룹에 넣고 스크롤로 찾게 한다

`GET /api/items` 실측: **49개** (딜러 각인 11 · 서포터 각인 7 · 재련재료 11 · 상급재련 8 · 재련보조 6 · 아크그리드젬 6).

## 무엇으로 바꾸나 — 대시보드가 이미 쓰는 언어를 그대로

```
[딜러 각인 11] [서포터 각인 7] [재련재료 11] [상급재련 8] [재련보조 6] [아크그리드젬 6]
[🧊 빙하의 숨결] [🔥 ...] [...]        ← 선택 카테고리의 품목만, 최대 11개
────────────────────────────────────────
[7일][30일][90일]  시작일 …  종료일 …
```

- **스크롤 헌팅 0회** — 한 화면에 최대 11개
- **대시보드와 같은 taxonomy** → 학습 비용 0, `categories.ts` 재사용
- **지금 무엇을 보는지 항상 화면에 있다** (활성 칩 + 아래 `LatestPriceCard`)

검색 콤보박스는 차선으로 판단했다: 49개는 검색이 필요할 만큼 많지 않고, 이 화면은 *"정확히 아는 걸 찾는"* 곳보다 *"뭐가 있나 둘러보는"* 곳에 가깝다.

## 설계 결정

**D-1. `categories.ts`를 `features/dashboard/` → `features/_shared/`로 옮긴다.**
타임라인이 대시보드 내부 모듈을 import하면 이 코드베이스가 이미 세운 규칙(D-07: *"extracted to _shared so the layer dependency never flows impact→timeline"*)을 깬다. 이제 두 화면이 공유하는 taxonomy이므로 `_shared`가 제자리다. 순수 rename + import 2줄.

**D-2. 타임라인은 `deriveCategories(items, 0)`으로 부른다 — 보석 leaf 없음.**
보석은 `TrackedItem`이 아니라 시계열이 없다. gemCount=0이면 보석 leaf가 아예 생성되지 않으므로, 차트를 그릴 수 없는 카테고리가 화면에 뜨는 일이 없다. 별도 분기 불필요.

**D-3. 카테고리를 바꾸면 그 카테고리의 첫 품목을 자동 선택한다.**
차트는 `itemId`가 null일 수 없다(빈 화면 금지 — 기존 D-06과 같은 원칙). 카테고리만 바꾸고 아무 일도 안 일어나면 활성 칩이 없는 상태가 되어 더 헷갈린다. 한 번 더 눌러 정확한 품목으로 가면 된다.

**D-4. 어느 카테고리에도 안 잡히는 물품은 `기타`로 모은다.**
현재는 발생하지 않는다(실측: 11+7+31 = 49 = 전체, 누락 0). 하지만 기존 `ItemSelect`가 명시적으로 보장하던 것이라(*"null roleGroup is included as a '기타' section so NO curated item is ever dropped (ICON-07)"*) 교체하면서 그 보장을 잃을 수는 없다. 시드가 늘어 분류가 밀릴 때 조용히 사라지는 대신 `기타`에 뜬다.

**D-5. `ItemSelect.tsx`는 삭제한다.**
`/impact` 페이지가 사라진 뒤(Phase 28) 유일한 소비자가 `TimelinePage`였다. 교체하면 아무도 안 쓴다 — 안 쓰는 선택기를 남겨 두면 다음 사람이 둘 중 뭘 쓸지 고민한다.

## 작업

1. `features/dashboard/categories.ts` → `features/_shared/categories.ts` (rename), `DashboardPage`·`CategoryNav` import 갱신.
2. `features/_shared/ItemPicker.tsx` 신설 — 카테고리 칩 행 + 품목 칩 그리드. 활성 상태는 색만으로 표시하지 않는다(`aria-current` + 배경 + 굵기, `CategoryNav` 규칙 그대로).
3. `TimelinePage`가 `ItemSelect` 대신 `ItemPicker` 사용. 컨트롤 바 재배치(선택기가 세로로 자라므로 기간 컨트롤을 아래 줄로).
4. `features/_shared/ItemSelect.tsx` 삭제.

## 검증

- `npm run build` (tsc -b + vite build)
- `VITE_API_TARGET=https://loaket.kr npm run dev` — 운영 실데이터로 확인
  - 6개 카테고리 전부 렌더, 합계 49
  - `?item=N` 진입 시 그 품목이 속한 카테고리가 활성화되는가
  - 카테고리 전환 → 첫 품목 자동 선택 → 차트 갱신
  - 품목명이 화면에 항상 보이는가(결함 1 해소)

## 하지 않는 것

- **검색/필터 입력** — 49개엔 과하다. 품목이 세 자릿수가 되면 그때.
- **기간 컨트롤 변경** — 이번 지적 범위 밖.
- **대시보드 변경** — `categories.ts` import 경로만 바뀌고 동작은 동일해야 한다.
- **백엔드** — 0줄.
