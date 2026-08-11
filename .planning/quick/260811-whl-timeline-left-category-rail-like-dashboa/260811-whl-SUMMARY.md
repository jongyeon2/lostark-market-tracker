---
quick_id: 260811-whl
slug: timeline-left-category-rail
description: 타임라인 품목 선택을 좌측 카테고리 아코디언 트리(리프 토글→펼친 품목 목록)로 개편, 우측 품목 칩 제거
date: 2026-08-11
status: complete
commits: [60dffcb, 67c2323]
---

# SUMMARY — 타임라인 좌측 카테고리 아코디언 트리 (quick-260811-whl)

타임라인 품목 선택기를 **좌측 세로 아코디언 트리**로 교체. 백엔드 0줄, 대시보드 무변경.

## 최종 설계 (사용자 피드백 반영 2회)

1차(60dffcb): 대시보드 CategoryNav를 좌측 레일로 + 우측 상단 품목 칩. → 라이브 확인 후 사용자가
"우측 아이템 칩을 없애고, 좌측 리프 카테고리에 토글을 달아 펼치면 그 안 품목 목록을 보여주는 방식"을
요청. 2차(67c2323)에서 아코디언 트리로 전환.

**최종**: 2열 그리드(`lg:grid-cols-[18rem_minmax(0,1fr)]`)
- **좌(lg)** — sticky 흰 카드 안 `TimelineItemTree`: 그룹 헤더(각인서/재료, 라벨) → 리프 카테고리
  (딜러 각인·서포터 각인·재련재료·상급재련·재련보조·아크그리드젬, 토글) → 펼치면 그 카테고리 품목
  목록(작은 아이콘+이름, 선택 항목 하이라이트). 품목 클릭 → 우측 차트 갱신.
- **우** — [기간] + [LatestPriceCard] + [차트]. **우측 품목 칩 제거.**
- **모바일** — 1열 stack(트리 → 차트). 트리는 전 화면폭에서 세로 렌더(별도 모바일 칩 행 없음).

## 변경 파일

| 파일 | 변경 |
|---|---|
| `features/timeline/TimelineItemTree.tsx` | **신설** — 아코디언 트리(그룹→리프 토글→품목 목록) |
| `features/timeline/TimelinePage.tsx` | 2열 재작성. 좌 트리/우 [기간·최신가·차트], 우측 칩·Chip 제거 |
| `features/_shared/categories.ts` | 공유 사유 주석 갱신(대시보드 CategoryNav + 타임라인 트리가 taxonomy 공유) |
| `features/dashboard/CategoryNav.tsx` | **원복**(_shared→dashboard) — 1차의 이동을 되돌림, 헤더 주석 원복 |
| `features/dashboard/DashboardPage.tsx` | import 원복(`./CategoryNav`) |
| `features/_shared/ItemPicker.tsx` | 삭제(1차, 사용처 소멸) |

## 핵심 설계 결정

- 🔑 **아코디언은 대시보드 CategoryNav와 공유하지 않는다.** 대시보드는 카테고리로 중앙 카드를 필터
  (flat 선택), 타임라인은 리프를 펼쳐 그 안 품목을 직접 고른다 — 동작이 근본적으로 다르다. 공유
  CategoryNav에 토글/인라인 목록을 넣으면 대시보드까지 바뀌므로, **타임라인 전용 트리를 새로 만들고
  CategoryNav를 대시보드 전용으로 되돌렸다**(대시보드 순 diff 0). taxonomy(`_shared/categories`)만 공유.
- 🔑 **아코디언 한 번에 하나**(사용자 결정). `expandedId`는 undefined 센티넬 방식 — 수동 조작 전엔
  "선택 품목이 속한 카테고리"를 따르고(진입·딥링크·첫 품목 자동선택 후에도 자동 펼침, useEffect 불필요),
  토글을 만지면 수동값이 이긴다(같은 걸 다시 누르면 전부 접힘). 선택은 펼친 카테고리 안에서만 일어나
  활성·펼침이 어긋나지 않는다.
- URL searchParams 단일 출처 유지. 보석 제외(gemCount=0)·기타 버킷 계승.

## 검증 (로컬 dev 풀스택, Docker on)

- ✅ `npm run build`(tsc -b + vite build) 그린 — node_modules 완성 후 실제 빌드 통과.
- ✅ **라이브 육안 검증**(bootRun dev + vite + playwright 스크린샷 3장): 진입 시 선택 품목 카테고리
  자동 펼침 / 재련보조 클릭 → 아코디언 단일 전환 + 전율 4종 목록 노출 / 전율 클릭 → `?item=56` 선택·
  하이라이트·차트 갱신. **대시보드(/dashboard) 회귀 0.**
- 🎁 전율 차트는 백엔드 백필(Phase 17.4)이 상세 Stats[]에서 8/5(레이드) 일별 평균을 채워 **실데이터
  곡선**으로 렌더(빈 "수집 중" 아님) — 신규 아이템 전 파이프라인 실증.

## 후속

- 브랜치 `quick/260811-whl-timeline-category-rail`(미push). PR 올려 frontend CI 그린 후 머지 → 배포(승인).
- 전율 4종(quick-260811-w1h)도 배포 후 이 트리의 "재련보조"에서 실시세로 확인된다.
