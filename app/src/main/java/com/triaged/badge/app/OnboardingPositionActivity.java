package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.triaged.badge.app.views.OnboardingDotsView;

/**
 * Created by Will on 7/10/14.
 */
public class OnboardingPositionActivity extends BadgeActivity {

    private Button continueButton = null;
    private TextView yourDepartmentButton = null;
    private TextView reportingToButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_position);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 1;
        onboardingDotsView.invalidate();

        continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CHECK FOR EMPTY VALUES
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingLocationActivity.class);
                startActivity(intent);
            }
        });

        yourDepartmentButton = (TextView) findViewById(R.id.your_department);
        yourDepartmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingDepartmentActivity.class);
                startActivity(intent);
            }
        });

        reportingToButton = (TextView) findViewById(R.id.reporting_to);
        reportingToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingReportingToActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);

        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("DEPARTMENT_ID")) {
                int deptId = extras.getInt("DEPARTMENT_ID", 0);
                String deptName = extras.getString("DEPARTMENT_NAME");
                yourDepartmentButton.setText(deptName);
                yourDepartmentButton.setSelected(true);
            } else if (extras.containsKey("REPORTS_TO_ID")) {
                int managerId = extras.getInt("REPORTS_TO_ID", 0);
                String managerName = extras.getString("REPORTS_TO_NAME");
                reportingToButton.setText(managerName);
                reportingToButton.setSelected(true);
            }
        }

    }
}
