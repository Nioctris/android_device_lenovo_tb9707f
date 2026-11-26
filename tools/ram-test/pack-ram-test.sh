#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

if [[ $# -lt 3 || $# -gt 5 ]]; then
	printf 'Usage: %s STATIC_ARM64_BUSYBOX ARTIFACT_DIR OUTPUT_DIR [ZUI_FIRMWARE_DIR] [DRM_TEST_DIR]\n' "$0" >&2
	exit 2
fi

busybox=$1
artifact_dir=$2
output=$3
firmware_dir=${4:-}
drm_test_dir=${5:-}
sources=$(dirname -- "${BASH_SOURCE[0]}")
image="$artifact_dir/kernel/arch/arm64/boot/Image"
fdt="$artifact_dir/tb9707f.dtb"
gen_init_cpio=$(realpath -- "$artifact_dir/kernel/usr/gen_init_cpio")

if ! LC_ALL=C readelf -h "$busybox" | grep 'Machine:.*AArch64' >/dev/null ||
   LC_ALL=C readelf -l "$busybox" | grep 'INTERP' >/dev/null; then
	printf 'Expected a static ARM64 BusyBox\n' >&2
	exit 1
fi

tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"' EXIT
install -m755 "$sources/ram-test-init" "$tmp/init"
install -m755 "$busybox" "$tmp/busybox"
touch -d @0 "$tmp/init" "$tmp/busybox"
cat > "$tmp/manifest" <<'MANIFEST'
dir /bin 0755 0 0
dir /config 0755 0 0
dir /dev 0755 0 0
dir /proc 0755 0 0
dir /sys 0755 0 0
dir /run 0755 0 0
nod /dev/console 0600 0 0 c 5 1
file /init init 0755 0 0
file /bin/busybox busybox 0755 0 0
MANIFEST
for applet in sh mount mkdir ln sleep uname cat dmesg reboot timeout wc; do
	printf 'slink /bin/%s busybox 0777 0 0\n' "$applet" >> "$tmp/manifest"
done

if [[ -n $firmware_dir ]]; then
	printf 'dir /lib 0755 0 0\ndir /lib/firmware 0755 0 0\n' >> "$tmp/manifest"
	for firmware in novatek_ts_fw.bin novatek_ts_mp.bin; do
		install -m644 "$firmware_dir/$firmware" "$tmp/$firmware"
		touch -d @0 "$tmp/$firmware"
		printf 'file /lib/firmware/%s %s 0644 0 0\n' \
			"$firmware" "$firmware" >> "$tmp/manifest"
	done
fi

if [[ -n $drm_test_dir ]]; then
	printf 'dir /drm-test 0755 0 0\n' >> "$tmp/manifest"
	for file in modetest ld-linux-aarch64.so.1 libdrm.so.2 libc.so.6; do
		install -m755 "$drm_test_dir/$file" "$tmp/$file"
		touch -d @0 "$tmp/$file"
		printf 'file /drm-test/%s %s 0755 0 0\n' "$file" "$file" >> "$tmp/manifest"
	done
fi

(cd "$tmp" && "$gen_init_cpio" -t 0 manifest) | gzip -n -9 > "$tmp/initramfs.cpio.gz"
gzip -t "$tmp/initramfs.cpio.gz"

mkdir -p "$output"
install -m644 "$image" "$fdt" "$tmp/initramfs.cpio.gz" "$output/"
(cd "$output" && sha256sum Image tb9707f.dtb initramfs.cpio.gz > SHA256SUMS)
