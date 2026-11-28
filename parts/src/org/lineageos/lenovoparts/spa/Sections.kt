/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.spa

import com.android.settingslib.spa.framework.common.SettingsPageProvider
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.audio.BluetoothAudioPageProvider
import org.lineageos.lenovoparts.charging.PowerPageProvider
import org.lineageos.lenovoparts.display.RefreshRatePageProvider
import org.lineageos.lenovoparts.perf.PerformancePageProvider
import org.lineageos.lenovoparts.sideswitch.SideSwitchPageProvider

/** A top-level section: its entry in the list and its page. */
class Section(
    val title: Int,
    val summary: Int,
    val icon: Int,
    val provider: SettingsPageProvider,
)

object Sections {
    val all = listOf(
        Section(R.string.charging_title, R.string.charging_summary,
            R.drawable.ic_bypass_charging, PowerPageProvider),
        Section(R.string.perf_profile_title, R.string.perf_profile_menu_summary,
            R.drawable.ic_perf_profile_mid, PerformancePageProvider),
        Section(R.string.refresh_rate_title, R.string.refresh_rate_menu_summary,
            R.drawable.ic_display, RefreshRatePageProvider),
        Section(R.string.bt_audio_title, R.string.bt_audio_summary,
            R.drawable.ic_bluetooth_audio, BluetoothAudioPageProvider),
        Section(R.string.side_switch_title, R.string.side_switch_menu_summary,
            R.drawable.ic_side_switch, SideSwitchPageProvider),
    )
}
