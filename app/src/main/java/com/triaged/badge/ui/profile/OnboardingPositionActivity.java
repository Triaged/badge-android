package com.triaged.badge.ui.profile;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.Contact;
import com.triaged.badge.net.DataProviderService;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.views.OnboardingDotsView;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Will on 7/10/14.
 */
public class OnboardingPositionActivity extends BadgeActivity {

    public static final int DEPARTMENT_REQUEST_CODE = 1;
    public static final int MANAGER_REQUEST_CODE = 2;

    protected Button continueButton = null;
    protected TextView yourDepartmentButton = null;
    protected TextView reportingToButton = null;
    protected EditText jobTitleField = null;

    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private ImageView nametag = null;
    private TextView tellUsMoreTitle = null;

    protected int managerId = 0;
    protected int departmentId = 0;
    protected BroadcastReceiver onboardingFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_position);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 1;


        jobTitleField = (EditText) findViewById(R.id.your_job_title);


        continueButton = (Button) findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CHECK FOR EMPTY VALUES
                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();
                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("department_id", departmentId);
                    data.put("manager_id", managerId);
                    employeeInfo.put("job_title", jobTitleField.getText().toString());
                    TypedJsonString typedJsonString = new TypedJsonString(user.toString());
                    RestService.instance().badge().updateAccount(typedJsonString, new Callback<Account>() {
                        @Override
                        public void success(Account account, Response response) {
                            // Put updated data into database.
                            ContentValues values = new ContentValues();
                            values.put(ContactsTable.COLUMN_CONTACT_JOB_TITLE, account.getCurrentUser().getEmployeeInfo().getJobTitle());
                            values.put(ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID, account.getCurrentUser().getDepartmentId());
                            values.put(ContactsTable.COLUMN_CONTACT_MANAGER_ID, account.getCurrentUser().getManagerId());
                            getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                    ContactsTable.COLUMN_ID + " =?",
                                    new String[] { App.accountId() + ""});
                            EventBus.getDefault().post(new UpdateAccountEvent());

                            Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingLocationActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(OnboardingPositionActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating patch body for position profile data", e);
                }
            }
        });

        yourDepartmentButton = (TextView) findViewById(R.id.your_department);
        yourDepartmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingDepartmentActivity.class);
                startActivityForResult(intent, DEPARTMENT_REQUEST_CODE);
            }
        });

        reportingToButton = (TextView) findViewById(R.id.reporting_to);
        reportingToButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingPositionActivity.this, OnboardingReportingToActivity.class);
                startActivityForResult(intent, MANAGER_REQUEST_CODE);
            }
        });
        localBroadcastManager.registerReceiver(onboardingFinishedReceiver, new IntentFilter(ONBOARDING_FINISHED_ACTION));

        nametag = (ImageView) findViewById(R.id.nametag);
        tellUsMoreTitle = (TextView) findViewById(R.id.tell_us_more_title);
        densityMultiplier = getResources().getDisplayMetrics().density;
        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = activityRootView.getRootView().getHeight();

                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                activityRootView.getWindowVisibleDisplayFrame(r);

                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75)) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    nametag.setVisibility(View.GONE);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) tellUsMoreTitle.getLayoutParams();
                    lp.setMargins(0, (int) (15 * densityMultiplier), 0, (int) (15 * densityMultiplier));
                    tellUsMoreTitle.setLayoutParams(lp);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    nametag.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) tellUsMoreTitle.getLayoutParams();
                    lp.setMargins(0, (int) (35 * densityMultiplier), 0, (int) (55 * densityMultiplier));
                    tellUsMoreTitle.setLayoutParams(lp);
                }
            }
        });

    }

    @Override
    protected void onDatabaseReady() {
        final Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();
        managerId = loggedInUser.managerId;
        departmentId = loggedInUser.departmentId;

        if (loggedInUser.jobTitle != null && !loggedInUser.jobTitle.equals("")) {
            jobTitleField.setText(loggedInUser.jobTitle);
        }

        if (loggedInUser.departmentName != null && !loggedInUser.departmentName.equals("")) {
            yourDepartmentButton.setText(loggedInUser.departmentName);
        }
        if (loggedInUser.departmentId > 0) {
            yourDepartmentButton.setSelected(true);
        }
        if (loggedInUser.managerName != null && !loggedInUser.managerName.equals("")) {
            reportingToButton.setText(loggedInUser.managerName);
        }
        if (loggedInUser.managerId > 0) {
            reportingToButton.setSelected(true);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case DEPARTMENT_REQUEST_CODE:
                    yourDepartmentButton.setText(data.getStringExtra(OnboardingDepartmentActivity.DEPT_NAME_EXTRA));
                    departmentId = resultCode;
                    yourDepartmentButton.setSelected(true);
                    break;
                case MANAGER_REQUEST_CODE:
                    reportingToButton.setText(data.getStringExtra(OnboardingReportingToActivity.MGR_NAME_EXTRA));
                    managerId = resultCode;
                    reportingToButton.setSelected(true);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(onboardingFinishedReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
