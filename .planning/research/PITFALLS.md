# Pitfalls Research

**Domain:** 기존 마켓 파이프라인에 품목 enrichment 추가
**Researched:** 2026-06-29
**Confidence:** HIGH (대부분 이 프로젝트의 기존 제약에서 직접 도출)

## Critical Pitfalls

### Pitfall 1: 실 API 키가 seed/문서/커밋에 새어나감

**What goes wrong:**
스파이크 중 쓴 JWT나 응답에 포함된 식별자가 findings 문서·seed 코드·로그·커밋에 남는다.

**Why it happens:**
스파이크 응답을 그대로 붙여넣으면서 Authorization 헤더·계정 식별 필드를 마스킹하지 않음.

**How to avoid:**
키는 셸 env에만. findings엔 **공개 CDN URL과 item id·display_name·grade만** 기록(가격/계정 식별자 제외). 커밋 전 `git diff`로 `bearer`/JWT 패턴 grep.

**Warning signs:**
findings/seed에 `eyJ...`(JWT) 또는 `Authorization` 문자열이 보임.

**Phase to address:** Phase 0(스파이크) — 캡처 규칙으로 차단.

---

### Pitfall 2: seed가 키 없이 재현돼야 하는데 아이콘이 비어버림

**What goes wrong:**
iconUrl을 "런타임에 API로 채운다"고 가정 → seed 프로파일(키 없음)에서 아이콘이 전부 null → 검증 기준("seed만으로 아이콘이 보인다") 실패.

**Why it happens:**
enrichment 출처와 런타임 데이터 출처를 혼동. v1.0의 spike-then-lock 패턴을 잊음.

**How to avoid:**
스파이크에서 캡처한 iconUrl·item id를 **WatchlistSeeder 상수로 베이크**. 런타임은 상수만으로 동작.

**Warning signs:**
seed로 띄운 대시보드에서 아이콘 슬롯이 전부 fallback.

**Phase to address:** seed/watchlist 확장 phase.

---

### Pitfall 3: 수집·캐시·event-impact 계산 로직을 건드림

**What goes wrong:**
enrichment를 넣다가 스케줄러·토큰버킷·캐시 무효화·`change_rate` 계산을 수정 → Core Value(수집 신뢰성) 회귀.

**Why it happens:**
"품목 데이터니까 수집기에서 같이 처리하자"는 유혹.

**How to avoid:**
enrichment는 **read-path additive only**. 수정 허용 파일을 화이트리스트(엔티티/DTO/매퍼/seed/프론트 컴포넌트)로 제한. 수집·캐시·EventImpactService diff는 0줄.

**Warning signs:**
`Scheduler`/`TokenBucket`/`CacheService`/`EventImpactService` 파일에 diff 발생.

**Phase to address:** 모든 백엔드 phase의 가드(verification에서 diff 0 단언).

---

### Pitfall 4: 기존 Flyway 마이그레이션(V1–V3)을 수정

**What goes wrong:**
`tracked_item`을 V1에서 직접 고침 → 체크섬 불일치 → 부팅·CI 실패.

**Why it happens:**
"컬럼 추가니까 원본 테이블 정의에"라는 착각.

**How to avoid:**
신규 `V4__add_item_enrichment.sql`만 추가. 컬럼은 **nullable**(기존 행·validate 호환).

**Warning signs:**
`git diff`에 `V1__`/`V2__`/`V3__` 변경.

**Phase to address:** enrichment 컬럼 phase.

---

### Pitfall 5: 큐레이션 목록을 실측 없이 확정 → 거래 불가 품목/잘못된 id

**What goes wrong:**
브리프 후보(원한·아드레날린·각성·상급/최상급 오레하…)를 API 확인 없이 그대로 넣었는데 일부가 비거래·다른 등급·id 불일치.

**Why it happens:**
게임 지식만 믿고 실제 거래소 검색 결과를 안 봄.

**How to avoid:**
**API 응답에서 거래 가능·item id 확인된 것만 확정.** 미확인 품목(아비도스/운명 계열)은 보류. 최종 12~20개로 제한.

**Warning signs:**
seed item id가 추정값·플레이스홀더.

**Phase to address:** Phase 0(스파이크) → seed phase가 확정 목록만 소비.

---

### Pitfall 6: 아이콘 깨짐/지연으로 레이아웃 시프트·UI 파손

**What goes wrong:**
`<img>`가 로드 실패하거나 늦게 떠 카드가 출렁이고, alt 텍스트가 깨진 아이콘 옆에 노출.

**Why it happens:**
고정 크기 슬롯·onError 핸들러 미설정.

**How to avoid:**
고정 width/height 슬롯 + `onError`→역할색 글리프. 핫링크 차단/CDN 장애도 동일 경로로 흡수.

**Warning signs:**
CDN 차단 상태에서 카드 높이 변동·broken-image 아이콘.

**Phase to address:** 프론트 아이콘 phase.

---

### Pitfall 7: 각인서 아이콘이 등급색만 다른 동일 글리프일 수 있음

**What goes wrong:**
유물 각인서들의 `Icon`이 사실상 같은 책 아이콘이면 시각 구분이 안 됨.

**Why it happens:**
각인서 카테고리 아이콘 특성 미확인.

**How to avoid:**
스파이크에서 각 각인서 iconUrl이 **서로 구별되는지** 눈으로 확인. 동일하면 fallback 차원에서 각인명 라벨 병기.

**Warning signs:**
여러 각인서 iconUrl이 동일 문자열.

**Phase to address:** Phase 0(스파이크) — 확인 결과를 findings에 명시.

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| iconUrl을 외부 CDN 핫링크 | 에셋 수급 불필요 | CDN 장애/핫링크차단 시 fallback 의존 | OK(데모) — fallback 필수 동반 |
| 큐레이션 목록 하드코딩 상수 | 단순·키 없이 재현 | 신규 품목 추가 시 코드 수정 | OK(범위 12~20 고정) |
| 그룹 필터 생략 | 범위 축소 | 품목 증가 시 탐색성 저하 | OK(v2 재평가) |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Lostark `/markets/items` | CategoryCode 추정 | 스파이크로 확정(각인서=40000 유력, 융화재료 코드 실측) |
| Lostark CDN 아이콘 | 프론트가 곧 깨질 거라 가정 안 함 | onError fallback 상시 |
| Flyway | 기존 마이그레이션 편집 | 신규 V4 additive |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| 카드마다 개별 아이콘 요청 | 다수 동시 img 요청 | 12~20개 규모라 무시 가능, 브라우저 캐시 활용 | 수백 품목(v2) |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| 스파이크 응답 원문(키/가격/계정) 커밋 | 키 유출·과대 데이터 | 공개 메타데이터만 발췌, 커밋 전 grep |
| 프론트에서 API 키로 직접 호출 | 클라이언트 번들에 키 노출 | 백엔드 DTO 경유만 |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| 깨진 아이콘 노출 | "미완성" 인상(면접 감점) | fallback 글리프로 항상 채움 |
| 그룹 구분 부재 | 12~20개가 뭉뚱그려짐 | 역할색 배지 |

## "Looks Done But Isn't" Checklist

- [ ] **아이콘:** seed(키 없음)에서 실제로 뜨는지 — DB가 아니라 브라우저에서 확인
- [ ] **fallback:** CDN 차단/네트워크 끊김에서 글리프로 대체되는지
- [ ] **무변경 가드:** 수집/캐시/event-impact diff 0줄인지
- [ ] **빌드:** backend `./gradlew build` + frontend `npm run build` 둘 다 그린
- [ ] **키 안전:** findings/seed/커밋에 JWT·Authorization 문자열 0건
- [ ] **큐레이션:** seed item id가 전부 스파이크 실측값인지

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| 키 커밋됨 | HIGH | 키 재발급(포털) + git 히스토리 정리 |
| V1 수정으로 체크섬 깨짐 | MEDIUM | V1 원복 + 변경을 V4로 이전 |
| seed 아이콘 비어있음 | LOW | 스파이크 상수를 seeder에 베이크 |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 키 유출 | Phase 0 | findings/커밋 grep 0건 |
| seed 아이콘 부재 | seed phase | 키 없이 브라우저에서 아이콘 표시 |
| 수집/계산 회귀 | 전 백엔드 phase | 해당 파일 diff 0줄 |
| Flyway 체크섬 | enrichment phase | V1–V3 diff 0 |
| 비거래/잘못된 id | Phase 0 → seed | id가 실측값 |
| 레이아웃 시프트 | 프론트 phase | CDN 차단 시 무파손 |
| 각인서 동일 아이콘 | Phase 0 | findings에 구별 여부 기록 |

## Sources

- 사용자 브리프(v1.2 불변 제약·검증 기준)
- PROJECT.md Key Decisions(spike-then-lock, 무변경 원칙)
- `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md` — 동일 API 모델 잠금 선례

---
*Pitfalls research for: item enrichment*
*Researched: 2026-06-29*
