/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.tiles;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.lenovoparts.R;
import org.lineageos.lenovoparts.perf.PerfProfile;

/** Cycles power saving, balanced and performance. */
public class PerfProfileTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile(PerfProfile.get(getContentResolver()));
    }

    @Override
    public void onClick() {
        super.onClick();
        String next;
        switch (PerfProfile.get(getContentResolver())) {
            case PerfProfile.POWERSAVE:
                next = PerfProfile.BALANCED;
                break;
            case PerfProfile.BALANCED:
                next = PerfProfile.PERFORMANCE;
                break;
            default:
                next = PerfProfile.POWERSAVE;
                break;
        }
        updateTile(next);
        // The power HAL call may block while the HAL starts.
        new Thread(() -> PerfProfile.set(this, next)).start();
    }

    private void updateTile(String profile) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        int label;
        switch (profile) {
            case PerfProfile.POWERSAVE:
                label = R.string.perf_profile_powersave;
                break;
            case PerfProfile.PERFORMANCE:
                label = R.string.perf_profile_performance;
                break;
            default:
                label = R.string.perf_profile_balanced;
                break;
        }
        tile.setSubtitle(getString(label));
        tile.setState(PerfProfile.BALANCED.equals(profile)
                ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
