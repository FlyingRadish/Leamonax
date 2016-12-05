package org.houxg.leamonax.widget;


import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerDecoration extends RecyclerView.ItemDecoration {
    private int mDividerSize;

    public DividerDecoration(int dividerSize) {
        mDividerSize = dividerSize;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int pos = parent.getChildAdapterPosition(view);
        int top = pos == 0 ? mDividerSize : 0;
        outRect.set(mDividerSize, top, mDividerSize, mDividerSize);
    }
}