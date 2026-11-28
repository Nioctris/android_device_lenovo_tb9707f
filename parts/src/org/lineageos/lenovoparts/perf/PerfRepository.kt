/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.perf

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.lineageos.lenovoparts.Monitor
import org.lineageos.lenovoparts.ui.Samples

/** CPU, GPU, temperature and memory readings with their load history. */
class PerfState(
    val stats: PerfStats,
    /** Whole-CPU load in percent, weighted by the cores per cluster; -1 if unknown. */
    val cpuLoad: Float,
    val ramLoad: Float,
    val cpuHistory: Samples,
    val gpuHistory: Samples,
    val ramHistory: Samples,
    val now: Long,
)

object PerfRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flow: StateFlow<PerfState?>? = null

    fun state(context: Context): StateFlow<PerfState?> = flow ?: synchronized(this) {
        flow ?: run {
            Monitor.start(context)
            Monitor.latest.filterNotNull().map { r ->
                val since = r.time - Monitor.WINDOW_MS - 2000
                PerfState(r.perf, r.cpuLoad, r.ramLoad, Monitor.cpu.snapshot(since),
                    Monitor.gpu.snapshot(since), Monitor.ram.snapshot(since), r.time)
            }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null).also { flow = it }
        }
    }
}
