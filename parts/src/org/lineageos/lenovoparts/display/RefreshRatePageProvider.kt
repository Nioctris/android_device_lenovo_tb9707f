/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.display

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.Category
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.PartsScaffold
import org.lineageos.lenovoparts.ui.rememberSystemSettings

private const val MIN_REFRESH_RATE = "min_refresh_rate"
private const val PEAK_REFRESH_RATE = "peak_refresh_rate"
private const val LOW = 60f
private const val HIGH = 120f
/* Lowest rate the panel's touch controller has a scan table for. */
private const val IDLE = 30f

private data class RefreshRate(val high: Boolean, val adaptive: Boolean)

private fun read(context: Context): RefreshRate {
    val cr = context.contentResolver
    val peak = Settings.System.getFloat(cr, PEAK_REFRESH_RATE, HIGH)
    val min = Settings.System.getFloat(cr, MIN_REFRESH_RATE, 0f)
    val high = peak > LOW + 1f
    return RefreshRate(high, high && min < HIGH - 1f)
}

private fun write(context: Context, rate: RefreshRate) {
    val cr = context.contentResolver
    val min = if (!rate.high) LOW else if (rate.adaptive) IDLE else HIGH
    Settings.System.putFloat(cr, MIN_REFRESH_RATE, min)
    Settings.System.putFloat(cr, PEAK_REFRESH_RATE, if (rate.high) HIGH else LOW)
}

object RefreshRatePageProvider : SettingsPageProvider {
    override val name = "Display"

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.refresh_rate_title)

    @Composable
    override fun Page(arguments: Bundle?) {
        val context = LocalContext.current
        val rate by rememberSystemSettings(MIN_REFRESH_RATE, PEAK_REFRESH_RATE) { read(it) }
        val selected = remember { mutableIntStateOf(0) }
        selected.intValue = if (rate.high) 1 else 0

        PartsScaffold(stringResource(R.string.refresh_rate_title)) {
            RadioPreferences(object : ListPreferenceModel {
                override val title = context.getString(R.string.refresh_rate_fixed_title)
                override val options = listOf(
                    ListPreferenceOption(0, context.getString(R.string.refresh_rate_60)),
                    ListPreferenceOption(1, context.getString(R.string.refresh_rate_120)),
                )
                override val selectedId: IntState = selected
                override val onIdSelected: (Int) -> Unit = {
                    write(context, rate.copy(high = it == 1))
                }
            })
            Category {
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title = context.getString(R.string.refresh_rate_adaptive_title)
                    override val summary = {
                        context.getString(if (rate.high) R.string.refresh_rate_adaptive_summary
                            else R.string.refresh_rate_adaptive_disabled)
                    }
                    override val checked = { rate.adaptive }
                    override val changeable = { rate.high }
                    override val onCheckedChange: (Boolean) -> Unit = {
                        write(context, rate.copy(adaptive = it))
                    }
                })
            }
        }
    }
}
