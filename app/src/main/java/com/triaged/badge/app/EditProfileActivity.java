package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.triaged.badge.data.Contact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Allow user to modify info after they've already gone through onboarding flow.
 *
 * Created by Will on 7/11/14.
 */
public class EditProfileActivity extends BadgeActivity {

    private static final int PICTURE_REQUEST_CODE = 1888;
    private static final int EDIT_MY_LOCATION_REQUEST_CODE = 20;

    protected static final String LOG_TAG = EditProfileActivity.class.getName();

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    /** Values may need to be updated on activity result, accessed during call to save */
    private ImageView profileImageView = null;
    private TextView profileImageMissingView = null;
    private EditProfileInfoView firstName = null;
    private EditProfileInfoView lastName = null;
    private EditProfileInfoView cellPhone = null;
    private EditProfileInfoView officePhone = null;
    private EditProfileInfoView jobTitle = null;
    private EditProfileInfoView department = null;
    private EditProfileInfoView reportingTo = null;
    private EditProfileInfoView officeLocation = null;
    private EditProfileInfoView startDate = null;
    private EditProfileInfoView birthDate = null;

    private String currentPhotoPath;

    private DatePickerDialogNoYear birthdayDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;

    private DatePickerDialog startDateDialog = null;
    protected Calendar startDateCalendar = null;
    protected SimpleDateFormat startDateFormat;

    /** Values set in onActivityResult callbacks */
    protected int managerId = 0;
    protected int departmentId = 0;
    protected int officeId = 0;
    protected byte[] newProfilePhotoData;

    protected DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess(int newId) {
            finish();
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText( EditProfileActivity.this, reason, Toast.LENGTH_SHORT ).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflater = LayoutInflater.from(this);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View backButtonBar = inflater.inflate(R.layout.actionbar_save, null);
        TextView backButton = (TextView) backButtonBar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView saveButton = (TextView) backButtonBar.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataProviderServiceBinding.saveAllProfileDataAsync(
                        firstName.valueToSave,
                        lastName.valueToSave,
                        cellPhone.valueToSave,
                        officePhone.valueToSave,
                        jobTitle.valueToSave,
                        departmentId,
                        managerId,
                        officeId,
                        startDate.valueToSave,
                        birthDate.valueToSave,
                        newProfilePhotoData,
                        saveCallback
                );
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_edit_profile);

        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();

        profileImageView = (ImageView) findViewById(R.id.contact_thumb);
        profileImageMissingView = (TextView) findViewById(R.id.no_photo_thumb);

        profileImageMissingView.setText(loggedInUser.initials);
        profileImageMissingView.setVisibility(View.VISIBLE);
        if (loggedInUser.avatarUrl != null) {
            dataProviderServiceBinding.setSmallContactImage(loggedInUser, profileImageView, profileImageMissingView);
        }

        firstName = (EditProfileInfoView) findViewById(R.id.edit_first_name);
        firstName.secondaryValue = loggedInUser.firstName;
        firstName.valueToSave = loggedInUser.firstName;

        lastName = (EditProfileInfoView) findViewById(R.id.edit_last_name);
        lastName.secondaryValue = loggedInUser.lastName;
        lastName.valueToSave = loggedInUser.lastName;

        cellPhone = (EditProfileInfoView) findViewById(R.id.edit_cell_phone);
        cellPhone.secondaryValue = loggedInUser.cellPhone != null ? loggedInUser.cellPhone: "Add";
        cellPhone.valueToSave = loggedInUser.cellPhone;

        officePhone = (EditProfileInfoView) findViewById(R.id.edit_office_phone);
        officePhone.secondaryValue = loggedInUser.officePhone != null ? loggedInUser.officePhone : "Add";
        officePhone.valueToSave = loggedInUser.officePhone;

        jobTitle = (EditProfileInfoView) findViewById(R.id.edit_job_title);
        jobTitle.secondaryValue = loggedInUser.jobTitle != null ? loggedInUser.jobTitle : "Add";
        jobTitle.valueToSave = loggedInUser.jobTitle;

        department = (EditProfileInfoView) findViewById(R.id.edit_department);
        department.secondaryValue = loggedInUser.departmentName != null ? loggedInUser.departmentName : "Add";
        departmentId = loggedInUser.departmentId;

        reportingTo = (EditProfileInfoView) findViewById(R.id.edit_reporting_to);
        reportingTo.secondaryValue = loggedInUser.managerName != null ? loggedInUser.managerName : "Add";
        managerId = loggedInUser.managerId;

        officeLocation = (EditProfileInfoView) findViewById(R.id.edit_office_location);
        officeLocation.secondaryValue = loggedInUser.officeName != null ? loggedInUser.officeName : "Add";
        officeId = loggedInUser.primaryOfficeLocationId;

        startDate = (EditProfileInfoView) findViewById(R.id.edit_start_date);
        startDate.secondaryValue = loggedInUser.startDateString != null ? loggedInUser.startDateString : "Add";
        startDate.valueToSave = loggedInUser.startDateString;

        birthDate = (EditProfileInfoView) findViewById(R.id.edit_birth_date);
        birthDate.secondaryValue = loggedInUser.birthDateString != null ? loggedInUser.birthDateString : "Add";
        birthDate.valueToSave = loggedInUser.birthDateString;

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
                                Intent editDepartmentIntent = new Intent(EditProfileActivity.this, OnboardingDepartmentActivity.class);
                                startActivityForResult(editDepartmentIntent, OnboardingPositionActivity.DEPARTMENT_REQUEST_CODE);
                                break;
                            case R.id.edit_reporting_to:
                                Intent editManagerIntent = new Intent(EditProfileActivity.this, OnboardingReportingToActivity.class);
                                startActivityForResult(editManagerIntent, OnboardingPositionActivity.MANAGER_REQUEST_CODE);
                                break;
                            case R.id.edit_office_location:
                                Intent editLocationIntent = new Intent(EditProfileActivity.this, EditLocationActivity.class);
                                startActivityForResult(editLocationIntent, EDIT_MY_LOCATION_REQUEST_CODE);
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
                                        editView.valueToSave = input.getText().toString();
                                        editView.invalidate();
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                        dialog.cancel();
                                    }
                                });
                                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                        dialog.cancel();
                                    }
                                });
                                alertDialog.show();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }
                    }
                });
            }
        }


        RelativeLayout editProfilePhotoButton = (RelativeLayout) findViewById(R.id.update_profile_photo_button);
        editProfilePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Determine Uri of camera image to save.
                try {
                    File photoFile = createImageFile();

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
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // BIRTHDAY CALENDAR AND DIALOG
        birthdayCalendar = Calendar.getInstance();
        birthdayCalendar.setTimeZone(Contact.GMT);
        birthdayCalendar.set( Calendar.YEAR, 1 );
        birthdayFormat = new SimpleDateFormat(Contact.BIRTHDAY_FORMAT_STRING, Locale.US);
        birthdayFormat.setTimeZone( Contact.GMT );

        if (loggedInUser.birthDateString != null) {
            try {
                birthdayCalendar.setTime( birthdayFormat.parse( loggedInUser.birthDateString ) );
            } catch (ParseException e) {
                Log.w( LOG_TAG, "Value got saved for birthdate format that is no bueno", e );
            }
        }

        birthdayCalendar.set( Calendar.HOUR, 0 );
        birthdayCalendar.set( Calendar.MINUTE, 0 );
        birthdayCalendar.set( Calendar.SECOND, 0 );

        birthdayDialog = new DatePickerDialogNoYear(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                birthDate.secondaryValue = birthdayFormat.format(birthdayCalendar.getTime());
                SimpleDateFormat iso8601Format = new SimpleDateFormat(Contact.ISO_8601_FORMAT_STRING);
                iso8601Format.setTimeZone( Contact.GMT );
                birthDate.valueToSave = iso8601Format.format(birthdayCalendar.getTime());
                birthDate.invalidate();
            }
        }, birthdayCalendar.get(Calendar.YEAR), birthdayCalendar.get(Calendar.MONTH), birthdayCalendar.get(Calendar.DAY_OF_MONTH));

        // START DATE CALENDAR AND DIALOG
        startDateCalendar = Calendar.getInstance();
        startDateCalendar.setTimeZone( Contact.GMT );
        startDateFormat = new SimpleDateFormat( Contact.START_DATE_FORMAT_STRING, Locale.US);
        startDateFormat.setTimeZone( Contact.GMT );
        if (loggedInUser.startDateString != null) {
            // assign startDate to calendar
            try {
                startDateCalendar.setTime(startDateFormat.parse(loggedInUser.startDateString));
            }
            catch (ParseException e) {
                Log.w(LOG_TAG, "Value got saved for start date format that is no bueno", e);
            }
        }
        startDateCalendar.set( Calendar.HOUR, 0 );
        startDateCalendar.set( Calendar.MINUTE, 0 );
        startDateCalendar.set( Calendar.SECOND, 0 );

        startDateDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                startDateCalendar.set(Calendar.YEAR, year);
                startDateCalendar.set(Calendar.MONTH, monthOfYear);
                startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                startDate.secondaryValue = startDateFormat.format(startDateCalendar.getTime());
                SimpleDateFormat iso8601Format = new SimpleDateFormat(Contact.ISO_8601_FORMAT_STRING);
                iso8601Format.setTimeZone( Contact.GMT );
                startDate.valueToSave = iso8601Format.format(startDateCalendar.getTime());
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
        if( resultCode != RESULT_CANCELED ) {
            switch (requestCode) {
                case PICTURE_REQUEST_CODE:
                    // GET FROM GALLERY
                    boolean isCamera = data == null || MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
                    Bitmap photo;
                    if (isCamera) {
                        photo = getPhotoFromFileSystem();
                    } else {
                        photo = getBitmapFromGallery(data, EditProfileActivity.this);
                    }
                    photo = ThumbnailUtils.extractThumbnail(photo, 300, 300);
                    profileImageView.setImageBitmap(photo);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    newProfilePhotoData = byteArrayOutputStream.toByteArray();
                    if (profileImageMissingView.getVisibility() == View.VISIBLE) {
                        profileImageMissingView.setVisibility(View.GONE);
                    }
                    break;
                case OnboardingPositionActivity.DEPARTMENT_REQUEST_CODE:
                    department.secondaryValue = data.getStringExtra(OnboardingDepartmentActivity.DEPT_NAME_EXTRA);
                    department.invalidate();
                    departmentId = resultCode;
                    break;
                case OnboardingPositionActivity.MANAGER_REQUEST_CODE:
                    reportingTo.secondaryValue = data.getStringExtra(OnboardingReportingToActivity.MGR_NAME_EXTRA);
                    reportingTo.invalidate();
                    managerId = resultCode;
                    break;
                case EDIT_MY_LOCATION_REQUEST_CODE:
                    String officeNameFromResult = data.getStringExtra(EditLocationActivity.OFFICE_NAME_EXTRA);
                    officeLocation.secondaryValue = officeNameFromResult == null ? "None" : officeNameFromResult;
                    officeLocation.invalidate();
                    officeId = resultCode;
                    break;
            }
        }
    }

    private Bitmap getPhotoFromFileSystem() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
        }
        catch (Exception e) {
            Log.d(LOG_TAG, "CRASH WHILE RETRIEVING FILE");
        }

        return bitmap;
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

    /**
     * Use for decoding camera response data.
     *
     * @param *data  @param context * @return
     */
    public Bitmap getBitmapFromGallery(Intent data, Context context){
        Uri selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(selectedImage,filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        currentPhotoPath = picturePath;
        return BitmapFactory.decodeFile(picturePath);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
