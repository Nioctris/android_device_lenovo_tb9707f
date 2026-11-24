/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.sideswitch;

import android.content.ContentResolver;
import android.provider.Settings;

/** Side switch action, kept in Settings.System so system_server can read it. */
public final class SideSwitch {

    public static final String SETTING = "lenovo_side_switch_action";

    public static final String ACTION_NONE = "none";
    public static final String ACTION_ROTATION_LOCK = "rotation_lock";
    public static final String ACTION_DND = "dnd";

    private SideSwitch() {}

    public static String getAction(ContentResolver cr) {
        String action = Settings.System.getString(cr, SETTING);
        return action != null ? action : ACTION_NONE;
    }

    public static void setAction(ContentResolver cr, String action) {
        Settings.System.putString(cr, SETTING, action);
    }
}
