package com.triaged.badge.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.EditProfileInfoView;
import com.triaged.badge.app.views.ProfileContactInfoView;
import com.triaged.badge.data.Contact;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Allow user to modify info after they've already gone through onboarding flow.
 *
 * Created by Will on 7/11/14.
 */
public class EditProfileActivity extends BadgeActivity {

    private static final int PICTURE_REQUEST_CODE = 1888;
    private static final String START_DATE_FORMAT_STRING = "MMMM d, yyyy";
    private static final String LOG_TAG = EditProfileActivity.class.getName();

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    /** Values may need to be updated on activity result, accessed during call to save */
    private ImageView profileImageView = null;
    private TextView profileImageMissingView = null;
    private EditProfileInfoView firstName = null;
    private EditProfileInfoView lastName = null;
    private EditProfileInfoView email = null;
    private EditProfileInfoView cellPhone = null;
    private EditProfileInfoView officePhone = null;
    private EditProfileInfoView jobTitle = null;
    private EditProfileInfoView department = null;
    private EditProfileInfoView reportingTo = null;
    private EditProfileInfoView officeLocation = null;
    private EditProfileInfoView startDate = null;
    private EditProfileInfoView birthDate = null;

    private DatePickerDialogNoYear birthdayDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;

    private DatePickerDialog startDateDialog = null;
    protected Calendar startDateCalendar = null;
    protected SimpleDateFormat startDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        setContentView(R.layout.activity_edit_profile);

        TextView backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();

        profileImageView = (ImageView) findViewById(R.id.contact_thumb);
        profileImageMissingView = (TextView) findViewById(R.id.no_photo_thumb);

        if (loggedInUser.avatarUrl != null) {
            dataProviderServiceBinding.setSmallContactImage(loggedInUser, profileImageView);
        } else {
            profileImageMissingView.setText(loggedInUser.initials);
            profileImageMissingView.setVisibility(View.VISIBLE);
        }

        firstName = (EditProfileInfoView) findViewById(R.id.edit_first_name);
        firstName.secondaryValue = loggedInUser.firstName;

        lastName = (EditProfileInfoView) findViewById(R.id.edit_last_name);
        lastName.secondaryValue = loggedInUser.lastName;

        email = (EditProfileInfoView) findViewById(R.id.edit_email);
        email.secondaryValue = loggedInUser.email != null ? loggedInUser.email : "Add";

        cellPhone = (EditProfileInfoView) findViewById(R.id.edit_cell_phone);
        cellPhone.secondaryValue = loggedInUser.cellPhone != null ? loggedInUser.cellPhone: "Add";

        officePhone = (EditProfileInfoView) findViewById(R.id.edit_office_phone);
        officePhone.secondaryValue = loggedInUser.officePhone != null ? loggedInUser.officePhone : "Add";

        jobTitle = (EditProfileInfoView) findViewById(R.id.edit_job_title);
        jobTitle.secondaryValue = loggedInUser.jobTitle != null ? loggedInUser.jobTitle : "Add";

        department = (EditProfileInfoView) findViewById(R.id.edit_department);
        department.secondaryValue = loggedInUser.departmentName != null ? loggedInUser.departmentName : "Add";

        // TODO: UPDATE
        reportingTo = (EditProfileInfoView) findViewById(R.id.edit_reporting_to);
        reportingTo.secondaryValue = loggedInUser.departmentName != null ? loggedInUser.departmentName : "Add";

        officeLocation = (EditProfileInfoView) findViewById(R.id.edit_office_location);
        officeLocation.secondaryValue = loggedInUser.departmentName != null ? loggedInUser.departmentName : "Add";

        startDate = (EditProfileInfoView) findViewById(R.id.edit_start_date);
        startDate.secondaryValue = loggedInUser.startDateString != null ? loggedInUser.startDateString : "Select Date";

        birthDate = (EditProfileInfoView) findViewById(R.id.edit_birth_date);
        birthDate.secondaryValue = loggedInUser.birthDateString != null ? loggedInUser.birthDateString : "Add";

        LinearLayout editFieldsWrapper = (LinearLayout) findViewById(R.id.edit_fields_wrapper);
        for (int i=0; i<editFieldsWrapper.getChildCount(); i++) {
            View v = editFieldsWrapper.getChildAt(i);
            if (v instanceof EditProfileInfoView) {
                final EditProfileInfoView editView = (EditProfileInfoView) v;
                editView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (editView.getId()) {
                            case R.id.edit_department:
                                Toast.makeText(EditProfileActivity.this, "STARTACTIVITYFORINTENT DEPT", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.edit_reporting_to:
                                Toast.makeText(EditProfileActivity.this, "STARTACTIVITYFORINTENT REPORTING TO", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.edit_office_location:
                                Toast.makeText(EditProfileActivity.this, "STARTACTIVITYFORINTENT OFFICE", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.edit_start_date:
                                startDateDialog.show();
                                break;
                            case R.id.edit_birth_date:
                                birthdayDialog.show();
                                DatePicker dp = findDatePicker((ViewGroup) birthdayDialog.getWindow().getDecorView());
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
                                break;
                            default:
                                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(EditProfileActivity.this);
                                alertDialog.setTitle(editView.primaryValue);
                                final EditText input = new EditText(EditProfileActivity.this);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT);
                                input.setLayoutParams(lp);
                                alertDialog.setView(input);
                                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // VALIDATION?
                                        editView.secondaryValue = input.getText().toString();
                                        editView.invalidate();
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(editView.getWindowToken(), 0);
                                    }
                                });
                                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(editView.getWindowToken(), 0);
                                    }
                                });
                                alertDialog.show();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                                break;
                        }
                    }
                });
            }
        }


        RelativeLayout editProfilePhotoButton = (RelativeLayout) findViewById(R.id.update_profile_photo_button);
        editProfilePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Camera.
                final List<Intent> cameraIntents = new ArrayList<Intent>();
                final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                final PackageManager packageManager = getPackageManager();
                final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
                for(ResolveInfo res : listCam) {
                    final String packageName = res.activityInfo.packageName;
                    final Intent intent = new Intent(captureIntent);
                    intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
                    intent.setPackage(packageName);
                    // intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    cameraIntents.add(intent);
                }

                // Filesystem.
                final Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                // Chooser of filesystem options.
                final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

                // Add the camera options.
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

                startActivityForResult(chooserIntent, PICTURE_REQUEST_CODE);
            }
        });

        // BIRTHDAY CALENDAR AND DIALOG
        birthdayCalendar = Calendar.getInstance();
        birthdayCalendar.set( Calendar.YEAR, 1 );
        if (loggedInUser.birthDateString != null) {
            // assign birthDate to calendar
        }
        birthdayFormat = new SimpleDateFormat(WelcomeActivity.BIRTHDAY_FORMAT_STRING, Locale.US);
        birthdayDialog = new DatePickerDialogNoYear(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                birthDate.secondaryValue = birthdayFormat.format(birthdayCalendar.getTime());
                birthDate.invalidate();
            }
        }, birthdayCalendar.get(Calendar.YEAR), birthdayCalendar.get(Calendar.MONTH), birthdayCalendar.get(Calendar.DAY_OF_MONTH));

        // START DATE CALENDAR AND DIALOG
        startDateCalendar = Calendar.getInstance();
        if (loggedInUser.startDateString != null) {
            // assign startDate to calendar
        }
        startDateFormat = new SimpleDateFormat(START_DATE_FORMAT_STRING, Locale.US);
        startDateDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                startDateCalendar.set(Calendar.YEAR, year);
                startDateCalendar.set(Calendar.MONTH, monthOfYear);
                startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                startDate.secondaryValue = startDateFormat.format(startDateCalendar.getTime());
                startDate.invalidate();
            }
        }, startDateCalendar.get(Calendar.YEAR),startDateCalendar.get(Calendar.MONTH), startDateCalendar.get(Calendar.DAY_OF_MONTH));

    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICTURE_REQUEST_CODE) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if (action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if (isCamera) {
                    // selectedImageUri = outputFileUri;
                } else {
                    selectedImageUri = data == null ? null : data.getData();
                }
            }
        }
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
