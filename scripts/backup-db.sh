#!/usr/bin/env bash
#
# DB 백업 — PostgreSQL을 덤프해 오라클 오브젝트 스토리지로 올린다. VM의 cron이 하루 1회 부른다.
#
# 이 프로젝트에서 되돌릴 수 없는 유일한 자산이 PG의 시계열이다. 보석 시세는 경매장 이력 API가
# 아예 없어 소실되면 영구히 못 만들고, 가격도 상세 통계로 ~2주가 한계이며, 이벤트·쿠폰은 관리자가
# 손으로 넣은 것이다. Redis는 캐시라, Caddy 인증서는 Let's Encrypt가 재발급하므로 대상이 아니다.
# 사이트가 죽는 건 재배포로 몇 분이면 복구되지만 이건 아니다 — 그래서 백업은 PG 하나만 지킨다.
#
# DB 전체를 뜬다. 값이 낮은 테이블(tracked_item은 시더가 재생성)이 섞여 있지만, 골라 뜨면 테이블이
# 늘 때마다 이 파일을 고쳐야 하고 빠뜨리면 조용히 소실된다. 전체도 수십 MB다.
#
# 🔑 이 스크립트는 성공을 거짓말하지 않는 것이 가장 중요하다. 어느 단계든 실패하면 set -e가
# 즉시 중단시켜 마지막 ping에 도달하지 못하고, 그러면 데드맨 스위치가 침묵을 감지해 알린다.
# "실패했는데 ping은 보내는" 경로가 생기는 순간 감시 전체가 무의미해진다.
#
# 사용법: scripts/backup-db.sh
# 필요:   .env.prod에 BACKUP_PAR_URL, BACKUP_PING_URL, POSTGRES_USER, POSTGRES_DB
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-${REPO_DIR}/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_DIR}/docker-compose.prod.yml}"

log() { printf '[backup-db] %s\n' "$1"; }
die() { printf '[backup-db] ERROR: %s\n' "$1" >&2; exit 1; }

[ -f "$ENV_FILE" ] || die "환경파일 없음: ${ENV_FILE}"

# .env.prod에서 키를 하나씩 꺼낸다. `source`/`. ./.env`는 쓰지 않는다 — 값에 공백이나 따옴표가
# 있으면 셸이 단어 분리·확장을 해버려 이 프로젝트에서 이미 키가 유출된 적이 있다. 여기서는 값을
# 해석하지 않고 문자열 그대로 읽는다.
read_env() {
	sed -n "s/^$1=//p" "$ENV_FILE" | tr -d '\r' | head -1
}

PAR_URL="$(read_env BACKUP_PAR_URL)"
PING_URL="$(read_env BACKUP_PING_URL)"
PG_USER="$(read_env POSTGRES_USER)"
PG_DB="$(read_env POSTGRES_DB)"

# 빈 값으로 조용히 진행하면 "백업이 도는 줄 알았는데 아니었다"가 된다. 여기서 끊는다.
[ -n "$PAR_URL" ] || die "BACKUP_PAR_URL이 비어 있다 (.env.prod 확인)"
[ -n "$PING_URL" ] || die "BACKUP_PING_URL이 비어 있다 (.env.prod 확인)"
[ -n "$PG_USER" ] || die "POSTGRES_USER가 비어 있다"
[ -n "$PG_DB" ] || die "POSTGRES_DB가 비어 있다"

STAMP="$(date -u +%Y-%m-%d)"
OBJECT="db/${STAMP}.dump"
TMP="$(mktemp "${TMPDIR:-/tmp}/lostark-db-XXXXXX.dump")"
trap 'rm -f "$TMP"' EXIT

compose() {
	docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

# -Fc(커스텀 포맷): 압축이 내장이고 pg_restore로 선택 복원이 되며 PG 버전이 달라도 복원된다.
# PG는 MVCC라 수집이 도는 중에 떠도 일관된 스냅샷이 나온다 — 서비스를 멈출 필요가 없다.
log "덤프 시작 (db=${PG_DB})"
compose exec -T postgres pg_dump -U "$PG_USER" -d "$PG_DB" -Fc >"$TMP"

# 🔑 업로드하기 전에 "이게 정말 읽히는 덤프인가"를 확인한다. pg_dump가 중간에 죽거나 디스크가
# 차면 0바이트나 잘린 파일이 남는데, 그대로 올리면 백업이 있다는 착각만 쌓이고 정작 복원하는 날
# 알게 된다. -l은 TOC 전체를 파싱하므로 구조가 깨지면 여기서 걸린다.
#
# 컨테이너 안에 실파일로 쓴 뒤 검사한다. 파이프(/dev/stdin)로 넘기면 안 된다 — 커스텀 포맷은
# seek이 필요한데 파이프는 seek이 안 돼서 **정상 덤프까지 거부당한다**(실측 2026-07-16).
# 호스트엔 pg_restore가 없으므로 postgres 컨테이너 안에서 돌린다.
SIZE="$(wc -c <"$TMP" | tr -d ' ')"
log "덤프 완료 (${SIZE} bytes) — 유효성 검사"
# shellcheck disable=SC2016  # 작은따옴표는 의도적이다 — $?/$rc는 컨테이너 안 셸이 확장해야 한다.
compose exec -T postgres sh -c \
	'cat > /tmp/verify.dump; pg_restore -l /tmp/verify.dump > /dev/null 2>&1; rc=$?; rm -f /tmp/verify.dump; exit $rc' \
	<"$TMP" ||
	die "덤프가 유효하지 않다 (${SIZE} bytes) — 업로드하지 않음"

# ⚠️ PAR URL은 그 자체가 자격증명(토큰 포함)이다. 절대 로그에 남기지 않는다.
# `curl -f`는 실패 메시지에 URL을 실을 수 있어 쓰지 않고, http_code만 직접 받는다.
log "업로드 → ${OBJECT}"
HTTP_CODE="$(curl -sS -X PUT --upload-file "$TMP" \
	-o /dev/null -w '%{http_code}' \
	"${PAR_URL%/}/${OBJECT}" 2>/dev/null || true)"

if [ "$HTTP_CODE" != "200" ]; then
	# 404/401이면 PAR이 만료됐거나 취소된 것이 가장 흔하다(런북 §11 참조).
	die "업로드 실패 (HTTP ${HTTP_CODE:-none}) — PAR 만료 여부 확인"
fi

log "업로드 성공 (HTTP 200, ${SIZE} bytes)"

# 여기까지 왔다는 건 덤프가 유효하고 실제로 올라갔다는 뜻이다. 그때만 신호를 보낸다.
# ping 실패로 스크립트를 죽이지는 않는다 — 백업은 이미 끝났고, 여기서 실패하면 "백업 안 됨"
# 알림이 오탐으로 뜰 뿐이다. 거짓 침묵보다 거짓 경보가 낫다.
if curl -sS -m 10 -o /dev/null "$PING_URL" 2>/dev/null; then
	log "완료"
else
	log "WARN: 백업은 성공했으나 ping 전송 실패 — 곧 오탐 알림이 올 수 있다"
fi
