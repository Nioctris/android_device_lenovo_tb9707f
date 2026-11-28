/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.theme.settingsBackground

/** True when a page is shown next to the section list (two panes). */
val LocalInPane = compositionLocalOf { false }

/**
 * Page frame: a collapsing title bar and a scrolling column. The back button
 * is hidden when the page sits in the detail pane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsScaffold(title: String, content: @Composable () -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val inPane = LocalInPane.current
    val nav = LocalNavController.current
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.settingsBackground,
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (!inPane) {
                        IconButton(onClick = { nav.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.settingsBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.settingsBackground,
                ),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            content()
            Spacer(Modifier.height(padding.calculateBottomPadding() + 16.dp))
        }
    }
}
