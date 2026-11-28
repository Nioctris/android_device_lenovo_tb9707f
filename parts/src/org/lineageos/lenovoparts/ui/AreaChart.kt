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
import org.lineageos.lenovoparts.Monitor
import org.lineageos.lenovoparts.R

/**
 * A time series drawn as a smoothed line with a gradient fill towards
 * [base]. The right edge is [now] and the full width spans [window] ms.
 * Samples are averaged into buckets fixed in time, so a new sample only
 * changes the newest point and the rest of the line just moves left.
 * Gaps in the samples (the device was asleep) break the line. Values
 * below [base] use [negativeColor] when given. Dragging across the chart
 * shows the value at that moment.
 */
@Composable
fun AreaChart(
    samples: Samples,
    now: Long,
    min: Float,
    max: Float,
    color: Color,
    gridLines: List<Pair<Float, String>>,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
    window: Long = Monitor.WINDOW_MS,
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
    val nowLabel = stringResource(R.string.chart_now)
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
        val axisHeight = measurer.measure(nowLabel, labelStyle).size.height
        val right = size.width - labelWidth - 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - axisHeight - 10.dp.toPx()
        fun y(v: Float) = (bottom - (v - min) / (max - min) * (bottom - top)).coerceIn(top, bottom)
        val baseY = y(base)
        val pxPerMs = right / window
        fun x(t: Long) = right - (now - t) * pxPerMs

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
        val windowMin = (window / 60000).toInt()
        val marks = listOf(0f to "−$windowMin $minutes", 0.5f to "−${windowMin / 2} $minutes",
            1f to nowLabel)
        for ((f, label) in marks) {
            val text = measurer.measure(label, labelStyle)
            val tx = (f * right - f * text.size.width).coerceIn(0f, right - text.size.width)
            drawText(text, topLeft = Offset(tx, axisY))
        }

        // Average into buckets about 2 px wide, aligned to whole multiples
        // of the bucket length so they stay the same between frames.
        val bucketMs = (ceil(2.dp.toPx() / pxPerMs / Monitor.PERIOD_MS).toLong()
            .coerceAtLeast(1)) * Monitor.PERIOD_MS
        val gapMs = 3 * Monitor.PERIOD_MS + bucketMs
        val from = now - window - bucketMs
        val ts = ArrayList<Long>()
        val vs = ArrayList<Float>()
        val segStart = ArrayList<Int>()
        var i = 0
        val times = samples.times
        val values = samples.values
        while (i < samples.size && times[i] < from) i++
        var lastTime = Long.MIN_VALUE
        while (i < samples.size) {
            val k = times[i] / bucketMs
            var sumT = 0.0
            var sumV = 0.0
            var n = 0
            val firstTime = times[i]
            while (i < samples.size && times[i] / bucketMs == k) {
                sumT += times[i]
                sumV += values[i]
                n++
                lastTime = times[i]
                i++
            }
            if (ts.isEmpty() || firstTime - prevTime(times, i - n) > gapMs) {
                segStart += ts.size
            }
            ts += (sumT / n).toLong()
            vs += (sumV / n).toFloat()
        }
        if (ts.isEmpty()) return@Canvas
        val xs = FloatArray(ts.size) { x(ts[it]) }
        val ys = FloatArray(ts.size) { y(vs[it]) }

        val line = Path()
        val area = Path()
        for (s in segStart.indices) {
            val a = segStart[s]
            val b = if (s + 1 < segStart.size) segStart[s + 1] else ts.size
            val seg = Path()
            monotone(seg, xs, ys, a, b)
            line.addPath(seg)
            area.addPath(seg)
            area.lineTo(xs[b - 1], baseY)
            area.lineTo(xs[a], baseY)
            area.close()
        }
        val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Above the base line in [color], below it in [negativeColor].
        fun drawSide(c: Color, clipTop: Float, clipBottom: Float, towardsTop: Boolean) {
            clipRect(0f, clipTop, right + stroke.width, clipBottom) {
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

        // Newest value: a dot with a soft halo, only while the line is live.
        val n = ts.size - 1
        if (now - lastTime <= gapMs) {
            val c = colorAt(vs[n])
            drawCircle(c.copy(alpha = 0.25f), 8.dp.toPx(), Offset(xs[n], ys[n]))
            drawCircle(cardColor, 4.5.dp.toPx(), Offset(xs[n], ys[n]))
            drawCircle(c, 3.dp.toPx(), Offset(xs[n], ys[n]))
        }

        // Scrubbing: the point under the finger.
        val tx = touchX ?: return@Canvas
        val px = tx.coerceIn(0f, right)
        var nearest = xs.binarySearch(px).let { if (it < 0) -it - 1 else it }
        if (nearest >= xs.size || (nearest > 0 && px - xs[nearest - 1] < xs[nearest] - px)) {
            nearest--
        }
        if (nearest < 0 || kotlin.math.abs(xs[nearest] - px) > gapMs * pxPerMs) return@Canvas
        val value = vs[nearest]
        val qx = xs[nearest]
        val qy = ys[nearest]
        drawLine(labelColor.copy(alpha = 0.6f), Offset(qx, top), Offset(qx, bottom),
            strokeWidth = 1.dp.toPx())
        drawCircle(cardColor, 5.dp.toPx(), Offset(qx, qy))
        drawCircle(colorAt(value), 3.5.dp.toPx(), Offset(qx, qy))
        val minutesAgo = ((now - ts[nearest]) / 60000).toInt()
        val whenText = if (minutesAgo < 1) nowLabel else "$minutesAgo $minutes $ago"
        val text = measurer.measure("${format(value)} · $whenText", bubbleText)
        val pad = 8.dp.toPx()
        val w = text.size.width + 2 * pad
        val h = text.size.height + pad
        val bx = (qx - w / 2).coerceIn(0f, (right - w).coerceAtLeast(0f))
        drawRoundRect(bubbleColor, Offset(bx, top), Size(w, h), CornerRadius(h / 2))
        drawText(text, topLeft = Offset(bx + pad, top + pad / 2))
    }
}

/** Time of the sample before index [i], or a very old time if there is none. */
private fun prevTime(times: LongArray, i: Int): Long =
    if (i > 0) times[i - 1] else Long.MIN_VALUE / 2

/**
 * Monotone cubic (Fritsch-Carlson) through points [a] until [b]: it never
 * overshoots past its neighbours, so peaks and flat runs stay as measured.
 */
private fun monotone(path: Path, xs: FloatArray, ys: FloatArray, a: Int, b: Int) {
    path.moveTo(xs[a], ys[a])
    val n = b - a
    if (n == 1) {
        path.lineTo(xs[a] + 1f, ys[a])
        return
    }
    val d = FloatArray(n - 1) { (ys[a + it + 1] - ys[a + it]) / (xs[a + it + 1] - xs[a + it]) }
    val m = FloatArray(n)
    m[0] = d[0]
    m[n - 1] = d[n - 2]
    for (k in 1 until n - 1) {
        m[k] = if (d[k - 1] * d[k] <= 0f) 0f else (d[k - 1] + d[k]) / 2f
    }
    for (k in 0 until n - 1) {
        if (d[k] == 0f) {
            m[k] = 0f
            m[k + 1] = 0f
            continue
        }
        val p = m[k] / d[k]
        val q = m[k + 1] / d[k]
        val s = p * p + q * q
        if (s > 9f) {
            val t = 3f / kotlin.math.sqrt(s)
            m[k] = t * p * d[k]
            m[k + 1] = t * q * d[k]
        }
    }
    for (k in 0 until n - 1) {
        val x0 = xs[a + k]
        val x1 = xs[a + k + 1]
        val h = (x1 - x0) / 3f
        path.cubicTo(x0 + h, ys[a + k] + m[k] * h, x1 - h, ys[a + k + 1] - m[k + 1] * h,
            x1, ys[a + k + 1])
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
