/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;
import org.lineageos.lenovoparts.widget.CardGridPreference;
import org.lineageos.lenovoparts.widget.ChargingPowerPreference;
import org.lineageos.lenovoparts.widget.PowerGraphView;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChargingFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener, ChargingPowerPreference.Listener {

    private static final long REFRESH_MS = 1000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mReader = Executors.newSingleThreadExecutor();

    private SwitchPreferenceCompat mBypass;
    private SwitchPreferenceCompat mFast;
    private ChargingPowerPreference mPower;
    private SwitchPreferenceCompat mHigh;

    private static final int CARD_STATUS = 0;
    private static final int CARD_POWER = 1;
    private static final int CARD_CHARGER = 2;
    private static final int CARD_PUMPS = 3;
    private static final int CARD_TEMP = 4;
    private static final int CARD_CAPACITY = 5;

    private CardGridPreference mDashboard;

    private final Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            mReader.execute(() -> {
                PowerStats stats = PowerStats.read();
                mHandler.post(() -> show(stats));
            });
            mHandler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charging_settings, rootKey);
        mBypass = findPreference("bypass_charging");
        mFast = findPreference("fast_charging");
        mPower = findPreference("charging_power");
        mPower.setRange(ChargingPower.MIN_WATTS, ChargingPower.MAX_WATTS,
                ChargingPower.SAFE_WATTS);
        mPower.setListener(this);
        mHigh = findPreference("charging_power_high");
        mBypass.setOnPreferenceChangeListener(this);
        mFast.setOnPreferenceChangeListener(this);
        mHigh.setOnPreferenceChangeListener(this);

        mDashboard = findPreference("power_dashboard");
        mDashboard.setCards(R.string.power_card_status, R.string.power_card_power,
                R.string.power_card_charger, R.string.power_card_pumps,
                R.string.power_card_temp, R.string.power_card_capacity);
        mDashboard.setGraphVisible(true);
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // The live readings change every second: no item change animation,
        // which also kept waking the scroll bar.
        getListView().setItemAnimator(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContentResolver cr = getContext().getContentResolver();
        mBypass.setChecked(BypassCharging.isEnabled(cr));
        mFast.setChecked(ChargingPower.isFastEnabled(cr));
        updatePower();
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

    private void show(PowerStats s) {
        if (!isResumed()) {
            return;
        }
        PowerGraphView.add((float) s.batteryWatts());
        mDashboard.refreshGraph();

        String mode = modeText(s);
        mDashboard.setCard(CARD_STATUS, s.capacity + "%",
                mode != null ? statusText(s) + " · " + mode : statusText(s));
        mDashboard.setCard(CARD_POWER, signedWatts(s.batteryWatts()),
                getString(R.string.power_card_power_detail, amps(s.batteryUa),
                        volts(s.batteryUv)));
        if (s.usbPresent) {
            mDashboard.setCard(CARD_CHARGER, chargerType(s),
                    volts(s.vbusUv) + " · " + watts(s.inputWatts()));
        } else {
            mDashboard.setCard(CARD_CHARGER,
                    getString(R.string.power_charger_none), null);
        }
        if (s.pumpEnabled) {
            mDashboard.setCard(CARD_PUMPS,
                    getString(R.string.power_pump_on),
                    getString(R.string.power_pump_on_detail, amps(s.pumpInputUa()),
                            celsius(s.pumpTemp)));
        } else {
            mDashboard.setCard(CARD_PUMPS,
                    getString(R.string.power_pump_off), null);
        }
        mDashboard.setCard(CARD_TEMP, celsius(s.batteryTemp),
                s.health);
        if (PowerStats.valid(s.chargeFullUah) && s.chargeDesignUah > 0) {
            mDashboard.setCard(CARD_CAPACITY,
                    Math.round(100.0 * s.chargeFullUah / s.chargeDesignUah) + "%",
                    getString(R.string.power_capacity_detail, s.chargeFullUah / 1000,
                            s.chargeDesignUah / 1000, s.cycles));
        }
    }

    private static String signedWatts(double w) {
        return String.format(Locale.getDefault(), "%+.1f W", w);
    }

    private String statusText(PowerStats s) {
        if (s.status == null) {
            return getString(R.string.power_status_unknown);
        }
        switch (s.status) {
            case "Charging":
                return getString(R.string.power_status_charging);
            case "Full":
                return getString(R.string.power_status_full);
            case "Not charging":
                return getString(R.string.power_status_not_charging);
            default:
                return getString(R.string.power_status_discharging);
        }
    }

    private String modeText(PowerStats s) {
        if (!s.usbPresent) {
            return null;
        }
        if (s.bypass) {
            return getString(R.string.power_mode_bypass);
        }
        if (s.limitBypass) {
            return getString(R.string.power_mode_limit);
        }
        return null;
    }

    private String chargerType(PowerStats s) {
        if (s.pdActive == 2) {
            return "USB-PD PPS";
        }
        if (s.pdActive == 1) {
            return "USB-PD";
        }
        return s.usbType != null ? s.usbType.replace('_', ' ') : "USB";
    }

    private static String amps(long ua) {
        return String.format(Locale.getDefault(), "%+.2f A", ua / 1e6);
    }

    private static String volts(long uv) {
        return String.format(Locale.getDefault(), "%.2f V", uv / 1e6);
    }

    private static String watts(double w) {
        return String.format(Locale.getDefault(), "%.1f W", w);
    }

    private static String celsius(int deci) {
        return String.format(Locale.getDefault(), "%.1f °C", deci / 10.0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mBypass) {
            BypassCharging.setEnabled(getContext(), (Boolean) newValue);
        } else if (preference == mFast) {
            ChargingPower.setFastEnabled(getContext(), (Boolean) newValue);
            mFast.setChecked((Boolean) newValue);
            updatePower();
        } else if (preference == mHigh) {
            if ((Boolean) newValue) {
                confirmHighPower();
                return false;
            }
            ChargingPower.setHighAllowed(getContext(), false);
            mHigh.setChecked(false);
            updatePower();
        }
        return true;
    }

    @Override
    public void onPowerCommitted(int watts) {
        ChargingPower.setWatts(getContext(), watts);
    }

    @Override
    public void onUnlockRequested() {
        confirmHighPower();
    }

    private void confirmHighPower() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.charging_power_warning_title)
                .setMessage(R.string.charging_power_warning_text)
                .setPositiveButton(R.string.charging_power_warning_confirm, (d, w) -> {
                    ChargingPower.setHighAllowed(getContext(), true);
                    mHigh.setChecked(true);
                    updatePower();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updatePower() {
        ContentResolver cr = getContext().getContentResolver();
        boolean fast = ChargingPower.isFastEnabled(cr);
        boolean high = ChargingPower.isHighAllowed(cr);
        mHigh.setChecked(high);
        mHigh.setEnabled(fast);
        mPower.setEnabled(fast);
        mPower.setLocked(!high);
        mPower.setValue(ChargingPower.getWatts(cr));
    }
}
