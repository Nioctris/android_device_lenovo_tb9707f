/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/**
 * Draws a time series as a smoothed line with a gradient fill towards a
 * base line. The series is averaged into buckets a few pixels wide, so 600
 * one-second samples stay smooth at any width.
 */
final class GraphPainter {

    interface Series {
        int count();

        /** i-th oldest sample. */
        float get(int i);
    }

    private static final float BUCKET_PX = 4f;

    private final Paint mLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final Path mArea = new Path();
    private float[] mX = new float[0];
    private float[] mY = new float[0];
    private int mColor;

    GraphPainter(float density) {
        mLine.setStyle(Paint.Style.STROKE);
        mLine.setStrokeWidth(2.5f * density);
        mLine.setStrokeCap(Paint.Cap.ROUND);
        mLine.setStrokeJoin(Paint.Join.ROUND);
        mFill.setStyle(Paint.Style.FILL);
    }

    void setColor(int color) {
        mColor = color;
        mLine.setColor(color);
    }

    /**
     * @param window number of samples the full width stands for
     * @param top y of {@code max}; @param bottom y of {@code min}
     * @param base value the fill runs to
     */
    void draw(Canvas canvas, Series series, int window, float left, float right, float top,
            float bottom, float min, float max, float base) {
        int count = series.count();
        if (count < 2 || right <= left) {
            return;
        }
        float width = right - left;
        int buckets = Math.max(2, (int) (width / BUCKET_PX));
        float perBucket = (float) window / buckets;
        float span = max - min;

        // Average the samples into buckets, newest at the right edge.
        int n = 0;
        if (mX.length < buckets + 1) {
            mX = new float[buckets + 1];
            mY = new float[buckets + 1];
        }
        int first = window - count; // window slots before the first sample
        for (int b = 0; b < buckets; b++) {
            int from = (int) Math.ceil(b * perBucket);
            int to = (int) Math.ceil((b + 1) * perBucket);
            from = Math.max(from, first);
            if (to <= from) {
                continue;
            }
            float sum = 0;
            for (int slot = from; slot < to; slot++) {
                sum += series.get(slot - first);
            }
            float v = sum / (to - from);
            mX[n] = left + (b + 0.5f) / buckets * width;
            mY[n] = bottom - (v - min) / span * (bottom - top);
            n++;
        }
        if (n < 2) {
            return;
        }

        // Catmull-Rom spline through the bucket points, as cubic Beziers.
        mPath.reset();
        mPath.moveTo(mX[0], mY[0]);
        for (int i = 0; i < n - 1; i++) {
            float x0 = mX[Math.max(i - 1, 0)], y0 = mY[Math.max(i - 1, 0)];
            float x1 = mX[i], y1 = mY[i];
            float x2 = mX[i + 1], y2 = mY[i + 1];
            float x3 = mX[Math.min(i + 2, n - 1)], y3 = mY[Math.min(i + 2, n - 1)];
            float c1x = x1 + (x2 - x0) / 6f;
            float c1y = clamp(y1 + (y2 - y0) / 6f, top, bottom);
            float c2x = x2 - (x3 - x1) / 6f;
            float c2y = clamp(y2 - (y3 - y1) / 6f, top, bottom);
            mPath.cubicTo(c1x, c1y, c2x, c2y, x2, y2);
        }

        float baseY = bottom - (base - min) / span * (bottom - top);
        mArea.set(mPath);
        mArea.lineTo(mX[n - 1], baseY);
        mArea.lineTo(mX[0], baseY);
        mArea.close();
        // Strongest at the far edge(s), fading out towards the base line.
        float basePos = clamp((baseY - top) / (bottom - top), 0f, 1f);
        int strong = withAlpha(mColor, 0x60), faint = withAlpha(mColor, 0x06);
        if (basePos > 0.98f) {
            mFill.setShader(new LinearGradient(0, top, 0, bottom, strong, faint,
                    Shader.TileMode.CLAMP));
        } else {
            mFill.setShader(new LinearGradient(0, top, 0, bottom,
                    new int[] {strong, faint, strong}, new float[] {0f, basePos, 1f},
                    Shader.TileMode.CLAMP));
        }
        canvas.drawPath(mArea, mFill);
        canvas.drawPath(mPath, mLine);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }
}
