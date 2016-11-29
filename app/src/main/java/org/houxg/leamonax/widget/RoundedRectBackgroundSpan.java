package org.houxg.leamonax.widget;


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.utils.DisplayUtils;

public class RoundedRectBackgroundSpan extends ReplacementSpan {

    private int mColor;
    private float mRadius;
    private int padding = 10;
    private RectF mTempRect = new RectF();

    public RoundedRectBackgroundSpan(int color, float radius) {
        this(color, radius, 0);
    }

    public RoundedRectBackgroundSpan(int mColor, float mRadius, int padding) {
        this.mColor = mColor;
        this.mRadius = mRadius;
        this.padding = padding;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + padding * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        mTempRect.set(x, top, x + paint.measureText(text, start, end) + padding * 2, bottom);
        float offsetY = DisplayUtils.dp2px(Leamonax.getContext(), 2);
        mTempRect.offset(0, -offsetY);
        int textColor = paint.getColor();
        paint.setColor(mColor);
        canvas.drawRoundRect(mTempRect, mRadius, mRadius, paint);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, x + padding, y - offsetY, paint);
    }
}
