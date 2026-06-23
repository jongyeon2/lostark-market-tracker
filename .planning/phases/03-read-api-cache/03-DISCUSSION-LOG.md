# Phase 3: Read API + Cache - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-23
**Phase:** 3-Read API + Cache
**Areas discussed:** 캐시 전략(latest), 타임라인 응답 형태 + 4A, 다운샘플 전략, 검증 계약 + health 형태

---

## 캐시 전략 (latest) — API-02

### 무효화 모드 (쓰기 시)
| Option | Description | Selected |
|--------|-------------|----------|
| Evict (키 삭제) | 쓰기 시 키 삭제 → 다음 읽기 DB lazy fill. 정석 cache-aside, 캐시히트 DB 0회 테스트 명확 | ✓ |
| Overwrite (write-through) | 수집기가 새 값 즉시 덮어쓰기, cold read 없음. cache-aside보단 write-through | |
| TTL-only (≤수집주기) | 무효화 없이 TTL 만료 의존. 최대 1주기 stale, 쇼케이스 약함 | |

### 캐시 값 형태
| Option | Description | Selected |
|--------|-------------|----------|
| min_price + collected_at | 작은 DTO/JSON, "얼마+언제 시점" | ✓ |
| min_price만 (Long) | 최소, 시각 맥락 없음 | |
| 전체 스냅샷 DTO | 과잉·직렬화 비용 | |

### 구현 방식
| Option | Description | Selected |
|--------|-------------|----------|
| 수동 RedisTemplate | hand-rolled get/set/evict, D-03 철학 정렬, 설명력 | ✓ |
| Spring @Cacheable/@CacheEvict | 적은 코드, 매직, 설명 약함 | |

**User's choice:** Evict / min_price+collected_at / 수동 RedisTemplate
**Notes:** 무효화 훅은 Phase 2 `PriceCollector.persistSnapshot`에 삽입(insert 경로 공유 — Worktree A·B 충돌 주의). 안전망 TTL은 재량.

---

## 타임라인 응답 형태 + 공유 윈도우 쿼리 (4A) — API-03

### 응답 구조
| Option | Description | Selected |
|--------|-------------|----------|
| 두 배열 {snapshots[], events[]} | 분리, 차트 오버레이 단순, 다운샘플과 직교 | ✓ |
| 단일 인터리브 타임라인 | type 태그 단일 스트림, 다운샘플 시 이벤트 처리 엮임 | |
| 스냅샷 + 근접 이벤트 임베드 | 중복·복잡, 재사용성 낮음 | |

### 이벤트 겹침 경계
| Option | Description | Selected |
|--------|-------------|----------|
| from ≤ occurred_at ≤ to (양끝 포함) | 직관적, 경계/빈 범위 테스트 명확 | ✓ |
| from ≤ occurred_at < to (반열림) | 인접 윈도우 중복 방지엔 유리, 덜 직관적 | |

### 4A 공유 윈도우 쿼리
| Option | Description | Selected |
|--------|-------------|----------|
| 공유 서비스 메서드 + repo Between | WindowQueryService가 조합, timeline+Phase5 재사용, DRY/N+1 회피 | ✓ |
| 컨트롤러가 repo 직호출 | 공유 계층 없음, Phase 5 쿼리 중복 위험(4A 위반) | |

**User's choice:** 두 배열 / 양끝 포함 / 공유 WindowQueryService + repo Between
**Notes:** `GameEventRepository` 신규(findByOccurredAtBetween). `PriceSnapshotRepository`는 read 메서드만 추가(Phase 2 insert 공유).

---

## 다운샘플 전략 (큰 범위) — API-04, 7A

### 버킷 집계 함수
| Option | Description | Selected |
|--------|-------------|----------|
| avg(min_price) per 버킷 | 추세선 표준, 단순, bucket_start+avg+sample_count | ✓ |
| min(min_price) per 버킷 | "최저가" 의미 보존, 스파이크 덜 매끈 | |
| OHLC | 캔들차트급, 구현·응답 복잡(v2) | |

### 발동 조건 / 적용
| Option | Description | Selected |
|--------|-------------|----------|
| raw 점 수 > N이면 자동 | 같은 /prices 자동, downsampled 메타, 작은 범위=raw | ✓ |
| 기간 > 임계(7일) | 시간 범위 기준, 공백 많은 기간서도 불필요 버킷팅 | |
| ?interval=1h 명시 파라미터 | 유연하나 서버 보호 약화·복잡(v2) | |

### 점 수 상한 + 버킷 폭
| Option | Description | Selected |
|--------|-------------|----------|
| ~500점, date_trunc 동적(시/일) | 차트 충분·페이로드 적정, 범위따라 hour/day | ✓ |
| ~200점 | 더 작은 페이로드, 거칠 수 있음 | |
| ~1000점 | 세밀하나 비용 증가·이득 감소 | |

**User's choice:** avg(min_price) / raw>N 자동 / ~500점·date_trunc 동적
**Notes:** PostgreSQL date_trunc 활용(설계 7A). 성공기준 4(30일→제한 점) 자연 충족.

---

## 검증 계약 + 에러 형태 + health — API-05, OPS-01

### 4xx 에러 바디
| Option | Description | Selected |
|--------|-------------|----------|
| 커스텀 @RestControllerAdvice | {timestamp,status,error,message} 일관 JSON | ✓ |
| Spring Boot 기본 에러 JSON | 코드 0, 일관성·제어 약함 | |

### 응답 타임존
| Option | Description | Selected |
|--------|-------------|----------|
| UTC ISO-8601 (...Z), 변환 없음 | 저장과 일치, off-by-9h 차단 | ✓ |
| KST(+09:00) 서버 변환 | off-by-9h 위험, 권장 안 함 | |

### /health/collection 필드
| Option | Description | Selected |
|--------|-------------|----------|
| counts+status+summary_message 마커 | last_run_at+started_at+attempted/succeeded/failed+status+marker, 키 금지 | ✓ |
| 위 + stale 판정 플래그 | 마지막 run N분 경과 시 stale:true 경고 | |
| 카운트만 (마커 제외) | auth/rate 신호 안 드러남 | |

### 입력 검증 경계
| Option | Description | Selected |
|--------|-------------|----------|
| 성공기준 5 그대로 | from>to→400, window≤0→400, 없는 itemId→404, 빈 범위→200 [] | ✓ |
| 없는 itemId도 200 빈 배열 | 404 대신 통일, "없는 품목"↔"빈 데이터" 구분 못함 | |

**User's choice:** 커스텀 advice / UTC ISO-8601 / counts+status+marker / 성공기준 5 그대로
**Notes:** API 키/Authorization 절대 금지. stale 플래그는 미채택(선택 후보).

---

## Claude's Discretion

- 캐시 키 네이밍·안전망 TTL·RedisTemplate JSON 직렬화 구성·latest DTO 필드명
- `/api/items` active 필터(findByActiveTrue 권장)·정렬
- `/latest` 스냅샷 0건 → 404 vs 204; 없는 item → 404
- 다운샘플 hour↔day 전환 임계·date_trunc 표현·sample_count 형태
- 패키지/레이어 구조, @RestControllerAdvice 위치, 에러 코드 enum 여부
- 타임라인/다운샘플 비캐시(latest만 캐시)
- health stale 플래그 채택 여부

## Deferred Ideas

- OHLC 다운샘플 — v2
- ?interval= 명시 파라미터 — v2
- health stale 플래그 — 선택
- 타임라인/다운샘플 결과 캐싱 — v2
- avg_price(YDayAvgPrice) per-tick 컬럼 / V3__add_price_metrics.sql — Phase 3 범위 아님
- KST 표시/포맷 — 클라이언트(프론트 제외)
- Micrometer 캐시 hit/miss 카운터 — v2
