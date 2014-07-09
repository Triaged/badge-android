package com.triaged.badge.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileManagesUserView extends ProfileContactInfoView {

    private Bitmap bitmap = null;
    private float densityMultiplier;

    public ProfileManagesUserView(Context context, AttributeSet attrs) {
        super(context, attrs);

        densityMultiplier = context.getResources().getDisplayMetrics().density;
        primaryPaint.setTextSize(18 * densityMultiplier);
        primaryTextTopOffset = 29 * densityMultiplier;
        noTitleTextOffset = 39 * densityMultiplier;
        secondaryTextTopOffset = 49 * densityMultiplier;
        leftTextOffset = 74f * densityMultiplier;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 16 * densityMultiplier, (getHeight() - bitmap.getHeight()) /2 , null);
        }
    }

    /**
     * When recycling this view, call this function to clear out the previous
     * bitmap while waiting for the new one.
     */
    public void clearBitmap() {
        this.bitmap = null;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }
}
