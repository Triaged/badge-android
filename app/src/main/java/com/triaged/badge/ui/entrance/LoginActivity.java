package com.triaged.badge.ui.entrance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.User;
import com.triaged.badge.net.FayeService;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.MixpanelActivity;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;

/**
 * Activity for authentication.
 * <p/>
 * Created by Will on 7/7/14.
 */
public class LoginActivity extends MixpanelActivity {

    @InjectView(R.id.login_email) EditText loginEmail;
    @InjectView(R.id.login_password) EditText loginPassword;
    @InjectView(R.id.login_button) Button loginButton;

    @InjectView(R.id.login_title) TextView loginTitle;
    @InjectView(R.id.login_info) TextView loginInfo;
    @InjectView(R.id.activity_root) RelativeLayout rootView;
    @InjectView(R.id.sign_up_for_account) TextView singUpButton;

    @OnClick(R.id.login_button)
    void tryToLogin() {
        loginButton.setEnabled(false);

        JSONObject postData = new JSONObject();
        JSONObject credentials = new JSONObject();
        try {
            credentials.put("email", loginEmail.getText().toString());
            credentials.put("password", loginPassword.getText().toString());
            postData.put("user_login", credentials);
        } catch (JSONException e) {
            App.gLogger.e("JSON exception creating post body for login", e);
        }
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.prepare(App.restAdapterMessaging, App.restAdapter);
        RestService.instance().badge().login(typedJsonString, new Callback<Account>() {
            @Override
            public void success(Account account, retrofit.client.Response response) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                sharedPreferences.edit()
                        .putString(getString(R.string.pref_account_company_id_key), account.getCompanyId())
                        .putString(getString(R.string.pref_api_token), account.getAuthenticationToken())
//                                    putString(COMPANY_NAME_PREFS_KEY, account.getString("company_name")).
                        .putInt(getString(R.string.pref_account_id_key), account.getId()).commit();

                App.dataProviderServiceBinding.setLoggedInUser(account.getCurrentUser());
                EventBus.getDefault().post(new LogedinSuccessfully());

                // register mixpanel
                MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(LoginActivity.this, App.MIXPANEL_TOKEN);
                mixpanelAPI.registerSuperProperties(constructMixpanelSuperProperties(account.getCurrentUser()));

                startService(new Intent(LoginActivity.this, FayeService.class));

                // Now seems like a good time to make sure we have a GCM id since
                // we know the network was working at least well enough to log the user in.
//                ensureGcmRegistration();


                mixpanel.track("login", new JSONObject());

                LocationTrackingService.scheduleAlarm(LoginActivity.this);

                Intent activityIntent = new Intent(LoginActivity.this, WelcomeActivity.class);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                loginButton.setEnabled(true);
                // TODO surface in designed error state UI.
                if (error.getMessage() != null) {
                    Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @OnClick(R.id.forgot_password_button)
    void openForgetPassword() {
        Intent resetPasswordIntent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(resetPasswordIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @OnClick(R.id.sign_up_for_account)
    void openSignUpPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.badge.co"));
        startActivity(browserIntent);
    }

    @OnEditorAction(R.id.login_password)
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            tryToLogin();
        }
        return false;
    }


    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already logged-in, go to the main activity.
        if (isAlreadyLoggedIn()) {
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            startActivity(mainActivityIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        setupLayoutChangeListener();
    }

    private void setupLayoutChangeListener() {
        densityMultiplier = getResources().getDisplayMetrics().density;

        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = activityRootView.getRootView().getHeight();

                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                activityRootView.getWindowVisibleDisplayFrame(r);

                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75)) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) loginTitle.getLayoutParams();
                    lp.setMargins(0, (int) (15 * densityMultiplier), 0, 0);
                    loginTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) loginInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (15 * densityMultiplier), 0, (int) (15 * densityMultiplier));
                    loginInfo.setLayoutParams(lp2);
                    ScrollView.MarginLayoutParams scrollParams = (ScrollView.MarginLayoutParams) rootView.getLayoutParams();
                    scrollParams.setMargins(0, 0, 0, 0);
                    rootView.setLayoutParams(scrollParams);
                    singUpButton.setVisibility(View.GONE);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) loginTitle.getLayoutParams();
                    lp.setMargins(0, (int) (100 * densityMultiplier), 0, 0);
                    loginTitle.setLayoutParams(lp);
                    RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) loginInfo.getLayoutParams();
                    lp2.setMargins(0, (int) (30 * densityMultiplier), 0, (int) (68 * densityMultiplier));
                    loginInfo.setLayoutParams(lp2);
                    ScrollView.MarginLayoutParams scrollParams = (ScrollView.MarginLayoutParams) rootView.getLayoutParams();
                    scrollParams.setMargins(0, 0, 0, (int) (40 * densityMultiplier));
                    rootView.setLayoutParams(scrollParams);
                    singUpButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean isAlreadyLoggedIn() {
        return SharedPreferencesUtil.getBoolean(R.string.pref_has_fetch_company, false);
    }

    protected JSONObject constructMixpanelSuperProperties(User user) {
        JSONObject mixpanelData = new JSONObject();
        try {
            mixpanelData.put("firstName", user.getFirstName());
            mixpanelData.put("lastName", user.getLastName());
            mixpanelData.put("email", user.getEmail());
            mixpanelData.put("company.identifier",
                    SharedPreferencesUtil.getString(R.string.pref_account_company_id_key, ""));
            return mixpanelData;
        } catch (JSONException e) {
            Log.w("LoginActivity", "Couldn't construct mix panel super property json");
        }
        return new JSONObject();
    }
}
