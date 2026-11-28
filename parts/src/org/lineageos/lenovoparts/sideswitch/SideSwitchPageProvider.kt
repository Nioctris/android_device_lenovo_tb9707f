/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.sideswitch

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.ui.Footer
import org.lineageos.lenovoparts.R
import org.lineageos.lenovoparts.ui.PartsScaffold
import org.lineageos.lenovoparts.ui.rememberSystemSettings

object SideSwitchPageProvider : SettingsPageProvider {
    override val name = "SideSwitch"

    override fun getTitle(arguments: Bundle?): String =
        SpaEnvironmentFactory.instance.appContext.getString(R.string.side_switch_title)

    @Composable
    override fun Page(arguments: Bundle?) {
        val context = LocalContext.current
        val actions = listOf(SideSwitch.ACTION_NONE, SideSwitch.ACTION_ROTATION_LOCK,
            SideSwitch.ACTION_DND)
        val labels = listOf(R.string.side_switch_none, R.string.side_switch_rotation_lock,
            R.string.side_switch_dnd)
        val action by rememberSystemSettings(SideSwitch.SETTING) {
            SideSwitch.getAction(it.contentResolver)
        }
        val selected = remember { mutableIntStateOf(0) }
        selected.intValue = actions.indexOf(action).coerceAtLeast(0)

        PartsScaffold(stringResource(R.string.side_switch_title)) {
            RadioPreferences(object : ListPreferenceModel {
                override val title = context.getString(R.string.side_switch_action_title)
                override val options = labels.mapIndexed { i, id ->
                    ListPreferenceOption(id = i, text = context.getString(id))
                }
                override val selectedId: IntState = selected
                override val onIdSelected: (Int) -> Unit = {
                    SideSwitch.setAction(context.contentResolver, actions[it])
                }
            })
            Footer(stringResource(R.string.side_switch_hint))
        }
    }
}
