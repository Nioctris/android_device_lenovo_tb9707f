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

/** Battery power over the last minutes: charging above the axis, discharge below. */
public class PowerGraphView extends View {

    static final int SAMPLES = 600; // 10 minutes at one sample per second

    private static final float[] HISTORY = new float[SAMPLES];
    private static int sCount;
    private static int sHead;

    private final Paint mLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();

    public PowerGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(new int[] {
                android.R.attr.colorAccent, android.R.attr.textColorSecondary});
        int accent = a.getColor(0, 0xff4caf50);
        int secondary = a.getColor(1, 0xff888888);
        a.recycle();
        float dp = getResources().getDisplayMetrics().density;
        mLine.setColor(accent);
        mLine.setStyle(Paint.Style.STROKE);
        mLine.setStrokeWidth(2 * dp);
        mAxis.setColor(secondary);
        mAxis.setStrokeWidth(dp);
        mAxis.setAlpha(96);
        mText.setColor(secondary);
        mText.setTextSize(12 * dp);
    }

    public static void add(float watts) {
        HISTORY[sHead] = watts;
        sHead = (sHead + 1) % SAMPLES;
        sCount = Math.min(sCount + 1, SAMPLES);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float pad = mText.getTextSize() * 1.4f;
        float max = 5, min = 0;
        for (int i = 0; i < sCount; i++) {
            float v = sample(i);
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        max = (float) Math.ceil(max / 5) * 5;
        min = (float) Math.floor(min / 5) * 5;
        float span = max - min;
        float top = pad, bottom = h - pad;
        float zero = bottom - (0 - min) / span * (bottom - top);

        canvas.drawLine(0, zero, w, zero, mAxis);
        canvas.drawText(Math.round(max) + " W", 0, top - mText.getTextSize() * 0.3f, mText);
        if (min < 0) {
            String low = Math.round(min) + " W";
            canvas.drawText(low, w - mText.measureText(low), h - mText.getTextSize() * 0.2f, mText);
        }
        canvas.drawText("−10 min", 0, h - mText.getTextSize() * 0.2f, mText);

        if (sCount < 2) {
            return;
        }
        mPath.reset();
        float step = (float) w / (SAMPLES - 1);
        for (int i = 0; i < sCount; i++) {
            float x = w - (sCount - 1 - i) * step;
            float y = bottom - (sample(i) - min) / span * (bottom - top);
            if (i == 0) {
                mPath.moveTo(x, y);
            } else {
                mPath.lineTo(x, y);
            }
        }
        canvas.drawPath(mPath, mLine);
    }

    /** i-th oldest sample. */
    private static float sample(int i) {
        return HISTORY[(sHead - sCount + i + SAMPLES) % SAMPLES];
    }
}
