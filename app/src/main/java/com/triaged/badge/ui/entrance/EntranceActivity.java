package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.triaged.badge.app.R;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.utils.SharedPreferencesHelper;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class EntranceActivity extends Activity {

    private static final int SIGN_UP_REQUEST = 1000;

    @OnClick(R.id.entrance_sign_up)
    void openSignUp() {
        startActivityForResult(new Intent(this, SignUpActivity.class), SIGN_UP_REQUEST);
    }

    @OnClick(R.id.entrance_login)
    void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        ButterKnife.inject(this);

        if (!TextUtils.isEmpty(SharedPreferencesHelper.instance().getString(R.string.pref_api_token, ""))) {
            EventBus.getDefault().post(new LogedinSuccessfully());
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SIGN_UP_REQUEST) {
            if (resultCode == RESULT_OK) {
                startActivity(new Intent(this, OnboardingInviteColleagueActivity.class));
                finish();
            }
        }
    }
}
