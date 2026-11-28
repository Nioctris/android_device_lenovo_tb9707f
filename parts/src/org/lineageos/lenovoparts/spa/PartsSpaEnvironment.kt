/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.spa

import android.content.Context
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spa.framework.common.createSettingsPage
import org.lineageos.lenovoparts.charging.PowerPageProvider

class PartsSpaEnvironment(context: Context) : SpaEnvironment(context) {
    override val pageProviderRepository = lazy {
        SettingsPageProviderRepository(
            allPageProviders = listOf(HomePageProvider) + Sections.all.map { it.provider },
            rootPages = listOf(HomePageProvider.createSettingsPage()),
        )
    }

    override val browseActivityClass = PartsBrowseActivity::class.java

    override val isSpaExpressiveEnabled = true
}
