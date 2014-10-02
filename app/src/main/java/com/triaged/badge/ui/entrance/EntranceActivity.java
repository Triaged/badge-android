package com.triaged.badge.ui.entrance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.triaged.badge.app.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

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

}
