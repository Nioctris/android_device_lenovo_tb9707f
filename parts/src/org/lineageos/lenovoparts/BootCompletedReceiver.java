/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.lineageos.lenovoparts.bypass.BypassCharging;
import org.lineageos.lenovoparts.perf.PerfProfile;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        PendingResult result = goAsync();
        new Thread(() -> {
            BypassCharging.restore(context);
            PerfProfile.restore(context);
            result.finish();
        }).start();
    }
}
