package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.triaged.badge.app.views.OnboardingDotsView;
import com.triaged.badge.data.Contact;

/**
 * Created by Will on 7/10/14.
 */
public class OnboardingPositionActivity extends BadgeActivity {

    protected static final int DEPARTMENT_REQUEST_CODE = 1;
    protected static final int MANAGER_REQUEST_CODE = 2;

    protected Button continueButton = null;
    protected TextView yourDepartmentButton = null;
    protected TextView reportingToButton = null;
    protected int managerId = -1;
    protected int deptartmentId = -1;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_position);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 1;

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();
        managerId = loggedInUser.managerId;
        deptartmentId = loggedInUser.departmentId;

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
        yourDepartmentButton.setText( loggedInUser.departmentName );
        yourDepartmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingDepartmentActivity.class);
                startActivityForResult(intent, DEPARTMENT_REQUEST_CODE);
            }
        });

        reportingToButton = (TextView) findViewById(R.id.reporting_to);
        reportingToButton.setText( loggedInUser.managerName );

        reportingToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingReportingToActivity.class);
                startActivityForResult(intent, MANAGER_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( resultCode != RESULT_CANCELED ) {
            switch (requestCode) {
                case DEPARTMENT_REQUEST_CODE:
                    yourDepartmentButton.setText( data.getStringExtra( OnboardingDepartmentActivity.DEPT_NAME_EXTRA ) );
                    deptartmentId = resultCode;
                    break;
                case MANAGER_REQUEST_CODE:
                    reportingToButton.setText( data.getStringExtra( OnboardingReportingToActivity.MGR_NAME_EXTRA ) );
                    managerId = resultCode;
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);

//        Intent intent = getIntent();
//
//        Bundle extras = intent.getExtras();
//        if (extras != null) {
//            if (extras.containsKey("DEPARTMENT_ID")) {
//                int deptId = extras.getInt("DEPARTMENT_ID", 0);
//                String deptName = extras.getString("DEPARTMENT_NAME");
//                yourDepartmentButton.setText(deptName);
//                yourDepartmentButton.setSelected(true);
//            } else if (extras.containsKey("REPORTS_TO_ID")) {
//                int managerId = extras.getInt("REPORTS_TO_ID", 0);
//                String managerName = extras.getString("REPORTS_TO_NAME");
//                reportingToButton.setText(managerName);
//                reportingToButton.setSelected(true);
//            }
//        }

    }
}
