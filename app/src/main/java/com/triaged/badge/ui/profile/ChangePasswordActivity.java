package com.triaged.badge.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.squareup.okhttp.Response;
import com.triaged.badge.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.net.DataProviderService;
import com.triaged.badge.net.api.AccountApi;
import com.triaged.badge.ui.base.BackButtonActivity;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Activity with 3 fields for current password,
 * new password, and confirm new password.
 *
 * @author Created by jc on 7/21/14.
 */
public class ChangePasswordActivity extends BackButtonActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_change_password);

        final EditText currentPasswordField = (EditText) findViewById(R.id.current_password);
        final EditText newPasswordField = (EditText) findViewById(R.id.new_password);
        final EditText confirmPasswordField = (EditText) findViewById(R.id.confirm_password);
        final Button changeButton = (Button) findViewById(R.id.change_password_button);

        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPasswordText = currentPasswordField.getText().toString();
                String newPasswordText = newPasswordField.getText().toString();
                String confirmPasswordText = confirmPasswordField.getText().toString();

                if (TextUtils.isEmpty(currentPasswordText)
                        || TextUtils.isEmpty(newPasswordText)
                        || TextUtils.isEmpty(confirmPasswordText)) {

                    Toast.makeText(ChangePasswordActivity.this, "Please fill in all fields to change password", Toast.LENGTH_SHORT).show();
                    return;
                }


                JSONObject postBody = new JSONObject();
                try {
                    JSONObject user = new JSONObject();
                    postBody.put("user", user);
                    user.put("current_password", currentPasswordText);
                    user.put("password", newPasswordText);
                    user.put("password_confirmation", confirmPasswordText);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for change password", e);
                    return;
                }
                TypedJsonString typedJsonString = new TypedJsonString(postBody.toString());
                App.restAdapter.create(AccountApi.class).changePassword(typedJsonString, new Callback<Response>() {
                    @Override
                    public void success(Response response, retrofit.client.Response response2) {
                        Toast.makeText(ChangePasswordActivity.this, "Your password has been changed successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Toast.makeText(ChangePasswordActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDatabaseReady() {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
