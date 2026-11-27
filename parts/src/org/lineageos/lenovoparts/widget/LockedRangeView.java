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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.slider.Slider;

import org.lineageos.lenovoparts.R;

/**
 * Drawn over a {@link Slider}: hatches the part of the track above a limit
 * and shows a lock on it. A tap on that part calls the unlock listener.
 */
public class LockedRangeView extends View {

    private final Paint mHatch = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClip = new Path();
    private final RectF mZone = new RectF();
    private final Drawable mLock;

    private Slider mSlider;
    private float mFrom;
    private boolean mLocked;
    private Runnable mOnUnlockRequest;
    private boolean mPressed;

    public LockedRangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(
                new int[] { com.google.android.material.R.attr.colorOnSurfaceVariant });
        int color = a.getColor(0, 0xff808080);
        a.recycle();
        float density = getResources().getDisplayMetrics().density;
        mHatch.setColor(color);
        mHatch.setAlpha(110);
        mHatch.setStrokeWidth(1.5f * density);
        mEdge.setColor(color);
        mEdge.setStrokeWidth(2f * density);
        mEdge.setStrokeCap(Paint.Cap.ROUND);
        mLock = context.getDrawable(R.drawable.ic_lock).mutate();
        mLock.setTint(color);
    }

    /** Locks the part of {@code slider} above {@code from}. */
    public void attach(Slider slider, float from, Runnable onUnlockRequest) {
        mSlider = slider;
        mFrom = from;
        mOnUnlockRequest = onUnlockRequest;
        invalidate();
    }

    public void setLocked(boolean locked) {
        if (mLocked != locked) {
            mLocked = locked;
            invalidate();
        }
    }

    private boolean computeZone() {
        if (mSlider == null || !mLocked || mSlider.getWidth() == 0) {
            return false;
        }
        float span = mSlider.getValueTo() - mSlider.getValueFrom();
        float left = mSlider.getTrackSidePadding()
                + (mFrom - mSlider.getValueFrom()) / span * mSlider.getTrackWidth();
        float right = mSlider.getTrackSidePadding() + mSlider.getTrackWidth();
        float center = mSlider.getTop() + mSlider.getHeight() / 2f;
        float half = mSlider.getTrackHeight() / 2f;
        mZone.set(left, center - half, right, center + half);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!computeZone()) {
            return;
        }
        float corner = Math.min(mSlider.getTrackCornerSize(), mZone.height() / 2f);
        mClip.reset();
        mClip.addRoundRect(mZone, new float[] { 0, 0, corner, corner, corner, corner, 0, 0 },
                Path.Direction.CW);
        canvas.save();
        canvas.clipPath(mClip);
        float step = mZone.height() / 2f;
        for (float x = mZone.left - mZone.height(); x < mZone.right; x += step) {
            canvas.drawLine(x, mZone.bottom, x + mZone.height(), mZone.top, mHatch);
        }
        canvas.restore();
        canvas.drawLine(mZone.left, mZone.top - step / 2f, mZone.left,
                mZone.bottom + step / 2f, mEdge);

        int size = (int) Math.min(mZone.height() * 1.4f,
                24 * getResources().getDisplayMetrics().density);
        int cx = (int) mZone.centerX();
        int top = (int) (mZone.top - size - step / 2f);
        mLock.setBounds(cx - size / 2, top, cx + size / 2, top + size);
        mLock.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !computeZone()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPressed = event.getX() >= mZone.left;
                return mPressed;
            case MotionEvent.ACTION_UP:
                if (mPressed && event.getX() >= mZone.left && mOnUnlockRequest != null) {
                    performClick();
                    mOnUnlockRequest.run();
                }
                mPressed = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mPressed = false;
                return true;
            default:
                return mPressed;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
