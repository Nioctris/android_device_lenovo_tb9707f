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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BluetoothAudioFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    /* Published by the Bluetooth stack for the active stream and peer. */
    private static final String PROP_ENCODING = "bluetooth.a2dp.encoding";
    private static final String PROP_BITRATE = "bluetooth.a2dp.bitrate";
    private static final String PROP_SBC_BITPOOL = "bluetooth.a2dp.sbc.bitpool";
    private static final String PROP_SBC_MODE = "bluetooth.a2dp.sbc.mode";
    private static final String PROP_PEER_SBC_BITPOOL = "bluetooth.a2dp.peer.sbc_max_bitpool";
    private static final String PROP_PEER_AAC_BITRATE = "bluetooth.a2dp.peer.aac_bitrate";

    private static final long REFRESH_MS = 2000;

    private static final int CARD_DEVICE = 0;
    private static final int CARD_CODEC = 1;
    private static final int CARD_FORMAT = 2;
    private static final int CARD_BITRATE = 3;
    private static final int CARD_ENCODING = 4;
    private static final int CARD_CHANNELS = 5;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private CardGridPreference mDashboard;
    private ListPreference mCodec;
    private ListPreference mLdac;
    private Preference mPeerCodecs;
    private SwitchPreferenceCompat mSbcXq;
    private ListPreference mSbcXqBitrate;
    private Preference mProfileHint;
    private BluetoothA2dp mA2dp;

    private BluetoothDevice mDevice;
    private BluetoothProfiles.Profile mProfile;

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
        mDashboard = findPreference("bt_dashboard");
        mDashboard.setCards(R.string.bt_card_device, R.string.bt_card_codec,
                R.string.bt_card_format, R.string.bt_card_bitrate, R.string.bt_card_encoding,
                R.string.bt_card_channels);
        mCodec = findPreference("bt_codec");
        mLdac = findPreference("ldac_quality");
        mPeerCodecs = findPreference("bt_peer_codecs");
        mSbcXq = findPreference("sbc_xq");
        mSbcXqBitrate = findPreference("sbc_xq_bitrate");
        mProfileHint = findPreference("bt_profile_hint");
        mCodec.setOnPreferenceChangeListener(this);
        mLdac.setOnPreferenceChangeListener(this);
        mSbcXq.setOnPreferenceChangeListener(this);
        mSbcXqBitrate.setOnPreferenceChangeListener(this);

        BluetoothManager bm = getContext().getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        if (adapter != null) {
            adapter.getProfileProxy(getContext(), mListener, BluetoothProfile.A2DP);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
        if (mDevice == null || mProfile == null) {
            return false;
        }
        if (preference == mCodec) {
            mProfile.codec = (String) newValue;
        } else if (preference == mLdac) {
            mProfile.ldacQuality = Long.parseLong((String) newValue);
            mProfile.codec = Integer.toString(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC);
        } else if (preference == mSbcXq) {
            mProfile.sbcXq = (Boolean) newValue;
        } else if (preference == mSbcXqBitrate) {
            mProfile.sbcXqBitrate = (String) newValue;
        }
        BluetoothProfiles.save(getContext(), mDevice, mProfile);
        BluetoothProfiles.apply(mA2dp, mDevice, mProfile);
        mHandler.postDelayed(this::refresh, 700);
        return false;
    }

    private void refresh() {
        if (!isResumed()) {
            return;
        }
        BluetoothDevice device = activeDevice();
        if (device == null || !device.equals(mDevice)) {
            mDevice = device;
            mProfile = device != null ? BluetoothProfiles.load(getContext(), device) : null;
        }
        BluetoothCodecStatus status = device != null ? mA2dp.getCodecStatus(device) : null;
        BluetoothCodecConfig config = status != null ? status.getCodecConfig() : null;
        updateControls(status);
        if (config == null) {
            mDashboard.setCard(CARD_DEVICE, getString(R.string.bt_not_connected), null);
            for (int card = CARD_CODEC; card <= CARD_CHANNELS; card++) {
                mDashboard.setCard(card, "—", null);
            }
            return;
        }

        String name = device.getAlias();
        int battery = device.getBatteryLevel();
        mDashboard.setCard(CARD_DEVICE, name != null ? name : device.getAddress(),
                battery >= 0 ? getString(R.string.bt_battery, battery) : null);

        int type = config.getCodecType();
        mDashboard.setCard(CARD_CODEC, codecName(type),
                type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
                        ? ldacMode(config.getCodecSpecific1()) : null);
        mDashboard.setCard(CARD_FORMAT, sampleRate(config.getSampleRate()),
                bits(config.getBitsPerSample()));

        long kbps = SystemProperties.getLong(PROP_BITRATE, 0) / 1000;
        String bitrateDetail = null;
        String channelDetail = null;
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC) {
            int pool = SystemProperties.getInt(PROP_SBC_BITPOOL, 0);
            String mode = SystemProperties.get(PROP_SBC_MODE, "");
            if (pool > 0) {
                kbps = sbcKbps(pool, "dual".equals(mode));
                bitrateDetail = getString(R.string.bt_bitpool, pool);
            }
            if ("dual".equals(mode)) {
                channelDetail = getString(R.string.bt_sbc_dual);
            } else if ("joint".equals(mode)) {
                channelDetail = getString(R.string.bt_sbc_joint);
            }
        }
        mDashboard.setCard(CARD_BITRATE, kbps > 0
                ? String.format(Locale.getDefault(), "%d kbps", kbps) : "—", bitrateDetail);

        String encoding = SystemProperties.get(PROP_ENCODING, "");
        mDashboard.setCard(CARD_ENCODING, getString("offload".equals(encoding)
                ? R.string.bt_encoding_dsp : "software".equals(encoding)
                        ? R.string.bt_encoding_cpu : R.string.bt_unknown), null);
        mDashboard.setCard(CARD_CHANNELS,
                config.getChannelMode() == BluetoothCodecConfig.CHANNEL_MODE_MONO
                        ? getString(R.string.bt_channels_mono)
                        : getString(R.string.bt_channels_stereo), channelDetail);
    }

    private void updateControls(BluetoothCodecStatus status) {
        List<BluetoothCodecConfig> selectable =
                status != null ? status.getCodecsSelectableCapabilities() : null;
        boolean connected = mProfile != null && selectable != null && !selectable.isEmpty();
        mCodec.setEnabled(connected);
        mSbcXq.setEnabled(connected);
        mSbcXqBitrate.setEnabled(connected && mProfile.sbcXq);
        if (!connected) {
            mCodec.setSummary(null);
            mLdac.setVisible(false);
            mPeerCodecs.setSummary(getString(R.string.bt_not_connected));
            mProfileHint.setSummary(R.string.bt_profile_hint_none);
            return;
        }
        String name = mDevice.getAlias() != null ? mDevice.getAlias() : mDevice.getAddress();
        mProfileHint.setSummary(getString(R.string.bt_profile_hint, name));

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.bt_codec_auto));
        values.add(BluetoothProfiles.CODEC_AUTO);
        StringBuilder peer = new StringBuilder();
        boolean ldac = false;
        for (BluetoothCodecConfig c : selectable) {
            int type = c.getCodecType();
            String name1 = codecName(type);
            entries.add(name1);
            values.add(Integer.toString(type));
            ldac |= type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC;
            if (peer.length() > 0) {
                peer.append('\n');
            }
            peer.append(name1).append(" · ").append(rates(c.getSampleRate()));
            String b = bitsList(c.getBitsPerSample());
            if (b != null) {
                peer.append(" · ").append(b);
            }
            String rate = maxBitrate(type);
            if (rate != null) {
                peer.append(" · ").append(rate);
            }
        }
        mPeerCodecs.setSummary(peer);
        mCodec.setEntries(entries.toArray(new CharSequence[0]));
        mCodec.setEntryValues(values.toArray(new CharSequence[0]));
        mCodec.setValue(mProfile.codec);
        CharSequence entry = mCodec.getEntry();
        mCodec.setSummary(BluetoothProfiles.CODEC_AUTO.equals(mProfile.codec)
                ? getString(R.string.bt_codec_auto_summary) : entry);

        mLdac.setVisible(ldac);
        mLdac.setValue(Long.toString(mProfile.ldacQuality));
        mLdac.setSummary(mLdac.getEntry());

        mSbcXq.setChecked(mProfile.sbcXq);
        String rate = mProfile.sbcXqBitrate;
        if (mSbcXqBitrate.findIndexOfValue(rate) < 0) {
            rate = BluetoothProfiles.DEFAULT_XQ_BITRATE;
        }
        mSbcXqBitrate.setValue(rate);
        mSbcXqBitrate.setSummary(getString(R.string.sbc_xq_bitrate_summary,
                mSbcXqBitrate.getEntry()));
    }

    /* The highest bitrate the headphones accept for a codec, if known. */
    private String maxBitrate(int type) {
        switch (type) {
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC: {
                int pool = SystemProperties.getInt(PROP_PEER_SBC_BITPOOL, 0);
                if (pool <= 0) {
                    return null;
                }
                return getString(R.string.bt_peer_sbc_rate, sbcKbps(Math.min(pool, 53), false),
                        sbcKbps(pool, true));
            }
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC: {
                long bps = SystemProperties.getLong(PROP_PEER_AAC_BITRATE, 0);
                return bps > 0 ? getString(R.string.bt_peer_max_rate, bps / 1000) : null;
            }
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC:
                return getString(R.string.bt_peer_ldac_rate);
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
                return getString(R.string.bt_peer_aptx_rate);
            case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
                return getString(R.string.bt_peer_aptx_hd_rate);
            default:
                return null;
        }
    }

    /* SBC at 44.1 kHz, 8 subbands, 16 blocks, stereo: frame size to kbps. */
    private static long sbcKbps(int bitpool, boolean dual) {
        int frame = dual ? 4 + 8 + 4 * bitpool : 4 + 8 + (8 + 16 * bitpool + 7) / 8;
        return Math.round(frame * 8 * 44100.0 / 128 / 1000);
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

    private static String rates(int mask) {
        int[] bits = {BluetoothCodecConfig.SAMPLE_RATE_44100, BluetoothCodecConfig.SAMPLE_RATE_48000,
                BluetoothCodecConfig.SAMPLE_RATE_88200, BluetoothCodecConfig.SAMPLE_RATE_96000,
                BluetoothCodecConfig.SAMPLE_RATE_176400, BluetoothCodecConfig.SAMPLE_RATE_192000};
        String[] names = {"44.1", "48", "88.2", "96", "176.4", "192"};
        String list = join(mask, bits, names);
        return list != null ? list + " kHz" : "—";
    }

    private static String bitsList(int mask) {
        int[] bits = {BluetoothCodecConfig.BITS_PER_SAMPLE_16,
                BluetoothCodecConfig.BITS_PER_SAMPLE_24, BluetoothCodecConfig.BITS_PER_SAMPLE_32};
        String list = join(mask, bits, new String[] {"16", "24", "32"});
        return list != null ? list + " bit" : null;
    }

    private static String join(int mask, int[] bits, String[] names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            if ((mask & bits[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(names[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
