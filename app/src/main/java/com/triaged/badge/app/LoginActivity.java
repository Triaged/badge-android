package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
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

    private EditText loginEmail = null;
    private EditText loginPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPassword = (EditText) findViewById(R.id.login_password);
        Button loginButton = (Button) findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = loginEmail.getText().toString();
                String password = loginPassword.getText().toString();



                Toast.makeText(LoginActivity.this, email, Toast.LENGTH_SHORT).show();
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
    }

}
