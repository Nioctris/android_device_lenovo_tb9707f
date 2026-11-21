#
# Copyright (C) 2026 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit common Lineage tablet Wi-Fi-only configuration.
$(call inherit-product, vendor/lineage/config/common_full_tablet_wifionly.mk)

# Inherit from the Y700 device tree.
$(call inherit-product, device/lenovo/tb9707f/device.mk)

PRODUCT_NAME := lineage_tb9707f
PRODUCT_DEVICE := tb9707f
PRODUCT_MANUFACTURER := Lenovo
PRODUCT_BRAND := Lenovo
PRODUCT_MODEL := TB-9707F
PRODUCT_CHARACTERISTICS := tablet

TARGET_SCREEN_WIDTH := 1600
TARGET_SCREEN_HEIGHT := 2560

PRODUCT_SHIPPING_API_LEVEL := 30
PRODUCT_GMS_CLIENTID_BASE := android-lenovo

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Lenovo/TB-9707F_PRC/TB-9707F:13/TKQ1.221013.002/15.0.342_231018:user/release-keys
