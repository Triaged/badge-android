package com.triaged.badge.ui.profile;

import android.os.Bundle;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.home.NewProfileFragment;

public class ProfileActivity extends BadgeActivity {

    public static final String PROFILE_ID_EXTRA = "profile_id_extra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (savedInstanceState == null) {
            int profileId = getIntent().getIntExtra(PROFILE_ID_EXTRA, -1);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, NewProfileFragment.newInstance(profileId))
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
