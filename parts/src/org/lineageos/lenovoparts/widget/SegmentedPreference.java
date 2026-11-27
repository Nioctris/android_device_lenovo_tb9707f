/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.lineageos.lenovoparts.R;

/**
 * Single choice shown as a row of connected buttons. Takes
 * {@code android:entries} and {@code android:entryValues} like a
 * ListPreference; icons can be set with {@link #setIcons}.
 */
public class SegmentedPreference extends Preference {

    private final CharSequence[] mEntries;
    private final CharSequence[] mValues;
    private int[] mIcons;
    private String mValue;

    public SegmentedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                new int[] { android.R.attr.entries, android.R.attr.entryValues });
        mEntries = a.getTextArray(0);
        mValues = a.getTextArray(1);
        a.recycle();
        setLayoutResource(R.layout.preference_segmented);
        setSelectable(false);
    }

    public void setIcons(int... icons) {
        mIcons = icons;
        notifyChanged();
    }

    public void setValue(String value) {
        if (value != null && !value.equals(mValue)) {
            mValue = value;
            notifyChanged();
        }
    }

    public String getValue() {
        return mValue;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        MaterialButtonToggleGroup group =
                (MaterialButtonToggleGroup) holder.findViewById(R.id.segments);
        group.clearOnButtonCheckedListeners();
        if (group.getChildCount() != mEntries.length) {
            group.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(group.getContext());
            for (int i = 0; i < mEntries.length; i++) {
                MaterialButton b = (MaterialButton) inflater.inflate(
                        R.layout.preference_segment_button, group, false);
                b.setId(android.view.View.generateViewId());
                group.addView(b);
            }
        }
        for (int i = 0; i < mEntries.length; i++) {
            MaterialButton b = (MaterialButton) group.getChildAt(i);
            b.setText(mEntries[i]);
            if (mIcons != null && i < mIcons.length) {
                b.setIconResource(mIcons[i]);
            }
            b.setEnabled(isEnabled());
            if (mValues[i].toString().equals(mValue)) {
                group.check(b.getId());
            }
        }
        group.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) {
                return;
            }
            int index = g.indexOfChild(g.findViewById(id));
            String value = mValues[index].toString();
            if (value.equals(mValue)) {
                return;
            }
            if (callChangeListener(value)) {
                mValue = value;
            } else {
                notifyChanged();
            }
        });
    }
}
