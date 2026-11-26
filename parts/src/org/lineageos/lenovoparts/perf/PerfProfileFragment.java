/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;
import org.lineageos.lenovoparts.widget.CardGridPreference;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfProfileFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final long REFRESH_MS = 1000;
    private static final int CARD_GPU = 3;
    private static final int CARD_TEMP = 4;
    private static final int CARD_MEMORY = 5;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mReader = Executors.newSingleThreadExecutor();

    private ListPreference mProfile;
    private CardGridPreference mDashboard;
    private PerfStats.Sampler mSampler;

    private final Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            mReader.execute(() -> {
                PerfStats stats = mSampler.read();
                mHandler.post(() -> show(stats));
            });
            mHandler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.perf_profile_settings, rootKey);
        mProfile = findPreference("perf_profile");
        mProfile.setOnPreferenceChangeListener(this);
        mDashboard = findPreference("perf_dashboard");
        mDashboard.setCards(R.string.perf_card_silver, R.string.perf_card_gold,
                R.string.perf_card_prime, R.string.perf_card_gpu, R.string.perf_card_temp,
                R.string.perf_card_memory);
        mSampler = new PerfStats.Sampler(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        mProfile.setValue(PerfProfile.get(getContext().getContentResolver()));
        mHandler.post(mRefresh);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mRefresh);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mReader.shutdown();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PerfProfile.set(getContext(), (String) newValue);
        return true;
    }

    private void show(PerfStats s) {
        if (!isResumed()) {
            return;
        }
        for (int i = 0; i < s.clusters.length; i++) {
            PerfStats.Cluster c = s.clusters[i];
            String load = c.loadPercent >= 0
                    ? getString(R.string.perf_load, c.loadPercent) : null;
            String range = ghz(c.minKhz * 1000) + "–" + ghz(c.maxKhz * 1000);
            mDashboard.setCard(i, ghz(c.curKhz * 1000),
                    load != null ? load + " · " + range : range);
        }
        mDashboard.setCard(CARD_GPU, mhz(s.gpuHz), s.gpuBusy >= 0
                ? getString(R.string.perf_load, s.gpuBusy) + " · " + mhz(s.gpuMaxHz)
                : mhz(s.gpuMaxHz));
        mDashboard.setCard(CARD_TEMP, celsius(s.cpuTemp),
                getString(R.string.perf_temp_detail, celsius(s.gpuTemp), celsius(s.skinTemp)));
        if (s.memTotalKb > 0) {
            String detail = getString(R.string.perf_memory_detail, gb(s.memTotalKb));
            if (s.swapTotalKb > 0) {
                detail += " · " + getString(R.string.perf_zram,
                        gb(s.swapTotalKb - s.swapFreeKb), gb(s.swapTotalKb));
            }
            mDashboard.setCard(CARD_MEMORY, gb(s.memAvailKb), detail);
        }
    }

    private static String ghz(long hz) {
        return hz <= 0 ? "—" : String.format(Locale.getDefault(), "%.2f GHz", hz / 1e9);
    }

    private static String mhz(long hz) {
        return hz <= 0 ? "—" : String.format(Locale.getDefault(), "%.0f MHz", hz / 1e6);
    }

    private static String celsius(float c) {
        return Float.isNaN(c) ? "—" : String.format(Locale.getDefault(), "%.1f °C", c);
    }

    private static String gb(long kb) {
        return String.format(Locale.getDefault(), "%.1f GB", kb / 1048576.0);
    }
}
