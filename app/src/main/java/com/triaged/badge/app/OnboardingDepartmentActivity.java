package com.triaged.badge.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Allow the user to select their department or add a new one.
 *
 * Created by Will on 7/14/14.
 */
public class OnboardingDepartmentActivity extends BadgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_department);
        TextView backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

}
