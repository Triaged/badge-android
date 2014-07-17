package com.triaged.badge.app.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.triaged.badge.app.R;

/**
 * Created by Will on 7/9/14.
 */
public class ProfileCurrentLocationView extends View {

    private Bitmap iconIn;
    private Bitmap iconOut;
    protected TextPaint primaryPaint = null;
    public String primaryValue = null;
    protected float leftTextOffset = 49f;
    public boolean isOn;
    private static final int offColor = Color.parseColor("#FF222222");
    private static final int onColor = Color.parseColor("#FF00A798");

    public ProfileCurrentLocationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        iconIn = BitmapFactory.decodeResource(context.getResources(), R.drawable.location_in);
        iconOut = BitmapFactory.decodeResource(context.getResources(), R.drawable.location_out);

        Typeface roboto = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");

        primaryPaint = new TextPaint();
        primaryPaint.setTypeface(roboto);

        primaryPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        float densityMultiplier = context.getResources().getDisplayMetrics().density;
        primaryPaint.setTextSize(18 * densityMultiplier);

        leftTextOffset = 50f * densityMultiplier;

    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect bounds = new Rect();
        primaryPaint.getTextBounds(primaryValue, 0, primaryValue.length(), bounds);
        if (isOn) {
            primaryPaint.setColor(onColor);
            canvas.drawBitmap(iconIn, 0, (getHeight() - iconIn.getHeight() ) / 2, null);
        } else {
            primaryPaint.setColor(offColor);
            canvas.drawBitmap(iconOut, 0, (getHeight() - iconOut.getHeight() ) / 2, null);
        }

        canvas.drawText(primaryValue, leftTextOffset, (getHeight() + bounds.height()) / 2, primaryPaint);

    }
}
