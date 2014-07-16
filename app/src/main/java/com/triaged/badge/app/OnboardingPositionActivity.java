package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    protected EditText jobTitleField = null;
    protected int managerId = 0;
    protected int deptartmentId = 0;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    protected DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess( int newId ) {
            Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingLocationActivity.class);
            startActivity(intent);
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText(OnboardingPositionActivity.this, reason, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_position);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 1;

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        final Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();
        managerId = loggedInUser.managerId;
        deptartmentId = loggedInUser.departmentId;

        jobTitleField = (EditText)findViewById( R.id.your_job_title );

        continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Contact transientContact = new Contact();
                transientContact.id = loggedInUser.id;
                transientContact.managerId = managerId;
                transientContact.departmentId = deptartmentId;
                transientContact.jobTitle = String.valueOf( jobTitleField.getText() );

                // CHECK FOR EMPTY VALUES

                dataProviderServiceBinding.savePositionProfileDataAsync( transientContact, saveCallback );
            }
        });

        yourDepartmentButton = (TextView) findViewById(R.id.your_department);
        yourDepartmentButton.setText( loggedInUser.departmentName );
        if( loggedInUser.departmentId > 0 ) {
            yourDepartmentButton.setSelected( true );
        }
        yourDepartmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingDepartmentActivity.class);
                startActivityForResult(intent, DEPARTMENT_REQUEST_CODE);
            }
        });

        reportingToButton = (TextView) findViewById(R.id.reporting_to);
        reportingToButton.setText( loggedInUser.managerName );
        if( loggedInUser.managerId > 0 ) {
            reportingToButton.setSelected( true );
        }
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
                    yourDepartmentButton.setSelected(true);
                    break;
                case MANAGER_REQUEST_CODE:
                    reportingToButton.setText( data.getStringExtra( OnboardingReportingToActivity.MGR_NAME_EXTRA ) );
                    managerId = resultCode;
                    reportingToButton.setSelected(true);
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
