package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

import com.triaged.badge.app.R;

/**
 * Set a custom font as an xml attribute
 * <p/>
 * Created by Will on 7/7/14.
 */
public class EditTextWithFont extends EditText {

    public EditTextWithFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attrValues = context.obtainStyledAttributes(attrs, R.styleable.TextViewWithFont);
        String fontFace = attrValues.getString(R.styleable.TextViewWithFont_font);
        if (fontFace != null) {
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontFace);
            setTypeface(typeface);
        }
    }
}
