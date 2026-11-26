/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** "Turn off" action of the bypass charging notification. */
public class BypassChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BypassCharging.ACTION_DISABLE.equals(intent.getAction())) {
            BypassCharging.setEnabled(context, false);
        }
    }
}
