package com.triaged.badge.ui.base.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Navigation dots displayed at the top of onboarding flow activities.
 * Parent activity should set the currentDotIndex.
 * <p/>
 * Created by Will on 7/10/14.
 */
public class OnboardingDotsView extends View {

    private static final int NUM_DOTS = 3;

    private Paint offPaint = null;
    private Paint onPaint = null;
    public int currentDotIndex = 0;
    private int dotRadius = 4;
    private int dotXMargin = 18;

    public OnboardingDotsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        offPaint = new Paint();
        offPaint.setColor(Color.parseColor("#d8dce0"));
        onPaint = new Paint();
        onPaint.setColor(Color.parseColor("#00A798"));

        float densityMultiplier = context.getResources().getDisplayMetrics().density;
        dotRadius = (int) (4 * densityMultiplier);
        dotXMargin = (int) (18 * densityMultiplier);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < NUM_DOTS; i++) {
            canvas.drawCircle(dotRadius + (i * dotXMargin), dotRadius, dotRadius, (currentDotIndex == i) ? onPaint : offPaint);
        }
    }
}
