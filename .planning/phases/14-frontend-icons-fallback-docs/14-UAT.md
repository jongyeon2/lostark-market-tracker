---
status: complete
phase: 14-frontend-icons-fallback-docs
source: [14-01-SUMMARY.md, 14-02-SUMMARY.md, 14-03-SUMMARY.md]
started: 2026-06-30T04:00:00Z
updated: 2026-06-30T06:30:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Dashboard 아이콘·역할 배지 + 역할군 정렬 (ICON-02)
expected: 대시보드 ItemCard 타이틀이 [아이콘][품목명][역할 배지] inline으로 보이고, 그리드가 역할군(딜러→서포터→융화재료→기타)→이름 순 정렬
result: pass
note: Playwright 실측 — 딜러9→서포터2→융화재료4(=15) 정렬, 실 CDN 아이콘+solid 역할 배지 inline 확인

### 2. 품목 셀렉터 역할군 그룹화 (ICON-03/06/07)
expected: 드롭다운이 딜러/서포터/융화재료/기타 섹션 헤더로 묶이고, 각 옵션에 [아이콘][품목명][역할 배지], 큐레이션 15개 누락 0
result: pass
note: Playwright 실측 — 딜러/서포터/융화재료 SelectLabel 헤더 + 옵션별 아이콘·배지, 선택 항목 체크, 15개 전부

### 3. 최신가 카드 아이콘·배지 (ICON-04)
expected: Timeline·Impact 양쪽 LatestPriceCard 타이틀에 아이콘 + 역할 배지가 품목명과 함께 표시
result: pass
note: Playwright 실측 — Timeline·Impact 모두 LatestPriceCard에 아이콘+융화재료 배지+154 G 확인

### 4. 이벤트 영향 정체성 아이콘·배지 1회 (ICON-05)
expected: Impact 페이지의 품목 정체성 영역(LatestPriceCard)에만 아이콘+배지 1회. 개별 이벤트 카드에는 역할 배지 미표시(EventTypeBadge/ImpactStatusBadge와 충돌 없음)
result: pass
note: Playwright 실측 — 정체성 카드 1회만, 이벤트 행은 outline(로아ON/대규모 업데이트)+tinted(비교 가능)만, 역할 배지 없음

### 5. 아이콘 fallback — 역할색 글리프 (ICON-01, D-02)
expected: iconUrl 없음 또는 로딩 실패 시 회색 플레이스홀더가 아니라 역할색 solid 글리프 타일(ScrollText/FlaskConical/Package)로 대체, 고정 슬롯이라 레이아웃 시프트 0
result: pass
note: Playwright 실측 — 15개 img 강제 로딩 실패 → 딜러 rose/서포터 emerald ScrollText, 융화재료 amber FlaskConical 타일로 대체, 레이아웃 시프트 0

### 6. 역할 배지 3색 처리 구별 (D-01/D-03)
expected: 역할 배지가 solid 색배경(딜러 rose / 서포터 emerald / 융화재료 amber) + 흰 텍스트 한글 라벨. 상태(tinted)·이벤트(outline) 배지와 처리 방식으로 즉시 구별
result: pass
note: Playwright 실측 — Impact 한 화면에 solid(역할)/outline(이벤트)/tinted(상태) 3처리 공존·구별 확인

### 7. README 시각 enrichment 문서 (ICON-08)
expected: 루트 README에 데이터 출처·API 실측 요약·fallback 전략·역할군=자산 섹터 서사 섹션, frontend/README에 ItemIcon 구현·fallback 확인법 기록
result: pass
note: 파일 실독 — 루트 README "시각 enrichment" 섹션(출처/실측/fallback/자산 섹터), frontend/README "아이콘+역할 배지" 섹션(ItemIcon 구현·fallback 확인) 확인

### 8. D-11 스크린샷 교체 (수동 사용자 액션)
expected: 아이콘·역할 배지가 반영된 새 3화면 스크린샷을 frontend/docs/screenshots/{dashboard,item-timeline,event-impact}.png로 직접 캡처·교체
result: pass
note: 사용자 선택('지금 캡처·교체')에 따라 Playwright(1280px viewport)로 3화면 재캡처 후 frontend/docs/screenshots/{dashboard,item-timeline,event-impact}.png 교체 — 아이콘·역할 배지 반영, seed 데이터

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]
