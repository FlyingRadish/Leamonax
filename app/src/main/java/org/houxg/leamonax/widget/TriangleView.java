package org.houxg.leamonax.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import org.houxg.leamonax.R;

public class TriangleView extends ImageView {
    private static final int DURATION = 200;
    private boolean mIsChecked = false;
    OnToggleListener mListener;

    public TriangleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImageResource(R.drawable.ic_triangle);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsChecked = !mIsChecked;
                if (mIsChecked) {
                    animate().rotation(-180)
                            .setDuration(DURATION)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                } else {
                    animate().rotation(0)
                            .setDuration(DURATION)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }
                if (mListener != null) {
                    mListener.onToggle(mIsChecked);
                }
            }
        });
    }

    public void setOnToggleListener(OnToggleListener listener) {
        mListener = listener;
    }

    public interface OnToggleListener {
        void onToggle(boolean isChecked);
    }
}
