#!/usr/bin/env bash
# 向 Telegram 频道推送发布通知
# 用法：
#   scripts/ssu-notify.sh --text "内容"
#   scripts/ssu-notify.sh --release v1.0.1
#
# 凭证放在 ~/.ssu-telegram（不进仓库）：
#   BOT_TOKEN=123456:ABC...
#   CHAT_ID=@TGSenSeiSU
set -euo pipefail

CFG="${HOME}/.ssu-telegram"
[ -f "$CFG" ] && . "$CFG"
: "${BOT_TOKEN:?缺少 BOT_TOKEN（写在 ~/.ssu-telegram）}"
: "${CHAT_IDS:?缺少 CHAT_IDS（写在 ~/.ssu-telegram，逗号分隔）}"

MODE="${1:-}"
case "$MODE" in
  --text)
    TEXT="${2:?需要文字}"
    ;;
  --release)
    TAG="${2:?需要 tag，例如 v1.0.1}"
    REPO="${REPO:-kylinmode/SENSEISU}"
    BODY=$(gh release view "$TAG" --repo "$REPO" --json body -q .body 2>/dev/null | head -c 1200 || true)
    TEXT="SenseiSU ${TAG} 已发布

${BODY}

下载：https://github.com/${REPO}/releases/tag/${TAG}
SUSFS 模块：https://github.com/kylinmode/SUSFS-Guard-LKM/releases"
    ;;
  *)
    echo "用法: $0 --text \"内容\" | --release vX.Y.Z" >&2
    exit 1
    ;;
esac

FAIL=0
for chat in ${CHAT_IDS//,/ }; do
  resp=""
  for attempt in 1 2 3 4 5; do
    resp=$(curl -s --retry 2 --retry-delay 2 --max-time 45 -X POST \
      "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" \
      --data-urlencode "chat_id=${chat}" \
      --data-urlencode "text=${TEXT}" || true)
    [ -n "$resp" ] && break
    sleep 5
  done
  ok=$(printf '%s' "$resp" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("ok"))' 2>/dev/null || echo False)
  if [ "$ok" = "True" ]; then
    echo "已发送到 ${chat}"
  else
    echo "发送到 ${chat} 失败：${resp}" >&2
    FAIL=1
  fi
done
exit $FAIL
