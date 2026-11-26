/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.bypass;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Manual bypass charging: the charger powers the tablet and the battery
 * is not charged, at any charge level. It is a separate kernel vote from
 * the charge limit of Lineage charging control, so the two do not fight.
 */
public final class BypassCharging {

    private static final String TAG = "LenovoBypassCharging";

    public static final String SETTING = "lenovo_bypass_charging";

    private static final String NODE = "/sys/class/power_supply/battery/bypass_charging";

    private BypassCharging() {}

    public static boolean isEnabled(ContentResolver cr) {
        return Settings.System.getInt(cr, SETTING, 0) == 1;
    }

    public static void setEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), SETTING, enabled ? 1 : 0);
        write(enabled);
    }

    public static void restore(Context context) {
        if (isEnabled(context.getContentResolver())) {
            write(true);
        }
    }

    private static void write(boolean enabled) {
        try (FileWriter writer = new FileWriter(NODE)) {
            writer.write(enabled ? "1" : "0");
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + NODE, e);
        }
    }
}
