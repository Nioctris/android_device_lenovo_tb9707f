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
from extract_utils.module import ProprietaryFile
from extract_utils.tools import android_root

namespace_imports = [
    'hardware/qcom-caf/sm8250',
    'hardware/qcom-caf/wlan',
    'vendor/qcom/opensource/commonsys-intf/display',
    'vendor/qcom/opensource/commonsys/display',
    'vendor/qcom/opensource/dataservices',
    'vendor/qcom/opensource/display',
]

# The stock blobs are used unmodified, so no blob or lib fixups are needed.
module = ExtractUtilsModule(
    'tb9707f',
    'lenovo',
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
