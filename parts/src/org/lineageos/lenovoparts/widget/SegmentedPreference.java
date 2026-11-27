/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
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
        group.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) {
                v.post(() -> fit(group));
            }
        });
        group.post(() -> fit(group));
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

    private static final float TEXT_SP = 14f;
    private static final float MIN_TEXT_SP = 10f;
    private static final int WIDE_PADDING_DP = 24;
    private static final int NARROW_PADDING_DP = 8;

    /**
     * Narrow columns cannot fit an icon and a label side by side: put the
     * icon above the label and use one smaller text size for all labels.
     */
    private static void fit(MaterialButtonToggleGroup group) {
        int count = group.getChildCount();
        if (count == 0 || group.getChildAt(0).getWidth() == 0) {
            return;
        }
        MaterialButton first = (MaterialButton) group.getChildAt(0);
        int cell = first.getWidth();
        float density = first.getResources().getDisplayMetrics().density;
        float scaled = first.getResources().getDisplayMetrics().scaledDensity;
        TextPaint paint = new TextPaint(first.getPaint());

        float widest = 0;
        for (int i = 0; i < count; i++) {
            MaterialButton b = (MaterialButton) group.getChildAt(i);
            paint.setTextSize(TEXT_SP * scaled);
            widest = Math.max(widest, paint.measureText(b.getText().toString()));
        }
        int icon = first.getIcon() != null ? first.getIconSize() + first.getIconPadding() : 0;
        boolean narrow = widest + icon + 2 * WIDE_PADDING_DP * density > cell;

        float size = TEXT_SP;
        if (narrow) {
            float room = cell - 2 * NARROW_PADDING_DP * density;
            size = Math.max(MIN_TEXT_SP, Math.min(TEXT_SP, TEXT_SP * room / widest));
            size = (float) Math.floor(size * 2) / 2;
        }
        int pad = Math.round((narrow ? NARROW_PADDING_DP : WIDE_PADDING_DP) * density);
        for (int i = 0; i < count; i++) {
            MaterialButton b = (MaterialButton) group.getChildAt(i);
            b.setIconGravity(narrow ? MaterialButton.ICON_GRAVITY_TOP
                    : MaterialButton.ICON_GRAVITY_TEXT_START);
            b.setMaxLines(1);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            b.setPaddingRelative(pad, b.getPaddingTop(), pad, b.getPaddingBottom());
        }
    }
}
