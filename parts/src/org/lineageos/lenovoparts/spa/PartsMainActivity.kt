/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.spa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.LocalInPane
import org.lineageos.lenovoparts.ui.PartsScaffold

/**
 * Launcher entry. Wide screens show the section list and the open section
 * side by side; narrow screens use the regular single-pane navigation.
 */
class PartsMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.screenWidthDp < 600) {
            startActivity(Intent(this, PartsBrowseActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        val activity = this
        setContent {
            SettingsTheme {
                var selected by rememberSaveable { mutableIntStateOf(0) }
                val nav = object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {
                        val index = Sections.all.indexOfFirst { it.provider.name == route }
                        if (index >= 0) selected = index
                    }

                    override fun navigateBack() = activity.finish()
                }
                CompositionLocalProvider(LocalNavController provides nav) {
                    Row {
                        Box(Modifier.width(360.dp).fillMaxHeight()) {
                            PartsScaffold(stringResource(R.string.app_name)) {
                                Sections.all.forEachIndexed { i, section ->
                                    SectionRow(section, i == selected) { selected = i }
                                }
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            CompositionLocalProvider(LocalInPane provides true) {
                                key(selected) { Sections.all[selected].provider.Page(null) }
                            }
                        }
                    }
                }
            }
        }
    }
}
