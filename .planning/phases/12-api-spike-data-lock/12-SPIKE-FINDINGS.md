# Phase 12 — Lostark markets API spike findings (Item Visual/Data Enrichment 게이트)

**Date:** 2026-06-29
**Endpoint base:** `https://developer-lostark.game.onstove.com`
**Auth:** `Authorization: bearer {JWT}` (키는 env에서만; 커밋·findings에 미기재)
**How captured:** `MarketsApiSpikeTest`(spike 프로파일, `@Disabled`) → `LostarkSpikeClient`로 `/markets/options` 1회 + 각인서(40000)/융화재료(50010) 검색. 실호출 1세션. 콘솔 출력만 사용(raw 응답 디스크 미덤프, D-10). 아래 모든 표는 **공개 메타데이터(Id/Name/Grade/Icon/Category)만** — 가격·키·계정 식별자 제외.

> **포맷 선례:** `.planning/phases/01-foundation-task-0/TASK0-FINDINGS.md`(동일 markets API 잠금) 계승.

---

## (a) CategoryCode 실측 (SPIKE-01)

`GET /markets/options` → `Categories[].Subs[].Code`. **leaf만 품목 반환**(부모 코드는 `TotalCount:0`, Task 0 규칙 재확인).

| 분류 | CategoryCode | leaf? | 비고 |
|---|---|---|---|
| **각인서** | **`40000`** | ✅ leaf | `Subs:[]` — 40000 자체가 leaf. 커뮤니티 추정값을 실측 확인. |
| 강화 재료(부모) | `50000` | ❌ 부모 | `TotalCount:0`. 직접 검색 불가. |
| └ **재련 재료** | **`50010`** | ✅ leaf | **융화재료(오레하·아비도스)가 여기서 검색됨.** |
| └ 재련 추가 재료 | `50020` | ✅ leaf | 융화재료 0건(검색 확인) — 융화재료는 50010 전용. |
| └ 기타 재료 | `51000` | ✅ leaf | (이번 큐레이션 범위 밖) |
| └ 무기 진화 재료 | `51100` | ✅ leaf | (범위 밖) |
| └ 아크 그리드 재료 | `230000` | ✅ leaf | (범위 밖) |

**부가 실측:** `ItemGrades = [일반,고급,희귀,영웅,전설,유물,고대,에스더]`, `ItemTiers = [2,3,4]`. 각인서는 등급(전설/유물)으로 구분되며 큐레이션은 **유물 등급**을 잠근다.

---

## (b) Icon URL 필드 확정 (SPIKE-02)

- **필드명:** `Icon` (list 응답 품목마다 존재, 부재 케이스 없음).
- **CDN 도메인:** `https://cdn-lostark.game.onstove.com/efui_iconatlas/use/<file>.png`
- 모든 응답(각인서·융화재료)에 `Icon`이 채워져 옴 → **fallback은 "부재"가 아니라 "로딩 실패/동일 아이콘" 대비용**(아래 (e)).
- 아래 표의 `icon_url`은 위 CDN base + 파일명으로 복원한다(전사 시 base 공통).

---

## (c) 품목당 6필드 큐레이션 표 (SPIKE-03, SPIKE-04) — 총 15개 확정

**잠금 스키마(D-08):** `external_item_id` / `display_name` / `category_code` / `icon_url` / `item_group` / `role_group`
**icon_url 표기:** CDN base(`https://cdn-lostark.game.onstove.com/efui_iconatlas/use/`) + 아래 파일명.
모든 행은 **실측 `external_item_id`·`icon_url`** 보유 — 추정/플레이스홀더 0건.

### 융화재료 4 (role_group=MATERIAL, item_group=강화재료, category_code=50010)

| external_item_id | display_name | category_code | icon_url | item_group | role_group |
|---|---|---|---|---|---|
| `6861009` | 상급 오레하 융화 재료 | 50010 | `use_8_109.png` | 강화재료 | MATERIAL |
| `6861011` | 최상급 오레하 융화 재료 | 50010 | `use_11_29.png` | 강화재료 | MATERIAL |
| `6861012` | 아비도스 융화 재료 | 50010 | `use_12_86.png` | 강화재료 | MATERIAL |
| `6861013` | 상급 아비도스 융화 재료 | 50010 | `use_13_252.png` | 강화재료 | MATERIAL |

*Grade(증거): 상급 오레하·최상급 오레하·상급 아비도스 = 영웅, 아비도스 = 희귀. 4종 모두 거래 가능·iconUrl 서로 구별됨.*

### 딜러 각인서 9 (role_group=DEALER, item_group=각인서, category_code=40000, Grade=유물)

| external_item_id | display_name | category_code | icon_url | item_group | role_group |
|---|---|---|---|---|---|
| `65200505` | 유물 원한 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65201005` | 유물 예리한 둔기 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65202805` | 유물 저주받은 인형 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65203905` | 유물 아드레날린 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65204305` | 유물 정밀 단도 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65203705` | 유물 타격의 대가 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65203005` | 유물 기습의 대가 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65203305` | 유물 돌격대장 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |
| `65201505` | 유물 결투의 대가 각인서 | 40000 | `use_9_25.png` | 각인서 | DEALER |

### 서포터 각인서 2 (role_group=SUPPORT, item_group=각인서, category_code=40000, Grade=유물)

| external_item_id | display_name | category_code | icon_url | item_group | role_group |
|---|---|---|---|---|---|
| `65203405` | 유물 각성 각인서 | 40000 | `use_9_25.png` | 각인서 | SUPPORT |
| `65204105` | 유물 전문의 각인서 | 40000 | `use_9_25.png` | 각인서 | SUPPORT |

**합계: 융화재료 4 + 딜러 9 + 서포터 2 = 15개** (D-04 범위 12~20 충족, role_group 3군 모두 표현).

### 제외/보류 (Pitfall 5 — 미확인 품목 미잠금)

| 후보 | 분류 | 처리 | 사유 |
|---|---|---|---|
| 만개 | 서포터 직업각(도화가) | **보류** | `ItemName` "만개" 검색 0건(매물 부재 또는 표기 상이 추정). 살리려면 Task 3 비준 시 재실측. |
| 구원 | — | **제외** | 0건. 실재 서포터 직업각인 아님(각성/만개/전문의 3종뿐). 후보 선정 오류. |
| (전설 등급 각인서 10종, ItemName 없는 40000 전체검색 첫 페이지) | 각인서 | **미채택** | CURRENT_MIN_PRICE ASC 정렬상 저가 전설 각인서(약자 무시·에테르 포식자 등)만 잡힘 — 딜러/서포터 안목 큐레이션 아님. |

---

## (d) 각인서 아이콘 구별 여부 (SPIKE-04; D-06; Pitfall 7)

- **유물 각인서 11종 = 전부 `use_9_25.png` (동일).** 전설 각인서 10종 = 전부 `use_9_24.png` (동일).
- 즉 **등급 내 각인서 아이콘은 구별되지 않음**(직업/효과와 무관, 등급 단일 글리프). 등급 간만 다름.
- → **identical**. Phase 14는 카드·셀렉터에 **품목명 라벨을 항상 병기**해 식별(D-06). 아이콘만으로 각인서를 구분하지 않는다.
- 대조적으로 **융화재료 4종은 iconUrl이 서로 구별됨**(`use_8_109`/`use_11_29`/`use_12_86`/`use_13_252`).

---

## (e) Fallback 전략 (SPIKE-05; D-05)

아이콘 부재(이번 실측엔 없음)·로딩 실패·동일 아이콘 대비:
- **역할색 배경 + lucide 글리프** — 각인서=책 계열, 융화재료=플라스크/망치 계열. 외부 라이브러리 없이 기존 lucide만.
- **고정 슬롯**(레이아웃 시프트 없음, ICON-01 확정).
- **동일 아이콘(각인서) → 라벨 병기**가 1차 식별 수단(위 (d)).
- 정확한 글리프·역할색 팔레트는 Phase 14 재량.

---

## (f) 키-안전 노트 (SPIKE-05; Pitfall 1)

- 실 JWT 키는 **셸 env(`LOSTARK_API_KEY`)에서만** 공급. `.env`는 gitignored(추적 0), 커밋 소스·CI 진입 금지.
- 스파이크는 `@Disabled` + spike 프로파일 → CI 미실행·키 불필요. `./gradlew build`는 키 없이 그린.
- findings·커밋엔 **공개 메타데이터(id/name/grade/category/iconUrl)만** — 가격 원문·키·계정 식별자 미기재.
- 캡처 경로: 콘솔 출력만 사용(raw 응답 디스크 미덤프, D-10). 커밋 전 점검:
  `grep -rEi "bearer [A-Za-z0-9._-]{20,}|eyJ[A-Za-z0-9._-]{10,}"` → 0건.

---

## (g) 다운스트림 계약 (이 findings가 잠그는 것)

- **Phase 13(백엔드 enrichment/seed):** 위 (c)의 6필드 표를 `TrackedItem`/seed/4개 read DTO/`WatchlistSeeder`·`SyntheticDemoData`에 **그대로 전사**. `category_code`·`icon_url`은 위 base로 복원.
- **Phase 14(프론트 아이콘):** `Icon` URL을 `<ItemIcon>`이 소비, 동일 아이콘은 **라벨 병기**, 실패 시 (e) fallback. `role_group`(DEALER/SUPPORT/MATERIAL) 배지.
- **도메인 안목 서사:** 고변동·고가 융화재료(오레하/아비도스)는 로아온·시즌말·대형 업데이트에 시세 변동이 가장 크다 — 큐레이션은 단순 덤프가 아니라 이 안목의 선택. `role_group`은 금융 마켓의 "자산 섹터" 비유와 동형(면접 서사).

---

*Phase: 12-api-spike-data-lock · Plan 12-01 · API Spike + Data Lock 게이트*
*Spike captured: 2026-06-29 · 큐레이션 15개(만개 보류) — Task 3 휴먼 비준 대상*
