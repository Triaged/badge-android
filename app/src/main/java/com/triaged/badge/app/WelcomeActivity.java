package com.triaged.badge.app;

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.triaged.badge.app.views.OnboardingDotsView;
import com.triaged.badge.data.Contact;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Allow user to setup first name, last name, cell number and birthday.
 *
 * @author Created by Will on 7/10/14.
 */
public class WelcomeActivity extends BadgeActivity implements DatePickerDialog.OnDateSetListener {

    public static final String BIRTHDAY_FORMAT_STRING = "MMMM d";

    private EditText firstName = null;
    private EditText lastName = null;
    private EditText cellNumber = null;
    private EditText birthday = null;
    private DatePickerDialogNoYear datePickerDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;
    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    protected BroadcastReceiver onboardingFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    protected DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess( int newId ) {
            Intent intent = new Intent(WelcomeActivity.this, OnboardingPositionActivity.class);
            startActivity(intent);
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText( WelcomeActivity.this, reason, Toast.LENGTH_LONG ).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        setContentView(R.layout.activity_welcome);
        firstName = (EditText) findViewById(R.id.first_name);
        lastName = (EditText) findViewById(R.id.last_name);
        cellNumber = (EditText) findViewById(R.id.cell_number);
        birthday = (EditText) findViewById(R.id.birthday);

        Contact account = dataProviderServiceBinding.getLoggedInUser();
        lastName.setText( account.lastName );
        firstName.setText( account.firstName );
        cellNumber.setText( account.cellPhone );
        birthday.setText( account.birthDateString );

        Button continueButton = (Button) findViewById(R.id.continue_button);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                birthdayCalendar.setTimeZone( Contact.GMT );
                dataProviderServiceBinding.saveBasicProfileDataAsync( firstName.getText().toString(), lastName.getText().toString(), birthdayCalendar.getTime().toString(), cellNumber.getText().toString(), saveCallback );
            }
        });

        birthdayCalendar = Calendar.getInstance();
        birthdayCalendar.set( Calendar.YEAR, 1 );

        birthdayFormat = new SimpleDateFormat(BIRTHDAY_FORMAT_STRING, Locale.US);

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
                    }
                    catch (SecurityException e) {
                        Log.d("ERROR", e.getMessage());
                    }
                    catch (IllegalArgumentException e) {
                        Log.d("ERROR", e.getMessage());
                    }
                    catch (IllegalAccessException e) {
                        Log.d("ERROR", e.getMessage());
                    }
                }
                return false;
            }
        });
        localBroadcastManager.registerReceiver( onboardingFinishedReceiver, new IntentFilter( ONBOARDING_FINISHED_ACTION ) );
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver( onboardingFinishedReceiver );
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
