/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.display;

import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;

/**
 * One refresh rate choice (60 / 120 Hz) plus an adaptive switch, mapped onto
 * the platform min/peak refresh rate settings:
 *   120 + adaptive  -> min 60,  peak 120 (SurfaceFlinger idles down to 60)
 *   120             -> min 120, peak 120
 *   60              -> min 60,  peak 60 (adaptive greyed out)
 */
public class RefreshRateFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final float LOW = 60f;
    private static final float HIGH = 120f;
    /* Lowest rate the panel's touch controller has a scan table for. */
    private static final float IDLE = 30f;

    private ListPreference mRate;
    private SwitchPreferenceCompat mAdaptive;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.refresh_rate_settings, rootKey);
        mRate = findPreference("refresh_rate");
        mAdaptive = findPreference("adaptive_refresh_rate");
        mRate.setOnPreferenceChangeListener(this);
        mAdaptive.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContentResolver cr = getContext().getContentResolver();
        float peak = Settings.System.getFloat(cr, PEAK_REFRESH_RATE, HIGH);
        float min = Settings.System.getFloat(cr, MIN_REFRESH_RATE, 0f);
        boolean high = peak > LOW + 1f;
        updateUi(high, high && min < HIGH - 1f);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean high;
        boolean adaptive;
        if (preference == mRate) {
            high = Integer.parseInt((String) newValue) > (int) LOW;
            adaptive = high && mAdaptive.isChecked();
        } else {
            high = isHigh();
            adaptive = (Boolean) newValue;
        }
        apply(high, adaptive);
        updateUi(high, adaptive);
        return true;
    }

    private boolean isHigh() {
        return Integer.parseInt(mRate.getValue()) > (int) LOW;
    }

    private void apply(boolean high, boolean adaptive) {
        ContentResolver cr = getContext().getContentResolver();
        float peak = high ? HIGH : LOW;
        float min = !high ? LOW : adaptive ? IDLE : HIGH;
        Settings.System.putFloat(cr, MIN_REFRESH_RATE, min);
        Settings.System.putFloat(cr, PEAK_REFRESH_RATE, peak);
    }

    private void updateUi(boolean high, boolean adaptive) {
        mRate.setValue(high ? "120" : "60");
        mAdaptive.setEnabled(high);
        mAdaptive.setChecked(high && adaptive);
        mAdaptive.setSummary(high ? R.string.refresh_rate_adaptive_summary
                : R.string.refresh_rate_adaptive_disabled);
    }
}
