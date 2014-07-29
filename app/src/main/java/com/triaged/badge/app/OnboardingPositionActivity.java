package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    protected BroadcastReceiver onboardingFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    protected DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess( int newId ) {
            Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingLocationActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

        if (loggedInUser.jobTitle != null && !loggedInUser.departmentName.equals("")) {
            jobTitleField.setText(loggedInUser.jobTitle);
        }

        continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CHECK FOR EMPTY VALUES
                dataProviderServiceBinding.savePositionProfileDataAsync( String.valueOf( jobTitleField.getText() ), deptartmentId, managerId, saveCallback );
            }
        });

        yourDepartmentButton = (TextView) findViewById(R.id.your_department);
        if (loggedInUser.departmentName != null && !loggedInUser.departmentName.equals("")) {
            yourDepartmentButton.setText( loggedInUser.departmentName );
        }
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
        if (loggedInUser.managerName != null && !loggedInUser.managerName.equals("")) {
            reportingToButton.setText(loggedInUser.managerName);
        }
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
        localBroadcastManager.registerReceiver( onboardingFinishedReceiver, new IntentFilter( ONBOARDING_FINISHED_ACTION ) );
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
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver( onboardingFinishedReceiver );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
