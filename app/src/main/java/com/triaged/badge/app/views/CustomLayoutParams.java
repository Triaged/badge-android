package com.triaged.badge.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.triaged.badge.app.R;

/**
 * Created by Will on 7/30/14.
 */
public class CustomLayoutParams extends ViewGroup.MarginLayoutParams {

    public int gravity = -1;

    public CustomLayoutParams(Context c, AttributeSet attrs) {
        super(c, attrs);

        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.FlowLayout_Layout);

        gravity = a.getInt(R.styleable.FlowLayout_Layout_android_layout_gravity, -1);

        a.recycle();
    }

    public CustomLayoutParams(int width, int height) {
        super(width, height);
    }

    public CustomLayoutParams(ViewGroup.LayoutParams source) {
        super(source);
    }

}
