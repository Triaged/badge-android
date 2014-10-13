package com.triaged.badge.ui.profile;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.home.ProfileFragment;

public class ProfileActivity extends BadgeActivity {

    public static final String PROFILE_ID_EXTRA = "profile_id_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (savedInstanceState == null) {
            int profileId = getIntent().getIntExtra(PROFILE_ID_EXTRA, -1);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, ProfileFragment.newInstance(profileId, true))
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
