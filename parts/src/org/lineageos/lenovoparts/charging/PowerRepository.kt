/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging

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

/** Battery and charger readings with the charging settings, once a second. */
data class PowerState(
    val stats: PowerStats,
    val bypass: Boolean,
    val fast: Boolean,
    val highAllowed: Boolean,
    val watts: Int,
    val history: FloatArray,
)

object PowerRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val history = History()
    private var flow: StateFlow<PowerState?>? = null

    /** Readings while someone observes them; the history is kept between screens. */
    fun state(context: Context): StateFlow<PowerState?> = flow ?: synchronized(this) {
        flow ?: run {
            val cr = context.applicationContext.contentResolver
            flow {
                while (true) {
                    val s = PowerStats.read()
                    history.add(s.batteryWatts().toFloat())
                    emit(
                        PowerState(
                            stats = s,
                            bypass = BypassCharging.isEnabled(cr),
                            fast = ChargingPower.isFastEnabled(cr),
                            highAllowed = ChargingPower.isHighAllowed(cr),
                            watts = ChargingPower.getWatts(cr),
                            history = history.snapshot(),
                        )
                    )
                    delay(1000)
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null).also { flow = it }
        }
    }
}
