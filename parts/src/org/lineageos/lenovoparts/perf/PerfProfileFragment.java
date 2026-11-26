/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;

public class PerfProfileFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private ListPreference mProfile;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.perf_profile_settings, rootKey);
        mProfile = findPreference("perf_profile");
        mProfile.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mProfile.setValue(PerfProfile.get(getContext().getContentResolver()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PerfProfile.set(getContext(), (String) newValue);
        return true;
    }
}
