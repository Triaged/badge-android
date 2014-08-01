package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.data.Contact;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Activity for authentication.
 *
 * Created by Will on 7/7/14.
 */
public class LoginActivity extends BadgeActivity {


    private static final String LOG_TAG = LoginActivity.class.getName();
    private BroadcastReceiver dataSyncedListener;
    private EditText loginEmail = null;
    private EditText loginPassword = null;
    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private TextView loginTitle = null;
    private TextView loginInfo = null;

    private TextView forgotPasswordButton = null;
    private TextView signupForBeta = null;

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
                // Now seems like a good time to make sure we have a GCM id since
                // we know the network was working at least well enough to log the user in.

                mixpanel.track("login", new JSONObject( ) );

                ensureGcmRegistration();
                startService( new Intent( LoginActivity.this, LocationTrackingService.class ) );


                Intent activityIntent = new Intent( LoginActivity.this, WelcomeActivity.class );
                activityIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity(activityIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

        loginPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick();
                }
                return false;
            }
        });

        densityMultiplier = getResources().getDisplayMetrics().density;
        loginTitle = (TextView) findViewById(R.id.login_title);
        loginInfo = (TextView) findViewById(R.id.login_info);

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
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) loginTitle.getLayoutParams();
                    lp.setMargins(0, (int) (15*densityMultiplier), 0, 0);
                    loginTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) loginInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (15*densityMultiplier), 0, (int) (15*densityMultiplier));
                    loginInfo.setLayoutParams(lp2);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) loginTitle.getLayoutParams();
                    lp.setMargins(0, (int) (100*densityMultiplier), 0, 0);
                    loginTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) loginInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (30*densityMultiplier), 0, (int) (88*densityMultiplier));
                    loginInfo.setLayoutParams(lp2);
                }
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resetPasswordIntent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(resetPasswordIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        signupForBeta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.badge.co"));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void logout() {
        // Do nothing since we're the UI to log back in.
    }
}
