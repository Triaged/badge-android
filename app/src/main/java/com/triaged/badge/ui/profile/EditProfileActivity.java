package com.triaged.badge.ui.profile;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.ContentValues;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.Contact;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.views.EditProfileInfoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.mime.TypedFile;

/**
 * Allow user to modify info after they've already gone through onboarding flow.
 * <p/>
 * Created by Will on 7/11/14.
 */
public class EditProfileActivity extends BadgeActivity {

    private static final int PICTURE_REQUEST_CODE = 1888;
    private static final int EDIT_MY_LOCATION_REQUEST_CODE = 20;

    protected static final String LOG_TAG = EditProfileActivity.class.getName();

    /**
     * Values may need to be updated on activity result, accessed during call to save
     */
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
    private EditProfileInfoView website;
    private EditProfileInfoView linkedin;

    private String currentPhotoPath;

    private DatePickerDialogNoYear birthdayDialog = null;
    protected Calendar birthdayCalendar = null;
    protected SimpleDateFormat birthdayFormat;

    private DatePickerDialog startDateDialog = null;
    protected Calendar startDateCalendar = null;
    protected SimpleDateFormat startDateFormat;

    /**
     * Values set in onActivityResult callbacks
     */
    protected int managerId = 0;
    protected int departmentId = 0;
    protected int officeId = 0;
    protected byte[] newProfilePhotoData;

    private ProgressBar pendingUploadBar;

    private boolean changesAwaitingSave = false;

    private TextView saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        saveButton = (TextView) backButtonBar.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changesAwaitingSave = false;
                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("first_name", firstName.valueToSave);
                    data.put("last_name", lastName.valueToSave);
                    data.put("department_id", departmentId);
                    data.put("manager_id", managerId);
                    data.put("primary_office_location_id", officeId);

                    employeeInfo.put("birth_date", birthDate.valueToSave);
                    employeeInfo.put("cell_phone", cellPhone.valueToSave);
                    employeeInfo.put("job_title", jobTitle.valueToSave);
                    employeeInfo.put("office_phone", officePhone.valueToSave);
                    employeeInfo.put("website", website.valueToSave);
                    employeeInfo.put("linkedin", linkedin.valueToSave);
                    employeeInfo.put("job_start_date", startDate.valueToSave);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                    return;
                }

                TypedJsonString updateDate = new TypedJsonString(user.toString());
                RestService.instance().badge().updateAccount(updateDate, new Callback<Account>() {
                    @Override
                    public void success(final Account account, retrofit.client.Response response) {

                        // OK now send avatar if there was a new one specified
                        if (newProfilePhotoData != null) {
                            TypedFile typedFile = new TypedFile("image/png", new File(currentPhotoPath));
                            RestService.instance().badge().postAvatar(typedFile, new Callback<Account>() {
                                @Override
                                public void success(Account accountWithAvatar, retrofit.client.Response response) {
                                    saveNewAccountAndFinish(accountWithAvatar);
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    saveNewAccountAndFinish(account);
                                    Toast.makeText(EditProfileActivity.this,
                                            "Cannot save your avatar", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            saveNewAccountAndFinish(account);
                        }
                    }


                    @Override
                    public void failure(RetrofitError error) {
                        Toast.makeText(EditProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_edit_profile);


        profileImageView = (ImageView) findViewById(R.id.contact_thumb);
        profileImageMissingView = (TextView) findViewById(R.id.no_photo_thumb);
        firstName = (EditProfileInfoView) findViewById(R.id.edit_first_name);
        lastName = (EditProfileInfoView) findViewById(R.id.edit_last_name);
        cellPhone = (EditProfileInfoView) findViewById(R.id.edit_cell_phone);
        officePhone = (EditProfileInfoView) findViewById(R.id.edit_office_phone);
        jobTitle = (EditProfileInfoView) findViewById(R.id.edit_job_title);
        department = (EditProfileInfoView) findViewById(R.id.edit_department);
        reportingTo = (EditProfileInfoView) findViewById(R.id.edit_reporting_to);
        officeLocation = (EditProfileInfoView) findViewById(R.id.edit_office_location);
        startDate = (EditProfileInfoView) findViewById(R.id.edit_start_date);
        birthDate = (EditProfileInfoView) findViewById(R.id.edit_birth_date);
        website = (EditProfileInfoView) findViewById(R.id.edit_website);
        linkedin = (EditProfileInfoView) findViewById(R.id.edit_linkedin);

        LinearLayout editFieldsWrapper = (LinearLayout) findViewById(R.id.edit_fields_wrapper);
        for (int i = 0; i < editFieldsWrapper.getChildCount(); i++) {
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
                                    } catch (SecurityException e) {
                                        Log.e("ERROR", e.getMessage());
                                    } catch (IllegalArgumentException e) {
                                        Log.e("ERROR", e.getMessage());
                                    } catch (IllegalAccessException e) {
                                        Log.e("ERROR", e.getMessage());
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
                                        changesAwaitingSave = true;
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
                    for (ResolveInfo res : listCam) {
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
        birthdayCalendar.set(Calendar.YEAR, 1);

        birthdayFormat = new SimpleDateFormat(Contact.BIRTHDAY_FORMAT_STRING, Locale.US);
        birthdayFormat.setTimeZone(Contact.GMT);


        birthdayCalendar.set(Calendar.HOUR, 0);
        birthdayCalendar.set(Calendar.MINUTE, 0);
        birthdayCalendar.set(Calendar.SECOND, 0);

        birthdayDialog = new DatePickerDialogNoYear(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                birthDate.secondaryValue = birthdayFormat.format(birthdayCalendar.getTime());
                SimpleDateFormat iso8601Format = new SimpleDateFormat(Contact.ISO_8601_FORMAT_STRING);
                iso8601Format.setTimeZone(Contact.GMT);
                birthDate.valueToSave = iso8601Format.format(birthdayCalendar.getTime());
                birthDate.invalidate();
                changesAwaitingSave = true;
            }
        }, birthdayCalendar.get(Calendar.YEAR), birthdayCalendar.get(Calendar.MONTH), birthdayCalendar.get(Calendar.DAY_OF_MONTH));

        // START DATE CALENDAR AND DIALOG
        startDateCalendar = Calendar.getInstance();
        startDateCalendar.setTimeZone(Contact.GMT);
        startDateFormat = new SimpleDateFormat(Contact.START_DATE_FORMAT_STRING, Locale.US);
        startDateFormat.setTimeZone(Contact.GMT);

        startDateCalendar.set(Calendar.HOUR, 0);
        startDateCalendar.set(Calendar.MINUTE, 0);
        startDateCalendar.set(Calendar.SECOND, 0);

        startDateDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                startDateCalendar.set(Calendar.YEAR, year);
                startDateCalendar.set(Calendar.MONTH, monthOfYear);
                startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                startDate.secondaryValue = startDateFormat.format(startDateCalendar.getTime());
                SimpleDateFormat iso8601Format = new SimpleDateFormat(Contact.ISO_8601_FORMAT_STRING);
                iso8601Format.setTimeZone(Contact.GMT);
                startDate.valueToSave = iso8601Format.format(startDateCalendar.getTime());
                startDate.invalidate();
                changesAwaitingSave = true;
            }
        }, startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH), startDateCalendar.get(Calendar.DAY_OF_MONTH));

        pendingUploadBar = (ProgressBar) findViewById(R.id.pending_upload);

    }

    private void saveNewAccountAndFinish(Account account) {
        ContentValues values = UserHelper.toContentValue(account.getCurrentUser());
        getContentResolver().update(ContactProvider.CONTENT_URI, values,
                ContactsTable.COLUMN_ID + " =?",
                new String[]{account.getCurrentUser().getId() + ""});
        // Notify DataProviderService to update its model of current account
        EventBus.getDefault().post(new UpdateAccountEvent());
        finish();
    }

    @Override
    protected void onDatabaseReady() {
        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();

        profileImageMissingView.setText(loggedInUser.initials);
        profileImageMissingView.setVisibility(View.VISIBLE);
        if (loggedInUser.avatarUrl != null) {
//            dataProviderServiceBinding.setSmallContactImage(loggedInUser, profileImageView, profileImageMissingView);
            ImageLoader.getInstance().displayImage(loggedInUser.avatarUrl, profileImageView, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    profileImageView.setVisibility(View.GONE);
                }
            });
        }

        firstName.secondaryValue = loggedInUser.firstName;
        firstName.valueToSave = loggedInUser.firstName;

        lastName.secondaryValue = loggedInUser.lastName;
        lastName.valueToSave = loggedInUser.lastName;

        cellPhone.secondaryValue = loggedInUser.cellPhone != null ? loggedInUser.cellPhone : "Add";
        cellPhone.valueToSave = loggedInUser.cellPhone;

        officePhone.secondaryValue = loggedInUser.officePhone != null ? loggedInUser.officePhone : "Add";
        officePhone.valueToSave = loggedInUser.officePhone;

        jobTitle.secondaryValue = loggedInUser.jobTitle != null ? loggedInUser.jobTitle : "Add";
        jobTitle.valueToSave = loggedInUser.jobTitle;

        department.secondaryValue = loggedInUser.departmentName != null ? loggedInUser.departmentName : "Add";
        departmentId = loggedInUser.departmentId;

        reportingTo.secondaryValue = loggedInUser.managerName != null ? loggedInUser.managerName : "Add";
        managerId = loggedInUser.managerId;

        officeLocation.secondaryValue = loggedInUser.officeName != null ? loggedInUser.officeName : "Add";
        officeId = loggedInUser.primaryOfficeLocationId;

        startDate.secondaryValue = loggedInUser.startDateString != null ? loggedInUser.startDateString : "Add";

        birthDate.secondaryValue = loggedInUser.birthDateString != null ? loggedInUser.birthDateString : "Add";

        website.secondaryValue = loggedInUser.website != null ? loggedInUser.website : "Add";
        website.valueToSave = loggedInUser.website;

        linkedin.secondaryValue = loggedInUser.linkedin != null ? loggedInUser.linkedin : "Add";
        linkedin.valueToSave = loggedInUser.linkedin;


        if (loggedInUser.birthDateString != null) {
            try {
                birthdayCalendar.setTime(birthdayFormat.parse(loggedInUser.birthDateString));
            } catch (ParseException e) {
                Log.w(LOG_TAG, "Value got saved for birthdate format that is no bueno", e);
            }
        }
        birthdayCalendar.set(Calendar.HOUR, 0);
        birthdayCalendar.set(Calendar.MINUTE, 0);
        birthdayCalendar.set(Calendar.SECOND, 0);


        if (loggedInUser.startDateString != null) {
            // assign startDate to calendar
            try {
                startDateCalendar.setTime(startDateFormat.parse(loggedInUser.startDateString));
            } catch (ParseException e) {
                Log.w(LOG_TAG, "Value got saved for start date format that is no bueno", e);
            }
        }
        startDateCalendar.set(Calendar.HOUR, 0);
        startDateCalendar.set(Calendar.MINUTE, 0);
        startDateCalendar.set(Calendar.SECOND, 0);


    }

    @Override
    public void onBackPressed() {
        if (changesAwaitingSave) {
            new AlertDialog.Builder(EditProfileActivity.this)
                    .setTitle("Save Changes?")
                    .setMessage("Do you want to save your changes?")
                    .setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            saveButton.performClick();
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton("DISCARD", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            changesAwaitingSave = false;
                            onBackPressed();
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case PICTURE_REQUEST_CODE:
                    // GET FROM GALLERY
                    new ProcessImageTask().execute(data);
                    break;
                case OnboardingPositionActivity.DEPARTMENT_REQUEST_CODE:
                    department.secondaryValue = data.getStringExtra(OnboardingDepartmentActivity.DEPT_NAME_EXTRA);
                    department.invalidate();
                    departmentId = resultCode;
                    changesAwaitingSave = true;
                    break;
                case OnboardingPositionActivity.MANAGER_REQUEST_CODE:
                    reportingTo.secondaryValue = data.getStringExtra(OnboardingReportingToActivity.MGR_NAME_EXTRA);
                    reportingTo.invalidate();
                    managerId = resultCode;
                    changesAwaitingSave = true;
                    break;
                case EDIT_MY_LOCATION_REQUEST_CODE:
                    String officeNameFromResult = data.getStringExtra(EditLocationActivity.OFFICE_NAME_EXTRA);
                    officeLocation.secondaryValue = officeNameFromResult == null ? "None" : officeNameFromResult;
                    officeLocation.invalidate();
                    officeId = resultCode;
                    changesAwaitingSave = true;
                    break;
            }
        }
    }

    private Bitmap getPhotoFromFileSystem() {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            bitmap = getRotatedBitmap(exif, bitmap);
        } catch (Exception e) {
            App.gLogger.e("CRASH WHILE RETRIEVING FILE");
        }

        return bitmap;
    }

    private Bitmap getRotatedBitmap(ExifInterface exif, Bitmap bitmap) {
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        Matrix matrix = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            matrix.postRotate(180);
        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            matrix.postRotate(270);
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
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
     * @param *data @param context * @return
     */
    public Bitmap getBitmapFromGallery(Intent data, Context context) {
        if (data.getData() == null) {
            return null;
        } else {
            if (Build.VERSION.SDK_INT < 19) {
                currentPhotoPath = getPath(data.getData());
                if (currentPhotoPath != null) {
                    Bitmap fileSystemBmp = getPhotoFromFileSystem();
                    if (fileSystemBmp != null) {
                        return fileSystemBmp;
                    } else {
                        return loadPicasaImageFromGallery(data.getData());
                    }
                } else {
                    return loadPicasaImageFromGallery(data.getData());
                }
            } else {
                ParcelFileDescriptor parcelFileDescriptor;
                try {
                    parcelFileDescriptor = getContentResolver().openFileDescriptor(data.getData(), "r");
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                    parcelFileDescriptor.close();
                    String path = getPath(data.getData());
                    if (path != null) {
                        ExifInterface exifInterface = new ExifInterface(path);
                        image = getRotatedBitmap(exifInterface, image);
                    }
                    if (image != null) {
                        return image;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        try {
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
                //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                return filePath;
            } else {
                return uri.getPath(); // FOR OI/ASTRO/Dropbox etc
            }
        } catch (Exception e) {
            return null;
        }
    }

    // NEW METHOD FOR PICASA IMAGE LOAD
    private Bitmap loadPicasaImageFromGallery(final Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (columnIndex != -1) {
                try {
                    Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    return bitmap;
                    // THIS IS THE BITMAP IMAGE WE ARE LOOKING FOR.
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        cursor.close();
        return null;
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

    private class ProcessImageTask extends AsyncTask<Intent, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pendingUploadBar.setVisibility(View.VISIBLE);
            profileImageView.setVisibility(View.GONE);
            profileImageMissingView.setVisibility(View.GONE);
        }

        @Override
        protected Bitmap doInBackground(Intent... params) {
            boolean isCamera = params[0] == null || MediaStore.ACTION_IMAGE_CAPTURE.equals(params[0].getAction());
            Bitmap photo;
            if (isCamera) {
                photo = getPhotoFromFileSystem();
            } else {
                photo = getBitmapFromGallery(params[0], EditProfileActivity.this);
            }
            photo = ThumbnailUtils.extractThumbnail(photo, 300, 300);
            return photo;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            pendingUploadBar.setVisibility(View.GONE);
            profileImageMissingView.setVisibility(View.VISIBLE);
            profileImageView.setVisibility(View.VISIBLE);
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                newProfilePhotoData = byteArrayOutputStream.toByteArray();
                if (profileImageMissingView.getVisibility() == View.VISIBLE) {
                    profileImageMissingView.setVisibility(View.GONE);
                }
                changesAwaitingSave = true;
            } else {
                Toast.makeText(EditProfileActivity.this, "Unable to retrieve photo", Toast.LENGTH_SHORT).show();
            }
            super.onPostExecute(bitmap);
        }
    }
}
