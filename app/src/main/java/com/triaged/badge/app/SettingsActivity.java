package com.triaged.badge.app;

import android.os.Bundle;

/**
 * User settings view
 *
 * Created by Will on 7/10/14.
 */
public class SettingsActivity extends BadgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

}
