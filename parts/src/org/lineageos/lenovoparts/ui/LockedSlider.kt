/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A whole-number slider whose range above [limit] is hatched and locked
 * while [locked]. The value label follows the thumb; [onCommit] runs on
 * release. A tap on the locked part calls [onUnlockRequest].
 */
@Composable
fun LockedSlider(
    title: String,
    value: Int,
    range: IntRange,
    limit: Int,
    locked: Boolean,
    enabled: Boolean,
    format: (Int) -> String,
    onCommit: (Int) -> Unit,
    onUnlockRequest: () -> Unit,
) {
    var drag by remember(value, locked) { mutableFloatStateOf(value.toFloat()) }
    val haptic = LocalHapticFeedback.current
    val hatch = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val span = (range.last - range.first).toFloat()
    // Same surface as the SPA items around it in a category.
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            Spacer(Modifier.weight(1f))
            Text(format(drag.toInt()), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Box {
            Slider(
                value = drag,
                onValueChange = {
                    // Whole watts; no step ticks, they clutter a 35-step track.
                    val v = Math.round(it).toFloat()
                        .coerceAtMost(if (locked) limit.toFloat() else range.last.toFloat())
                    if (locked && it > limit && drag < limit) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    drag = v
                },
                onValueChangeFinished = { onCommit(drag.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                enabled = enabled,
            )
            if (locked) {
                // The locked part of the track: hatched, with a lock; a tap asks to unlock.
                val fraction = (limit - range.first) / span
                Row(Modifier.matchParentSize()) {
                    Spacer(Modifier.weight(fraction.coerceAtLeast(0.01f)))
                    Box(
                        Modifier.weight((1f - fraction).coerceAtLeast(0.01f)).height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = enabled,
                                onClick = onUnlockRequest,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(
                            Modifier.fillMaxWidth().padding(end = 2.dp).height(16.dp)
                                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        ) {
                            val step = 8.dp.toPx()
                            var x = -size.height
                            while (x < size.width) {
                                drawLine(hatch, Offset(x, size.height), Offset(x + size.height, 0f),
                                    strokeWidth = 1.5.dp.toPx())
                                x += step
                            }
                        }
                        Icon(Icons.Outlined.Lock, contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(bottom = 0.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row {
            Text(format(range.first), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(format(range.last), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
