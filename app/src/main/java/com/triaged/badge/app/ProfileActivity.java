package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Will on 7/7/14.
 */
public class ProfileActivity extends BadgeActivity {

    private static final String LOG = ProfileActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Intent intent = getIntent();
        int id = intent.getIntExtra("PROFILE_ID", 0);
        Log.d(LOG, String.valueOf(id));
    }

}
