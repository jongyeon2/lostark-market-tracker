---
quick_id: 260714-sxn
slug: refine-material-reclassify
date: 2026-07-14
requirements: [MKT-02]
---

# Quick 260714-sxn — 재련 재료 분류 교정 + 상급재련 재료 편입

## 배경 (도메인 교정)

로아 유저(사용자) 확인: Phase 22에서 `item_group=상급재련`으로 묶은 **야금술/재봉술 : 업화 [15-18]·[19-20]** 4종은
사실 **일반 재련(일반강화)의 성공률 보조 재료**다. 진짜 **상급 재련 추가 재료**는 **장인의 야금술(무기)/장인의
재봉술(방어구) 1~4단계**이며, 이전 워치리스트엔 빠져 있었다. (용암/빙하의 숨결 2종은 상급재련 유지 — 사용자 확인.)

## 결정 (AskUserQuestion 비준)

- 업화 4종 → 새 그룹 **`재련보조`**로 재분류(기존 `재련재료`에 합치지 않음).
- 장인의 야금술/재봉술 **1~4단계 전부(8종)** 추적 편입 — 스파이크로 3·4단계 Id·등급 잠금 후.

## Task 1 — 스파이크로 장인 책 1~4단계 Id·등급 실측 (spike-then-lock)

- **files:** `src/test/java/.../spike/MarketsApiSpikeTest.java`, `build/spike-refine-master.txt`(gitignored 산출물)
- **action:** `captureRefineMasterBooks()` 추가 — 이름검색 `장인의 야금술`/`장인의 재봉술`(부분일치) × DESC/ASC p1,
  category 50020. Gradle stdout이 cp949로 한글을 손실하므로 **UTF-8 파일로 직접 기록**(`appendItemFields`).
  실행 시에만 클래스 `@Disabled` 임시 해제, 키는 `.env`→env 주입(값 미출력), 종료 후 `@Disabled` 복원.
- **verify:** 8종 전부 200 OK로 Id/등급 확인 — 1=영웅/2=전설/3=유물/4=고대, 아이콘 전부 distinct.
- **done:** 야금술 66112711/713/715/717(use_12_242/244, use_13_221/223), 재봉술 66112712/714/716/718(use_12_243/245, use_13_222/224).

## Task 2 — WatchlistSeeder 재분류 + 편입 (Core Value 로직 0줄)

- **files:** `src/main/java/.../collect/WatchlistSeeder.java`
- **action:** 업화 4종 `상급재련`→`재련보조`. 장인 책 8종 신규(category=50020, item_group=상급재련, role=MATERIAL).
  헤더/클래스 javadoc 카운트·그룹 갱신(41→49, 재련보조 그룹 추가, 도메인 교정 노트).
- **verify:** 워치리스트 49, MATERIAL 31, item_group {강화재료,재련재료,상급재련,재련보조,아크그리드젬,각인서}.
- **done:** 멱등 upsert 그대로 — 기존 41 유지 + 8 신규, 업화는 라벨만 변경.

## Task 3 — 테스트 + 문서

- **files:** `WatchlistSeederIT.java`, `21-SPIKE-FINDINGS.md`, `22-01-SUMMARY.md`, `STATE.md`, `ROADMAP.md`
- **action:** IT hasSize 41→49, MATERIAL 23→31L, isIn +재련보조, 멱등 49, 66112551 샘플=재련보조, 장인 책(66112717) 샘플 추가.
  findings/SUMMARY에 장인 책 카탈로그 + 도메인 교정 기록.
- **verify:** `./gradlew build`(Testcontainers) 그린.
- **done:** 프론트 무변경(itemGroup=`z.string().nullable()` freeform).

## 불변 제약

수집/캐시/event-impact/서빙 **로직 0줄** — 워치리스트 **데이터만** 변경(라벨 재분류 + 8종 추가). 실 시크릿 미출력.
