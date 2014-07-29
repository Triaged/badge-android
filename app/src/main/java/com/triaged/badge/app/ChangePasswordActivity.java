package com.triaged.badge.app;

import android.app.Activity;
import android.content.Intent;
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

        final DataProviderService.LocalBinding dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;

        changeButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataProviderServiceBinding.changePassword(
                        currentPasswordField.getText().toString(),
                        newPasswordField.getText().toString(),
                        confirmPasswordField.getText().toString(),
                        new DataProviderService.AsyncSaveCallback() {
                            @Override
                            public void saveSuccess(int newId) {
                                Toast.makeText( ChangePasswordActivity.this, "Your password has been changed successfully.", Toast.LENGTH_SHORT ).show();
                                finish();
                            }

                            @Override
                            public void saveFailed(String reason) {
                                Toast.makeText( ChangePasswordActivity.this, reason, Toast.LENGTH_SHORT ).show();
                            }
                        }
                );
            }
        });

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
