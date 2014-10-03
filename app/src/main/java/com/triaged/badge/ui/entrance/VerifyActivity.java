package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.models.Account;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesHelper;

import org.apache.http.HttpStatus;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class VerifyActivity extends Activity implements Validator.ValidationListener{

    Validator validator = new Validator(this);
    ProgressDialog progressDialog;

    @Required(order = 1)
    @InjectView(R.id.verify_code) EditText verifyCodeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        ButterKnife.inject(this);
        validator.setValidationListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.verify, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_next) {
            validator.validate();
            return true;
        } else if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onValidationSucceeded() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Validating");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

        JsonObject authParams = new JsonObject();
        authParams.addProperty("id", SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1));
        authParams.addProperty("challenge_code", verifyCodeEditText.getText().toString());

        JsonObject postData = new JsonObject();
        postData.add("auth_params", authParams);
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().validate(typedJsonString, new Callback<Account>() {
            @Override
            public void success(Account account, Response response) {
                progressDialog.dismiss();
                // TODO: Need to check for null values
                SharedPreferencesHelper.instance()
                        .putString(R.string.pref_api_token, account.getAuthenticationToken())
                        .putString(R.string.pref_account_company_id_key, account.getCompanyId())
                        .commit();

                getContentResolver().insert(UserProvider.CONTENT_URI, UserHelper.toContentValue(account.getCurrentUser()));
                EventBus.getDefault().post(new LogedinSuccessfully());

                GeneralUtils.dismissKeyboard(VerifyActivity.this);
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                if (error.getResponse().getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                    App.toast("Wrong code.");
                } else {
                    App.toast("Something went wrong.");
                }
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {

    }
}
