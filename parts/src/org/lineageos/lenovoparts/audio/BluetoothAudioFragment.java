/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.lenovoparts.R;
import org.lineageos.lenovoparts.widget.CardGridPreference;

import java.util.List;
import java.util.Locale;

public class BluetoothAudioFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    /* Read by the Bluetooth stack when it configures an SBC stream. */
    private static final String PROP_SBC_XQ = "persist.bluetooth.sbc_xq";
    private static final String PROP_SBC_XQ_BITRATE = "persist.bluetooth.sbc_xq_bitrate";
    private static final String DEFAULT_XQ_BITRATE = "552";
    /* Published by the Bluetooth stack for the active stream. */
    private static final String PROP_ENCODING = "bluetooth.a2dp.encoding";
    private static final String PROP_BITRATE = "bluetooth.a2dp.bitrate";
    private static final String PROP_SBC_BITPOOL = "bluetooth.a2dp.sbc.bitpool";
    private static final String PROP_SBC_MODE = "bluetooth.a2dp.sbc.mode";

    private static final long REFRESH_MS = 2000;

    private static final int CARD_DEVICE = 0;
    private static final int CARD_CODEC = 1;
    private static final int CARD_FORMAT = 2;
    private static final int CARD_BITRATE = 3;
    private static final int CARD_ENCODING = 4;
    private static final int CARD_CHANNELS = 5;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private SwitchPreferenceCompat mSbcXq;
    private ListPreference mSbcXqBitrate;
    private CardGridPreference mDashboard;
    private BluetoothA2dp mA2dp;

    private final BluetoothProfile.ServiceListener mListener =
            new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mA2dp = (BluetoothA2dp) proxy;
            refresh();
        }

        @Override
        public void onServiceDisconnected(int profile) {
            mA2dp = null;
        }
    };

    private final Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            refresh();
            mHandler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.bluetooth_audio_settings, rootKey);
        mSbcXq = findPreference("sbc_xq");
        mSbcXq.setOnPreferenceChangeListener(this);
        mSbcXqBitrate = findPreference("sbc_xq_bitrate");
        mSbcXqBitrate.setOnPreferenceChangeListener(this);
        mDashboard = findPreference("bt_dashboard");
        mDashboard.setCards(R.string.bt_card_device, R.string.bt_card_codec,
                R.string.bt_card_format, R.string.bt_card_bitrate, R.string.bt_card_encoding,
                R.string.bt_card_channels);

        BluetoothManager bm = getContext().getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        if (adapter != null) {
            adapter.getProfileProxy(getContext(), mListener, BluetoothProfile.A2DP);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean xq = SystemProperties.getBoolean(PROP_SBC_XQ, false);
        mSbcXq.setChecked(xq);
        setBitrate(SystemProperties.get(PROP_SBC_XQ_BITRATE, DEFAULT_XQ_BITRATE));
        mSbcXqBitrate.setEnabled(xq);
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
        BluetoothManager bm = getContext().getSystemService(BluetoothManager.class);
        if (mA2dp != null && bm != null && bm.getAdapter() != null) {
            bm.getAdapter().closeProfileProxy(BluetoothProfile.A2DP, mA2dp);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSbcXq) {
            boolean xq = (Boolean) newValue;
            SystemProperties.set(PROP_SBC_XQ, xq ? "true" : "false");
            mSbcXqBitrate.setEnabled(xq);
        } else if (preference == mSbcXqBitrate) {
            SystemProperties.set(PROP_SBC_XQ_BITRATE, (String) newValue);
            setBitrate((String) newValue);
            return false;
        }
        return true;
    }

    private void setBitrate(String value) {
        if (mSbcXqBitrate.findIndexOfValue(value) < 0) {
            value = DEFAULT_XQ_BITRATE;
        }
        mSbcXqBitrate.setValue(value);
        mSbcXqBitrate.setSummary(getString(R.string.sbc_xq_bitrate_summary,
                mSbcXqBitrate.getEntry()));
    }

    private void refresh() {
        if (!isResumed()) {
            return;
        }
        BluetoothDevice device = activeDevice();
        BluetoothCodecStatus status = device != null ? mA2dp.getCodecStatus(device) : null;
        BluetoothCodecConfig config = status != null ? status.getCodecConfig() : null;
        if (config == null) {
            mDashboard.setCard(CARD_DEVICE, getString(R.string.bt_not_connected), null);
            for (int card = CARD_CODEC; card <= CARD_CHANNELS; card++) {
                mDashboard.setCard(card, "—", null);
            }
            return;
        }

        String name = device.getAlias();
        mDashboard.setCard(CARD_DEVICE, name != null ? name : device.getAddress(), null);

        int type = config.getCodecType();
        mDashboard.setCard(CARD_CODEC, codecName(type),
                type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
                        ? ldacMode(config.getCodecSpecific1()) : null);

        mDashboard.setCard(CARD_FORMAT, sampleRate(config.getSampleRate()),
                bits(config.getBitsPerSample()));

        long bps = SystemProperties.getLong(PROP_BITRATE, 0);
        String bitpool = null;
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC) {
            int pool = SystemProperties.getInt(PROP_SBC_BITPOOL, 0);
            if (pool > 0) {
                bitpool = getString(R.string.bt_bitpool, pool);
            }
        }
        mDashboard.setCard(CARD_BITRATE, bps > 0
                ? String.format(Locale.getDefault(), "%d kbps", bps / 1000) : "—", bitpool);

        String encoding = SystemProperties.get(PROP_ENCODING, "");
        mDashboard.setCard(CARD_ENCODING, getString("offload".equals(encoding)
                ? R.string.bt_encoding_dsp : "software".equals(encoding)
                        ? R.string.bt_encoding_cpu : R.string.bt_unknown), null);

        String channels = config.getChannelMode() == BluetoothCodecConfig.CHANNEL_MODE_MONO
                ? getString(R.string.bt_channels_mono) : getString(R.string.bt_channels_stereo);
        String detail = null;
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC) {
            String mode = SystemProperties.get(PROP_SBC_MODE, "");
            if ("dual".equals(mode)) {
                detail = getString(R.string.bt_sbc_dual);
            } else if ("joint".equals(mode)) {
                detail = getString(R.string.bt_sbc_joint);
            }
        }
        mDashboard.setCard(CARD_CHANNELS, channels, detail);
    }

    private BluetoothDevice activeDevice() {
        if (mA2dp == null) {
            return null;
        }
        BluetoothManager bm = getContext().getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        if (adapter == null) {
            return null;
        }
        List<BluetoothDevice> devices = adapter.getActiveDevices(BluetoothProfile.A2DP);
        return devices.isEmpty() ? null : devices.get(0);
    }

    private static String codecName(int type) {
        switch (type) {
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC:
                return "SBC";
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC:
                return "AAC";
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
                return "aptX";
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
                return "aptX HD";
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC:
                return "LDAC";
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS:
                return "Opus";
            default:
                return "—";
        }
    }

    private String ldacMode(long specific1) {
        switch ((int) (specific1 % 10)) {
            case 0:
                return getString(R.string.bt_ldac_quality);
            case 1:
                return getString(R.string.bt_ldac_normal);
            case 2:
                return getString(R.string.bt_ldac_connection);
            default:
                return getString(R.string.bt_ldac_adaptive);
        }
    }

    private static String sampleRate(int rate) {
        switch (rate) {
            case BluetoothCodecConfig.SAMPLE_RATE_44100:
                return "44.1 kHz";
            case BluetoothCodecConfig.SAMPLE_RATE_48000:
                return "48 kHz";
            case BluetoothCodecConfig.SAMPLE_RATE_88200:
                return "88.2 kHz";
            case BluetoothCodecConfig.SAMPLE_RATE_96000:
                return "96 kHz";
            case BluetoothCodecConfig.SAMPLE_RATE_176400:
                return "176.4 kHz";
            case BluetoothCodecConfig.SAMPLE_RATE_192000:
                return "192 kHz";
            default:
                return "—";
        }
    }

    private static String bits(int bits) {
        switch (bits) {
            case BluetoothCodecConfig.BITS_PER_SAMPLE_16:
                return "16 bit";
            case BluetoothCodecConfig.BITS_PER_SAMPLE_24:
                return "24 bit";
            case BluetoothCodecConfig.BITS_PER_SAMPLE_32:
                return "32 bit";
            default:
                return null;
        }
    }
}
