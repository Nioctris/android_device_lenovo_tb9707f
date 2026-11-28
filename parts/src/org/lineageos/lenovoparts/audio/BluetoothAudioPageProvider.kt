/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.audio

import android.bluetooth.BluetoothCodecConfig
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.Footer
import java.util.Locale
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.Metric
import org.lineageos.lenovoparts.ui.MetricGrid
import org.lineageos.lenovoparts.ui.PartsScaffold

object BluetoothAudioPageProvider : SettingsPageProvider {
    override val name = "BluetoothAudio"

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.bt_audio_title)

    @Composable
    override fun Page(arguments: Bundle?) = BluetoothAudioPage()
}

/* Codec types in a fixed order: the stack sorts by priority, which moves the chosen one. */
private val CODEC_ORDER = listOf(
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC, BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD,
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX, BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC, BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS,
)
private const val AUTO_ID = -1
private val LDAC_VALUES = listOf(-1L, 1000L, 1001L, 1002L, 1003L)
private val LDAC_LABELS = listOf(R.string.bt_ldac_default, R.string.bt_ldac_quality,
    R.string.bt_ldac_normal, R.string.bt_ldac_connection, R.string.bt_ldac_adaptive)
private val XQ_RATES = listOf("452", "512", "552", "617")
private val XQ_LABELS = listOf(R.string.sbc_xq_bitrate_452, R.string.sbc_xq_bitrate_512,
    R.string.sbc_xq_bitrate_552, R.string.sbc_xq_bitrate_617)

@Composable
private fun BluetoothAudioPage() {
    val context = LocalContext.current
    val state by BluetoothAudioRepository.state(context).collectAsStateWithLifecycle()
    val s = state
    val profile = s?.profile
    val connected = s?.device != null && s.selectable.isNotEmpty() && profile != null

    PartsScaffold(stringResource(R.string.bt_audio_title)) {
        MetricGrid(streamMetrics(context, s))

        if (connected) {
            val codecs = s!!.selectable.sortedBy { CODEC_ORDER.indexOf(it.codecType) }
            val codecId = remember { mutableIntStateOf(AUTO_ID) }
            codecId.intValue = profile!!.codec.toIntOrNull() ?: AUTO_ID
            RadioPreferences(object : ListPreferenceModel {
                override val title = context.getString(R.string.bt_codec_category)
                override val options = listOf(
                    ListPreferenceOption(AUTO_ID, context.getString(R.string.bt_codec_auto),
                        context.getString(R.string.bt_codec_auto_detail))
                ) + codecs.map { c ->
                    ListPreferenceOption(c.codecType, codecName(c.codecType),
                        codecSummary(context, s, c))
                }
                override val selectedId: IntState = codecId
                override val onIdSelected: (Int) -> Unit = { id ->
                    BluetoothAudioRepository.update(context) {
                        it.codec = if (id == AUTO_ID) BluetoothProfiles.CODEC_AUTO
                        else id.toString()
                    }
                }
            })

            if (codecs.any { it.codecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC }) {
                val ldacId = remember { mutableIntStateOf(0) }
                ldacId.intValue = LDAC_VALUES.indexOf(profile.ldacQuality).coerceAtLeast(0)
                Category {
                    ListPreference(object : ListPreferenceModel {
                        override val title = context.getString(R.string.ldac_quality_title)
                        override val options = LDAC_LABELS.mapIndexed { i, id ->
                            ListPreferenceOption(i, context.getString(id))
                        }
                        override val selectedId: IntState = ldacId
                        override val onIdSelected: (Int) -> Unit = { i ->
                            BluetoothAudioRepository.update(context) {
                                it.ldacQuality = LDAC_VALUES[i]
                                it.codec =
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC.toString()
                            }
                        }
                    })
                }
            }
        }

        Category(title = stringResource(R.string.bt_sbc_category)) {
            SwitchPreference(object : SwitchPreferenceModel {
                override val title = context.getString(R.string.sbc_xq_title)
                override val summary = { context.getString(R.string.sbc_xq_summary) }
                override val checked = { profile?.sbcXq }
                override val changeable = { connected }
                override val onCheckedChange: (Boolean) -> Unit = { on ->
                    BluetoothAudioRepository.update(context) { it.sbcXq = on }
                }
            })
            val rateId = remember { mutableIntStateOf(2) }
            rateId.intValue = XQ_RATES.indexOf(profile?.sbcXqBitrate).let { if (it < 0) 2 else it }
            ListPreference(object : ListPreferenceModel {
                override val title = context.getString(R.string.sbc_xq_bitrate_title)
                override val enabled = { connected && profile?.sbcXq == true }
                override val options = XQ_LABELS.mapIndexed { i, id ->
                    ListPreferenceOption(i, context.getString(id))
                }
                override val selectedId: IntState = rateId
                override val onIdSelected: (Int) -> Unit = { i ->
                    BluetoothAudioRepository.update(context) { it.sbcXqBitrate = XQ_RATES[i] }
                }
            })
        }
        Footer(
            if (connected) stringResource(R.string.bt_profile_hint, s!!.name ?: "")
            else stringResource(R.string.bt_profile_hint_none)
        )
        Footer(stringResource(R.string.sbc_xq_hint))
    }
}

private fun codecSummary(context: Context, s: BluetoothAudioState, c: BluetoothCodecConfig):
        String {
    val parts = mutableListOf<String>()
    range(c.sampleRate, RATE_BITS, RATE_NAMES, " kHz")?.let { parts += it }
    range(c.bitsPerSample, DEPTH_BITS, DEPTH_NAMES, " bit")?.let { parts += it }
    peerRate(context, s, c.codecType)?.let { parts += it }
    return parts.joinToString(" · ")
}

/** The highest bitrate the headphones accept for a codec, if known. */
private fun peerRate(context: Context, s: BluetoothAudioState, type: Int): String? = when (type) {
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> if (s.peerSbcBitpool > 0) {
        context.getString(R.string.bt_rate_sbc, sbcKbps(minOf(s.peerSbcBitpool, 53), false),
            sbcKbps(s.peerSbcBitpool, true))
    } else null
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> if (s.peerAacBitrateBps > 0) {
        context.getString(R.string.bt_rate_up_to, s.peerAacBitrateBps / 1000)
    } else null
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> context.getString(R.string.bt_rate_up_to, 990)
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> context.getString(R.string.bt_rate_up_to, 384)
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD ->
        context.getString(R.string.bt_rate_up_to, 576)
    else -> null
}

private fun streamMetrics(context: Context, s: BluetoothAudioState?): List<Metric> {
    fun str(id: Int, vararg args: Any) = context.getString(id, *args)
    val c = s?.codec
    if (s?.device == null || c == null) {
        return listOf(Metric(str(R.string.bt_card_device), str(R.string.bt_not_connected))) +
            listOf(R.string.bt_card_codec, R.string.bt_card_format, R.string.bt_card_bitrate,
                R.string.bt_card_encoding, R.string.bt_card_channels).map { Metric(str(it), "—") }
    }
    val type = c.codecType
    var kbps = s.bitrateBps / 1000
    var bitrateDetail: String? = null
    var channelDetail: String? = null
    if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC) {
        if (s.sbcBitpool > 0) {
            kbps = sbcKbps(s.sbcBitpool, s.sbcMode == "dual")
            bitrateDetail = str(R.string.bt_bitpool, s.sbcBitpool)
        }
        channelDetail = when (s.sbcMode) {
            "dual" -> str(R.string.bt_sbc_dual)
            "joint" -> str(R.string.bt_sbc_joint)
            else -> null
        }
    }
    return listOf(
        Metric(str(R.string.bt_card_device), s.name ?: "—",
            if (s.battery >= 0) str(R.string.bt_battery, s.battery) else null),
        Metric(str(R.string.bt_card_codec), codecName(type),
            if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC)
                ldacMode(context, c.codecSpecific1) else null),
        Metric(str(R.string.bt_card_format), sampleRate(c.sampleRate), bits(c.bitsPerSample)),
        Metric(str(R.string.bt_card_bitrate),
            if (kbps > 0) "$kbps kbps" else "—", bitrateDetail),
        Metric(str(R.string.bt_card_encoding), when (s.encoding) {
            "offload" -> str(R.string.bt_encoding_dsp)
            "software" -> str(R.string.bt_encoding_cpu)
            else -> str(R.string.bt_unknown)
        }),
        Metric(str(R.string.bt_card_channels),
            if (c.channelMode == BluetoothCodecConfig.CHANNEL_MODE_MONO)
                str(R.string.bt_channels_mono) else str(R.string.bt_channels_stereo),
            channelDetail),
    )
}

/* SBC at 44.1 kHz, 8 subbands, 16 blocks, stereo: frame size to kbps. */
private fun sbcKbps(bitpool: Int, dual: Boolean): Long {
    val frame = if (dual) 12 + 4 * bitpool else 12 + (8 + 16 * bitpool + 7) / 8
    return Math.round(frame * 8 * 44100.0 / 128 / 1000)
}

private fun codecName(type: Int) = when (type) {
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> "SBC"
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> "AAC"
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> "aptX"
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> "aptX HD"
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> "LDAC"
    BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS -> "Opus"
    else -> "—"
}

private fun ldacMode(context: Context, specific1: Long) = context.getString(
    when ((specific1 % 10).toInt()) {
        0 -> R.string.bt_ldac_quality
        1 -> R.string.bt_ldac_normal
        2 -> R.string.bt_ldac_connection
        else -> R.string.bt_ldac_adaptive
    })

private fun sampleRate(rate: Int) = when (rate) {
    BluetoothCodecConfig.SAMPLE_RATE_44100 -> "44.1 kHz"
    BluetoothCodecConfig.SAMPLE_RATE_48000 -> "48 kHz"
    BluetoothCodecConfig.SAMPLE_RATE_88200 -> "88.2 kHz"
    BluetoothCodecConfig.SAMPLE_RATE_96000 -> "96 kHz"
    BluetoothCodecConfig.SAMPLE_RATE_176400 -> "176.4 kHz"
    BluetoothCodecConfig.SAMPLE_RATE_192000 -> "192 kHz"
    else -> "—"
}

private fun bits(bits: Int) = when (bits) {
    BluetoothCodecConfig.BITS_PER_SAMPLE_16 -> "16 bit"
    BluetoothCodecConfig.BITS_PER_SAMPLE_24 -> "24 bit"
    BluetoothCodecConfig.BITS_PER_SAMPLE_32 -> "32 bit"
    else -> null
}

private val RATE_BITS = listOf(BluetoothCodecConfig.SAMPLE_RATE_44100,
    BluetoothCodecConfig.SAMPLE_RATE_48000, BluetoothCodecConfig.SAMPLE_RATE_88200,
    BluetoothCodecConfig.SAMPLE_RATE_96000, BluetoothCodecConfig.SAMPLE_RATE_176400,
    BluetoothCodecConfig.SAMPLE_RATE_192000)
private val RATE_NAMES = listOf("44.1", "48", "88.2", "96", "176.4", "192")
private val DEPTH_BITS = listOf(BluetoothCodecConfig.BITS_PER_SAMPLE_16,
    BluetoothCodecConfig.BITS_PER_SAMPLE_24, BluetoothCodecConfig.BITS_PER_SAMPLE_32)
private val DEPTH_NAMES = listOf("16", "24", "32")

/* "44.1–96 kHz" from a capability mask, or the single value. */
private fun range(mask: Int, bits: List<Int>, names: List<String>, unit: String): String? {
    val set = bits.indices.filter { mask and bits[it] != 0 }
    if (set.isEmpty()) return null
    return (if (set.size == 1) names[set[0]] else "${names[set.first()]}–${names[set.last()]}") +
        unit
}
