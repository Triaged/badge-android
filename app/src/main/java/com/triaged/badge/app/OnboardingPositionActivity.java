package com.triaged.badge.app;

import android.os.Bundle;

import com.triaged.badge.app.views.OnboardingDotsView;

/**
 * Created by Will on 7/10/14.
 */
public class OnboardingPositionActivity extends BadgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_position);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 1;
        onboardingDotsView.invalidate();

    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }
}
