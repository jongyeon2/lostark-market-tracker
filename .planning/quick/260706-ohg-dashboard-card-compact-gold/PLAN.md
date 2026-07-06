---
quick_id: 260706-ohg
slug: dashboard-card-compact-gold
date: 2026-07-06
files_modified:
  - frontend/src/features/dashboard/ItemCard.tsx
  - frontend/src/features/dashboard/DashboardPage.tsx
---

# Quick: 대시보드 카드 컴팩트화 + 골드 이모티콘

**요청(웹 라이브 피드백):** 대시보드 각 물품 카드가 너무 길고 커서 이름(좌)↔가격(우) 시선이 두 번 움직임. 카드 크기를 줄이고, 골드 가격에 이모티콘을 붙여 직관성 향상.

## 변경
1. **ItemCard 컴팩트화** — `Card`의 기본 세로 패딩 `py-6`→`py-3`, CardContent의 잉여 `py-4` 제거(세로 높이 축소). 가격/수집시각 블록 tighten(space-y-0.5, 수집시각 text-xs).
2. **골드 이모티콘** — minPrice 앞에 🪙 추가(`text-base` 유지, gap-1 인라인).
3. **시선 이동 축소** — DashboardPage 아이템 섹션 래퍼에 `max-w-3xl`로 행 폭 제한(이름↔가격 거리 단축).

## 보존(회귀 금지)
딥링크 `/timeline?item=`·ItemIcon·RoleBadge·minPrice·formatKst·pending/error/success 상태 매핑·focus-visible ring. 백엔드 0줄.

## 검증
`cd frontend && npm run build` 그린 + 딥링크/RoleBadge/formatKst grep 존재.
