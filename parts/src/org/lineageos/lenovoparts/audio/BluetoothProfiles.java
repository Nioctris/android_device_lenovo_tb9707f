/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;

/**
 * Per-headphones audio settings, stored by device address and applied when
 * the device connects: codec, LDAC quality, SBC XQ and its bitrate.
 */
public final class BluetoothProfiles {

    public static final String CODEC_AUTO = "auto";
    public static final String DEFAULT_XQ_BITRATE = "552";
    /* LDAC quality values for codec specific 1, -1 for the stack default. */
    public static final long LDAC_DEFAULT = -1;

    /* Read by the Bluetooth stack when it configures an SBC stream. */
    static final String PROP_SBC_XQ = "persist.bluetooth.sbc_xq";
    static final String PROP_SBC_XQ_BITRATE = "persist.bluetooth.sbc_xq_bitrate";

    private static final String PREFS = "bluetooth_profiles";

    private BluetoothProfiles() {}

    public static final class Profile {
        public String codec = CODEC_AUTO;
        public long ldacQuality = LDAC_DEFAULT;
        public boolean sbcXq;
        public String sbcXqBitrate = DEFAULT_XQ_BITRATE;
    }

    private static SharedPreferences prefs(Context context) {
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Profile load(Context context, BluetoothDevice device) {
        SharedPreferences p = prefs(context);
        String key = device.getAddress();
        Profile profile = new Profile();
        profile.codec = p.getString(key + ".codec", CODEC_AUTO);
        profile.ldacQuality = p.getLong(key + ".ldac", LDAC_DEFAULT);
        profile.sbcXq = p.getBoolean(key + ".sbc_xq", false);
        profile.sbcXqBitrate = p.getString(key + ".sbc_xq_bitrate", DEFAULT_XQ_BITRATE);
        return profile;
    }

    public static void save(Context context, BluetoothDevice device, Profile profile) {
        String key = device.getAddress();
        prefs(context).edit()
                .putString(key + ".codec", profile.codec)
                .putLong(key + ".ldac", profile.ldacQuality)
                .putBoolean(key + ".sbc_xq", profile.sbcXq)
                .putString(key + ".sbc_xq_bitrate", profile.sbcXqBitrate)
                .apply();
    }

    /* Sets the SBC XQ properties the stack reads for the next SBC stream. */
    public static void applySbc(Profile profile) {
        SystemProperties.set(PROP_SBC_XQ, profile.sbcXq ? "true" : "false");
        SystemProperties.set(PROP_SBC_XQ_BITRATE, profile.sbcXqBitrate);
    }

    /**
     * Applies the profile to the connected device: SBC XQ, then the codec
     * preference, which also makes the stack reconfigure the stream.
     */
    public static void apply(BluetoothA2dp a2dp, BluetoothDevice device, Profile profile) {
        applySbc(profile);
        BluetoothCodecStatus status = a2dp.getCodecStatus(device);
        if (status == null) {
            return;
        }
        BluetoothCodecConfig target = null;
        if (CODEC_AUTO.equals(profile.codec)) {
            BluetoothCodecConfig current = status.getCodecConfig();
            if (current != null && profile.sbcXq
                    && current.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC) {
                // Reconfigure the SBC stream so that SBC XQ takes effect.
                target = new BluetoothCodecConfig.Builder()
                        .setCodecType(current.getCodecType())
                        .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT)
                        .build();
            }
        } else {
            int type = Integer.parseInt(profile.codec);
            for (BluetoothCodecConfig c : status.getCodecsSelectableCapabilities()) {
                if (c.getCodecType() != type) {
                    continue;
                }
                BluetoothCodecConfig.Builder b = new BluetoothCodecConfig.Builder()
                        .setCodecType(type)
                        .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                        .setSampleRate(Integer.highestOneBit(c.getSampleRate()))
                        .setBitsPerSample(Integer.highestOneBit(c.getBitsPerSample()))
                        .setChannelMode(Integer.highestOneBit(c.getChannelMode()));
                if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC
                        && profile.ldacQuality != LDAC_DEFAULT) {
                    b.setCodecSpecific1(profile.ldacQuality);
                }
                target = b.build();
                break;
            }
        }
        if (target != null) {
            a2dp.setCodecConfigPreference(device, target);
        }
    }
}
