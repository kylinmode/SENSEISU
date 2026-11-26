#!/usr/bin/env bash
# SenseiSU 一键发布：出包 → GitHub Release → Telegram 通知
#
# 用法:
#   ./scripts/ssu-release.sh "更新说明"      # 版本号自动 +256
#   ./scripts/ssu-release.sh ""              # 无说明
#
# 版本号规则：读 version.txt → +256 → 写回 → 用于 tag 与 APK versionCode
#
# 依赖: gh（已登录）、curl；TG 配置从 ssu.env 读取
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
NOTES=${1:-""}
REPO=${SSU_REPO:-kylinmode/SENSEISU}

say() { printf '\033[1;36m==> %s\033[0m\n' "$*"; }
die() { printf '\033[1;31m!! %s\033[0m\n' "$*" >&2; exit 1; }

# ── 0. 前置检查 ─────────────────────────────────────────────
command -v gh >/dev/null || die "缺 gh"
gh auth status >/dev/null 2>&1 || die "gh 未登录"
[ -f "$ROOT/ssu.env" ] && set -a && . "$ROOT/ssu.env" && set +a

# ── 1. 版本号 +256 并写回 ───────────────────────────────────
VERFILE="$ROOT/version.txt"
KVERFILE="$ROOT/kernel-version.txt"
OLD_VER=$(cat "$VERFILE" 2>/dev/null || echo 10001)
NEW_VER=$((OLD_VER + 256))
echo "$NEW_VER" > "$VERFILE"
export SSU_VERSION_CODE="$NEW_VER"
say "版本号：$OLD_VER → $NEW_VER（+256，已写入 version.txt）"
if [ -f "$KVERFILE" ]; then
  OLD_KVER=$(cat "$KVERFILE")
  NEW_KVER=$((OLD_KVER + 1))
  echo "$NEW_KVER" > "$KVERFILE"
  say "内核版本号：$OLD_KVER → $NEW_KVER（+1，已写入 kernel-version.txt）"
fi

# ── 2. 收集产物 ─────────────────────────────────────────────
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT

APK="$ROOT/manager/app/build/outputs/apk/release"
if ls "$APK"/*.apk >/dev/null 2>&1; then
  say "收集 APK"
  cp "$APK"/*.apk "$STAGE/SSU-v${VER}-release.apk"
else
  say "没有 APK，跳过（先跑 ssu-build-apk.sh）"
fi

say "收集 LKM 模块"
shopt -s nullglob
for ko in "$ROOT"/out/ko/*/ssu.ko; do
  kmi=$(basename "$(dirname "$ko")")
  cp "$ko" "$STAGE/ssu-${kmi}.ko"
done
shopt -u nullglob

count=$(ls -1 "$STAGE" | wc -l)
[ "$count" -gt 0 ] || die "没有任何产物可发"
say "共 $count 个附件"

# ── 3. 发 GitHub Release ────────────────────────────────────
VER="$NEW_VER"
say "创建 Release v$VER"
gh release create "v$VER" \
  --repo "$REPO" \
  --title "SenseiSU v$VER" \
  --notes "${NOTES:-SenseiSU v$VER}" \
  "$STAGE"/* 2>&1 | tail -2

URL="https://github.com/$REPO/releases/tag/v$VER"

# ── 4. 发 Telegram ──────────────────────────────────────────
if [ -x "$ROOT/scripts/ssu-notify.sh" ] && [ -f "$HOME/.ssu-telegram" ]; then
  say "发送 Telegram 通知"
  "$ROOT/scripts/ssu-notify.sh" --release "v$VER" || echo "  （通知发送失败，已跳过）"
else
  echo "  （未配置 ~/.ssu-telegram，跳过通知）"
fi

# ── 5. 输出镜像地址（jsDelivr，国内可用）────────────────────
say "镜像地址（jsDelivr CDN）"
for f in "$STAGE"/*; do
  name=$(basename "$f")
  echo "  https://cdn.jsdelivr.net/gh/${REPO%%/*}/senseisu@v${VER}/${name}"
done

say "完成: $URL"
