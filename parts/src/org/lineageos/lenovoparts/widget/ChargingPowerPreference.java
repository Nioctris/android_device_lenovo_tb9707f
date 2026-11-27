/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;

import org.lineageos.lenovoparts.R;

/**
 * Power slider with a locked upper range. The value label follows the thumb;
 * the value is committed when the thumb is released.
 */
public class ChargingPowerPreference extends Preference {

    public interface Listener {
        void onPowerCommitted(int watts);

        void onUnlockRequested();
    }

    private int mMin = 10;
    private int mMax = 45;
    private int mSafe = 35;
    private int mValue = 35;
    private boolean mLocked = true;
    private boolean mTracking;
    private Listener mListener;

    private TextView mValueView;

    public ChargingPowerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_charging_power);
        setSelectable(false);
    }

    public void setRange(int min, int max, int safe) {
        mMin = min;
        mMax = max;
        mSafe = safe;
        notifyChanged();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setValue(int watts) {
        mValue = clamp(watts);
        notifyChanged();
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        mValue = clamp(mValue);
        notifyChanged();
    }

    private int clamp(int watts) {
        return Math.max(mMin, Math.min(mLocked ? mSafe : mMax, watts));
    }

    private String format(float watts) {
        return getContext().getString(R.string.charging_power_summary, Math.round(watts));
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        mValueView = (TextView) holder.findViewById(R.id.value);
        ((TextView) holder.findViewById(R.id.min_label)).setText(format(mMin));
        ((TextView) holder.findViewById(R.id.max_label)).setText(format(mMax));

        Slider slider = (Slider) holder.findViewById(R.id.slider);
        LockedRangeView locked = (LockedRangeView) holder.findViewById(R.id.locked_range);

        slider.clearOnChangeListeners();
        slider.clearOnSliderTouchListeners();
        slider.setValueFrom(mMin);
        slider.setValueTo(mMax);
        slider.setValue(mValue);
        slider.setTickVisible(false);
        slider.setEnabled(isEnabled());
        slider.setLabelFormatter(this::format);
        mValueView.setText(format(mValue));
        mValueView.setEnabled(isEnabled());

        locked.attach(slider, mSafe, () -> {
            if (mListener != null) {
                mListener.onUnlockRequested();
            }
        });
        locked.setLocked(mLocked);
        locked.setEnabled(isEnabled());
        locked.setAlpha(isEnabled() ? 1f : 0.38f);

        slider.addOnChangeListener((s, value, fromUser) -> {
            if (mLocked && value > mSafe) {
                s.setValue(mSafe);
                s.performHapticFeedback(HapticFeedbackConstants.REJECT);
                return;
            }
            mValueView.setText(format(value));
            if (fromUser && !mTracking) {
                commit(Math.round(value));
            }
        });
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider s) {
                mTracking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider s) {
                mTracking = false;
                commit(Math.round(s.getValue()));
            }
        });
    }

    private void commit(int watts) {
        if (watts == mValue) {
            return;
        }
        mValue = watts;
        if (mListener != null) {
            mListener.onPowerCommitted(watts);
        }
    }
}
