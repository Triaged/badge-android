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


    boolean waitingForResult = false;
    private int requestCodeEnter = 100;

    @OnClick(R.id.entrance_sign_up)
    void openSignUp() {
        waitingForResult = true;
        startActivityForResult(new Intent(this, SignUpActivity.class), requestCodeEnter);
    }

    @OnClick(R.id.entrance_login)
    void openLogin() {
        waitingForResult = true;
        startActivityForResult(new Intent(this, LoginActivity.class), requestCodeEnter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        ButterKnife.inject(this);
    }

    @Override
    protected void onResume() {
        if (!waitingForResult) {
            isLoggedInAlready();
        }
        super.onResume();
    }

    private void isLoggedInAlready() {
        if (!TextUtils.isEmpty(SharedPreferencesHelper.instance().getString(R.string.pref_api_token, ""))) {
            EventBus.getDefault().post(new LogedinSuccessfully());
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == requestCodeEnter) {
            startActivity(new Intent(this, OnboardingInviteColleagueActivity.class));
            finish();
        }

    }

}
