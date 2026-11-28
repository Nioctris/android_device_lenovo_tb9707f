/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.audio

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothCodecConfig
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The active A2DP stream, what the headphones support and their saved profile. */
class BluetoothAudioState(
    val device: BluetoothDevice?,
    val name: String?,
    val battery: Int,
    val codec: BluetoothCodecConfig?,
    val selectable: List<BluetoothCodecConfig>,
    val profile: BluetoothProfiles.Profile?,
    /* Published by the Bluetooth stack (see the stack patch). */
    val encoding: String,
    val bitrateBps: Long,
    val sbcBitpool: Int,
    val sbcMode: String,
    val peerSbcBitpool: Int,
    val peerAacBitrateBps: Long,
)

object BluetoothAudioRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var a2dp: BluetoothA2dp? = null
    private var flow: StateFlow<BluetoothAudioState?>? = null

    fun state(context: Context): StateFlow<BluetoothAudioState?> = flow ?: synchronized(this) {
        flow ?: callbackFlow {
            val app = context.applicationContext
            val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            fun emitNow() {
                launch(Dispatchers.IO) { trySend(read(app)) }
            }
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    a2dp = proxy as BluetoothA2dp
                    emitNow()
                }

                override fun onServiceDisconnected(profile: Int) {
                    a2dp = null
                    emitNow()
                }
            }
            adapter?.getProfileProxy(app, listener, BluetoothProfile.A2DP)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) = emitNow()
            }
            val filter = IntentFilter().apply {
                addAction(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
                addAction(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            }
            app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            // The stack properties (bitrate, bitpool) change without a broadcast.
            val poll = launch {
                while (true) {
                    delay(2000)
                    trySend(read(app))
                }
            }
            awaitClose {
                poll.cancel()
                app.unregisterReceiver(receiver)
                a2dp?.let { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
                a2dp = null
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null).also { flow = it }
    }

    private fun read(context: Context): BluetoothAudioState {
        val proxy = a2dp
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val device = if (proxy != null) {
            adapter?.getActiveDevices(BluetoothProfile.A2DP)?.firstOrNull()
        } else null
        val status = device?.let { proxy?.getCodecStatus(it) }
        return BluetoothAudioState(
            device = device,
            name = device?.alias ?: device?.address,
            battery = device?.batteryLevel ?: -1,
            codec = status?.codecConfig,
            selectable = status?.codecsSelectableCapabilities ?: emptyList(),
            profile = device?.let { BluetoothProfiles.load(context, it) },
            encoding = SystemProperties.get("bluetooth.a2dp.encoding", ""),
            bitrateBps = SystemProperties.getLong("bluetooth.a2dp.bitrate", 0),
            sbcBitpool = SystemProperties.getInt("bluetooth.a2dp.sbc.bitpool", 0),
            sbcMode = SystemProperties.get("bluetooth.a2dp.sbc.mode", ""),
            peerSbcBitpool = SystemProperties.getInt("bluetooth.a2dp.peer.sbc_max_bitpool", 0),
            peerAacBitrateBps = SystemProperties.getLong("bluetooth.a2dp.peer.aac_bitrate", 0),
        )
    }

    /** Saves the profile for the active headphones and applies it to the stream. */
    fun update(context: Context, change: (BluetoothProfiles.Profile) -> Unit) {
        val proxy = a2dp ?: return
        val app = context.applicationContext
        scope.launch {
            val device = app.getSystemService(BluetoothManager::class.java)?.adapter
                ?.getActiveDevices(BluetoothProfile.A2DP)?.firstOrNull() ?: return@launch
            val profile = BluetoothProfiles.load(app, device)
            change(profile)
            BluetoothProfiles.save(app, device, profile)
            BluetoothProfiles.apply(proxy, device, profile)
        }
    }
}
