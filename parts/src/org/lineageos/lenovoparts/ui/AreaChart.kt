/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import kotlin.math.ceil
import org.lineageos.lenovoparts.R

/**
 * A time series drawn as a smoothed line with a gradient fill towards
 * [base]. [samples] is oldest first, one per second; [window] is how many
 * samples the full width stands for, so the line grows in from the right.
 * Values below [base] use [negativeColor] when given. Dragging across the
 * chart shows the value at that moment.
 */
@Composable
fun AreaChart(
    samples: FloatArray,
    window: Int,
    min: Float,
    max: Float,
    color: Color,
    gridLines: List<Pair<Float, String>>,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    base: Float = min,
    negativeColor: Color? = null,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val bubbleColor = MaterialTheme.colorScheme.inverseSurface
    val bubbleText = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.inverseOnSurface)
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val measurer = rememberTextMeasurer()
    val now = stringResource(R.string.chart_now)
    val minutes = stringResource(R.string.chart_minutes)
    val ago = stringResource(R.string.chart_ago)
    var touchX by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier.fillMaxWidth().height(height)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touchX = down.position.x
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        touchX = change.position.x
                        change.consume()
                    }
                    touchX = null
                }
            }
    ) {
        val labelWidth = gridLines.maxOfOrNull {
            measurer.measure(it.second, labelStyle).size.width
        } ?: 0
        val axisHeight = measurer.measure(now, labelStyle).size.height
        val right = size.width - labelWidth - 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - axisHeight - 10.dp.toPx()
        fun y(v: Float) = bottom - (v - min) / (max - min) * (bottom - top)
        val baseY = y(base)

        // Grid and scale.
        for ((value, label) in gridLines) {
            val gy = y(value)
            drawLine(
                if (value == base) labelColor.copy(alpha = 0.5f) else grid,
                Offset(0f, gy), Offset(right, gy), strokeWidth = 1.dp.toPx(),
            )
            val text = measurer.measure(label, labelStyle)
            drawText(text, topLeft = Offset(right + 10.dp.toPx(), gy - text.size.height / 2f))
        }
        // Time axis.
        val axisY = size.height - axisHeight
        val marks = listOf(0f to "−${window / 60} $minutes", 0.5f to "−${window / 120} $minutes",
            1f to now)
        for ((f, label) in marks) {
            val text = measurer.measure(label, labelStyle)
            val x = (f * right - f * text.size.width).coerceIn(0f, right - text.size.width)
            drawText(text, topLeft = Offset(x, axisY))
        }
        if (samples.size < 2) return@Canvas

        // Average into buckets a few pixels wide, newest at the right edge.
        val buckets = (right / 3.dp.toPx()).toInt().coerceAtLeast(2)
        val perBucket = window.toFloat() / buckets
        val first = window - samples.size
        val xs = ArrayList<Float>(buckets)
        val ys = ArrayList<Float>(buckets)
        for (b in 0 until buckets) {
            val from = maxOf(ceil(b * perBucket).toInt(), first)
            val to = ceil((b + 1) * perBucket).toInt()
            if (to <= from) continue
            var sum = 0f
            for (slot in from until to) sum += samples[slot - first]
            xs += (b + 0.5f) / buckets * right
            ys += y(sum / (to - from)).coerceIn(top, bottom)
        }
        if (xs.size < 2) return@Canvas
        // The newest sample sits on the right edge.
        xs[xs.size - 1] = right
        ys[ys.size - 1] = y(samples.last()).coerceIn(top, bottom)

        // Catmull-Rom through the points, as cubic Beziers.
        val line = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 0 until xs.size - 1) {
                val i0 = maxOf(i - 1, 0)
                val i3 = minOf(i + 2, xs.size - 1)
                cubicTo(
                    xs[i] + (xs[i + 1] - xs[i0]) / 6f,
                    (ys[i] + (ys[i + 1] - ys[i0]) / 6f).coerceIn(top, bottom),
                    xs[i + 1] - (xs[i3] - xs[i]) / 6f,
                    (ys[i + 1] - (ys[i3] - ys[i]) / 6f).coerceIn(top, bottom),
                    xs[i + 1], ys[i + 1],
                )
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(xs.last(), baseY)
            lineTo(xs.first(), baseY)
            close()
        }
        val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Above the base line in [color], below it in [negativeColor].
        fun drawSide(c: Color, clipTop: Float, clipBottom: Float, towardsTop: Boolean) {
            clipRect(0f, clipTop, size.width, clipBottom) {
                val edge = if (towardsTop) top else bottom
                drawPath(area, Brush.verticalGradient(
                    listOf(c.copy(alpha = 0.42f), c.copy(alpha = 0.03f)),
                    startY = edge, endY = baseY))
                drawPath(line, c, style = stroke)
            }
        }
        val below = negativeColor ?: color
        drawSide(color, 0f, baseY, towardsTop = true)
        if (baseY < bottom) drawSide(below, baseY, size.height, towardsTop = false)

        fun colorAt(v: Float) = if (v < base) below else color

        // Current value: a dot with a soft halo on the right edge.
        val lastY = ys.last()
        val lastColor = colorAt(samples.last())
        drawCircle(lastColor.copy(alpha = 0.25f), 8.dp.toPx(), Offset(right, lastY))
        drawCircle(cardColor, 4.5.dp.toPx(), Offset(right, lastY))
        drawCircle(lastColor, 3.dp.toPx(), Offset(right, lastY))

        // Scrubbing: the value under the finger.
        val tx = touchX
        if (tx != null) {
            val x = tx.coerceIn(xs.first(), right)
            val slot = (x / right * window).toInt().coerceIn(first, window - 1)
            val value = samples[slot - first]
            val secondsAgo = window - 1 - slot
            val py = y(value).coerceIn(top, bottom)
            drawLine(labelColor.copy(alpha = 0.6f), Offset(x, top), Offset(x, bottom),
                strokeWidth = 1.dp.toPx())
            drawCircle(cardColor, 5.dp.toPx(), Offset(x, py))
            drawCircle(colorAt(value), 3.5.dp.toPx(), Offset(x, py))
            val whenText = if (secondsAgo < 60) now
            else "${secondsAgo / 60} $minutes $ago"
            val text = measurer.measure("${format(value)} · $whenText", bubbleText)
            val pad = 8.dp.toPx()
            val w = text.size.width + 2 * pad
            val h = text.size.height + pad
            val bx = (x - w / 2).coerceIn(0f, (right - w).coerceAtLeast(0f))
            val by = top
            drawRoundRect(bubbleColor, Offset(bx, by), Size(w, h), CornerRadius(h / 2))
            drawText(text, topLeft = Offset(bx + pad, by + pad / 2))
        }
    }
}

/** A titled card with a current value on the right and a chart below. */
@Composable
fun ChartCard(
    title: String,
    value: String?,
    modifier: Modifier = Modifier,
    chart: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (value != null) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            chart()
        }
    }
}
