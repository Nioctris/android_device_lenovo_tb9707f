/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.bypass;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;

public class BypassChargingFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat mBypass;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.bypass_charging_settings, rootKey);
        mBypass = findPreference("bypass_charging");
        mBypass.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBypass.setChecked(BypassCharging.isEnabled(getContext().getContentResolver()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        BypassCharging.setEnabled(getContext(), (Boolean) newValue);
        return true;
    }
}
