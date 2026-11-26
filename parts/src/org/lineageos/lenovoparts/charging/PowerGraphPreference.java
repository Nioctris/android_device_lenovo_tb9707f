/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lenovoparts.R;

public class PowerGraphPreference extends Preference {

    private PowerGraphView mGraph;

    public PowerGraphPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.power_graph);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mGraph = (PowerGraphView) holder.findViewById(R.id.power_graph);
    }

    void refresh() {
        if (mGraph != null) {
            mGraph.invalidate();
        }
    }
}
