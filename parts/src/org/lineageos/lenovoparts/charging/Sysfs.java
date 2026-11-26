/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;

final class Sysfs {

    private static final String TAG = "LenovoCharging";

    private Sysfs() {}

    static void write(String path, String value) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(value);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + path, e);
        }
    }
}
