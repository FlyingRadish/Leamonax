package org.houxg.leamonax.widget;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.R;

public class AlphabetDrawable extends Drawable {

    private Paint mPaint;
    private int mBackgroundColor = Color.BLUE;
    private String mAlphabet;

    public AlphabetDrawable() {
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mBackgroundColor = Leamonax.getContext().getResources().getColor(R.color.colorAccent);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bound = getBounds();
        float rad = Math.min(bound.width(), bound.height()) / 2.0f;
        mPaint.setColor(mBackgroundColor);
        canvas.drawCircle(bound.centerX(), bound.centerY(), rad, mPaint);
        if (!TextUtils.isEmpty(mAlphabet)) {
            mPaint.setColor(Color.WHITE);
            float textSize = bound.height() * 0.6f;
            mPaint.setTextSize(textSize);
            Paint.FontMetrics metrics = mPaint.getFontMetrics();
            float baseline = (bound.bottom + bound.top - metrics.bottom - metrics.top) / 2;
            canvas.drawText(mAlphabet, bound.centerX(), baseline, mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    public void setAlphabet(String alphabet) {
        if (!TextUtils.isEmpty(alphabet)) {
            mAlphabet = alphabet.substring(0, 1);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
