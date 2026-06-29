---
quick_id: 260630-0rh
slug: timeline-impact-latestpricecard
status: complete
date: 2026-06-30
commit: ec0bc97
files_modified:
  - frontend/src/features/_shared/LatestPriceCard.tsx
---

# Quick 260630-0rh: Timeline·Impact LatestPriceCard 긴 품목명 세로 잘림 수정 Summary

**Timeline·Impact 공유 정체성 카드의 긴 큐레이션 품목명이 좁은 카드에서 글자 단위로 세로 잘리던 문제를 CardTitle 폰트 축소(text-xl→text-base) + 이름 whitespace-nowrap으로 가로 한 줄 처리해 해소 — playwright before/after 검증**

## 문제 (근본 원인)

Phase 14-02에서 `LatestPriceCard`(Timeline·Impact 공유)의 CardTitle을 `text-xl` +
[ItemIcon][displayName][RoleBadge] flex 행으로 바꿨다. 이 카드는 `w-fit min-w-56`(≈224px)
좁은 폭이라, "상급 아비도스 융화 재료"·"유물 저주받은 인형 각인서" 같은 긴 이름의 span
텍스트가 글자 단위로 세로 줄바꿈되고 아이콘·배지가 가운데 끼어 깨졌다(원래 짧은 이름
"수호석 조각"용 설계). 데이터·404 문제가 아닌 순수 레이아웃 회귀.

## 수정

`frontend/src/features/_shared/LatestPriceCard.tsx` CardTitle만:
- `text-xl` → `text-base` (제목 폰트 축소)
- displayName `<span>`에 `whitespace-nowrap` 추가 → 한 줄 유지, `w-fit` 카드가 가로로 확장

Dashboard `ItemCard`는 별도 컴포넌트(넓은 그리드)라 미수정. 데이터 계약·zod·백엔드 무변경.

## 검증

- `npm run build`(tsc -b && vite build) **그린**
- playwright 캡처:
  - **before** (`timeline-before.png`): "상급/아비/도스/융화/재료" 5줄 세로 잘림, 아이콘·배지 중앙 끼임
  - **after** (`timeline-after.png`): `[아이콘] 상급 아비도스 융화 재료 [융화재료]` 가로 한 줄
  - **after** (`impact-after.png`): `[아이콘] 유물 저주받은 인형 각인서 [딜러]` 가로 한 줄, 하단 이벤트 표 충돌 없음(D-04 유지)
- 디버깅 부수 확인: 직전 콘솔 404는 `favicon.ico`(무해), "수집 헬스 실패"는 백엔드 시드 기동 중 일시 상태 — 새로고침 후 `/api/health/collection` SUCCESS 27/27. 데이터/API 버그 없음.

## Task Commit

- **fix: LatestPriceCard 가로 한 줄 + 제목 축소** — `ec0bc97`

## 결과

Timeline·Impact 두 탭의 최신가 카드가 긴 품목명에서도 가로 한 줄로 깔끔하게 렌더되고
레이아웃 비율이 정상화됨.
