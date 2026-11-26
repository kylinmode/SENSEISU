#!/usr/bin/env bash
# 编译 ksuinit —— 必须静态链接
#
# ksuinit 是内核启动后执行的第一个程序（PID 1），此时 /system 尚未挂载，
# 动态链接器 /system/bin/linker64 不存在，动态产物会导致内核 panic + 无限重启。
#
# 用法: ./scripts/ssu-ksuinit.sh [aarch64-linux-android]
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TARGET=${1:-aarch64-linux-android}

export PATH="/root/.cargo/bin:$PATH"
NDK=${ANDROID_NDK_HOME:-/opt/android-sdk/ndk/29.0.14206865}
SYSROOT="$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
API=26

export CC_${TARGET//-/_}="$BIN/${TARGET}${API}-clang"
export CARGO_TARGET_$(echo "$TARGET" | tr 'a-z-' 'A-Z_')_LINKER="$BIN/${TARGET}${API}-clang"
export CFLAGS_${TARGET//-/_}="--sysroot=$SYSROOT -isystem $SYSROOT/usr/include/${TARGET%-*}-android"

# 关键：静态链接 + 16 KB 页对齐，与上游 CI 一致
RT="$NDK/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/21/lib/linux"
export RUSTFLAGS="-C target-feature=+crt-static \
  -C link-arg=--sysroot=$SYSROOT \
  -C link-arg=-L$RT/aarch64 \
  -C link-arg=-L$SYSROOT/usr/lib/${TARGET}/${API} \
  -C link-arg=-Wl,-z,max-page-size=16384 \
  -C link-arg=-Wno-unused-command-line-argument"

echo "==> 编译 ksuinit（$TARGET）"
cargo build --release --target "$TARGET" --manifest-path "$ROOT/userspace/ksuinit/Cargo.toml"

OUT="$ROOT/userspace/ksuinit/target/$TARGET/release/ksuinit"
SIZE=$(stat -c%s "$OUT")

echo "==> 校验"
if file "$OUT" | grep -q "statically linked"; then
  echo "  ✅ 静态链接"
else
  echo "  ❌ 不是静态链接！会造成无限重启"
  exit 1
fi

if [ "$SIZE" -lt 500000 ]; then
  echo "  ⚠️ 体积 ${SIZE} 字节偏小，疑似动态链接，请复查"
else
  echo "  ✅ 体积 $((SIZE/1024)) KB（正常）"
fi

echo "==> 安装到 ksud 资产目录"
mkdir -p "$ROOT/userspace/ksud/bin/${TARGET%%-*}"; cp "$OUT" "$ROOT/userspace/ksud/bin/${TARGET%%-*}/ksuinit"
echo "  → userspace/ksud/bin/${TARGET%%-*}/ksuinit"
