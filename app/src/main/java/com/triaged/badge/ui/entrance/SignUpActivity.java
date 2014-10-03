package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.makeramen.RoundedImageView;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.triaged.badge.app.R;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.responses.AuthenticationResponse;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.utils.MediaPickerUtils;
import com.triaged.utils.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SignUpActivity extends Activity implements Validator.ValidationListener  {

    private static final int request_code_gallery = 1000;

    Validator validator;
    ProgressDialog progressDialog;

    @InjectView(R.id.root_view) View rootView;
    @Required(order = 1) @InjectView(R.id.first_name_edit_text) EditText firstNameView;
    @Required(order = 2) @InjectView(R.id.last_name_edit_text) EditText lastNameView;
    @Required(order = 3) @Email(order = 4, messageResId = R.string.invalid_email_error)
    @InjectView(R.id.email_edit_text) EditText emailView;
    @InjectView(R.id.phone_edit_text) EditText phoneView;
    @InjectView(R.id.user_image) RoundedImageView userImage;




    @OnClick(R.id.add_image_layout)
    void selectImage() {
        MediaPickerUtils.getImageFromGallery(this, request_code_gallery);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.inject(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        validator = new Validator(this);
        validator.setValidationListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sign_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                validator.validate();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onValidationSucceeded() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Signing up");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        JsonObject authParams = new JsonObject();
        authParams.addProperty("first_name", firstNameView.getText().toString());
        authParams.addProperty("last_name", lastNameView.getText().toString());
        authParams.addProperty("email", emailView.getText().toString());
        authParams.addProperty("phone_number", phoneView.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("auth_params", authParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().signUp(typedJsonString, new Callback<AuthenticationResponse>() {
            @Override
            public void success(AuthenticationResponse authenticationResponse, Response response) {
                progressDialog.dismiss();

                // Store account info in shared preferences.
                SharedPreferencesHelper.instance()
                        .putBoolean(R.string.pref_is_a_company_email_key, isACompanyEmail(emailView.getText().toString()))
                        .putString(R.string.pref_account_email_key, emailView.getText().toString())
                        .putInt(R.string.pref_account_id_key, authenticationResponse.id())
                        .commit();

                startActivity(new Intent(SignUpActivity.this, VerifyActivity.class));
            }

            @Override
            public void failure(RetrofitError error) {
                progressDialog.dismiss();
                Toast.makeText(SignUpActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {
        String message = failedRule.getFailureMessage();

        if (failedView instanceof EditText) {
            failedView.requestFocus();
            ((EditText) failedView).setError(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @DebugLog
    private boolean isACompanyEmail(String email) {
        if (email != null) {
            int atIndex = email.lastIndexOf('@');
            if (atIndex > 0) {
                String host = email.substring(atIndex + 1);
                try {
                    InputStream inputStream = getAssets().open("blacklist.hosts");
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8" );
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.equals(host)) {
                            return false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == request_code_gallery) {
            if (resultCode == RESULT_OK) {
                String imagePath = MediaPickerUtils.processImagePath(data, this);
                if (!TextUtils.isEmpty(imagePath)) {
                    Uri imageUri = Uri.fromFile(new File(imagePath));
                    userImage.setImageURI(imageUri);
                }
            }
        }
    }
}
