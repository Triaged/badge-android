package com.triaged.badge.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.DepartmentsAdapter;
import com.triaged.badge.app.views.OfficeLocationsAdapter;
import com.triaged.badge.app.views.OnboardingDotsView;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

/**
 * Created by Will on 7/10/14.
 */
public class OnboardingLocationActivity extends BadgeActivity {

    private Button continueButton = null;
    private ListView officeLocationsList = null;
    private OfficeLocationsAdapter officeLocationsAdapter = null;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_location);

        OnboardingDotsView onboardingDotsView = (OnboardingDotsView) findViewById(R.id.onboarding_dots);
        onboardingDotsView.currentDotIndex = 2;
        onboardingDotsView.invalidate();
        continueButton = (Button)findViewById( R.id.continue_button );
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingLocationActivity.this, ContactsActivity.class);
                startActivity(intent);
            }
        });

        officeLocationsList = (ListView) findViewById(R.id.office_locations_list);
        officeLocationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor officeCursor = (Cursor)officeLocationsAdapter.getItem( position );
                officeLocationsAdapter.usersOffice = Contact.getIntSafelyFromCursor( officeCursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID );
                officeLocationsAdapter.notifyDataSetChanged();
            }
        });

        LayoutInflater inflater = LayoutInflater.from(this);
        final Button noLocationView = (Button) inflater.inflate(R.layout.item_no_location, null);
        noLocationView.setText(getString(R.string.no_office_button));
        officeLocationsList.addFooterView(noLocationView);
        noLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                officeLocationsAdapter.usersOffice = -1;
                officeLocationsAdapter.notifyDataSetChanged();

            }
        });

        Button addView = (Button) inflater.inflate(R.layout.item_add_new, null);
        addView.setText(getString(R.string.add_new_location));
        officeLocationsList.addFooterView(addView);
        addView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingLocationActivity.this, OnboardingMapActivity.class);
                startActivity(intent);
            }
        });

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;

        officeLocationsAdapter = new OfficeLocationsAdapter( this, dataProviderServiceBinding, R.layout.item_office_location);
        officeLocationsList.setAdapter( officeLocationsAdapter );
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        officeLocationsAdapter.destroy();
    }
}
