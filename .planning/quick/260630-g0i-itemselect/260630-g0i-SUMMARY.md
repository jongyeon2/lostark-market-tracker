---
quick_id: 260630-g0i
slug: itemselect
status: complete
date: 2026-06-30
files_modified:
  - frontend/src/features/_shared/ItemSelect.tsx
---

# Quick 260630-g0i: ItemSelect 트리거 카테고리명만 표기 Summary

**Timeline·Impact 공유 드롭다운(`ItemSelect`)의 트리거가 긴 품목명으로 잘리던 문제를, 트리거에 상위 카테고리 라벨(딜러/서포터/융화재료/기타)만 렌더하고 폭을 좁혀 해결. 목록은 개별 품목(아이콘+이름+배지) 그대로 유지, 실제 품목명은 페이지 LatestPriceCard에 표시되어 정보 손실 없음. playwright 실데이터로 검증.**

## 문제

`SelectValue`가 선택된 옵션의 전체 content(아이콘+긴 이름+배지)를 트리거에 그대로 복제 →
"유물 저주받은 인형 각인서" 같은 긴 이름이 트리거 폭(w-72)을 넘겨 잘림.

## 수정 (`frontend/src/features/_shared/ItemSelect.tsx`)

- `value`로 선택 item을 찾아 `triggerLabel = roleGroup ? ROLE_LABEL[roleGroup] : '기타'` 계산.
- 트리거를 `SelectValue` 대신 `{triggerLabel ?? <placeholder>}`로 직접 렌더 — 카테고리 라벨만 표기.
- 트리거 폭 `w-72` → `w-40`, pending Skeleton `w-56`→`w-40`, error trigger `w-72`→`w-40` 정합.
- 드롭다운 목록(SECTIONS/SelectItem: 아이콘+이름+배지) 무변경 — 모든 품목 선택 가능.
- 정보 손실 없음: 선택 품목명은 TimelinePage/ImpactPage의 `LatestPriceCard`에 표시(확인됨).

## 검증

- `npm run build`(tsc -b && vite build) **그린** (triggerLabel 미사용 진단은 Edit 중간 stale, tsc 통과).
- playwright(5173, dev 실데이터):
  - 기본 선택(융화재료): 트리거 text="융화재료", clientWidth 158 = scrollWidth 158, **clipped=false**.
  - 긴 이름 선택(유물 저주받은 인형 각인서): 트리거 text=**"딜러"**, 158=158, **clipped=false**, `cardShowsFullName=true`(LatestPriceCard에 풀네임).
  - 드롭다운 목록: 딜러9·서포터2·융화재료4 그룹 + 옵션별 품목명+배지 유지.

## 결과

긴 각인서 이름에도 트리거가 짧은 카테고리 라벨 한 줄로 고정되어 잘림 0. 목록 선택성·품목 식별성(카드)은 그대로. Timeline·Impact 동시 적용(공유 컴포넌트 1곳).
