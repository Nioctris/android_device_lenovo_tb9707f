/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.Footer
import java.util.Locale
import kotlin.math.roundToInt
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.AreaChart
import org.lineageos.lenovoparts.ui.Metric
import org.lineageos.lenovoparts.ui.MetricGrid
import org.lineageos.lenovoparts.ui.PartsScaffold
import org.lineageos.lenovoparts.ui.rememberSystemSettings

object PerformancePageProvider : SettingsPageProvider {
    override val name = "Performance"

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.perf_profile_title)

    @Composable
    override fun Page(arguments: Bundle?) = PerformancePage()
}

private val PROFILES = listOf(
    Triple(PerfProfile.POWERSAVE, R.string.perf_profile_powersave, R.drawable.ic_perf_profile_low),
    Triple(PerfProfile.BALANCED, R.string.perf_profile_balanced, R.drawable.ic_perf_profile_mid),
    Triple(PerfProfile.PERFORMANCE, R.string.perf_profile_performance,
        R.drawable.ic_perf_profile_high),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerformancePage() {
    val context = LocalContext.current
    val state by PerfRepository.state(context).collectAsStateWithLifecycle()
    val profile by rememberSystemSettings(PerfProfile.SETTING) {
        PerfProfile.get(it.contentResolver)
    }

    PartsScaffold(stringResource(R.string.perf_profile_title)) {
        MetricGrid(perfMetrics(context, state?.stats))
        Spacer(Modifier.height(8.dp))
        LoadCard(state)

        Category(title = stringResource(R.string.perf_profile_title)) {
            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 0.dp)
            ) {
                PROFILES.forEachIndexed { i, (key, label, icon) ->
                    SegmentedButton(
                        selected = profile == key,
                        onClick = { PerfProfile.set(context, key) },
                        shape = SegmentedButtonDefaults.itemShape(i, PROFILES.size),
                        icon = {
                            Icon(painterResource(icon), contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                    ) {
                        Text(stringResource(label), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Footer(stringResource(R.string.perf_profile_hint))
    }
}

@Composable
private fun LoadCard(state: PerfState?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.load_graph_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LoadRow(stringResource(R.string.load_cpu), state?.cpuHistory,
                colorResource(R.color.graph_cpu))
            LoadRow(stringResource(R.string.load_gpu), state?.gpuHistory,
                colorResource(R.color.graph_gpu))
            LoadRow(stringResource(R.string.load_ram), state?.ramHistory,
                colorResource(R.color.graph_ram))
        }
    }
}

@Composable
private fun LoadRow(label: String, history: FloatArray?, color: Color) {
    val samples = history ?: FloatArray(0)
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(if (samples.isEmpty()) "—" else "${samples.last().roundToInt()}%",
                style = MaterialTheme.typography.titleMedium, color = color)
        }
        AreaChart(
            samples = samples, window = 600, min = 0f, max = 100f, color = color,
            gridLines = listOf(100f to "100%", 50f to "50%", 0f to "0%"),
            format = { "${it.roundToInt()}%" },
            height = 110.dp,
        )
    }
}

private fun perfMetrics(context: Context, s: PerfStats?): List<Metric> {
    fun str(id: Int, vararg args: Any) = context.getString(id, *args)
    val labels = listOf(R.string.perf_card_silver, R.string.perf_card_gold,
        R.string.perf_card_prime, R.string.perf_card_gpu, R.string.perf_card_temp,
        R.string.perf_card_memory)
    if (s == null) return labels.map { Metric(str(it), "—") }
    val clusters = s.clusters.mapIndexed { i, c ->
        val range = "${ghz(c.minKhz * 1000)}–${ghz(c.maxKhz * 1000)}"
        Metric(str(labels[i]), ghz(c.curKhz * 1000),
            if (c.loadPercent >= 0) "${str(R.string.perf_load, c.loadPercent)} · $range"
            else range)
    }
    val gpu = Metric(str(R.string.perf_card_gpu),
        if (s.gpuHz > 0) mhz(s.gpuHz) else str(R.string.perf_gpu_idle),
        if (s.gpuBusy >= 0) "${str(R.string.perf_load, s.gpuBusy)} · ${mhz(s.gpuMaxHz)}"
        else mhz(s.gpuMaxHz))
    val temp = Metric(str(R.string.perf_card_temp), celsius(s.cpuTemp),
        str(R.string.perf_temp_detail, celsius(s.gpuTemp), celsius(s.skinTemp)))
    val memory = if (s.memTotalKb > 0) {
        var detail = str(R.string.perf_memory_detail, gb(s.memTotalKb))
        if (s.swapTotalKb > 0) {
            detail += " · " + str(R.string.perf_zram, gb(s.swapTotalKb - s.swapFreeKb),
                gb(s.swapTotalKb))
        }
        Metric(str(R.string.perf_card_memory), gb(s.memAvailKb), detail)
    } else Metric(str(R.string.perf_card_memory), "—")
    return clusters + gpu + temp + memory
}

private fun ghz(hz: Long) =
    if (hz <= 0) "—" else String.format(Locale.getDefault(), "%.2f\u00a0GHz", hz / 1e9)
private fun mhz(hz: Long) =
    if (hz <= 0) "—" else String.format(Locale.getDefault(), "%.0f\u00a0MHz", hz / 1e6)
private fun celsius(c: Float) =
    if (c.isNaN()) "—" else String.format(Locale.getDefault(), "%.1f\u00a0°C", c)
private fun gb(kb: Long) = String.format(Locale.getDefault(), "%.1f\u00a0GB", kb / 1048576.0)
