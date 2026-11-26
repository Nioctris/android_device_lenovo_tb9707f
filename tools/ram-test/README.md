# TB-9707F kernel bring-up tools (RAM test)

Historical tools from the first kernel bring-up, before the LineageOS build
existed. The Lineage build does not use them: it builds the kernel from
`kernel/lenovo/sm8250` and ships the stock dtbo (`prebuilt/dtbo.img`).
They are kept for low-level debugging of a new kernel without touching any
partition.

| File | Purpose |
|---|---|
| `build-kernel.sh OUT` | Build `Image.gz`, modules and `kona-v2.1.dtb` with Clang (`KERNEL_SRC` overrides the kernel path) |
| `prepare-dtb.sh BASE_DTB STOCK_OV1_DTB OUT` | Apply the stock ZUI `ov1.dtb` and `y700.dts` on top of the Kona v2.1 DTB; build the ABL DTBO (`abl.dts`, `board-id = <0x02010008 0>`) and check panel, backlight, touch, audio and charger links |
| `pack-boot.sh STOCK_BOOT IMAGE_GZ DTB OUT` | Repack a stock v2 boot image with the new kernel and DTB (no AVB footer) |
| `pack-ram-test.sh BUSYBOX ARTIFACTS OUT [FW] [DRM]` | Build a U-Boot RAM-test directory (Image, DTB, initramfs with `ram-test-init`) |
| `ram-test-init` | Diagnostic initramfs: report over USB ACM, `input`, `modes`, `display N`, `display-split`, `planes`, `reboot` |

Requirements: Android Clang r450784d, LLVM binutils, OpenSSL 3, DTC with
libfdt tools (tested 1.7.2), AOSP `mkbootimg`/`unpack_bootimg`/`mkdtboimg`,
and a static ARM64 BusyBox for the RAM test.

## One-shot boot from U-Boot (slot A)

Copy the `y700-ram-test` directory to the FAT partition of the microSD and
run in the U-Boot console:

```text
load mmc 0:1 ${kernel_addr_r} /y700-ram-test/Image && load mmc 0:1 ${fdt_addr_r} /y700-ram-test/tb9707f.dtb && load mmc 0:1 ${ramdisk_addr_r} /y700-ram-test/initramfs.cpio.gz && setenv bootargs console=tty0 loglevel=7 oops=panic panic=5 msm_drm.reset_boot_display=1 msm_drm.dsi_display0=qcom,mdss_dsi_nt36523_panel_vid: && booti ${kernel_addr_r} ${ramdisk_addr_r}:${filesize} ${fdt_addr_r}
```

The initramfs mounts nothing from the tablet. `reboot` returns to the normal
extlinux entry.

## Findings (2026-11-21)

- `msm_drm.dsi_display0=...` is required (the stock ABL passes it); without
  it the DSI driver waits for `dsi_prop` and no DRM connector appears.
- `msm_drm.reset_boot_display=1` drops the U-Boot scanout handoff so the first
  userspace modeset reprograms both SDE interfaces and DSI controllers
  (`800x2560` each, 471 panel init commands).
- Full-screen `1600x2560@120` and `@60` work only with two DRM planes of
  `800x2560`; a single `1600x2560` plane underruns and leaves a stripe at the
  top of the panel.
- The stock first-stage Android `init` mounted `system`, `system_ext`,
  `product`, `vendor` and `odm` of slot B read-only through dm-verity with
  this kernel.
