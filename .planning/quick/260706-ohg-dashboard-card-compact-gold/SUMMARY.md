---
quick_id: 260706-ohg
slug: dashboard-card-compact-gold
status: complete
date: 2026-07-06
commit: 161018d
files_modified:
  - frontend/src/features/dashboard/ItemCard.tsx
  - frontend/src/features/dashboard/DashboardPage.tsx
---

# Quick Summary: 대시보드 카드 컴팩트화 + 골드 🪙

**웹 라이브 피드백 반영 — ItemCard 행 높이를 줄이고(py-6→py-3), 행 폭을 max-w-3xl로 제한해 이름↔가격 시선 이동을 줄였으며, 골드 가격에 🪙 이모티콘 추가**

## 변경
- **ItemCard 컴팩트:** `Card` 기본 세로 패딩 `py-6`→`py-3`, CardContent 잉여 `py-4` 제거 → 행 높이 대폭 축소. 가격/수집시각 블록 tighten(`space-y-0.5`, 수집시각 `text-sm`→`text-xs`), pending Skeleton `h-7 w-32`→`h-6 w-28`.
- **골드 이모티콘:** minPrice 앞에 `<span aria-hidden>🪙</span>` 인라인(gap-1, 가격 `font-medium`).
- **시선 이동 감소:** DashboardPage 아이템 섹션 래퍼 `max-w-3xl`로 행 폭 제한.

## 보존
딥링크 `/timeline?item=`·ItemIcon·RoleBadge·minPrice(`toLocaleString('ko-KR')`)·formatKst·pending/error/success 상태 매핑·focus-visible ring. 백엔드 0줄.

## 검증
`cd frontend && npm run build` 그린. 보존 요소 grep(deeplink/RoleBadge/formatKst) + 신규(🪙/max-w-3xl/py-3) 전부 확인.

## 비고
- 무관 파일 `build.gradle`(IDE 세션발 빈 줄)은 스테이징 제외 유지.
- 폭(`max-w-3xl`)·이모티콘 종류는 라이브에서 취향에 맞게 추가 조정 가능.

*Commit: 161018d*
