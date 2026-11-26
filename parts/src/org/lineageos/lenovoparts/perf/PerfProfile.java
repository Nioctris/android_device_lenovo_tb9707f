/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf;

import android.content.ContentResolver;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;

import google.hardware.power.extension.pixel.IPowerExt;

/**
 * Performance profile, applied through the power HAL extension as the
 * PROFILE_* hints of powerhint.json. Balanced sets no hint.
 */
public final class PerfProfile {

    private static final String TAG = "LenovoPerfProfile";

    public static final String SETTING = "lenovo_perf_profile";

    public static final String POWERSAVE = "powersave";
    public static final String BALANCED = "balanced";
    public static final String PERFORMANCE = "performance";

    private static final String HINT_POWERSAVE = "PROFILE_POWERSAVE";
    private static final String HINT_PERFORMANCE = "PROFILE_PERFORMANCE";

    private static final String POWER_SERVICE = "android.hardware.power.IPower/default";

    private PerfProfile() {}

    public static String get(ContentResolver cr) {
        String profile = Settings.System.getString(cr, SETTING);
        if (POWERSAVE.equals(profile) || PERFORMANCE.equals(profile)) {
            return profile;
        }
        return BALANCED;
    }

    public static void set(Context context, String profile) {
        Settings.System.putString(context.getContentResolver(), SETTING, profile);
        apply(profile);
    }

    public static void restore(Context context) {
        apply(get(context.getContentResolver()));
    }

    private static void apply(String profile) {
        IPowerExt ext = getPowerExt();
        if (ext == null) {
            return;
        }
        try {
            ext.setMode(HINT_POWERSAVE, POWERSAVE.equals(profile));
            ext.setMode(HINT_PERFORMANCE, PERFORMANCE.equals(profile));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set profile " + profile, e);
        }
    }

    private static IPowerExt getPowerExt() {
        try {
            IBinder power = ServiceManager.waitForDeclaredService(POWER_SERVICE);
            IBinder ext = power != null ? power.getExtension() : null;
            if (ext == null) {
                Log.e(TAG, "No power HAL extension");
                return null;
            }
            return IPowerExt.Stub.asInterface(ext);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get the power HAL extension", e);
            return null;
        }
    }
}
