package org.houxg.leamonax.widget;


import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v13.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DashDividerDecoration extends RecyclerView.ItemDecoration {

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path mPath;

    public DashDividerDecoration(int color, int dashGap, int dashWidth, int height) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(height);
        mPaint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashGap}, 0));
        mPath = new Path();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();
        final int strokeWidth = (int) mPaint.getStrokeWidth();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin +
                    Math.round(ViewCompat.getTranslationY(child));
            int offsetY = top + strokeWidth / 2;

            mPath.reset();
            mPath.moveTo(left, offsetY);
            mPath.lineTo(right, offsetY);
            c.drawPath(mPath, mPaint);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(0, 0, 0, (int) mPaint.getStrokeWidth());
    }
}