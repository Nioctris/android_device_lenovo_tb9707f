/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** Applies the saved headphones profile when an A2DP device connects. */
public class BluetoothConnectReceiver extends BroadcastReceiver {

    /* Let the stack finish its own codec setup before overriding it. */
    private static final long APPLY_DELAY_MS = 1500;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())
                || intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                        != BluetoothProfile.STATE_CONNECTED) {
            return;
        }
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice.class);
        BluetoothManager bm = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        if (device == null || adapter == null) {
            return;
        }
        Context app = context.getApplicationContext();
        BluetoothProfiles.Profile profile = BluetoothProfiles.load(app, device);
        PendingResult result = goAsync();
        adapter.getProfileProxy(app, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int type, BluetoothProfile proxy) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    BluetoothProfiles.apply((BluetoothA2dp) proxy, device, profile);
                    adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy);
                    result.finish();
                }, APPLY_DELAY_MS);
            }

            @Override
            public void onServiceDisconnected(int type) {}
        }, BluetoothProfile.A2DP);
    }
}
