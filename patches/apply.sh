#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Apply the TB-9707F fixes to the LineageOS source tree.
# Usage: device/lenovo/tb9707f/patches/apply.sh   (from the source root)
# A patch file name is the project path with '/' replaced by '_'.
set -euo pipefail

patches=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
top=${ANDROID_BUILD_TOP:-$(pwd)}
projects=(
	bootable/recovery
	build/soong
	external/XMP-Toolkit-SDK
	external/google-highway
	external/skia
	frameworks/base
	hardware/qcom-caf/sm8250/audio
	hardware/qcom-caf/wlan
	kernel/configs
	system/vold
	vendor/qcom/opensource/data-ipa-cfg-mgr-legacy-um
	vendor/qcom/opensource/display
	vendor/qcom/opensource/vibrator
)

for project in "${projects[@]}"; do
	patch="$patches/${project//\//_}.patch"
	if git -C "$top/$project" apply --reverse --check "$patch" 2>/dev/null; then
		printf 'already applied: %s\n' "$project"
	else
		git -C "$top/$project" apply "$patch"
		printf 'applied: %s\n' "$project"
	fi
done
