package com.triaged.badge.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity with 3 fields for current password,
 * new password, and confirm new password.
 *
 * @author Created by jc on 7/21/14.
 */
public class ChangePasswordActivity extends BackButtonActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView( R.layout.activity_change_password );

        final EditText currentPasswordField = (EditText)findViewById( R.id.current_password );
        final EditText newPasswordField = (EditText)findViewById( R.id.new_password );
        final EditText confirmPasswordField = (EditText)findViewById( R.id.confirm_password );
        final Button changeButton = (Button)findViewById( R.id.change_password_button );

        changeButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPasswordText = currentPasswordField.getText().toString();
                String newPasswordText = newPasswordField.getText().toString();
                String confirmPasswordText = confirmPasswordField.getText().toString();
                if (currentPasswordText.equals("") || newPasswordText.equals("") || confirmPasswordText.equals("")) {
                    Toast.makeText(ChangePasswordActivity.this, "Please fill in all fields to change password", Toast.LENGTH_SHORT).show();
                } else {
                    dataProviderServiceBinding.changePassword(
                            currentPasswordText,
                            newPasswordText,
                            confirmPasswordText,
                            new DataProviderService.AsyncSaveCallback() {
                                @Override
                                public void saveSuccess(int newId) {
                                    Toast.makeText(ChangePasswordActivity.this, "Your password has been changed successfully.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void saveFailed(String reason) {
                                    Toast.makeText(ChangePasswordActivity.this, reason, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
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
