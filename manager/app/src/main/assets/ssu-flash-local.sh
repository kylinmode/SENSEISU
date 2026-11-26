#!/bin/sh
# 在手机上直接刷入修补过的 boot / init_boot（不需要 PC）。
#
# 前提：设备当前已经有任意形式的 root（本脚本靠 su 写块设备）。
#       没有 root 的设备请用 fastboot（另一台手机的 Termux + OTG，或 PC）刷一次。
#
# 用法（在手机 Termux / adb shell 里，用 su 执行）：
#   su -c 'sh ssu-flash-local.sh /sdcard/Download/ssu-patched.img [init_boot|boot]'
set -eu

IMG=${1:?用法: $0 <修补后的镜像> [init_boot|boot]}
PART=${2:-init_boot}

[ -f "$IMG" ] || { echo "找不到镜像: $IMG"; exit 1; }
[ "$(id -u)" = 0 ] || { echo "需要 root（已有任意 root 才能用这条路）"; exit 1; }

# 测试用：把 /dev/block 根目录换掉（真机不需要设置）
BLOCK_ROOT=${SSU_BLOCK_ROOT:-/dev/block}

# 当前槽位
SLOT=$(getprop ro.boot.slot_suffix 2>/dev/null || true)
[ -n "$SLOT" ] || SLOT=""
DEV=""
for base in "$BLOCK_ROOT/by-name" "$BLOCK_ROOT/bootdevice/by-name"; do
  for cand in "${PART}${SLOT}" "${PART}"; do
    if [ -e "$base/$cand" ]; then DEV="$base/$cand"; break 2; fi
  done
done
[ -n "$DEV" ] || { echo "找不到分区 $PART"; ls "$BLOCK_ROOT/by-name" | head -20; exit 1; }

echo "目标分区: $DEV (slot=${SLOT:-无})"
echo "分区大小: $(blockdev --getsize64 "$DEV" 2>/dev/null || echo '?') 字节"
echo "镜像大小: $(stat -c %s "$IMG") 字节"

# 备份
BACKUP_DIR=${SSU_BACKUP_DIR:-/sdcard/Download}
BAK="$BACKUP_DIR/${PART}${SLOT}.orig.img"
echo "备份原分区到 $BAK ..."
dd if="$DEV" of="$BAK" bs=1M 2>/dev/null || { echo "备份失败，中止"; exit 1; }
echo "备份完成: $(stat -c %s "$BAK") 字节"

# 写入（只写需要的长度，不破坏分区尾部）
dd if="$IMG" of="$DEV" bs=1M conv=fsync 2>&1 | tail -n 2
sync

# 校验：读回前 N 字节比对
SIZE=$(stat -c %s "$IMG")
if cmp -n "$SIZE" "$IMG" "$DEV" 2>/dev/null; then
  echo "写入校验通过"
else
  echo "!! 校验失败，尝试回滚"
  dd if="$BAK" of="$DEV" bs=1M conv=fsync 2>/dev/null || true
  exit 1
fi

echo
echo "完成。现在重启：  reboot"
echo "重启后到 SSU 管理器里确认『工作中』，再跑一次 ssu-check.sh。"
