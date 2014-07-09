package com.triaged.badge.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileManagesUserView extends ProfileContactInfoView {

    public ProfileManagesUserView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float densityMultiplier = context.getResources().getDisplayMetrics().density;
        primaryPaint.setTextSize(18 * densityMultiplier);
        primaryTextTopOffset = 29 * densityMultiplier;
        noTitleTextOffset = 39 * densityMultiplier;
        secondaryTextTopOffset = 49 * densityMultiplier;
        leftTextOffset = 53f * densityMultiplier;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}
