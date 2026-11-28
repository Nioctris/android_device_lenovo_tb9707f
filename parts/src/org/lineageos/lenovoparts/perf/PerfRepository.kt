/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.lineageos.lenovoparts.ui.History

/** CPU, GPU, temperature and memory readings with their load history. */
class PerfState(
    val stats: PerfStats,
    /** Whole-CPU load in percent, weighted by the cores per cluster; -1 if unknown. */
    val cpuLoad: Float,
    val ramLoad: Float,
    val cpuHistory: FloatArray,
    val gpuHistory: FloatArray,
    val ramHistory: FloatArray,
)

object PerfRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cpu = History()
    private val gpu = History()
    private val ram = History()
    private var flow: StateFlow<PerfState?>? = null

    fun state(context: Context): StateFlow<PerfState?> = flow ?: synchronized(this) {
        flow ?: run {
            val sampler = PerfStats.Sampler(context.applicationContext)
            flow {
                while (true) {
                    val s = sampler.read()
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
                    // Unknown readings repeat the last value.
                    cpu.add(if (cpuLoad >= 0) cpuLoad else cpu.last() ?: 0f)
                    gpu.add(if (s.gpuBusy >= 0) s.gpuBusy.toFloat() else gpu.last() ?: 0f)
                    ram.add(if (ramLoad >= 0) ramLoad else ram.last() ?: 0f)
                    emit(PerfState(s, cpuLoad, ramLoad, cpu.snapshot(), gpu.snapshot(),
                        ram.snapshot()))
                    delay(1000)
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null).also { flow = it }
        }
    }
}
