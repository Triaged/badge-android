package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.triaged.badge.app.R;

/**
 * Created by Will on 7/15/14.
 */
public class EditProfileInfoView extends ProfileContactInfoView {

    public String valueToSave;

    public EditProfileInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attrValues = context.obtainStyledAttributes(attrs, R.styleable.ProfileContactInfoView);
        String primaryVal = attrValues.getString(R.styleable.ProfileContactInfoView_primaryValue);
        if (primaryVal != null) {
            primaryValue = primaryVal;
        }
        float densityMultiplier = context.getResources().getDisplayMetrics().density;
        primaryPaint.setTextSize(18 * densityMultiplier);
        primaryTextTopOffset = 28 * densityMultiplier;
        secondaryTextTopOffset = 48 * densityMultiplier;
    }

}