package com.triaged.badge.app;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by Will on 8/1/14.
 */
public class ForgotPasswordActivity extends BackButtonActivity {

    private EditText resetEmail = null;
    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private TextView resetTitle = null;
    private TextView resetInfo = null;
    private Button resetButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        backButton.setText("Login");
        resetEmail = (EditText) findViewById(R.id.reset_email);
        resetTitle = (TextView) findViewById(R.id.reset_title);
        resetInfo = (TextView) findViewById(R.id.reset_info);
        resetButton = (Button) findViewById(R.id.reset_button);

        resetEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    resetButton.performClick();
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
