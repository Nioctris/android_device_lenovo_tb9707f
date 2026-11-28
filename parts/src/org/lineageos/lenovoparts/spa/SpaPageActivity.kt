/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.spa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme

/**
 * Shows one section page on its own: the entries in the system Settings and
 * the quick settings tile long press open these.
 */
abstract class SpaPageActivity(private val provider: SettingsPageProvider) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activity = this
        val nav = object : NavControllerWrapper {
            override fun navigate(route: String, popUpCurrent: Boolean) {}

            override fun navigateBack() = activity.finish()
        }
        setContent {
            SettingsTheme {
                CompositionLocalProvider(LocalNavController provides nav) {
                    provider.Page(null)
                }
            }
        }
    }
}
