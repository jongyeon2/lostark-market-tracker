---
quick_id: 260714-sxn
slug: refine-material-reclassify
date: 2026-07-14
status: complete
requirements: [MKT-02]
---

# 260714-sxn SUMMARY — 재련 재료 분류 교정 + 상급재련 재료 편입 (워치리스트 41→49)

## 무엇을 했나

Phase 22의 **도메인 오분류**를 사용자(로아 유저) 지적으로 교정했다. `야금술/재봉술 : 업화 [15-18]·[19-20]`은
상급 재련 재료가 **아니라** 일반 재련 성공률 보조 재료이고, 진짜 상급 재련 추가 재료는 **장인의 야금술/재봉술
1~4단계**다. **Core Value 로직 0줄** — 워치리스트 데이터만 변경.

- **스파이크 실측(spike-then-lock):** `MarketsApiSpikeTest.captureRefineMasterBooks()` 신규 — 이름검색
  `장인의 야금술`/`장인의 재봉술` × DESC/ASC(category 50020)로 **8종 전부** 실측. Gradle stdout이 cp949로 한글
  등급을 U+FFFD로 손실시켜, 테스트가 **UTF-8 파일**(`appendItemFields`→`build/spike-refine-master.txt`)로 직접
  기록하도록 우회. 키는 `.env`→env 주입(값 미출력), 실행 중에만 `@Disabled` 임시 해제 후 복원.
  - 장인의 야금술(무기): 1단계 66112711(영웅/use_12_242) · 2단계 66112713(전설/use_12_244) · 3단계 66112715(유물/use_13_221) · 4단계 66112717(고대/use_13_223)
  - 장인의 재봉술(방어구): 1단계 66112712(영웅/use_12_243) · 2단계 66112714(전설/use_12_245) · 3단계 66112716(유물/use_13_222) · 4단계 66112718(고대/use_13_224)
  - 단계별 등급 영웅→전설→유물→고대 상승, 아이콘 전부 distinct.
- **`WatchlistSeeder`:** 장인 책 **8종 신규**(category=50020, item_group=상급재련, role=MATERIAL) → **41→49**(MATERIAL 23→31).
  재분류: 업화 4종(66112551/552/553/554)은 일반 재련 보조, **숨결 2종(66111131/132)은 상급·일반 겸용** → 6종 모두
  `상급재련`→**`재련보조`**. 결과 item_group=상급재련은 **장인 책 8종 전용**, 재련보조=숨결 2+업화 4=6.
  헤더/클래스 javadoc 카운트·그룹·도메인 교정 노트 갱신.
- **`WatchlistSeederIT`:** hasSize 41→49, MATERIAL 23→31L, itemGroup isIn +`재련보조`, 멱등 49. 샘플: 장인 책 상급재련
  (66112717, 고대/use_13_223), 재련보조 업화(66112551), 재련보조 숨결(66111131 용암의 숨결/use_12_171).
- **프론트 무변경:** `itemGroup: z.string().nullable()` freeform이라 `재련보조` 자동 수용(roleGroup만 enum, 전부 MATERIAL).
- **`SyntheticDemoData`·수집기·캐시·event-impact 무변경:** `findByActiveTrue()` 동적 로드라 8종 신규가 seed 프로파일에서 자동 편입.

## 검증

- **`./gradlew build`(Testcontainers Postgres+Redis) BUILD SUCCESSFUL(2m 14s)** — 전체 스위트 그린.
- `WatchlistSeederIT`가 증명: 49종 시드 + role 분포(MATERIAL 31/DEALER 11/SUPPORT 7), item_group 6종, 장인 책·재련보조 샘플, 멱등 49.
- **레이트리밋**: 49종/10분 틱 ≪ 100/min. **DB**: 49×144행/일 ≈ 2.6M행/년 — 무료 VM 여유.
- **보안**: 스파이크는 공개 메타데이터(Id/Name/Grade/Icon)만 기록 — 가격·키 미출력. `.env` 커밋 0.

## 후속 (Phase 23 연결)

item_group이 이제 **6종**(강화재료/재련재료/상급재련/재련보조/아크그리드젬/각인서). Phase 23 대시보드 3열 좌측
재료 세분에서 상급재련(장인 책 8)과 재련보조(숨결 2+업화 4)를 별도 소그룹으로 자연스럽게 분리 가능.

## 커밋

- (code) MarketsApiSpikeTest.java(captureRefineMasterBooks+appendItemFields) · WatchlistSeeder.java(재분류+8종) · WatchlistSeederIT.java(단언 갱신)
- (docs) 260714-sxn-PLAN/SUMMARY · 21-SPIKE-FINDINGS · 22-01-SUMMARY · STATE · ROADMAP
