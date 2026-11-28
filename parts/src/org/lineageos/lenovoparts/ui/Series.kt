/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

/** Time-stamped samples, oldest first; times are elapsedRealtime in ms. */
class Samples(val times: LongArray, val values: FloatArray) {
    val size get() = values.size
    fun lastOrNull(): Float? = if (values.isEmpty()) null else values[values.size - 1]

    companion object {
        val EMPTY = Samples(LongArray(0), FloatArray(0))
    }
}

/** Fixed-size ring buffer of time-stamped samples. */
class Series(private val capacity: Int) {
    private val times = LongArray(capacity)
    private val values = FloatArray(capacity)
    private var head = 0
    private var count = 0

    @Synchronized
    fun add(time: Long, value: Float) {
        times[head] = time
        values[head] = value
        head = (head + 1) % capacity
        count = minOf(count + 1, capacity)
    }

    @Synchronized
    fun last(): Float? = if (count == 0) null else values[(head - 1 + capacity) % capacity]

    /** Samples not older than [since]. */
    @Synchronized
    fun snapshot(since: Long): Samples {
        var skip = 0
        while (skip < count && times[index(skip)] < since) skip++
        val n = count - skip
        return Samples(
            LongArray(n) { times[index(skip + it)] },
            FloatArray(n) { values[index(skip + it)] },
        )
    }

    private fun index(i: Int) = (head - count + i + capacity) % capacity
}
