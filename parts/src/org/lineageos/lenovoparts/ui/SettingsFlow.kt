/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Emits [read] now and again whenever one of the System settings [names] changes. */
fun <T> Context.systemSettingsFlow(vararg names: String, read: () -> T): Flow<T> =
    callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(read())
            }
        }
        for (name in names) {
            contentResolver.registerContentObserver(
                Settings.System.getUriFor(name), false, observer)
        }
        trySend(read())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }.conflate()

/** Compose state of System settings, refreshed when they change. */
@Composable
fun <T> rememberSystemSettings(vararg names: String, read: (Context) -> T): State<T> {
    val context = LocalContext.current
    return produceState(read(context), *names) {
        context.systemSettingsFlow(*names) { read(context) }.collect { value = it }
    }
}
