# LineageOS device tree for the Lenovo Legion Y700 2022 (TB-9707F)

| | |
|---|---|
| SoC | Qualcomm SM8250-AC (Snapdragon 870, kona) |
| RAM / storage | 12 GB / UFS 3.1 |
| Display | 8.8" 2560x1600 120 Hz, NT36523 TDDI (dual-DSI C-PHY), KTZ8866 backlight |
| Audio | 2x CS35L45 (CSPL), WCD938x 3.5 mm jack, Dolby Atmos |
| Wi-Fi / BT | QCA6390 |
| Shipped with | ZUI 15 (Android 13, vendor Android 11) |

Target: `lineage_tb9707f`, LineageOS 24.0.

## Sources

```xml
<!-- .repo/local_manifests/tb9707f.xml -->
<manifest>
  <remote name="nioctris" fetch="https://github.com/Nioctris" />
  <project path="device/lenovo/tb9707f" name="android_device_lenovo_tb9707f" remote="nioctris" revision="lineage-24.0" />
  <project path="vendor/lenovo/tb9707f" name="proprietary_vendor_lenovo_tb9707f" remote="nioctris" revision="lineage-24.0" />
  <project path="kernel/lenovo/sm8250" name="android_kernel_lenovo_sm8250" remote="nioctris" revision="lineage-24.0-tb9707f" />
</manifest>
```

The kernel is a fork of
[Sparxikle/android_kernel_lenovo_sm8250](https://github.com/Sparxikle/android_kernel_lenovo_sm8250)
(`vendor/kona-perf_defconfig` + `vendor/lenovo/tb9707f.config`, Clang r450784d).

Proprietary files come from
[proprietary_vendor_lenovo_tb9707f](https://github.com/Nioctris/proprietary_vendor_lenovo_tb9707f)
(ZUI 15.0.342). Some Dolby blobs there are not stock, see its README before
running `extract-files.py`.

## Build

A few LineageOS projects need local fixes (missing includes, `vendor_available`
for libraries the stock blobs link against, recovery on the split DSI panel,
a framework NPE). Apply them once after `repo sync`:

```bash
device/lenovo/tb9707f/patches/apply.sh
source build/envsetup.sh
lunch lineage_tb9707f trunk_staging userdebug
m bacon
```

## Boot and partitions

- Stock ABL, boot image header v2, no `vendor_boot` / `init_boot`.
- A/B, dynamic partitions in the stock `super` (`system`, `system_ext`,
  `product`, `vendor`, `odm`).
- The ABL-selected dtbo is the stock ZUI one (`prebuilt/dtbo.img`).
- The precompiled SELinux policy lives in `odm`: flash `odm` together with
  `vendor` after sepolicy changes.

## Status

Working:

- Display 60/120 Hz and adaptive refresh, brightness and auto-brightness,
  LiveDisplay picture adjustment
- Touch and pen
- Wi-Fi, Bluetooth, sensors (SSC: accelerometer, gyro, compass, light,
  proximity), dual haptic motors
- Speakers (CS35L45), built-in microphones, 3.5 mm headphones, Dolby Atmos
- Rear camera (photo, video), front camera preview
- Charging, charging limit, deep sleep (~12 mA screen off with Wi-Fi)
- Side switch (rotation lock or Do Not Disturb), LenovoParts settings

Known issues:

- Pen pressure/axis scale for WGP pens is not calibrated yet.
- Headset button thresholds need calibration.

## Tools

`patches/build_soong.patch` only limits soong's memory use (GOMEMLIMIT
22 GiB); skip it on hosts with plenty of RAM.

`tools/ram-test/` holds the early kernel bring-up tools (U-Boot RAM boot with
a diagnostic initramfs). The Lineage build does not use them.
