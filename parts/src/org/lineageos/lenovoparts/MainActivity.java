/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.audio.BluetoothAudioFragment;
import org.lineageos.lenovoparts.charging.ChargingFragment;
import org.lineageos.lenovoparts.display.RefreshRateFragment;
import org.lineageos.lenovoparts.perf.PerfProfileFragment;
import org.lineageos.lenovoparts.sideswitch.SideSwitchFragment;
import org.lineageos.lenovoparts.widget.PaneItemPreference;

/**
 * Launcher entry: every tablet setting in one place. On a wide screen the
 * sections list stays on the left and the open section fills the right.
 */
public class MainActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new HostFragment())
                    .commit();
        }
    }

    public static class HostFragment extends Fragment {

        private boolean mTwoPane;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mTwoPane = getResources().getConfiguration().screenWidthDp >= 600;
            View view = inflater.inflate(mTwoPane ? R.layout.main_two_pane
                    : R.layout.main_one_pane, container, false);
            if (savedInstanceState == null) {
                var tx = getChildFragmentManager().beginTransaction()
                        .replace(R.id.list_pane, new MainFragment());
                if (mTwoPane) {
                    tx.replace(R.id.detail_pane, new ChargingFragment());
                }
                tx.commit();
            }
            return view;
        }

        boolean isTwoPane() {
            return mTwoPane;
        }

        void showDetail(Fragment fragment) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.detail_pane, fragment)
                    .commit();
        }
    }

    public static class MainFragment extends SettingsBasePreferenceFragment {

        private static final String[] KEYS = {"power", "performance", "display", "bluetooth_audio",
                "side_switch"};
        private static final String STATE_SELECTED = "selected";

        private String mSelected = "power";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.main_settings, rootKey);
            if (savedInstanceState != null) {
                mSelected = savedInstanceState.getString(STATE_SELECTED, mSelected);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            HostFragment host = (HostFragment) getParentFragment();
            select(host != null && host.isTwoPane() ? mSelected : null);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(STATE_SELECTED, mSelected);
        }

        private void select(String key) {
            for (String k : KEYS) {
                PaneItemPreference item = findPreference(k);
                if (item != null) {
                    item.setSelected(k.equals(key));
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            HostFragment host = (HostFragment) getParentFragment();
            Fragment detail = detailFor(preference.getKey());
            if (host != null && host.isTwoPane() && detail != null) {
                mSelected = preference.getKey();
                select(mSelected);
                host.showDetail(detail);
                return true;
            }
            Intent intent = preference.getIntent();
            if (intent != null) {
                startActivity(intent);
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        private static Fragment detailFor(String key) {
            switch (key) {
                case "power":
                    return new ChargingFragment();
                case "performance":
                    return new PerfProfileFragment();
                case "display":
                    return new RefreshRateFragment();
                case "bluetooth_audio":
                    return new BluetoothAudioFragment();
                case "side_switch":
                    return new SideSwitchFragment();
                default:
                    return null;
            }
        }
    }
}
