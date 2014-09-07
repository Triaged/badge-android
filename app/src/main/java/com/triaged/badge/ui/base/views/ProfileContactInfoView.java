package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileContactInfoView extends View {

    protected TextPaint primaryPaint = null;
    protected TextPaint secondaryPaint = null;
    public String primaryValue = null;
    public String secondaryValue = null;
    protected float primaryTextTopOffset = 24f;
    protected float noTitleTextOffset = 33f;
    protected float secondaryTextTopOffset = 43f;
    protected float leftTextOffset = 8f;

    public ProfileContactInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Typeface roboto = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");

        primaryPaint = new TextPaint();
        primaryPaint.setColor(Color.parseColor("#FF222222"));
        primaryPaint.setTypeface(roboto);

        secondaryPaint = new TextPaint();
        secondaryPaint.setColor(Color.parseColor("#FF777777"));
        secondaryPaint.setTypeface(roboto);

        float densityMultiplier = context.getResources().getDisplayMetrics().density;
        primaryPaint.setTextSize(16 * densityMultiplier);
        primaryPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        secondaryPaint.setTextSize(14 * densityMultiplier);
        secondaryPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        primaryTextTopOffset = 24 * densityMultiplier;
        secondaryTextTopOffset = 43 * densityMultiplier;
        noTitleTextOffset = 33 * densityMultiplier;
        leftTextOffset = 8f * densityMultiplier;

    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (secondaryValue == null) {
            canvas.drawText(primaryValue, leftTextOffset, noTitleTextOffset, primaryPaint);
        } else {
            canvas.drawText(primaryValue, leftTextOffset, primaryTextTopOffset, primaryPaint);
            canvas.drawText(secondaryValue, leftTextOffset, secondaryTextTopOffset, secondaryPaint);
        }
    }
}
