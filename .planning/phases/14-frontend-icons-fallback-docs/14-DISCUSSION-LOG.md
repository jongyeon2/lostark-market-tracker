# Phase 14: Frontend Icons + Fallback + Docs - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-29
**Phase:** 14-Frontend Icons + Fallback + Docs
**Areas discussed:** 역할 시각 시스템, 동일 각인서 아이콘 처리, 셀렉터·대시보드 조직, Docs 범위·서사

---

## 역할 시각 시스템

### 역할 3군 팔레트
| Option | Description | Selected |
|--------|-------------|----------|
| 도메인 직관색 | 딜러=레드(공격), 서포터=블루/그린(회복·버프), 융화재료=앰버/골드(재화) | ✓ |
| slate 테마 단색 변주 | 채도 낮은 통일감, 3군 구분 약함 | |
| 다른 색 구성(직접 설명) | 자유 입력 | |

### fallback lucide 글리프 세트
| Option | Description | Selected |
|--------|-------------|----------|
| ScrollText + FlaskConical | 각인서=ScrollText, 융화재료=FlaskConical, D-05 힌트 충실 | ✓ |
| BookMarked + Hammer | 각인서=책, 융화재료=강화 망치 | |
| You decide | lucide 내 가독성 best 자동 | |

### 역할 배지 형태
| Option | Description | Selected |
|--------|-------------|----------|
| 색 배경 텍스트 배지 | 한글 라벨, 기존 ui/badge.tsx + StatusBadge 관습 재사용 | ✓ |
| 색 dot + 텍스트 | 가벼움, 시선 끄는 힘 약함 | |
| 아이콘(글리프) 배지 | 텍스트 없음, 식별 약함 | |

### 역할 배지 배치
| Option | Description | Selected |
|--------|-------------|----------|
| 품목명 옆 inline | 이름과 함께 읽혀 식별 명확, 4곳 공통 적용 쉬움 | ✓ |
| 아이콘 코너 오버레이 | 작은 셀렉터 옵션에서 가독성 저하 | |
| 카드 상단 헤더 | 셀렉터 옵션엔 부적합, 일관성 깨짐 | |

**User's choice:** 네 가지 모두 추천안 선택
**Notes:** fallback 비주얼(팔레트·글리프)과 배지(형태·배치)를 한 묶음으로 정해 일관된 시각 언어 확보.

---

## 동일 각인서 아이콘 처리

### 11종 동일 아이콘 반복 처리
| Option | Description | Selected |
|--------|-------------|----------|
| 실아이콘 그대로 + 배지/라벨 식별 | 실측 데이터 정직 노출, 라벨 병기(D-06) + 역할 배지가 식별 캐리 | ✓ |
| 각인서는 ScrollText 글리프 강제 | 반복 회피하나 실아이콘 버림, 서사 약화 | |
| 아이콘 위 이니셜 오버레이 | 지저분, 라벨 병기와 중복 | |

### `<ItemIcon>` fallback 트리거 범위
| Option | Description | Selected |
|--------|-------------|----------|
| onError + iconUrl null 둘 다 | 로딩 실패·null/미제공 모두 글리프 흡수, 가장 방어적 | ✓ |
| onError만 | null/미제공 케이스 미방어 | |

**User's choice:** 두 가지 모두 추천안 선택
**Notes:** "fallback이 미완성처럼 안 보이는지"(사용자 확인 포인트)와 직결 — 실아이콘 + 라벨/배지로 식별.

---

## 셀렉터·대시보드 조직

### 품목 정렬 기준(공통)
| Option | Description | Selected |
|--------|-------------|----------|
| 역할군 → 이름 | DEALER→SUPPORT→MATERIAL 후 이름순, 배지·색과 시너지 | ✓ |
| 이름순 평면 | 단순, 역할 군집 안 보임 | |
| 기존 순서 유지 | 15개 추가 후 무질서해 보일 수 있음 | |

### ItemSelect 역할군 그룹 헤더
| Option | Description | Selected |
|--------|-------------|----------|
| 역할군 그룹 헤더 표시 | SelectGroup/SelectLabel 섹션 구분(정적, v2 필터와 다름) | ✓ |
| 평면 목록 + 배지로 구분 | 그룹 헤더 없이 배지만 | |

**User's choice:** 두 가지 모두 추천안 선택
**Notes:** 필터 컨트롤은 v2 확정 — 정적 그룹 헤더 + 배지로 대체.

---

## Docs 범위·서사

### README 역할 분담
| Option | Description | Selected |
|--------|-------------|----------|
| 루트=서사, frontend=실행 | 루트=출처·실측·도메인 안목 서사, frontend=구현·실행 | ✓ |
| 둘 다 동일 내용 | 유지보수 중복 위험 | |
| 루트 README에만 | frontend/README 빈약, ICON-08 충족 애매 | |

### 스파이크 findings 노출 정도
| Option | Description | Selected |
|--------|-------------|----------|
| 요약 + findings 링크 + 자산섹터 비유 | 출처·실측·fallback 요약 + 12-SPIKE-FINDINGS.md 링크 + 서사 | ✓ |
| findings 전체 전사 | README 비대 | |
| 최소(출처·fallback 한 줄) | 서사·안목 안 드러남 | |

### 스크린샷 갱신
| Option | Description | Selected |
|--------|-------------|----------|
| 아이콘·배지 반영 새 스크린샷 | 3화면 갱신, 시각 enrichment 실제 증거 | ✓ |
| 이번 페이즈엔 생략 | 시각 기능인데 증거 없음 | |
| 기존 스크린샷 유지 | 구버전 오해 소지 | |

**User's choice:** 세 가지 모두 추천안 선택
**Notes:** 스크린샷 실제 캡처는 수동(앱 실행 + 브라우저) — 사용자 액션.

---

## Claude's Discretion
- 정확한 역할색 hex/Tailwind 토큰·대비, 글리프 크기, `<ItemIcon>` 슬롯/`img` sizing·`alt` 문구, 배지 spacing
- zod 스키마 확장 형태(enrichment 3필드 배치, event-impact wrapper nesting 매핑), 클라 정렬 구현 위치
- `SelectGroup` 헤더 라벨 문구, README 정확 문구·스크린샷 도구

## Deferred Ideas
- 그룹 필터 컨트롤 — v2(FILTER-V2-01)
- 등급별 색상·정렬 정교화 — v2(GRADE-V2-01)
- 운명 계열 융화재료 / 만개·구원 각인서 — Phase 12 미확인, v1.2 범위 밖(큐레이션 15개 고정)
- 다크모드·i18n·실시간 갱신 — FE-V2
