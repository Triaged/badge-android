package com.triaged.badge.app;

import android.app.Activity;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Will on 8/1/14.
 */
public class ForgotPasswordActivity extends BackButtonActivity {

    private static final String LOG_TAG = ForgotPasswordActivity.class.getName();
    private EditText resetEmail = null;
    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private TextView resetTitle = null;
    private TextView resetInfo = null;
    private Button resetButton = null;

    private DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess(int newId) {
            Toast.makeText(ForgotPasswordActivity.this, "Check your email to reset your password.", Toast.LENGTH_LONG).show();
            onBackPressed();
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText(ForgotPasswordActivity.this, reason, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        backButton.setText("Login");
        resetEmail = (EditText) findViewById(R.id.reset_email);
        resetTitle = (TextView) findViewById(R.id.reset_title);
        resetInfo = (TextView) findViewById(R.id.reset_info);
        resetButton = (Button) findViewById(R.id.reset_button);
        densityMultiplier = getResources().getDisplayMetrics().density;

        resetEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    resetButton.performClick();
                }
                return false;
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = resetEmail.getText().toString();
                if (!email.equals("")) {
                    new RequestResetPasswordTask().execute(email);
                }
            }
        });

        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = activityRootView.getRootView().getHeight();

                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                activityRootView.getWindowVisibleDisplayFrame(r);

                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75) ) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) resetTitle.getLayoutParams();
                    lp.setMargins(0, (int) (15*densityMultiplier), 0, 0);
                    resetTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) resetInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (15*densityMultiplier), 0, (int) (15*densityMultiplier));
                    resetInfo.setLayoutParams(lp2);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) resetTitle.getLayoutParams();
                    lp.setMargins(0, (int) (100*densityMultiplier), 0, 0);
                    resetTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) resetInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (30*densityMultiplier), 0, (int) (88*densityMultiplier));
                    resetInfo.setLayoutParams(lp2);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private class RequestResetPasswordTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            resetButton.setEnabled(false);
        }

        @Override
        protected Void doInBackground(String... params) {
            ((App)getApplication()).dataProviderServiceBinding.requestResetPassword( params[0], saveCallback);
            return null;
        }
    }

    @Override
    protected void logout() {
        // not logged in
    }
}
