/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

import org.lineageos.lenovoparts.sideswitch.SideSwitch;

/**
 * Loaded into system_server (config_deviceKeyHandlerClasses). Turns the side
 * switch taps of the game_mode_switcher input device into the action picked
 * in the side switch settings.
 */
public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = "LenovoPartsKeyHandler";

    private static final String SWITCH_DEVICE = "game_mode_switcher";
    private static final int SCANCODE_ON = 183;  // KEY_F13, line high
    private static final int SCANCODE_OFF = 184; // KEY_F14, line low

    private final Context mContext;

    public KeyHandler(Context context) {
        mContext = context;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        if (scanCode != SCANCODE_ON && scanCode != SCANCODE_OFF) {
            return event;
        }
        InputDevice device = event.getDevice();
        if (device == null || !SWITCH_DEVICE.equals(device.getName())) {
            return event;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            apply(scanCode == SCANCODE_ON);
        }
        return null;
    }

    private void apply(boolean on) {
        ContentResolver cr = mContext.getContentResolver();
        String action = SideSwitch.getAction(cr);
        Log.d(TAG, "side switch " + (on ? "on" : "off") + ", action " + action);
        try {
            switch (action) {
                case SideSwitch.ACTION_ROTATION_LOCK:
                    Settings.System.putIntForUser(cr, Settings.System.ACCELEROMETER_ROTATION,
                            on ? 0 : 1, UserHandle.USER_CURRENT);
                    break;
                case SideSwitch.ACTION_DND:
                    mContext.getSystemService(NotificationManager.class).setZenMode(
                            on ? Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS
                               : Settings.Global.ZEN_MODE_OFF,
                            null, TAG);
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "side switch action " + action + " failed", e);
        }
    }
}
