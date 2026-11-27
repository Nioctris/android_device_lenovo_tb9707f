/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import org.lineageos.lenovoparts.R;

/** CPU, GPU and memory load in percent over the last 10 minutes. */
public class LoadGraphView extends View {

    public static final int CPU = 0;
    public static final int GPU = 1;
    public static final int RAM = 2;
    private static final int SERIES = 3;

    static final int SAMPLES = 600; // 10 minutes at one sample per second
    private static final float[][] HISTORY = new float[SERIES][SAMPLES];
    private static int sCount;
    private static int sHead;

    private final Paint[] mLines = new Paint[SERIES];
    private final Paint mAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();

    public LoadGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(
                new int[] {android.R.attr.textColorSecondary});
        int secondary = a.getColor(0, 0xff888888);
        a.recycle();
        float dp = getResources().getDisplayMetrics().density;
        int[] colors = {context.getColor(R.color.graph_cpu), context.getColor(R.color.graph_gpu),
                context.getColor(R.color.graph_ram)};
        for (int i = 0; i < SERIES; i++) {
            mLines[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            mLines[i].setColor(colors[i]);
            mLines[i].setStyle(Paint.Style.STROKE);
            mLines[i].setStrokeWidth(2 * dp);
            mLines[i].setStrokeJoin(Paint.Join.ROUND);
        }
        mAxis.setColor(secondary);
        mAxis.setStrokeWidth(dp);
        mAxis.setAlpha(64);
        mText.setColor(secondary);
        mText.setTextSize(12 * dp);
    }

    /** Adds one sample; a negative value means unknown and repeats the last one. */
    public static void add(float cpu, float gpu, float ram) {
        float[] values = {cpu, gpu, ram};
        for (int i = 0; i < SERIES; i++) {
            float v = values[i];
            if (v < 0) {
                v = sCount > 0 ? HISTORY[i][(sHead - 1 + SAMPLES) % SAMPLES] : 0;
            }
            HISTORY[i][sHead] = Math.max(0, Math.min(100, v));
        }
        sHead = (sHead + 1) % SAMPLES;
        sCount = Math.min(sCount + 1, SAMPLES);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float pad = mText.getTextSize() * 1.4f;
        float top = pad, bottom = h - pad;
        for (int pct = 0; pct <= 100; pct += 50) {
            float y = bottom - pct / 100f * (bottom - top);
            canvas.drawLine(0, y, w, y, mAxis);
        }
        canvas.drawText("100%", 0, top - mText.getTextSize() * 0.3f, mText);
        canvas.drawText("−10 min", 0, h - mText.getTextSize() * 0.2f, mText);
        if (sCount < 2) {
            return;
        }
        float step = (float) w / (SAMPLES - 1);
        for (int s = SERIES - 1; s >= 0; s--) {
            mPath.reset();
            for (int i = 0; i < sCount; i++) {
                float x = w - (sCount - 1 - i) * step;
                float y = bottom - sample(s, i) / 100f * (bottom - top);
                if (i == 0) {
                    mPath.moveTo(x, y);
                } else {
                    mPath.lineTo(x, y);
                }
            }
            canvas.drawPath(mPath, mLines[s]);
        }
    }

    private static float sample(int series, int i) {
        return HISTORY[series][(sHead - sCount + i + SAMPLES) % SAMPLES];
    }
}
