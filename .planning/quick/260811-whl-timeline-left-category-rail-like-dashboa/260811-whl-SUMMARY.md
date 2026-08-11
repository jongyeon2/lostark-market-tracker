---
quick_id: 260811-whl
slug: timeline-left-category-rail
description: 타임라인 품목 선택을 대시보드처럼 좌측 카테고리 레일 + 우측 품목 칩·차트로 개편
date: 2026-08-11
status: complete
commits: [60dffcb]
---

# SUMMARY — 타임라인 좌측 카테고리 레일 UI (quick-260811-whl)

타임라인 품목 선택기를 대시보드와 같은 **좌측 세로 카테고리 레일**로 교체. 백엔드 0줄.

## 무엇을 했나

`TimelinePage`를 2열 그리드(`lg:grid-cols-[18rem_minmax(0,1fr)]`)로 재구성:
- **좌(lg)** — sticky 흰 카드 안 `<CategoryNav>`(대시보드와 동일 스타일). 카테고리 선택.
- **우** — [선택 카테고리 품목 칩] + [기간 컨트롤] + [LatestPriceCard] + [차트].
- **모바일** — 1열 stack: CategoryNav가 가로 칩으로 자동 축약 → 품목 칩 → 차트(기존 ItemPicker 2단과 동일 흐름).

## 변경 파일

| 파일 | 변경 |
|---|---|
| `features/_shared/CategoryNav.tsx` | `features/dashboard/`에서 이동(git rename R096) + 헤더 주석 갱신 |
| `features/dashboard/DashboardPage.tsx` | import `./CategoryNav` → `@/features/_shared/CategoryNav` (동작 무변경) |
| `features/timeline/TimelinePage.tsx` | 2열 레이아웃 재작성. 카테고리 파생·활성카테고리·품목칩을 이관, 좌 CategoryNav/우 품목칩+차트. ChartArea 무변경 |
| `features/_shared/categories.ts` | "timeline's ItemPicker" 낡은 주석 → 공유 CategoryNav 레일로 갱신 |
| `features/_shared/ItemPicker.tsx` | **삭제**(사용처 소멸) |

## 핵심 설계 결정

- 🔑 **CategoryNav를 `_shared/`로 이동** — 타임라인이 대시보드 내부 모듈을 import하면 D-07 레이어 규칙
  위반(`categories.ts`를 _shared로 옮긴 그 이유). CategoryNav는 dashboard-local import가 없어 이동이
  깨끗하고, 사용처가 DashboardPage 하나뿐이라 import 경로만 갱신하면 됐다.
- **URL searchParams 단일 출처 유지**(useTimelineParams). 로컬 카테고리 state 0 — 활성 카테고리를
  "선택 품목이 속한 곳"으로 매 렌더 유도하고, 카테고리 클릭 시 첫 품목을 즉시 setItem하므로 둘이 어긋난
  상태가 존재할 수 없다(ItemPicker 무상태 수법·대시보드 selectedId 유도 계승).
- **보석 제외(gemCount=0)·기타 버킷 계승** — 보석은 시계열이 없어 그릴 수 없는 차트로 데려간다.

## 검증

- 🔑 **로컬 `node_modules`가 불완전**(typescript·.bin 미존재 — 이전 세션 EPERM 잔재로 추정)해 완전 빌드 불가.
  npx가 받은 7.0-preview tsc(CI보다 엄격)로 타입체크 시 **수정한 4개 파일 오류 0건**(오류는 전부 tsconfig
  baseUrl 폐기·vite.config node 타입 등 설정/환경 아티팩트). 코드 패턴은 기존 ItemPicker/DashboardPage에서 계승.
- 🚦 **최종 게이트 = PR CI**(러너 clean npm ci + `tsc -b && vite build`) — Task 2·postcss 선례와 동일.
- 배포 후 loaket.kr `/timeline`에서 좌측 레일·카테고리 클릭→첫 품목 선택·`?item=` 딥링크·모바일 stack 육안 확인(사용자 몫).

## 후속

- 브랜치 `quick/260811-whl-timeline-category-rail`(미push). PR 올려 frontend CI 그린 확인 후 머지 → 배포(승인).
- 이 레일에 새 전율 4종(quick-260811-w1h)도 배포 후 "재련보조" 카테고리에 자동 노출된다(두 작업이 만나는 지점).
