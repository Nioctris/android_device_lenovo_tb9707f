/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
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
        holder.itemView.setForeground(mSelected
                ? getContext().getDrawable(R.drawable.pane_item_selected) : null);
    }
}
