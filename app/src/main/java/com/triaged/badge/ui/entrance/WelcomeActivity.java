package com.triaged.badge.ui.entrance;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.User;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.profile.OnboardingPositionActivity;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Allow user to setup first name, last name, cell number and birthday.
 *
 * @author Created by Will on 7/10/14.
 */
public class WelcomeActivity extends BadgeActivity implements DatePickerDialog.OnDateSetListener {

    private static final String LOG_TAG = WelcomeActivity.class.getName();

    private EditText firstName = null;
    private EditText lastName = null;
    private EditText cellNumber = null;
    private EditText birthday = null;
    private DatePickerDialogNoYear datePickerDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;

    private TextView welcomeTitle = null;
    private TextView welcomeInfo = null;

    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;

    protected BroadcastReceiver onboardingFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onDatabaseReady() {
        Contact account = dataProviderServiceBinding.getLoggedInUser();
        lastName.setText(account.lastName);
        firstName.setText(account.firstName);
        cellNumber.setText(account.cellPhone);
        birthday.setText(account.birthDateString);

        if (account.birthDateString != null && !account.birthDateString.equals("")) {
            try {
                birthdayCalendar.setTime(birthdayFormat.parse(account.birthDateString));
                birthday.setText(account.birthDateString);
            } catch (ParseException e) {
                Log.w(LOG_TAG, "Value got saved for birthdate format that is no bueno", e);
            }
        } else {
            birthdayCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            birthdayCalendar.set(Calendar.DAY_OF_MONTH, 1);
        }

        birthdayCalendar.set(Calendar.HOUR, 0);
        birthdayCalendar.set(Calendar.MINUTE, 0);
        birthdayCalendar.set(Calendar.SECOND, 0);

        if (account.sharingOfficeLocation == Contact.SHARING_LOCATION_UNAVAILABLE) {

            JSONObject user = new JSONObject();
            try {
                JSONObject data = new JSONObject();
                user.put("user", data);
                data.put("sharing_office_location", true);

            } catch (JSONException e) {
                App.gLogger.e("JSON exception creating post body for basic profile data", e);
                return;
            }
            TypedJsonString typedJsonString = new TypedJsonString(user.toString());
            RestService.instance().badge().updateAccount(typedJsonString, new Callback<Account>() {
                @Override
                public void success(Account account, Response response) {
                    SharedPreferencesUtil.store(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true);

                    ContentValues values = new ContentValues(1);
                    values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, User.SHARING_LOCATION_ONE);
                    getContentResolver().update(ContactProvider.CONTENT_URI, values,
                            ContactsTable.COLUMN_ID + " =?",
                            new String[]{App.accountId() + ""});
                    EventBus.getDefault().post(new UpdateAccountEvent());

                    LocationTrackingService.scheduleAlarm(WelcomeActivity.this);
                }

                @Override
                public void failure(RetrofitError error) {
                    Toast.makeText(WelcomeActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        firstName = (EditText) findViewById(R.id.first_name);
        lastName = (EditText) findViewById(R.id.last_name);
        cellNumber = (EditText) findViewById(R.id.cell_number);
        birthday = (EditText) findViewById(R.id.birthday);


        Button continueButton = (Button) findViewById(R.id.continue_button);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                birthdayCalendar.setTimeZone(Contact.GMT);
                SimpleDateFormat iso8601Format = new SimpleDateFormat(Contact.ISO_8601_FORMAT_STRING);
                iso8601Format.setTimeZone(Contact.GMT);
                String birthDateValue;
                if ("".equals(birthday.getText().toString())) {
                    birthDateValue = null;
                } else {
                    birthDateValue = iso8601Format.format(birthdayCalendar.getTime());
                }
//                dataProviderServiceBinding.saveBasicProfileDataAsync(firstName.getText().toString(),
//                        lastName.getText().toString(), birthDateValue, cellNumber.getText().toString(), saveCallback);

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("first_name", firstName.getText().toString());
                    data.put("last_name", lastName.getText().toString());
                    employeeInfo.put("birth_date", birthDateValue);
                    employeeInfo.put("cell_phone", cellNumber.getText().toString());
                    RestService.instance().badge().updateAccount(new TypedJsonString(user.toString()), new Callback<Account>() {
                        @Override
                        public void success(Account account, Response response) {
                            // Update account info in database.
                            ContentValues values = new ContentValues();
                            values.put(ContactsTable.COLUMN_CONTACT_FIRST_NAME, account.getCurrentUser().getFirstName());
                            values.put(ContactsTable.COLUMN_CONTACT_LAST_NAME, account.getCurrentUser().getLastName());
                            values.put(ContactsTable.COLUMN_CONTACT_CELL_PHONE, account.getCurrentUser().getEmployeeInfo().getCellPhone());
                            if (account.getCurrentUser().getEmployeeInfo().getBirthDate() != null) {
                                values.put(ContactsTable.COLUMN_CONTACT_BIRTH_DATE,
                                        Contact.convertBirthDateString(account.getCurrentUser().getEmployeeInfo().getBirthDate()));
                            }
                            getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                    ContactsTable.COLUMN_ID + " =?",
                                    new String[]{App.accountId() + ""});
                            EventBus.getDefault().post(new UpdateAccountEvent());
                            // Start next activity.
                            Intent intent = new Intent(WelcomeActivity.this, OnboardingPositionActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(WelcomeActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });


                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                }
            }
        });

        birthdayCalendar = Calendar.getInstance();
        birthdayCalendar.set(Calendar.YEAR, 1);

        birthdayFormat = new SimpleDateFormat(Contact.BIRTHDAY_FORMAT_STRING, Locale.US);


        birthdayCalendar.set(Calendar.HOUR, 0);
        birthdayCalendar.set(Calendar.MINUTE, 0);
        birthdayCalendar.set(Calendar.SECOND, 0);

        datePickerDialog = new DatePickerDialogNoYear(this, this, birthdayCalendar.get(Calendar.YEAR), birthdayCalendar.get(Calendar.MONTH), birthdayCalendar.get(Calendar.DAY_OF_MONTH));

        birthday.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                datePickerDialog.show();
                DatePicker dp = findDatePicker((ViewGroup) datePickerDialog.getWindow().getDecorView());
                if (dp != null) {
                    try {
                        Field f[] = dp.getClass().getDeclaredFields();
                        for (Field field : f) {
                            if (field.getName().equals("mYearPicker") || field.getName().equals("mYearSpinner")) {
                                field.setAccessible(true);
                                Object yearPicker = new Object();
                                yearPicker = field.get(dp);
                                ((View) yearPicker).setVisibility(View.GONE);
                            }
                        }
                    } catch (SecurityException e) {
                        Log.e("ERROR", e.getMessage());
                    } catch (IllegalArgumentException e) {
                        Log.e("ERROR", e.getMessage());
                    } catch (IllegalAccessException e) {
                        Log.e("ERROR", e.getMessage());
                    }
                }
                return false;
            }
        });
        localBroadcastManager.registerReceiver(onboardingFinishedReceiver, new IntentFilter(ONBOARDING_FINISHED_ACTION));

        welcomeTitle = (TextView) findViewById(R.id.welcome_title);
        welcomeInfo = (TextView) findViewById(R.id.welcome_info);
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
                    welcomeInfo.setVisibility(View.GONE);
                    welcomeTitle.setVisibility(View.GONE);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    welcomeInfo.setVisibility(View.VISIBLE);
                    welcomeTitle.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        if ( SharedPreferencesUtil.getBoolean(R.string.pref_has_fetch_company, false) ) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(onboardingFinishedReceiver);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        birthday.setText(birthdayFormat.format(birthdayCalendar.getTime()));
    }

    class DatePickerDialogNoYear extends DatePickerDialog {

        public DatePickerDialogNoYear(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
            super(context, callBack, year, monthOfYear, dayOfMonth);
            setTitle(birthdayFormat.format(birthdayCalendar.getTime()));
        }

        @Override
        public void onDateChanged(DatePicker view, int year, int month, int day) {
            super.onDateChanged(view, year, month, day);
            birthdayCalendar.set(Calendar.MONTH, month);
            birthdayCalendar.set(Calendar.DAY_OF_MONTH, day);
            setTitle(birthdayFormat.format(birthdayCalendar.getTime()));
        }
    }

    private DatePicker findDatePicker(ViewGroup group) {
        if (group != null) {
            for (int i = 0, j = group.getChildCount(); i < j; i++) {
                View child = group.getChildAt(i);
                if (child instanceof DatePicker) {
                    return (DatePicker) child;
                } else if (child instanceof ViewGroup) {
                    DatePicker result = findDatePicker((ViewGroup) child);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }

}
