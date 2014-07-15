package com.triaged.badge.app;

import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ButtonWithFont;
import com.triaged.badge.app.views.ProfileContactInfoView;
import com.triaged.badge.app.views.ProfileCurrentLocationView;
import com.triaged.badge.app.views.ProfileManagesAdapter;
import com.triaged.badge.data.Contact;

/**
 * Generic abstract class for my own profile and other profiles
 *
 * Created by Will on 7/7/14.
 */
public abstract class AbstractProfileActivity extends BadgeActivity  {

    private static final String LOG = AbstractProfileActivity.class.getName();

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private ServiceConnection dataProviderServiceConnnection = null;
    protected Contact contact = null;
    private TextView profileName = null;
    private TextView profileTitle = null;
    private ImageView profileImage = null;
    private ButtonWithFont departmentView = null;
    private ProfileContactInfoView emailView = null;
    private ProfileContactInfoView officePhoneView = null;
    private ProfileContactInfoView cellPhoneView = null;
    private ProfileContactInfoView birthDateView = null;
    private ProfileContactInfoView startDateView = null;
    private ProfileCurrentLocationView currentLocationView = null;
    private TextView managesHeader = null;

    private ListView manangesListView = null;
    private ProfileManagesAdapter managesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewLayout();

        profileName = (TextView) findViewById(R.id.profile_name);
        profileTitle = (TextView) findViewById(R.id.profile_title);
        departmentView = (ButtonWithFont) findViewById(R.id.profile_department);
        emailView = (ProfileContactInfoView) findViewById(R.id.profile_email);
        officePhoneView = (ProfileContactInfoView) findViewById(R.id.profile_office_phone);
        cellPhoneView = (ProfileContactInfoView) findViewById(R.id.profile_cell_phone);
        birthDateView = (ProfileContactInfoView) findViewById(R.id.profile_birth_date);
        startDateView = (ProfileContactInfoView) findViewById(R.id.profile_start_date);
        currentLocationView = (ProfileCurrentLocationView) findViewById(R.id.profile_current_location);
        profileImage = (ImageView)findViewById( R.id.profile_image );
        manangesListView = (ListView) findViewById(R.id.manages_list);
        managesHeader = (TextView) findViewById(R.id.profile_heading_manages);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        final Intent intent = getIntent();
        int id = intent.getIntExtra("PROFILE_ID", 0);
        contact = dataProviderServiceBinding.getContact(id);

        departmentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent departmentIntent = new Intent(AbstractProfileActivity.this, ContactsActivity.class);
//                startActivity(intent);
            }
        });

    }

    /**
     * Subclasses must set content view
     */
    protected abstract void setContentViewLayout();

    @Override
    protected void onStart() {
        super.onStart();
        setupProfile();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int id = intent.getIntExtra("PROFILE_ID", 0);
        contact = dataProviderServiceBinding.getContact(id);
        setupProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( managesAdapter != null ) {
            managesAdapter.destroy();
        }
    }

    /**
     * Repopulates profile information on creation and on new intent.
     *
     */
    protected void setupProfile() {
        manangesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int userId = dataProviderServiceBinding.getLoggedInUser().id;
                int clickedId = managesAdapter.getCachedContact(position).id;
                Intent intent;
                if (userId == clickedId) {
                    intent = new Intent(AbstractProfileActivity.this, MyProfileActivity.class);
                } else {
                    intent = new Intent(AbstractProfileActivity.this, OtherProfileActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", clickedId);
                startActivity(intent);
            }
        });
        dataProviderServiceBinding.setLargeContactImage( contact, profileImage );
        if (contact != null) {
            profileName.setText(contact.name);
            profileTitle.setText(contact.jobTitle);
            if (contact.departmentName != null) {
                departmentView.setVisibility(View.VISIBLE);
                departmentView.setText(contact.departmentName);
            }
            if (contact.email != null) {
                emailView.setVisibility(View.VISIBLE);
                emailView.primaryValue = contact.email;
                emailView.secondaryValue = "Email";
                emailView.invalidate();
            }
            if (contact.officePhone != null) {
                officePhoneView.setVisibility(View.VISIBLE);
                officePhoneView.primaryValue = contact.officePhone;
                officePhoneView.secondaryValue = "Office";
                officePhoneView.invalidate();
            }
            if (contact.cellPhone != null) {
                cellPhoneView.setVisibility(View.VISIBLE);
                cellPhoneView.primaryValue = contact.cellPhone;
                cellPhoneView.secondaryValue = "Mobile";
                cellPhoneView.invalidate();
            }
            if (contact.birthDateString != null) {
                birthDateView.setVisibility(View.VISIBLE);
                birthDateView.primaryValue = contact.birthDateString;
                birthDateView.secondaryValue = "Birthday";
                birthDateView.invalidate();
            }
            if (contact.startDateString != null) {
                startDateView.setVisibility(View.VISIBLE);
                startDateView.primaryValue = contact.startDateString;
                startDateView.secondaryValue = "Start Date";
                startDateView.invalidate();
            }
            Cursor managedContactsCursor = dataProviderServiceBinding.getContactsManaged(contact.id);
            if (managedContactsCursor.getCount() > 0) {
                managesHeader.setVisibility(View.VISIBLE);
            } else {
                managesHeader.setVisibility(View.GONE);
            }
            managesAdapter = new ProfileManagesAdapter(getBaseContext(), managedContactsCursor, dataProviderServiceBinding);
            manangesListView.setAdapter(managesAdapter);
            currentLocationView.isOn = false;
            currentLocationView.primaryValue = "Unavailable";
            currentLocationView.invalidate();
        }
    }
}
