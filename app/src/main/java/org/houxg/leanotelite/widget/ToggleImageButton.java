package org.houxg.leanotelite.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

import org.houxg.leanotelite.R;


public class ToggleImageButton extends ImageView implements Checkable {

    private boolean mIsChecked = false;
    private Drawable mCheckedDrawable;
    private Drawable mUncheckedDrawable;

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ToggleImageButton);
        mCheckedDrawable = array.getDrawable(R.styleable.ToggleImageButton_checkedDrawable);
        mUncheckedDrawable = array.getDrawable(R.styleable.ToggleImageButton_uncheckedDrawable);
        array.recycle();
        setClickable(true);
        setChecked(mIsChecked);
    }

    @Override
    public void setChecked(boolean checked) {
        mIsChecked = checked;
        if (mIsChecked) {
            if (mCheckedDrawable != null) {
                setImageDrawable(mCheckedDrawable);
            }
        } else {
            if (mUncheckedDrawable != null) {
                setImageDrawable(mUncheckedDrawable);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }
}
