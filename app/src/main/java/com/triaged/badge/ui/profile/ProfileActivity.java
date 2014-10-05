package com.triaged.badge.ui.profile;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.home.ProfileFragment;

public class ProfileActivity extends Activity {

    public static final String PROFILE_ID_EXTRA = "profile_id_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (savedInstanceState == null) {
            int profileId = getIntent().getIntExtra(PROFILE_ID_EXTRA, -1);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, ProfileFragment.newInstance(profileId))
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
}
