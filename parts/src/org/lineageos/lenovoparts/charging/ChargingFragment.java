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
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChargingFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final long REFRESH_MS = 1000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mReader = Executors.newSingleThreadExecutor();

    private SwitchPreferenceCompat mBypass;
    private SwitchPreferenceCompat mFast;
    private SeekBarPreference mPower;

    private PowerDashboardPreference mDashboard;

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
        mPower.setMin(ChargingPower.MIN_WATTS);
        mPower.setMax(ChargingPower.MAX_WATTS);
        mBypass.setOnPreferenceChangeListener(this);
        mFast.setOnPreferenceChangeListener(this);
        mPower.setOnPreferenceChangeListener(this);

        mDashboard = findPreference("power_dashboard");
    }

    @Override
    public void onResume() {
        super.onResume();
        ContentResolver cr = getContext().getContentResolver();
        mBypass.setChecked(BypassCharging.isEnabled(cr));
        mFast.setChecked(ChargingPower.isFastEnabled(cr));
        setPower(ChargingPower.getWatts(cr));
        mPower.setEnabled(mFast.isChecked());
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
        mDashboard.setCard(PowerDashboardPreference.CARD_STATUS, s.capacity + "%",
                mode != null ? statusText(s) + " · " + mode : statusText(s));
        mDashboard.setCard(PowerDashboardPreference.CARD_POWER, signedWatts(s.batteryWatts()),
                getString(R.string.power_card_power_detail, amps(s.batteryUa),
                        volts(s.batteryUv)));
        if (s.usbPresent) {
            mDashboard.setCard(PowerDashboardPreference.CARD_CHARGER, chargerType(s),
                    volts(s.vbusUv) + " · " + watts(s.inputWatts()));
        } else {
            mDashboard.setCard(PowerDashboardPreference.CARD_CHARGER,
                    getString(R.string.power_charger_none), null);
        }
        if (s.pumpEnabled) {
            mDashboard.setCard(PowerDashboardPreference.CARD_PUMPS,
                    getString(R.string.power_pump_on),
                    getString(R.string.power_pump_on_detail, amps(s.pumpInputUa()),
                            celsius(s.pumpTemp)));
        } else {
            mDashboard.setCard(PowerDashboardPreference.CARD_PUMPS,
                    getString(R.string.power_pump_off), null);
        }
        mDashboard.setCard(PowerDashboardPreference.CARD_TEMP, celsius(s.batteryTemp),
                s.health);
        if (PowerStats.valid(s.chargeFullUah) && s.chargeDesignUah > 0) {
            mDashboard.setCard(PowerDashboardPreference.CARD_CAPACITY,
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
            boolean fast = (Boolean) newValue;
            ChargingPower.setFastEnabled(getContext(), fast);
            mPower.setEnabled(fast);
        } else if (preference == mPower) {
            int watts = (Integer) newValue;
            int old = ChargingPower.getWatts(getContext().getContentResolver());
            if (watts > ChargingPower.SAFE_WATTS && old <= ChargingPower.SAFE_WATTS) {
                confirmHighPower(watts, old);
                return false;
            }
            ChargingPower.setWatts(getContext(), watts);
            setPower(watts);
        }
        return true;
    }

    private void confirmHighPower(int watts, int old) {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.charging_power_warning_title)
                .setMessage(R.string.charging_power_warning_text)
                .setPositiveButton(R.string.charging_power_warning_confirm, (d, w) -> {
                    ChargingPower.setWatts(getContext(), watts);
                    setPower(watts);
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> setPower(old))
                .setOnCancelListener(d -> setPower(old))
                .show();
    }

    private void setPower(int watts) {
        mPower.setValue(watts);
        mPower.setSummary(getString(R.string.charging_power_summary, watts));
    }
}
