/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.ui

/** Fixed-size ring buffer of samples, oldest first in [snapshot]. */
class History(val capacity: Int = 600) {
    private val data = FloatArray(capacity)
    private var head = 0
    private var count = 0

    @Synchronized
    fun add(value: Float) {
        data[head] = value
        head = (head + 1) % capacity
        count = minOf(count + 1, capacity)
    }

    @Synchronized
    fun last(): Float? = if (count == 0) null else data[(head - 1 + capacity) % capacity]

    @Synchronized
    fun snapshot(): FloatArray =
        FloatArray(count) { data[(head - count + it + capacity) % capacity] }
}
