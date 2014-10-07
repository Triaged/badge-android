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


    @OnClick(R.id.entrance_sign_up)
    void openSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
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
    }

    @Override
    protected void onResume() {
        isLoggedInAlready();
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

}
