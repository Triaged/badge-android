package com.triaged.badge.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import com.triaged.badge.app.R;

/**
 * Created by Will on 7/14/14.
 */
public class AutocompleteTextViewWithFont extends AutoCompleteTextView {

    public AutocompleteTextViewWithFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attrValues = context.obtainStyledAttributes(attrs, R.styleable.TextViewWithFont);
        String fontFace = attrValues.getString( R.styleable.TextViewWithFont_font );
        if( fontFace != null ) {
            Typeface typeface = Typeface.createFromAsset( context.getAssets(), fontFace );
            setTypeface( typeface );
        }
    }

}
