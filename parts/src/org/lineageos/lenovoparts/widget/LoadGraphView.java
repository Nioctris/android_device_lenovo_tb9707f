/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import org.lineageos.lenovoparts.R;

/**
 * One load metric in percent over the last 10 minutes: the name and the
 * current value on top, a smoothed area chart below. The CPU, GPU and memory
 * rows share one history.
 */
public class LoadGraphView extends View {

    public static final int CPU = 0;
    public static final int GPU = 1;
    public static final int RAM = 2;
    private static final int SERIES = 3;

    static final int SAMPLES = 600; // 10 minutes at one sample per second
    private static final float[][] HISTORY = new float[SERIES][SAMPLES];
    private static int sCount;
    private static int sHead;

    private final GraphPainter mPainter;
    private final Paint mGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mValue = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mScale = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mDp;
    private int mSeries = CPU;
    private String mName = "";

    private final GraphPainter.Series mData = new GraphPainter.Series() {
        @Override
        public int count() {
            return sCount;
        }

        @Override
        public float get(int i) {
            return HISTORY[mSeries][(sHead - sCount + i + SAMPLES) % SAMPLES];
        }
    };

    public LoadGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDp = getResources().getDisplayMetrics().density;
        mPainter = new GraphPainter(mDp);
        TypedArray a = context.obtainStyledAttributes(attrs, new int[] {android.R.attr.tag});
        String tag = a.getString(0);
        a.recycle();
        if ("gpu".equals(tag)) {
            mSeries = GPU;
        } else if ("ram".equals(tag)) {
            mSeries = RAM;
        }
        int[] colors = {R.color.graph_cpu, R.color.graph_gpu, R.color.graph_ram};
        int[] names = {R.string.load_cpu, R.string.load_gpu, R.string.load_ram};
        mPainter.setColor(context.getColor(colors[mSeries]));
        mName = context.getString(names[mSeries]);

        int label = context.getColor(R.color.card_label);
        mGrid.setColor(GraphPainter.withAlpha(label, 0x40));
        mGrid.setStrokeWidth(mDp);
        mGrid.setPathEffect(new DashPathEffect(new float[] {4 * mDp, 4 * mDp}, 0));
        mLabel.setColor(label);
        mLabel.setTextSize(13 * mDp);
        mValue.setColor(context.getColor(R.color.card_value));
        mValue.setTextSize(20 * mDp);
        mValue.setTypeface(Typeface.create(Typeface.DEFAULT, 500, false));
        mValue.setTextAlign(Paint.Align.RIGHT);
        mScale.setColor(GraphPainter.withAlpha(label, 0xb0));
        mScale.setTextSize(10 * mDp);
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
        float header = mValue.getTextSize() * 1.25f;
        canvas.drawText(mName, 0, header * 0.72f, mLabel);
        String value = sCount > 0 ? Math.round(mData.get(sCount - 1)) + "%" : "—";
        canvas.drawText(value, w, header * 0.78f, mValue);

        float top = header + 6 * mDp, bottom = h - mDp;
        float scaleWidth = mScale.measureText("100%") + 6 * mDp;
        float right = w - scaleWidth;
        for (int pct = 0; pct <= 100; pct += 50) {
            float y = bottom - pct / 100f * (bottom - top);
            canvas.drawLine(0, y, right, y, mGrid);
            canvas.drawText(pct + "%", right + 6 * mDp, y + mScale.getTextSize() * 0.35f,
                    mScale);
        }
        mPainter.draw(canvas, mData, SAMPLES, 0, right, top, bottom, 0, 100, 0);
    }
}
