package com.triaged.badge.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.triaged.badge.app.R;
import com.triaged.badge.models.Contact;
import com.triaged.badge.ui.base.views.OnboardingDotsView;

/**
 * Subclass used only to edit location after onboarding complete.
 * <p/>
 * Created by Will on 7/16/14.
 */
public class EditLocationActivity extends OnboardingLocationActivity {

    public static final String OFFICE_NAME_EXTRA = "office_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        continueButton.setText("Back");

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.setVisibility(View.GONE);

        ImageView cityscape = (ImageView) findViewById(R.id.cityscape);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) cityscape.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        cityscape.setLayoutParams(lp);
    }

    @Override
    protected void onContinue() {
        Intent intent = new Intent(EditLocationActivity.this, EditProfileActivity.class);
        intent.putExtra(OFFICE_NAME_EXTRA, officeLocationsAdapter.usersOfficeName);
        setResult(officeLocationsAdapter.usersOffice, intent);
        finish();
    }

    @Override
    protected void onDatabaseReady() {
        super.onDatabaseReady();
        Contact loggedInUser = dataProviderServiceBinding.getLoggedInUser();
        if (loggedInUser.officeName == null || loggedInUser.officeName.equals("")) {
            noLocationCheck.setVisibility(View.VISIBLE);
        } else {
            noLocationCheck.setVisibility(View.GONE);
            officeLocationsAdapter.usersOffice = loggedInUser.primaryOfficeLocationId;
            officeLocationsAdapter.usersOfficeName = loggedInUser.officeName;
            officeLocationsAdapter.refresh();
        }
    }
}
