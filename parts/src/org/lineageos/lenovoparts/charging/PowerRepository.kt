/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging

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

/** Battery and charger readings with the charging settings. */
class PowerState(
    val stats: PowerStats,
    val bypass: Boolean,
    val fast: Boolean,
    val highAllowed: Boolean,
    val watts: Int,
    val history: Samples,
    val now: Long,
)

object PowerRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flow: StateFlow<PowerState?>? = null

    /** Follows the [Monitor] readings while someone observes them. */
    fun state(context: Context): StateFlow<PowerState?> = flow ?: synchronized(this) {
        flow ?: run {
            Monitor.start(context)
            val cr = context.applicationContext.contentResolver
            Monitor.latest.filterNotNull().map { r ->
                PowerState(
                    stats = r.power,
                    bypass = BypassCharging.isEnabled(cr),
                    fast = ChargingPower.isFastEnabled(cr),
                    highAllowed = ChargingPower.isHighAllowed(cr),
                    watts = ChargingPower.getWatts(cr),
                    history = Monitor.power.snapshot(r.time - Monitor.WINDOW_MS - 2000),
                    now = r.time,
                )
            }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null).also { flow = it }
        }
    }
}
