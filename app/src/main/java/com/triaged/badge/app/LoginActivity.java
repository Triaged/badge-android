package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.triaged.badge.app.views.EditTextWithFont;

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

        final DataProviderService.LoginFailedCallback loginFailedCallback = new DataProviderService.LoginFailedCallback() {
            @Override
            public void loginFailed(String reason) {
                loginButton.setEnabled( true );
                // TODO surface in designed error state UI.
                Toast.makeText( LoginActivity.this, reason, Toast.LENGTH_SHORT ).show();
            }
        };

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.setEnabled( false );
                String email = loginEmail.getText().toString();
                String password = loginPassword.getText().toString();
                ((BadgeApplication)getApplication()).dataProviderServiceBinding.loginAsync( email, password, loginFailedCallback );
//                Toast.makeText(LoginActivity.this, email, Toast.LENGTH_SHORT).show();
            }
        });

//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Intent intent = new Intent(LoginActivity.this, ContactsActivity.class);
//                startActivity(intent);
//            }
//        }, 2000);

        dataSyncedListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO change this to the onboard flow once it's ready.
                Intent activityIntent = new Intent( LoginActivity.this, ContactsActivity.class );
                activityIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity(activityIntent);
                finish();
            }
        };

        // TODO This should actually be the sync finished action.
        localBroadcastManager.registerReceiver( dataSyncedListener, new IntentFilter( DataProviderService.CONTACTS_AVAILABLE_ACTION ));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver( dataSyncedListener );
    }

    @Override
    protected void logout() {
        // Do nothing since we're the UI to log back in.
    }
}
