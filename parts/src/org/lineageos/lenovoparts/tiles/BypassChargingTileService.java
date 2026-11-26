/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.tiles;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.lenovoparts.bypass.BypassCharging;

public class BypassChargingTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        BypassCharging.setEnabled(this, !BypassCharging.isEnabled(getContentResolver()));
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(BypassCharging.isEnabled(getContentResolver())
                ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
