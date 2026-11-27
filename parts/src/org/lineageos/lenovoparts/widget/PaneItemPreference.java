/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.lenovoparts.R;

/** Sections list entry that shows when its section is the open one. */
public class PaneItemPreference extends Preference {

    private boolean mSelected;

    public PaneItemPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSelected(boolean selected) {
        if (mSelected != selected) {
            mSelected = selected;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        // Tint the entry's own rounded background, so the highlight keeps its
        // shape and margins and the title stays readable.
        holder.itemView.setBackgroundTintList(mSelected
                ? ColorStateList.valueOf(getContext().getColor(R.color.pane_selected))
                : null);
    }
}
