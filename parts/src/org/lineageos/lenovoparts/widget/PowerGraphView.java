/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.lineageos.lenovoparts.R;

/**
 * Battery power over the last 10 minutes as a smoothed area chart around
 * zero: charging above the line, discharging below.
 */
public class PowerGraphView extends View {

    static final int SAMPLES = 600; // 10 minutes at one sample per second
    private static final float[] HISTORY = new float[SAMPLES];
    private static int sCount;
    private static int sHead;

    private final GraphPainter mPainter;
    private final Paint mGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mZero = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mScale = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mDp;

    private final GraphPainter.Series mData = new GraphPainter.Series() {
        @Override
        public int count() {
            return sCount;
        }

        @Override
        public float get(int i) {
            return HISTORY[(sHead - sCount + i + SAMPLES) % SAMPLES];
        }
    };

    public PowerGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDp = getResources().getDisplayMetrics().density;
        mPainter = new GraphPainter(mDp);
        mPainter.setColor(context.getColor(R.color.graph_power));
        int label = context.getColor(R.color.card_label);
        mGrid.setColor(GraphPainter.withAlpha(label, 0x40));
        mGrid.setStrokeWidth(mDp);
        mGrid.setPathEffect(new DashPathEffect(new float[] {4 * mDp, 4 * mDp}, 0));
        mZero.setColor(GraphPainter.withAlpha(label, 0x80));
        mZero.setStrokeWidth(mDp);
        mScale.setColor(GraphPainter.withAlpha(label, 0xb0));
        mScale.setTextSize(10 * mDp);
    }

    public static void add(float watts) {
        HISTORY[sHead] = watts;
        sHead = (sHead + 1) % SAMPLES;
        sCount = Math.min(sCount + 1, SAMPLES);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float max = 5, min = -5;
        for (int i = 0; i < sCount; i++) {
            float v = mData.get(i);
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        max = (float) Math.ceil(max / 5) * 5;
        min = (float) Math.floor(min / 5) * 5;
        float top = mScale.getTextSize(), bottom = h - mScale.getTextSize();
        float right = w - mScale.measureText("−00 W") - 6 * mDp;

        float[] marks = {max, 0, min};
        for (float m : marks) {
            float y = bottom - (m - min) / (max - min) * (bottom - top);
            canvas.drawLine(0, y, right, y, m == 0 ? mZero : mGrid);
            String text = (m > 0 ? "+" : m < 0 ? "−" : "") + Math.round(Math.abs(m)) + " W";
            canvas.drawText(text, right + 6 * mDp, y + mScale.getTextSize() * 0.35f, mScale);
        }
        mPainter.draw(canvas, mData, SAMPLES, 0, right, top, bottom, min, max, 0);
    }
}
