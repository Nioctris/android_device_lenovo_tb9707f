# Lenovo Legion Y700 (TB-9707F) — LineageOS 24 device tree

Target: `lineage_tb9707f` (SM8250-AC / kona, 12 GB, UFS, 8.8" 2560x1600).

## Boot and partition model

- Stock ABL: boot image header **v2**, no `vendor_boot`, no `init_boot`.
- A/B slots, A/B OTA updater. Virtual A/B is disabled for the first bring-up.
- Dynamic partitions live in the stock 8 GiB `super`: `qti_dynamic_partitions`,
  `system`, `system_ext`, `product`, `vendor`, `odm`.
- Stock partition sizes: boot 96 MiB, dtbo 24 MiB, recovery 100 MiB,
  userdata 96 GiB (current tablet layout).
- Stock vendor is Android 11 / VNDK 30 (HIDL); system is Android 13.
- The ABL-selected dtbo overlay is shipped as `prebuilt/dtbo.img`
  (built from the Y700 kernel tree).

## Kernel

- Source path: `kernel/lenovo/sm8250`.
- Config: `vendor/kona-perf_defconfig vendor/lenovo/tb9707f.config`.
- Clang: `r450784d`.

## Vendor blobs

The blob list and extractor are in:

- `vendor/lenovo/tb9707f/proprietary-files.txt` (997 entries, 74 missing
  from the Xiaomi kona template; see the vendor README).
- `device/lenovo/tb9707f/extract-files.py`.

The list is the Xiaomi `sm8250-common` kona base plus the Lenovo delta:
Lenovo HIDL services, CS35L45 + `libcirrusspkrprot`, Novatek touch firmware,
KTZ8866, QCA6390, Dolby, RichTap, Pandora camera plugins and the Lenovo
audio haptic firmware. Every entry was checked against the extracted stock
`vendor`/`odm`/`system_ext` trees.

## Known bring-up gaps

- Run `extract-files.py` in the source tree to materialize the blobs; the
  checked-in vendor tree currently only contains the list and makefile shim.
- Vendor SELinux policy for the Lenovo HIDL services still has to be added.
- Audio: the stock vendor mixer/audio policy files are used as-is; the
  CS35L45 calibration/protection blobs are in the list.
- Virtual A/B can be enabled after the first slot-A boot.
- The stock Android graphical stack was not brought to a frame in the RAM
  test; the kernel display path was validated separately with `modetest`
  (full screen 1600x2560@120, two DRM planes).

## Safety

Test images only in slot A. Do not touch slot B, `xbl*`, `abl*`, `tz`,
`hyp`, `keymaster`, `devcfg`, `aop`, `persist`, `modemst*`, `fsg`, `fsc`,
`devinfo`, `frp`, `ddr`, `cdt`. Never use `qbootctl -s`.
