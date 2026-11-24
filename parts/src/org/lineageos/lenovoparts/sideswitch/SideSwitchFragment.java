/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.sideswitch;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;

public class SideSwitchFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private ListPreference mAction;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.side_switch_settings, rootKey);
        mAction = findPreference("side_switch_action");
        mAction.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAction.setValue(SideSwitch.getAction(getContext().getContentResolver()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SideSwitch.setAction(getContext().getContentResolver(), (String) newValue);
        return true;
    }
}
