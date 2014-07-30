package com.triaged.badge.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.DepartmentsAdapter;
import com.triaged.badge.app.views.OfficeLocationsAdapter;
import com.triaged.badge.app.views.OnboardingDotsView;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import org.w3c.dom.Text;

/**
 * Allow the user to select a primary office location.
 *
 * Created by Will on 7/10/14.
 */
public class OnboardingLocationActivity extends BadgeActivity {

    protected static final int ADD_OFFICE_REQUEST_CODE = 1;

    private Button continueButton = null;
    protected ListView officeLocationsList = null;
    protected ImageView noLocationCheck = null;
    protected OfficeLocationsAdapter officeLocationsAdapter = null;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    protected DataProviderService.AsyncSaveCallback saveCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess(int newId) {
            Intent intent = new Intent(OnboardingLocationActivity.this, ContactsActivity.class);
            startActivity(intent);
            localBroadcastManager.sendBroadcast( new Intent( ONBOARDING_FINISHED_ACTION ) );
            finish();
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText( OnboardingLocationActivity.this, reason, Toast.LENGTH_SHORT ).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_location);

        continueButton = (Button)findViewById( R.id.continue_button );
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContinue();
            }
        });

        officeLocationsList = (ListView) findViewById(R.id.office_locations_list);
        officeLocationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    Cursor officeCursor = (Cursor) officeLocationsAdapter.getItem(position - 1);
                    officeLocationsAdapter.usersOffice = Contact.getIntSafelyFromCursor(officeCursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID);
                    officeLocationsAdapter.usersOfficeName = Contact.getStringSafelyFromCursor(officeCursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME);
                    officeLocationsAdapter.notifyDataSetChanged();
                    noLocationCheck.setVisibility(View.GONE);
                }
            }
        });

        LayoutInflater inflater = LayoutInflater.from(this);

        RelativeLayout locationHeader = (RelativeLayout) inflater.inflate(R.layout.include_onboarding_location_header, null);
        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) locationHeader.findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 2;
        onboardingDotsView.invalidate();
        officeLocationsList.addHeaderView(locationHeader, null, false);

        RelativeLayout noLocationView = (RelativeLayout) inflater.inflate(R.layout.item_no_location, null);
        noLocationCheck = (ImageView) noLocationView.findViewById(R.id.selected_icon);

        officeLocationsList.addFooterView(noLocationView);
        noLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                officeLocationsAdapter.usersOffice = -1;
                officeLocationsAdapter.usersOfficeName = null;
                officeLocationsAdapter.notifyDataSetChanged();
                noLocationCheck.setVisibility(View.VISIBLE);
                //Toast.makeText(OnboardingLocationActivity.this, "NO LOCATION", Toast.LENGTH_SHORT).show();
            }
        });

        Button addView = (Button) inflater.inflate(R.layout.item_add_new, null);
        addView.setText(getString(R.string.add_new_location));
        officeLocationsList.addFooterView(addView);
        addView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingLocationActivity.this, OnboardingMapActivity.class);
                startActivityForResult(intent, ADD_OFFICE_REQUEST_CODE);
            }
        });

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;

        officeLocationsAdapter = new OfficeLocationsAdapter( this, dataProviderServiceBinding, R.layout.item_office_location);
        officeLocationsList.setAdapter( officeLocationsAdapter );

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        officeLocationsAdapter.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == ADD_OFFICE_REQUEST_CODE && resultCode != RESULT_CANCELED ) {
            officeLocationsAdapter.usersOffice = resultCode;
            officeLocationsAdapter.refresh();
            noLocationCheck.setVisibility(View.GONE);
        }
    }

    /** Called when the "Continue" Button is clicked. Subclasses may override */
    protected void onContinue() {
        dataProviderServiceBinding.savePrimaryLocationASync( officeLocationsAdapter.usersOffice, saveCallback );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}