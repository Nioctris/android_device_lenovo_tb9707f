#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

if [[ $# != 3 ]]; then
	printf 'Usage: %s KONA_V2_1_DTB STOCK_OV1_DTB OUTPUT_DIR\n' "$0" >&2
	exit 2
fi

base=$1
stock_overlay=$2
output=$3
sources=$(dirname -- "${BASH_SOURCE[0]}")
mkdir -p -- "$output"
tmp=$(mktemp -d -- "$output/.dtb.XXXXXXXX")
trap 'rm -rf -- "$tmp"' EXIT

expect() {
	local expected=$1 actual
	shift
	actual=$(fdtget "$@")
	if [[ $actual != "$expected" ]]; then
		printf 'Expected %q but found %q for fdtget %s\n' "$expected" "$actual" "$*" >&2
		exit 1
	fi
}

expect '164 20001' -t x "$base" / qcom,msm-id
expect '0 0' -t x "$base" / qcom,board-id
expect '2010008 0' -t x "$stock_overlay" / qcom,board-id

dtc -@ -I dts -O dtb -o "$tmp/y700.dtbo" "$sources/y700.dts"
dtc -@ -I dts -O dtb -o "$tmp/abl.dtbo" "$sources/abl.dts"
fdtoverlay -i "$base" -o "$tmp/tb9707f.dtb" "$stock_overlay" "$tmp/y700.dtbo"

# ZUI links these consumers to PM8150B's PD extcon. In fixed peripheral
# mode, DWC3 starts the gadget without waiting for a provider.
for node in /soc/ssusb@a600000 "$(fdtget "$tmp/tb9707f.dtb" /__symbols__ usb_qmp_dp_phy)"; do
	fdtput -d "$tmp/tb9707f.dtb" "$node" extcon
done

expect 'Lenovo Legion Y700 (TB-9707F)' "$tmp/tb9707f.dtb" / model
expect '164 20001' -t x "$tmp/tb9707f.dtb" / qcom,msm-id
expect '0 0' -t x "$tmp/tb9707f.dtb" / qcom,board-id
expect '8' -t u "$tmp/tb9707f.dtb" /memory ddr_device_type
expect '2010008 0' -t x "$tmp/abl.dtbo" / qcom,board-id
expect $'qcom,panel-supply-entry@0\nqcom,panel-supply-entry@1\nqcom,panel-supply-entry@2' \
	-l "$tmp/tb9707f.dtb" /soc/dsi_panel_pwr_supply_tb9707f
board="$tmp/tb9707f.dtb"
symbols=/__symbols__
panel=$(fdtget "$board" "$symbols" dsi_nt36523_panel_vid)
expect '0 1' -t x "$board" "$panel" qcom,dsi-ctrl-num
expect '0 1' -t x "$board" "$panel" qcom,dsi-phy-num
expect '' "$board" "$panel" qcom,panel-cphy-mode
expect 'bl_ctrl_external' "$board" "$panel" qcom,mdss-dsi-bl-pmic-control-type

backlight=$(fdtget "$board" "$symbols" y700_backlight)
display=$(fdtget "$board" "$symbols" sde_dsi)
bias=$(fdtget "$board" "$symbols" y700_lcd_bias)
expect "$(fdtget -t x "$board" "$panel" phandle)" -t x "$board" "$display" qcom,dsi-default-panel
expect 'lenovo,tb9707f-ktz8866' "$board" "$backlight" compatible
expect "$(fdtget -t x "$board" "$backlight" phandle)" -t x "$board" "$display" backlight
expect "$(fdtget -t x "$board" "$bias" phandle)" -t x "$board" "$display" lcd-bias-supply

for node in sde_dp sde_dsi1; do
	expect 'disabled' "$board" "$(fdtget "$board" "$symbols" "$node")" status
done
connectors=()
for node in sde_wb sde_dsi sde_rscc; do
	connectors+=("$(fdtget -t x "$board" "$(fdtget "$board" "$symbols" "$node")" phandle)")
done
expect "${connectors[*]}" -t x "$board" "$(fdtget "$board" "$symbols" mdss_mdp)" connectors

touch=/soc/spi@880000/novatek@0
expect 'novatek,NVT-ts-spi' "$board" "$touch" compatible
expect 'novatek_ts_fw.bin' "$board" "$touch" firmware-name
expect 'novatek_ts_mp.bin' "$board" "$touch" novatek,mp-name
expect '' "$board" "$touch" novatek,pen-support

sound=$(fdtget "$board" "$symbols" kona_snd)
codec=$(fdtget -t x "$board" "$(fdtget "$board" "$symbols" wcd938x_codec)" phandle)
amp1=$(fdtget "$board" "$symbols" cs35l45_1)
amp2=$(fdtget "$board" "$symbols" cs35l45_2)
expect 'SPK1' "$board" "$amp1" sound-name-prefix
expect 'SPK2' "$board" "$amp2" sound-name-prefix
expect "$codec $(fdtget -t x "$board" "$amp1" phandle) $(fdtget -t x "$board" "$amp2" phandle)" \
	-t x "$board" "$sound" qcom,codec-aux-devs

for node in pm8150b_charger pm8150b_pdphy pm8150b_fg smb1396 smb1396_slave; do
	expect 'disabled' "$board" "$(fdtget "$board" "$symbols" "$node")" status
done
pump=$(fdtget "$board" "$symbols" smb1396_div2_cp_master)
expect '1000000' -t u "$board" "$pump" qcom,min-ilim-ua
expect '1000000' -t u "$board" "$pump" qcom,div2-cp-min-ilim-ua
expect 'ti,bq27541' "$board" /soc/i2c@884000/bq27541@55 compatible

fdtoverlay -i "$tmp/tb9707f.dtb" -o "$tmp/final.dtb" "$tmp/abl.dtbo"
fdtget "$tmp/final.dtb" /soc lenovo,abl-dtbo-placeholder >/dev/null
mkdtboimg create "$tmp/dtbo-tb9707f.img" --page_size=4096 "$tmp/abl.dtbo"

mv -- "$tmp/tb9707f.dtb" "$tmp/dtbo-tb9707f.img" "$output/"
