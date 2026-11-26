/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

/**
 * Charging power cap, applied as a kernel cap on the battery charge current
 * (charge_current_limit). The charger and the charge pump policy both
 * follow it. Without fast charging the current stays at a standard 2 A.
 */
public final class ChargingPower {

    public static final String SETTING_FAST = "lenovo_fast_charging";
    public static final String SETTING_WATTS = "lenovo_charging_power_w";

    public static final int MIN_WATTS = 10;
    public static final int MAX_WATTS = 45;
    public static final int DEFAULT_WATTS = 35;
    /** Above this, the tablet is known to overheat while charging. */
    public static final int SAFE_WATTS = 35;

    private static final String NODE = "/sys/class/power_supply/battery/charge_current_limit";
    private static final int STANDARD_UA = 2000000;
    /* Charger output power to battery current, at about 4.5 V per amp. */
    private static final int UA_PER_WATT = 1000000 * 10 / 45;

    private ChargingPower() {}

    public static boolean isFastEnabled(ContentResolver cr) {
        return Settings.System.getInt(cr, SETTING_FAST, 1) == 1;
    }

    public static int getWatts(ContentResolver cr) {
        int watts = Settings.System.getInt(cr, SETTING_WATTS, DEFAULT_WATTS);
        return Math.max(MIN_WATTS, Math.min(MAX_WATTS, watts));
    }

    public static void setFastEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), SETTING_FAST, enabled ? 1 : 0);
        apply(context.getContentResolver());
    }

    public static void setWatts(Context context, int watts) {
        Settings.System.putInt(context.getContentResolver(), SETTING_WATTS, watts);
        apply(context.getContentResolver());
    }

    public static void restore(Context context) {
        apply(context.getContentResolver());
    }

    private static void apply(ContentResolver cr) {
        int ua = isFastEnabled(cr) ? getWatts(cr) * UA_PER_WATT : STANDARD_UA;
        Sysfs.write(NODE, Integer.toString(ua));
    }
}
