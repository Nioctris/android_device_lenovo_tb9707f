#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

if [[ $# != 4 ]]; then
	printf 'Usage: %s STOCK_BOOT_IMG IMAGE_GZ BOARD_DTB OUTPUT_BOOT_IMG\n' "$0" >&2
	exit 2
fi

stock_boot=$1
kernel=$2
dtb=$3
output=$4
tmp=$(mktemp -d -- "$(dirname -- "$output")/.boot.XXXXXXXX")
trap 'rm -rf -- "$tmp"' EXIT

# Use the stock header parameters and ramdisk. Pass arguments as an array
# so the Android command line is preserved as a single string.
unpack_bootimg --boot_img "$stock_boot" --out "$tmp/stock" \
	--format mkbootimg -0 > "$tmp/stock.args"
mapfile -d '' -t arguments < "$tmp/stock.args"
if [[ ${arguments[0]} != --header_version || ${arguments[1]} != 2 ]]; then
	printf 'Expected a stock Android boot header version 2\n' >&2
	exit 1
fi

for ((i = 0; i < ${#arguments[@]}; i += 2)); do
	case "${arguments[i]}" in
		--kernel) arguments[i+1]=$kernel ;;
		--dtb) arguments[i+1]=$dtb ;;
	esac
done

mkbootimg "${arguments[@]}" -o "$tmp/boot.img"
unpack_bootimg --boot_img "$tmp/boot.img" --out "$tmp/repacked" \
	--format mkbootimg -0 > "$tmp/repacked.args"
mapfile -d '' -t repacked < "$tmp/repacked.args"
if [[ ${#repacked[@]} != "${#arguments[@]}" ]]; then
	printf 'Boot image header argument count changed\n' >&2
	exit 1
fi
for ((i = 0; i < ${#repacked[@]}; i += 2)); do
	case "${repacked[i]}" in
		--kernel) repacked[i+1]=$kernel ;;
		--dtb) repacked[i+1]=$dtb ;;
		--ramdisk) repacked[i+1]=$tmp/stock/ramdisk ;;
	esac
	if [[ ${repacked[i]} != "${arguments[i]}" ||
	      ${repacked[i+1]} != "${arguments[i+1]}" ]]; then
		printf 'Boot image header changed at %s\n' "${repacked[i]}" >&2
		exit 1
	fi
done
cmp -- "$tmp/stock/ramdisk" "$tmp/repacked/ramdisk"
cmp -- "$kernel" "$tmp/repacked/kernel"
cmp -- "$dtb" "$tmp/repacked/dtb"
mv -- "$tmp/boot.img" "$output"
