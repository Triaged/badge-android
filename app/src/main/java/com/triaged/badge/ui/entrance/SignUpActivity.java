package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
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
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.models.Account;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SignUpActivity extends Activity implements Validator.ValidationListener  {

    Validator validator;
    ProgressDialog progressDialog;

    @InjectView(R.id.root_view) View rootView;
    @Required(order = 1) @InjectView(R.id.first_name_edit_text) EditText firstNameView;
    @Required(order = 2) @InjectView(R.id.last_name_edit_text) EditText lastNameView;
    @Required(order = 3) @Email(order = 4, messageResId = R.string.invalid_email_error)
    @InjectView(R.id.email_edit_text) EditText emailView;
    @Password(order = 5, messageResId = R.string.invalid_password_error)
    @InjectView(R.id.password_edit_text) EditText passwordView;
    @InjectView(R.id.phone_edit_text) EditText phoneView;
    @InjectView(R.id.user_image) RoundedImageView userImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.inject(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);

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

        JsonObject registrationParams = new JsonObject();
        registrationParams.addProperty("first_name", firstNameView.getText().toString());
        registrationParams.addProperty("last_name", lastNameView.getText().toString());
        registrationParams.addProperty("email", emailView.getText().toString());
        registrationParams.addProperty("password", passwordView.getText().toString());
        registrationParams.addProperty("phone_number", phoneView.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("registration", registrationParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().signUp(typedJsonString, new Callback<Account>() {
            @Override
            public void success(Account account, Response response) {
                getContentResolver().insert(UserProvider.CONTENT_URI, UserHelper.toContentValue(account.getCurrentUser()));

                progressDialog.dismiss();
                // Store account info in shared preferences.
                SharedPreferencesHelper.instance()
                        .putBoolean(R.string.pref_is_a_company_email_key, SignUpActivity.isACompanyEmail(account.getCurrentUser().getEmail()))
                        .putString(R.string.pref_account_email_key, account.getCurrentUser().getEmail())
                        .putInt(R.string.pref_account_id_key, account.getId())
                        .putString(R.string.pref_api_token, account.getAuthenticationToken())
                        .putString(R.string.pref_account_company_id_key, account.getCompanyId())
                        .commit();

                EventBus.getDefault().post(new LogedinSuccessfully());
                GeneralUtils.dismissKeyboard(SignUpActivity.this);
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                progressDialog.dismiss();
                // 422 error if exits already
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
    public static boolean isACompanyEmail(String email) {
        if (email != null) {
            int atIndex = email.lastIndexOf('@');
            if (atIndex > 0) {
                String host = email.substring(atIndex + 1);
                try {
                    InputStream inputStream = App.context().getAssets().open("blacklist.hosts");
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

}
