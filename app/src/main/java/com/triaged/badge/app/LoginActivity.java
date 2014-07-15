package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.triaged.badge.data.Contact;

/**
 * Activity for authentication.
 *
 * Created by Will on 7/7/14.
 */
public class LoginActivity extends BadgeActivity {


    private BroadcastReceiver dataSyncedListener;
    private EditText loginEmail = null;
    private EditText loginPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPassword = (EditText) findViewById(R.id.login_password);
        final Button loginButton = (Button) findViewById(R.id.login_button);

        final DataProviderService.LoginCallback loginCallback = new DataProviderService.LoginCallback() {
            @Override
            public void loginFailed(String reason) {
                loginButton.setEnabled( true );
                // TODO surface in designed error state UI.
                Toast.makeText( LoginActivity.this, reason, Toast.LENGTH_SHORT ).show();
            }

            @Override
            public void loginSuccess( Contact user ) {
                Intent activityIntent = new Intent( LoginActivity.this, WelcomeActivity.class );
                activityIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity(activityIntent);
                finish();
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.setEnabled( false );
                String email = loginEmail.getText().toString();
                String password = loginPassword.getText().toString();
                ((BadgeApplication)getApplication()).dataProviderServiceBinding.loginAsync( email, password, loginCallback);
//                Toast.makeText(LoginActivity.this, email, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void logout() {
        // Do nothing since we're the UI to log back in.
    }
}
