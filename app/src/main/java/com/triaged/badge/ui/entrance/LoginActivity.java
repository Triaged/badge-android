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
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.models.Account;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.responses.AuthenticationResponse;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.MediaPickerUtils;
import com.triaged.utils.SharedPreferencesHelper;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity implements Validator.ValidationListener {

    @Required(order = 3) @Email(order = 4, messageResId = R.string.invalid_email_error)
    @InjectView(R.id.email_edit_text) EditText emailView;
    @Password(order = 5, messageResId = R.string.invalid_password_error)
    @InjectView(R.id.password_edit_text) EditText passwordView;

    Validator validator;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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
            progressDialog.setMessage("Logging in");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        JsonObject registrationParams = new JsonObject();
        registrationParams.addProperty("email", emailView.getText().toString());
        registrationParams.addProperty("password", passwordView.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("user_login", registrationParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());

        RestService.instance().badge().login(typedJsonString, new Callback<Account>() {
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

                GeneralUtils.dismissKeyboard(LoginActivity.this);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                progressDialog.dismiss();
                // 422 error if exits already
                Toast.makeText(LoginActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {

    }

}
