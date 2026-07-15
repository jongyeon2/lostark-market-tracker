---
phase: 24-auction-gem-spike
status: passed
verified: 2026-07-15
method: 실 API 실호출(HTTP 200) + build 그린 + 키 유출 스캔
requirements: [GEM-01]
---

# Phase 24 VERIFICATION — 경매장 보석 카탈로그 스파이크

**결과: ✅ PASSED** (🚦 휴먼 비준 1건 대기 — 스파이크 phase의 정상 종료 상태, Phase 21 선례)

## 성공조건 대조 (ROADMAP Phase 24)

| # | 성공조건 | 검증 | 결과 |
|---|---|---|---|
| 1 | 기존 JWT로 200 + 레이트리밋 버킷 공유 판정 | `/auctions/options`·`/auctions/items` 전부 **200**. 통제 프로브: 경매장 `a2=89` → 거래소 `m1=88`(정확히 -1), `limit`·`reset` 동일 → **공유 확정** | ✅ |
| 2 | 티어4 8~10레벨 보석 실측 확정 | 이름검색 6건 전부 `TotalCount>0`·distinct name=1. **전수 증명** 겁화 1083 + 작열 1136 = 2219 = 티어4 전체 | ✅ |
| 3 | "현재가" 정의 잠금 | 후보 4개 실측 대조 → **`min(BuyPrice)`** 채택. `StartPrice=1` 미끼 매물·`BidPrice=0` 다수를 근거로 반증 | ✅ |
| 4 | 표시 필드 + 아이콘 CDN 패턴 + 페이징·정렬 | 아이템 키 8종 열거(**`Id` 부재**), 아이콘 `cdn-lostark.../efui_iconatlas/use/*.png` 전체 URL·**전부 distinct**, `PageNo` 1-based·`PageSize:10`·`TotalCount`=매물수, ASC 정상·**DESC는 null 선두** | ✅ |
| 5 | findings에 실측 근거 + 잠금 + exit-gate, 공개 메타데이터만 | `24-SPIKE-FINDINGS.md` — H1~H7 표·가설반증·Exit-gate 수록. 키 문자열 **0건**(스캔) | ✅ |
| 6 | Core Value 0줄 · 프로덕트 UI 0줄 | 수집/캐시/event-impact/`price_snapshot` 무변경, 마이그레이션 0, 프론트 0. `@Profile("spike")`+`@Disabled` 이중 격리 | ✅ |

## must_haves 대조 (24-01-PLAN)

| truth | 검증 | 결과 |
|---|---|---|
| 200 여부를 실측으로 확인, 추측 금지 | 4개 케이스 전부 HTTP 200 실호출 | ✅ |
| 버킷 공유 판정 → 캐시 TTL 설계 입력 | 공유 확정. findings §H2에 "캐시 필수" + 6콜/refresh 근거 기재 | ✅ |
| 보석 카탈로그를 아는 대로 적지 않는다 | `멸화`/`홍염` 가설 **0건으로 반증**, `Level` 필드 기대도 반증(1640=아이템레벨) — 실측이 가정을 이겼음이 문서에 기록 | ✅ 실증 |
| "현재가" 정의를 근거와 함께 잠금 | 4후보 표 + null 처리 규칙 + 거래소 `CurrentMinPrice` 대응 논거 | ✅ |
| 거래소와 다른 지점 명시(식별자·가격·페이징) | `Id` 부재 · `AuctionInfo` 분리 · 1-based 페이징 · 클라이언트 재사용 금지를 §GEM-02 제약으로 명문화 | ✅ |
| 수집·캐시 0줄, UI 0줄 | `grep -rn auctions src/main/java/.../collect/` → **0건** | ✅ |

## 빌드/보안

- `./gradlew compileJava` · `compileTestJava` · **`./gradlew build` 그린** — `@Disabled` 복원 상태라
  전체 스위트가 경매장을 호출하지 않는다(CI 네트워크 0·키 불요).
- **키 유출 스캔**: findings·스파이크 소스에 실키 prefix 매칭 **0건**, `eyJ` 패턴 **0건**.
- 캡처 원문은 `build/`(gitignored)에만 — 커밋 불가.

## 비고 / 후속

- 🔐 **키 노출 사고**: 실행 중 `. ./.env` 소싱 실패로 **키 일부가 콘솔에 출력**됐다(`.env` 키 값에 공백
  포함 → bash가 뒷부분을 명령으로 해석). **산출물·커밋엔 미기재**지만 재발급을 권고한다.
  이후 호출은 값을 인용부호로 감싸 전달해 재노출 없음.
- 🚦 **휴먼 비준 대기**(GEM-02 착수 전제): (a) 보석 6종 카탈로그가 로아 유저 상식과 맞는지,
  (b) "현재가 = 최저 즉시구매가" 채택 여부. Phase 21이 큐레이션을 비준받은 것과 동일한 게이트.
- GEM-02는 **신규 화면**이므로 `/gsd-ui-phase`로 UI-SPEC 선행 후 계획.
