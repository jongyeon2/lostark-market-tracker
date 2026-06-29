# Phase 12: API Spike + Data Lock (게이트) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-29
**Phase:** 12-API Spike + Data Lock (게이트)
**Areas discussed:** 큐레이션 구성·분류, Fallback·아이콘 구별, findings 산출물 계약, 스파이크 실행·안전캡처

---

## 큐레이션 구성·분류

### 융화재료 범위
| Option | Description | Selected |
|--------|-------------|----------|
| 오레하 상급+최상급만 | 거래 활발·고변동 핵심 2종, 실측 확정 가장 안전 | |
| +아비도스 융화재료 (추천) | 오레하 2종 + 아비도스, 상위 티어 커버; 운명 계열은 보류 | ✓ |
| +아비도스+운명 계열 | 최대 범위, 운명 계열은 재련재료일 수 있어 실측 필수 | |

**User's choice:** +아비도스 융화재료 (추천)
**Notes:** 운명 계열은 스파이크 거래/식별 확인 시에만 추가 검토 → Deferred로 보존.
**보정 (2026-06-29, 논의 후):** 사용자 도메인 확인으로 **상급 아비도스 융화재료(T4)** 추가 — 아비도스 융화재료가 등급(아비도스/상급 아비도스)으로 갈리는데 초안이 한 종으로 뭉쳤음. 융화재료 3종 → **4종**(상급 오레하/최상급 오레하/아비도스/상급 아비도스), 총 ~16 → **~17개**. 거래 가능·id·iconUrl은 스파이크 실측 확정.

### 각인서 구성 기준
| Option | Description | Selected |
|--------|-------------|----------|
| 딜러·서포터 균형 (추천) | 딜러 다수 + 서포터 각인 포함, role_group 3군 데모 선명 | ✓ |
| 거래량·가격 상위 | 등급·거래 상위 자동 선별, 서포터 누락 위험 | |
| 딜러 중심 | 대표 딜러 각인 위주, role_group 구분 가치 약함 | |

**User's choice:** 딜러·서포터 균형 (추천)

### role_group 분류 체계
| Option | Description | Selected |
|--------|-------------|----------|
| 3값 DEALER/SUPPORT/MATERIAL (추천) | 배지 3색 구분, item_group은 별도 | ✓ |
| 4값 +COMMON | 공용 각인 분리, 12~20 규모엔 과함 | |
| 2값 ENGRAVING/MATERIAL | 딜러/서포터 미구분, 균형 선택 효과 안 보임 | |

**User's choice:** 3값 DEALER/SUPPORT/MATERIAL (추천)
**Notes:** item_group은 각인서/강화재료(융화재료) 품목군 구분으로 별개 사용.

### 총 큐레이션 개수 목표
| Option | Description | Selected |
|--------|-------------|----------|
| ~16개 중간 (추천) | 융화재료 3 + 각인서 ~13 (딜러 ~9/서포터 ~4) → 보정 후 융화재료 4 + 각인서 ~13 = ~17 | ✓ |
| ~12개 최소 | 가장 깔끔, 서포터 수 적어 균형 얕아짐 | |
| ~20개 상한 | 가장 풍부, 실측·seed 분량·거래확인 폭 큼 | |

**User's choice:** ~16개 중간 (추천)
**Notes:** 최종 개수는 스파이크 거래확인 통과분으로 12~20 범위 내 확정.

---

## Fallback·아이콘 구별

### 아이콘 부재/실패 fallback 비주얼
| Option | Description | Selected |
|--------|-------------|----------|
| 역할색 배경 + lucide 글리프 (추천) | role_group 색 + 의미 글리프(각인서=책, 융화재료=플라스크/망치), lucide만 사용 | ✓ |
| 역할색 배경 + 한글 이니셜 | 품목명 첫 글자, 한글 이니셜 처리 로직 필요 | |
| 역할색 배경 + 글리프 + 이니셜 | 정보량 최대, 작은 슬롯에서 복잡 | |

**User's choice:** 역할색 배경 + lucide 글리프 (추천)
**Notes:** 고정 슬롯·레이아웃 시프트 없음은 ICON-01에서 이미 확정. 글리프·팔레트는 Phase 14 재량.

### 각인서 아이콘 동일 시 처리
| Option | Description | Selected |
|--------|-------------|----------|
| 라벨 병기로 충분 (추천) | 품목명 라벨이 항상 붙어 식별 가능, findings에 구별 여부만 명시 | ✓ |
| 동일 시 역할배지 강조 | 시각 구분을 배지에 위임, 사실상 추천과 중복 | |
| 동일 시 fallback 글리프 강제 | 실 아이콘 버림, 과함·비직관 | |

**User's choice:** 라벨 병기로 충분 (추천)
**Notes:** 스파이크는 각 iconUrl 구별 여부를 눈으로 확인해 findings에 "구별됨/동일함" 명시(Pitfall 7).

---

## findings 산출물 계약

### 품목당 잠금 스키마
| Option | Description | Selected |
|--------|-------------|----------|
| 최소 6필드 계약 (추천) | external_item_id/display_name/category_code/icon_url/item_group/role_group | ✓ |
| + grade 증거 포함 | 6필드 + grade(유물 확인 증거), seed 계약은 6필드 유지 | |
| + grade + bundle_count | 6필드 + 등급 + 번들수량, enrichment 범위 밖 데이터까지 | |

**User's choice:** 최소 6필드 계약 (추천)
**Notes:** 파일 = `12-SPIKE-FINDINGS.md` (TASK0-FINDINGS 포맷). grade·CategoryCode는 산문 증거로만, 가격·키 제외. 6필드 표 + 마스킹 발췌 구성.

---

## 스파이크 실행·안전캡처

### 실행 메커니즘
| Option | Description | Selected |
|--------|-------------|----------|
| 기존 spike 인프라 확장 (추천) | MarketsApiSpikeTest + /markets/options + 카테고리 검색, LostarkSpikeClient options 메서드, @Disabled·spike 프로파일 | ✓ |
| 신규 별도 spike 테스트 | EnrichmentSpikeTest 신규, 클라이언트 중복 부담 | |
| 당신이 결정 | planner/executor 재량 (기본: 확장) | |

**User's choice:** 기존 spike 인프라 확장 (추천)

### 안전 캡처 절차
| Option | Description | Selected |
|--------|-------------|----------|
| 콘솔 출력 → 마스킹 수기정리 (추천) | 콘솔 출력만(디스크 미기록) → 6필드 표·마스킹 발췌 → 커밋 전 grep | ✓ |
| 마스킹 초안 자동출력 → gitignored | 테스트가 마스킹 초안 파일 출력, 마스킹 로직·gitignore 신뢰 의존 | |
| raw 덤프 → 수동 발췌 | 전체 응답 임시파일 덤프, raw가 디스크에 잠시 존재 유출 표면↑ | |

**User's choice:** 콘솔 출력 → 마스킹 수기정리 (추천)
**Notes:** 사용자가 로컬 env JWT로 1회 실행. 원문 디스크 미잔존, 커밋 전 `bearer`/JWT/가격 grep 0건 확인.

---

## Claude's Discretion

- 정확한 lucide 글리프 선택·역할색 팔레트 (Phase 14 UI)
- spike 테스트 케이스 분할·`LostarkSpikeClient` options 메서드 시그니처
- findings 표 열 정렬·발췌 형식, `/markets/options` 응답 파싱 디테일

## Deferred Ideas

- 운명 계열 융화재료 — 스파이크 거래/식별 확인 시 큐레이션 추가 검토, 미확인 시 v1.2 범위 밖
- 그룹 필터 컨트롤 — v2 (FILTER-V2-01), v1.2는 역할 배지로 대체
- 등급별 색상·정렬 정교화 — v2 (GRADE-V2-01)
