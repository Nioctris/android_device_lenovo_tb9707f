/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts

import android.app.Application
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import org.lineageos.lenovoparts.spa.PartsSpaEnvironment

class LenovoPartsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SpaEnvironmentFactory.reset(PartsSpaEnvironment(this))
        Monitor.start(this)
    }
}
