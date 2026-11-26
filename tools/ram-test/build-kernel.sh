#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

if [[ $# != 1 ]]; then
	printf 'Usage: %s KBUILD_OUTPUT\n' "$0" >&2
	exit 2
fi

# Defaults to kernel/lenovo/sm8250 in the Lineage source tree.
source_dir=$(cd -- "${KERNEL_SRC:-$(dirname -- "${BASH_SOURCE[0]}")/../../../../../kernel/lenovo/sm8250}" && pwd)
mkdir -p -- "$1"
output=$(realpath -- "$1")
kernel_make=(
	make -C "$source_dir" O="$output" ARCH=arm64
	CROSS_COMPILE=aarch64-linux-gnu- CROSS_COMPILE_COMPAT=arm-linux-gnueabi-
	CC=clang LD=ld.lld AR=llvm-ar NM=llvm-nm OBJCOPY=llvm-objcopy
	OBJDUMP=llvm-objdump READELF=llvm-readelf STRIP=llvm-strip
	HOSTCC=clang HOSTCXX=clang++
)
if [[ -n ${CRYPTO_CFLAGS:-} ]]; then
	kernel_make+=("CRYPTO_CFLAGS=$CRYPTO_CFLAGS")
fi
if [[ -n ${CRYPTO_LIBS:-} ]]; then
	kernel_make+=("CRYPTO_LIBS=$CRYPTO_LIBS")
fi

"${kernel_make[@]}" vendor/kona-perf_defconfig vendor/lenovo/tb9707f.config
"${kernel_make[@]}" -j"${JOBS:-$(nproc)}" Image.gz modules vendor/qcom/kona-v2.1.dtb
