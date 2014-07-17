package com.triaged.badge.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import com.triaged.badge.app.views.OnboardingDotsView;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

/**
 * Subclass used only to edit location after onboarding complete.
 *
 * Created by Will on 7/16/14.
 */
public class EditLocationActivity extends OnboardingLocationActivity {

    public static final String OFFICE_NAME_EXTRA = "office_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.setVisibility(View.GONE);

        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();
        if (loggedInUser.officeName == null || loggedInUser.officeName.equals("")) {
            noLocationCheck.setVisibility(View.VISIBLE);
        } else {
            officeLocationsAdapter.usersOffice = loggedInUser.primaryOfficeLocationId;
            officeLocationsAdapter.usersOfficeName = loggedInUser.officeName;
            officeLocationsAdapter.refresh();
        }
    }

    @Override
    protected void onContinue() {
        Intent intent = new Intent(EditLocationActivity.this, EditProfileActivity.class);
        intent.putExtra( OFFICE_NAME_EXTRA, officeLocationsAdapter.usersOfficeName );
        setResult(officeLocationsAdapter.usersOffice, intent);
        finish();
    }
}
