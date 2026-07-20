---
quick_id: 260720-jkx
slug: dark-mode
description: 다크모드 — 클래스 기반 테마 전환 + 하드코딩 색 토큰화
date: 2026-07-20
---

# Quick Task 260720-jkx — 다크모드

`index.css`가 "Light single theme only (dark mode is FE-V2-03)"로 미뤄둔 항목. 프로젝트 마지막
기능으로 착수한다. 프론트만, 백엔드 0줄.

## 사전 조사 — 왜 싼가

색을 전부 CSS 변수로 소비하는 구조라 대부분이 `.dark` 블록 하나로 따라온다. **하드코딩 색을 전수
조사한 결과 5곳뿐**이었다:

| 위치 | 내용 |
|---|---|
| `PriceTimelineChart.tsx` | grid `#E2E8F0`, tick `#64748B`×2, 시리즈 `#2563EB`/`#D97706` |
| `impactFormat.ts` | `text-[#DC2626]` / `text-[#1D4ED8]` / `text-[#64748B]` |
| `eventMarkers.ts` | 이벤트 8색 hex |
| `class-icons/*.svg` 30개 | `fill="#222222"` — 다크에서 안 보임 |
| `--grade-*` 5색 | 흰 배경 기준으로 어둡게 튜닝됨 |

나머지 `text-white` 용례는 전부 진한 색 알약 위라 테마와 무관하다.

## 🔑 함정 — `impactFormat.ts`를 토큰으로 "정리"하면 안 된다

세 hex를 기존 `--up`/`--down`으로 바꾸는 게 자연스러워 보이지만 **틀렸다**. 주석에 명시돼 있듯이
이 화면은 국내 거래소/MTS 관례(**상승=빨강 · 하락=파랑**)를 따르고, 이는 `--up`(green) /
`--down`(red)과 **의미 축이 정반대**다. 같은 토큰을 쓰면 라이트에서 색이 뒤집힌다.

→ 전용 토큰 `--change-up` / `--change-down` / `--change-flat`을 새로 만들고, 라이트 값은 **지금 hex
그대로** 옮긴다(라이트 화면 무변경). 다크 값만 새로 정한다.

## 작업

### 1. `index.css` — 테마 기반

- `@custom-variant dark (&:where(.dark, .dark *))` — Tailwind v4의 `dark:`는 기본이 미디어쿼리라
  수동 토글이 안 먹는다. 클래스 기반으로 전환.
- `.dark { … }` 블록: `:root`의 모든 토큰을 다크 값으로 미러링(shadcn 표준 slate 계열, 사용자 결정).
  라이트가 bg=slate-50 / card=흰색(카드가 배경보다 밝음)이므로 다크도 같은 관계를 유지한다
  (bg=slate-950 / card=slate-900).
- 신규 토큰 3종: `--change-up/down/flat`(위 함정), 차트용 `--chart-min`/`--chart-backfill`,
  이벤트 8색 `--event-*`.
- `--grade-*`는 다크에서 밝은 변형으로. 라이트 값은 손대지 않는다.

### 2. FOUC 차단 + 토글

- `index.html` `<head>`에 **페인트 전 인라인 스크립트**: localStorage 값이 있으면 그것을, 없으면
  `prefers-color-scheme`를 읽어 `<html>`에 `dark`를 붙인다. React가 뜨기 전에 끝나야 흰 화면이
  번쩍하지 않는다(그래서 컴포넌트가 아니라 인라인 스크립트다).
- `ThemeToggle` — 해/달 버튼 하나(2단계, 사용자 결정). 첫 방문은 시스템 설정, 한 번 누르면 그
  선택을 localStorage에 기억. `TopNav` 우측에 배치.
- 수동 선택 전에는 시스템 설정 변경을 실시간으로 따라간다(`matchMedia` 리스너).

### 3. 하드코딩 색 → 토큰

- `PriceTimelineChart`: hex → `var(--…)`. Recharts는 SVG `stroke`/`fill`에 CSS 변수를 그대로 받는다.
- `impactFormat.ts`: `text-[#…]` → `text-change-up` 등.
- `eventMarkers.ts`: `color: '#7C3AED'` → `color: 'var(--event-loa-on)'`. 소비처 3곳(차트 `stroke`,
  배지 inline `style`, 범례 `backgroundColor`)이 전부 var()를 그대로 받으므로 **소비처 수정 0**.
- 클래스 아이콘: `<img>`에 `dark:invert`. 모노크롬 `#222222`라 반전하면 `#DDDDDD`가 된다.
  마스크/두 번째 아이콘 세트보다 싸고, `onError` 폴백도 그대로 살아있다.

## 검증

- `npm run build` 통과.
- **대비 실측**: 다크 배경 위 본문·등급색·차트색 전부 스크립트로 계산해 기록. 텍스트 AA 4.5:1,
  비텍스트(차트 선·마커) 3:1 기준.
- 라이트 화면이 **한 픽셀도 안 변해야 한다** — 라이트 토큰 값을 하나도 바꾸지 않는 것으로 보장하고,
  라이트/다크 양쪽 라이브 QA로 확인.
- 전 라우트(대시보드·타임라인·아바타·모험의 서) 라이트/다크 육안 확인.

## 범위 밖

- 백엔드 0줄. 테마는 순수 클라이언트 상태(서버 저장 안 함).
- 관리자 콘솔(`/admin`)은 별도 셸이라 이번에 포함하되 정밀 QA는 하지 않는다.
