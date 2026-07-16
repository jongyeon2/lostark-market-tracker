---
task: DB 자동 백업 — 오브젝트 스토리지(쓰기전용 PAR)
quick_id: 260716-h1e
date: 2026-07-16
status: in-progress
---

# PLAN — DB 자동 백업 (스크립트 + 문서)

전체 백업 작업 중 **1·2번(메커니즘)**만. 버킷 생성·PAR 발급·cron 등록은 사용자 몫이라 이 작업에
포함되지 않는다. 앱 코드 0줄.

## 왜 PostgreSQL만인가

| 대상 | 백업? | 이유 |
|---|---|---|
| PostgreSQL | ✅ | **유일하게 되돌릴 수 없다** — 아래 참조 |
| Redis | ❌ | 캐시. 죽으면 다시 채워진다 |
| Caddy 인증서 | ❌ | Let's Encrypt가 재발급 |

PG 안에서도 가치가 다르다: `gem_price_snapshot`은 **경매장 이력 API가 없어 영구 소실**,
`price_snapshot`은 상세 통계로 ~2주만 부분 복구, `game_event`/`coupon`은 **관리자 수기 입력**.
반면 `tracked_item`은 `WatchlistSeeder`가 다시 만든다. 그래도 **DB 전체를 덤프한다** — 골라 뜨면
테이블이 늘 때마다 스크립트를 고쳐야 하고, 빠뜨리면 조용히 소실된다. 전체 덤프도 수십 MB다.

## 설계 (사용자 결정 2026-07-16)

| | |
|---|---|
| 방식 | `pg_dump -Fc` — 논리 덤프, 압축 내장, **MVCC라 무중단** |
| 보관 | 오라클 오브젝트 스토리지 · 버저닝 ON · **서버측 수명주기 30일** |
| 업로드 | **쓰기 전용 버킷 PAR** + `curl -X PUT` |
| 객체명 | `db/YYYY-MM-DD.dump` (UTC) |
| 주기 | 하루 1회 · **RPO 24시간** |
| 감시 | **데드맨 스위치** — 성공 시에만 ping |

**쓰기 전용 PAR을 고른 대가**: (a) PAR은 만료된다 → 데드맨 스위치가 필수가 된다, (b) **PAR로는
다운로드가 안 된다** → 복원 시 콘솔에서 받아야 한다(문서에 명시). 얻는 것: OCI CLI·API 키가 VM에
없고, **VM이 털려도 기존 백업을 읽지도 지우지도 못한다**.

**스크립트에 삭제 로직이 없다** — 권한 자체가 없다. 보관은 서버가 한다.

## 산출물

### 1. `scripts/backup-db.sh` (신규)

- `set -euo pipefail` — 어느 단계든 실패하면 **ping에 도달하지 못한다**. 실패를 성공으로 보고하는
  경로가 없어야 데드맨 스위치가 의미를 가진다.
- `.env.prod`에서 **sed+tr로 키 추출**. `. ./.env` 소싱 금지 — 값에 공백이 있으면 유출된다
  (이 프로젝트에서 이미 겪은 사고).
- 필수 변수 누락 시 즉시 실패(빈 값으로 조용히 진행 금지).
- 덤프: `docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres pg_dump -Fc`
- **🔑 업로드 전 덤프 검증**: `pg_restore -l`로 TOC를 파싱해 **읽히는 덤프인지 확인**.
  0바이트·잘린 덤프를 성공으로 착각하면 백업이 있다는 착각만 남는다. (컨테이너 안에서 실행 —
  호스트엔 pg_restore가 없다.)
- 업로드: `curl`은 **http_code만 출력**. ⚠️ **PAR URL은 토큰이라 절대 로그 금지** — `curl -f`가
  에러에 URL을 실을 수 있어 `-o /dev/null -w '%{http_code}'`로 직접 코드만 받는다.
- ping: 성공 시에만. **ping 실패는 스크립트를 실패시키지 않는다** — 백업은 이미 됐다. 오탐 알림이
  오는 건 감수한다(거짓 침묵보다 낫다).
- 임시 파일은 `trap`으로 정리.

### 2. `docs/deploy/oracle-vm-runbook.md` (백업 섹션 + **복원 절차**)

- 버킷 생성(버저닝 ON) · PAR 발급(쓰기 전용·목록 조회 끄기·만료일 길게) · 수명주기 30일
- healthchecks.io 설정 · cron 등록
- **복원 절차** ⚠️ 이게 핵심:
  - PAR로 다운로드 **불가** → 콘솔에서 받는다
  - 일회용 컨테이너 검증 → 그 다음 운영 복원
  - `--clean`은 **파괴적**이라 경고

### 3. `.env.prod.example`

`BACKUP_PAR_URL` · `BACKUP_PING_URL` 추가 — **이름만, 값은 비운다**.

## 검증

로컬 dev의 `lostark-postgres`(postgres:16)에 **실제 스키마 + 실데이터**가 있다(기준선:
`price_snapshot` 9109 · `item_daily_stats` 1084 · `collection_run` 276 · `tracked_item` 53 ·
`gem_price_snapshot` 18 · `coupon` 2 · `game_event` 1 · `flyway_schema_history` 9).

1. 이 DB를 `pg_dump -Fc`로 덤프
2. **일회용 PG16 컨테이너**에 `pg_restore`
3. **테이블별 `COUNT(*)` 대조** — 추정치(`n_live_tup`)가 아니라 실제 행 수
4. `pg_restore -l` 검증 단계가 **깨진 덤프를 실제로 거부하는지** 확인(잘린 파일로 시험)
5. shellcheck 통과 (docker `koalaman/shellcheck`)

⚠️ **업로드·ping 경로는 PAR·ping URL이 없어 검증 불가** — SUMMARY에 정직히 미검증으로 남긴다.
dev DB는 **읽기만** 한다(복원은 일회용 컨테이너로).

## 하지 않는 것

- 버킷·PAR·cron·healthchecks 실제 생성 — **사용자 몫**(시크릿)
- 앱 코드·마이그레이션 — **0줄**
- 시크릿 값 출력·커밋 — **금지**
