/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lineageos.lenovoparts.charging.PowerStats
import org.lineageos.lenovoparts.perf.PerfStats
import org.lineageos.lenovoparts.ui.Series

/**
 * Samples power and load twice a second for the whole life of the app
 * process, so the charts already hold the last minutes when a page opens.
 * It takes no wake lock: while the device sleeps the loop sleeps too, and
 * the charts show the gap.
 */
object Monitor {
    const val PERIOD_MS = 500L
    const val WINDOW_MS = 10 * 60 * 1000L
    private const val CAPACITY = (WINDOW_MS / PERIOD_MS).toInt() + 16

    class Reading(
        val time: Long,
        val power: PowerStats,
        val perf: PerfStats,
        /** Whole-CPU load in percent, weighted by the cores per cluster; -1 if unknown. */
        val cpuLoad: Float,
        val ramLoad: Float,
    )

    val power = Series(CAPACITY)
    val cpu = Series(CAPACITY)
    val gpu = Series(CAPACITY)
    val ram = Series(CAPACITY)

    private val _latest = MutableStateFlow<Reading?>(null)
    /** The newest full reading while observed; a new value means the series have grown. */
    val latest: StateFlow<Reading?> = _latest.asStateFlow()

    private var started = false

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val sampler = PerfStats.Sampler(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            while (true) {
                sample(sampler)
                delay(PERIOD_MS)
            }
        }
    }

    private fun sample(sampler: PerfStats.Sampler) {
        val time = SystemClock.elapsedRealtime()
        // Full readings only while a page shows them; the charts alone need
        // just a handful of nodes.
        val full = _latest.subscriptionCount.value > 0
        val p = if (full) PowerStats.read() else null
        val watts = p?.batteryWatts() ?: PowerStats.readBatteryWatts()
        val s = sampler.read(full)
        var cores = 0
        var total = 0f
        for (i in s.clusters.indices) {
            val load = s.clusters[i].loadPercent
            if (load >= 0) {
                val n = PerfStats.CLUSTERS[i].size
                total += load * n
                cores += n
            }
        }
        val cpuLoad = if (cores > 0) total / cores else -1f
        val ramLoad = if (s.memTotalKb > 0)
            100f * (s.memTotalKb - s.memAvailKb) / s.memTotalKb else -1f

        if (!watts.isNaN()) power.add(time, watts.toFloat())
        // Unknown readings repeat the last value; the first CPU reading has
        // no previous one to compare with.
        (if (cpuLoad >= 0) cpuLoad else cpu.last())?.let { cpu.add(time, it) }
        gpu.add(time, if (s.gpuBusy >= 0) s.gpuBusy.toFloat() else gpu.last() ?: 0f)
        ram.add(time, if (ramLoad >= 0) ramLoad else ram.last() ?: 0f)
        // Without a page, drop the last reading so the next one to subscribe
        // waits for a full reading instead of getting a stale one.
        _latest.value = if (p != null) Reading(time, p, s, cpuLoad, ramLoad) else null
    }
}
