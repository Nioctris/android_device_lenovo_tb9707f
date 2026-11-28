/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.Footer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.AreaChart
import org.lineageos.lenovoparts.ui.ChartCard
import org.lineageos.lenovoparts.ui.LockedSlider
import org.lineageos.lenovoparts.ui.Metric
import org.lineageos.lenovoparts.ui.MetricGrid
import org.lineageos.lenovoparts.ui.PartsScaffold

object PowerPageProvider : SettingsPageProvider {
    override val name = "Power"

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.charging_title)

    @Composable
    override fun Page(arguments: Bundle?) = PowerPage()
}

@Composable
private fun PowerPage() {
    val context = LocalContext.current
    val state by PowerRepository.state(context).collectAsStateWithLifecycle()
    var askHigh by remember { mutableStateOf(false) }

    PartsScaffold(stringResource(R.string.charging_title)) {
        MetricGrid(powerMetrics(context, state?.stats))
        Spacer(Modifier.height(8.dp))
        val history = state?.history ?: FloatArray(0)
        val max = ceil(maxOf(5f, history.maxOrNull() ?: 0f) / 5f) * 5f
        val min = floor(minOf(-5f, history.minOrNull() ?: 0f) / 5f) * 5f
        ChartCard(
            title = stringResource(R.string.power_graph_title),
            value = state?.let { signedWatts(it.stats.batteryWatts()) },
        ) {
            AreaChart(
                samples = history, window = 600, min = min, max = max, base = 0f,
                color = colorResource(R.color.graph_charge),
                negativeColor = colorResource(R.color.graph_discharge),
                format = { signedWatts(it.toDouble()) },
                gridLines = listOf(max to "+${max.toInt()} W", 0f to "0 W",
                    min to "−${abs(min.toInt())} W"),
            )
        }

        Category(title = stringResource(R.string.power_controls_title)) {
            SwitchPreference(object : SwitchPreferenceModel {
                override val title = context.getString(R.string.bypass_charging_title)
                override val summary = { context.getString(R.string.bypass_charging_summary) }
                override val checked = { state?.bypass }
                override val onCheckedChange: (Boolean) -> Unit =
                    { BypassCharging.setEnabled(context, it) }
            })
            SwitchPreference(object : SwitchPreferenceModel {
                override val title = context.getString(R.string.fast_charging_title)
                override val summary = { context.getString(R.string.fast_charging_summary) }
                override val checked = { state?.fast }
                override val onCheckedChange: (Boolean) -> Unit =
                    { ChargingPower.setFastEnabled(context, it) }
            })
            val s = state
            if (s != null) {
                LockedSlider(
                    title = stringResource(R.string.charging_power_title),
                    value = s.watts,
                    range = ChargingPower.MIN_WATTS..ChargingPower.MAX_WATTS,
                    limit = ChargingPower.SAFE_WATTS,
                    locked = !s.highAllowed,
                    enabled = s.fast,
                    format = { context.getString(R.string.charging_power_summary, it) },
                    onCommit = { ChargingPower.setWatts(context, it) },
                    onUnlockRequest = { askHigh = true },
                )
            }
            SwitchPreference(object : SwitchPreferenceModel {
                override val title = context.getString(R.string.charging_power_high_title)
                override val summary = { context.getString(R.string.charging_power_high_summary) }
                override val checked = { state?.highAllowed }
                override val changeable = { state?.fast == true }
                override val onCheckedChange: (Boolean) -> Unit = {
                    if (it) askHigh = true else ChargingPower.setHighAllowed(context, false)
                }
            })
        }
        Footer(stringResource(R.string.charging_hint))
    }

    if (askHigh) {
        AlertDialog(
            onDismissRequest = { askHigh = false },
            title = { Text(stringResource(R.string.charging_power_warning_title)) },
            text = { Text(stringResource(R.string.charging_power_warning_text)) },
            confirmButton = {
                TextButton(onClick = {
                    ChargingPower.setHighAllowed(context, true)
                    askHigh = false
                }) { Text(stringResource(R.string.charging_power_warning_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { askHigh = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun powerMetrics(context: Context, s: PowerStats?): List<Metric> {
    fun str(id: Int, vararg args: Any) = context.getString(id, *args)
    if (s == null) {
        return listOf(R.string.power_card_status, R.string.power_card_power,
            R.string.power_card_charger, R.string.power_card_pumps, R.string.power_card_temp,
            R.string.power_card_capacity).map { Metric(str(it), "—") }
    }
    val status = when (s.status) {
        null -> str(R.string.power_status_unknown)
        "Charging" -> str(R.string.power_status_charging)
        "Full" -> str(R.string.power_status_full)
        "Not charging" -> str(R.string.power_status_not_charging)
        else -> str(R.string.power_status_discharging)
    }
    val mode = when {
        !s.usbPresent -> null
        s.bypass -> str(R.string.power_mode_bypass)
        s.limitBypass -> str(R.string.power_mode_limit)
        else -> null
    }
    val charger = when {
        !s.usbPresent -> Metric(str(R.string.power_card_charger), str(R.string.power_charger_none))
        else -> Metric(
            str(R.string.power_card_charger),
            when (s.pdActive) {
                2 -> "USB-PD PPS"
                1 -> "USB-PD"
                else -> s.usbType?.replace('_', ' ') ?: "USB"
            },
            "${volts(s.vbusUv)} · ${watts(s.inputWatts())}",
        )
    }
    val pumps = if (s.pumpEnabled) {
        Metric(str(R.string.power_card_pumps), str(R.string.power_pump_on),
            str(R.string.power_pump_on_detail, amps(s.pumpInputUa()), celsius(s.pumpTemp)))
    } else {
        Metric(str(R.string.power_card_pumps), str(R.string.power_pump_off))
    }
    val capacity = if (PowerStats.valid(s.chargeFullUah) && s.chargeDesignUah > 0) {
        Metric(str(R.string.power_card_capacity),
            "${Math.round(100.0 * s.chargeFullUah / s.chargeDesignUah)}%",
            str(R.string.power_capacity_detail, s.chargeFullUah / 1000,
                s.chargeDesignUah / 1000, s.cycles))
    } else {
        Metric(str(R.string.power_card_capacity), "—")
    }
    return listOf(
        Metric(str(R.string.power_card_status), "${s.capacity}%",
            if (mode != null) "$status · $mode" else status),
        Metric(str(R.string.power_card_power), signedWatts(s.batteryWatts()),
            str(R.string.power_card_power_detail, amps(s.batteryUa), volts(s.batteryUv))),
        charger,
        pumps,
        Metric(str(R.string.power_card_temp), celsius(s.batteryTemp), s.health),
        capacity,
    )
}

private fun signedWatts(w: Double) = String.format(Locale.getDefault(), "%+.1f\u00a0W", w)
private fun watts(w: Double) = String.format(Locale.getDefault(), "%.1f\u00a0W", w)
private fun amps(ua: Long) = String.format(Locale.getDefault(), "%+.2f\u00a0A", ua / 1e6)
private fun volts(uv: Long) = String.format(Locale.getDefault(), "%.2f\u00a0V", uv / 1e6)
private fun celsius(deci: Int) = String.format(Locale.getDefault(), "%.1f\u00a0°C", deci / 10.0)
