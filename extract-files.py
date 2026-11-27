#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from os import path

from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)
from extract_utils.fixups_blob import (
    blob_fixup,
    blob_fixups_user_type,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.module import ProprietaryFile
from extract_utils.tools import android_root


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups = {
    **lib_fixups,
    ('libmmosal',): lib_fixup_vendor_suffix,
}  # type: lib_fixups_user_type

blob_fixups: blob_fixups_user_type = {
    (
        'vendor/lib64/libwvhidl.so',
        'vendor/lib64/mediadrm/libwvdrmengine.so',
    ): blob_fixup()
        .add_needed('libcrypto_shim.so'),
    # The Dolby pregain handler derives from AHandler. Its Android 11 layout
    # overlaps the delivery mutex added in Android 14, and the handler thread
    # then spins on that mutex. Use the Android 13 foundation library.
    (
        'vendor/lib/libdlbpreg.so',
        'vendor/lib64/libdlbpreg.so',
        'vendor/lib/soundfx/libdlbvol.so',
        'vendor/lib64/soundfx/libdlbvol.so',
    ): blob_fixup()
        .replace_needed('libstagefright_foundation.so', 'libstagefright_foundation-v33.so'),
    # The camera provider already runs 6 binder threads; asking for 4
    # aborts ("Binder threadpool cannot be shrunk after starting").
    'vendor/lib64/vendor.qti.hardware.camera.postproc@1.0-service-impl.so': blob_fixup()
        .binary_regex_replace(b'\x80\x00\x80\x52\x21\x00\x80\x52',
                              b'\xc0\x00\x80\x52\x21\x00\x80\x52'),
}  # fmt: skip


namespace_imports = [
    'hardware/qcom-caf/sm8250',
    'hardware/qcom-caf/wlan',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/dataservices',
    'vendor/qcom/opensource/display',
]

# libmmosal is installed as libmmosal_vendor on the vendor partition.
module = ExtractUtilsModule(
    'tb9707f',
    'lenovo',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    proprietary_files=[
        ProprietaryFile(
            path.join(
                android_root,
                'vendor',
                'lenovo',
                'tb9707f',
                'proprietary-files.txt',
            ),
        ),
    ],
    skip_main_proprietary_file=True,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
