package com.triaged.badge.app;

import android.app.DatePickerDialog;
import android.content.Context;
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

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Allow user to setup first name, last name, cell number and birthday.
 *
 * Created by Will on 7/10/14.
 */
public class WelcomeActivity extends BadgeActivity implements DatePickerDialog.OnDateSetListener {

    private OnboardingDotsView onboardingDotsView = null;
    private EditText firstName = null;
    private EditText lastName = null;
    private EditText cellNumber = null;
    private EditText birthday = null;
    private DatePickerDialogNoYear datePickerDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 0;
        onboardingDotsView.invalidate();
        firstName = (EditText) findViewById(R.id.first_name);
        lastName = (EditText) findViewById(R.id.last_name);
        cellNumber = (EditText) findViewById(R.id.cell_number);
        birthday = (EditText) findViewById(R.id.birthday);

        Button continueButton = (Button) findViewById(R.id.continue_button);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstNameString = firstName.getText().toString();
                String lastNameString = lastName.getText().toString();
                String cellNumberString = cellNumber.getText().toString();
                String birthdayString = birthday.getText().toString();
                Toast.makeText(WelcomeActivity.this, firstNameString, Toast.LENGTH_SHORT).show();
            }
        });

        birthdayCalendar = Calendar.getInstance();
        String birthdayFormatString = "MMMM dd";
        birthdayFormat = new SimpleDateFormat(birthdayFormatString, Locale.US);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
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
