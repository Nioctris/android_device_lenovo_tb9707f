/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.tiles;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import org.lineageos.lenovoparts.MainActivity;
import org.lineageos.lenovoparts.charging.ChargingActivity;
import org.lineageos.lenovoparts.perf.PerfProfileActivity;

/** Opens the settings screen of the tile that was long-pressed. */
public class TilePreferencesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentName tile = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME,
                ComponentName.class);
        Class<?> target = MainActivity.class;
        if (tile != null) {
            String name = tile.getClassName();
            if (name.equals(BypassChargingTileService.class.getName())) {
                target = ChargingActivity.class;
            } else if (name.equals(PerfProfileTileService.class.getName())) {
                target = PerfProfileActivity.class;
            }
        }
        startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
