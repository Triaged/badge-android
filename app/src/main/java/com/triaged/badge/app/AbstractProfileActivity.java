package com.triaged.badge.app;

import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
    private TextView missingProfileImage = null;
    private ButtonWithFont departmentView = null;
    private ProfileContactInfoView emailView = null;
    private ProfileContactInfoView officePhoneView = null;
    private ProfileContactInfoView cellPhoneView = null;
    private ProfileContactInfoView birthDateView = null;
    private ProfileContactInfoView primaryOfficeView = null;
    private ProfileContactInfoView startDateView = null;
    private ProfileCurrentLocationView currentLocationView = null;
    private TextView managesHeader = null;

    private ListView managesListView = null;
    private ProfileManagesAdapter managesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;


        final Intent intent = getIntent();
        int id = intent.getIntExtra("PROFILE_ID", 0);
        contact = dataProviderServiceBinding.getContact(id);
        if( dataProviderServiceBinding.getLoggedInUser().id == contact.id ) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR);
        }

        setContentViewLayout();

        profileName = (TextView) findViewById(R.id.profile_name);
        profileTitle = (TextView) findViewById(R.id.profile_title);
        departmentView = (ButtonWithFont) findViewById(R.id.profile_department);
        emailView = (ProfileContactInfoView) findViewById(R.id.profile_email);
        officePhoneView = (ProfileContactInfoView) findViewById(R.id.profile_office_phone);
        primaryOfficeView = (ProfileContactInfoView) findViewById( R.id.profile_primary_office );
        cellPhoneView = (ProfileContactInfoView) findViewById(R.id.profile_cell_phone);
        birthDateView = (ProfileContactInfoView) findViewById(R.id.profile_birth_date);
        startDateView = (ProfileContactInfoView) findViewById(R.id.profile_start_date);
        currentLocationView = (ProfileCurrentLocationView) findViewById(R.id.profile_current_location);
        profileImage = (ImageView)findViewById( R.id.profile_image );
        missingProfileImage = (TextView) findViewById(R.id.missing_profile_image);
        managesListView = (ListView) findViewById(R.id.manages_list);
        managesHeader = (TextView) findViewById(R.id.profile_heading_manages);


        departmentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent departmentIntent = new Intent(AbstractProfileActivity.this, ContactsActivity.class);
//                startActivity(intent);
            }
        });

        Cursor reportsCursor = getNewManagesContactsCursor();
        managesAdapter = new ProfileManagesAdapter(getBaseContext(), reportsCursor, dataProviderServiceBinding);
        managesListView.setAdapter( managesAdapter );
        managesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        Cursor reportsCursor = getNewManagesContactsCursor();
        managesAdapter.changeCursor( reportsCursor );
        managesAdapter.notifyDataSetChanged();
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

    protected Cursor getNewManagesContactsCursor() {
        Cursor managedContactsCursor = dataProviderServiceBinding.getContactsManaged(contact.id);
        if (managedContactsCursor.getCount() > 0) {
            managesHeader.setVisibility(View.VISIBLE);
        } else {
            managesHeader.setVisibility(View.GONE);
        }
        return managedContactsCursor;
    }

    /**
     * Repopulates profile information on creation and on new intent.
     *
     */
    protected void setupProfile() {
        if (contact != null) {
            if (contact.avatarUrl != null) {
                profileImage.setVisibility(View.VISIBLE);
                missingProfileImage.setVisibility(View.GONE);
                dataProviderServiceBinding.setLargeContactImage( contact, profileImage );
            } else {
                missingProfileImage.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.GONE);
                missingProfileImage.setText(contact.initials);
            }
            profileName.setText(contact.name);
            profileTitle.setText(contact.jobTitle);
            if ( isNotBlank( contact.departmentName ) ) {
                departmentView.setVisibility(View.VISIBLE);
                departmentView.setText(contact.departmentName);
            }
            else {
                departmentView.setVisibility(View.GONE);
            }
            if (isNotBlank( contact.email ) ) {
                emailView.setVisibility(View.VISIBLE);
                emailView.primaryValue = contact.email;
                emailView.secondaryValue = "Email";
                emailView.invalidate();
            }
            else {
                emailView.setVisibility( View.GONE );
            }
            if (isNotBlank( contact.officePhone )) {
                officePhoneView.setVisibility(View.VISIBLE);
                officePhoneView.primaryValue = contact.officePhone;
                officePhoneView.secondaryValue = "Office";
                officePhoneView.invalidate();
            }
            else {
                officePhoneView.setVisibility(View.GONE );
            }

            if ( isNotBlank( contact.cellPhone ) ) {
                cellPhoneView.setVisibility(View.VISIBLE);
                cellPhoneView.primaryValue = contact.cellPhone;
                cellPhoneView.secondaryValue = "Mobile";
                cellPhoneView.invalidate();
            }
            else {
                cellPhoneView.setVisibility(View.GONE);
            }

            if ( isNotBlank( contact.birthDateString ) ) {
                birthDateView.setVisibility(View.VISIBLE);
                birthDateView.primaryValue = contact.birthDateString;
                birthDateView.secondaryValue = "Birthday";
                birthDateView.invalidate();
            }
            else {
                birthDateView.setVisibility(View.GONE);
            }

            if ( isNotBlank( contact.startDateString ) ) {
                startDateView.setVisibility(View.VISIBLE);
                startDateView.primaryValue = contact.startDateString;
                startDateView.secondaryValue = "Start Date";
                startDateView.invalidate();
            }
            else {
                startDateView.setVisibility(View.GONE);
            }

            if( isNotBlank( contact.officeName ) ) {
                primaryOfficeView.primaryValue = contact.officeName;
                primaryOfficeView.secondaryValue = "Primary Office";
                primaryOfficeView.setVisibility(View.VISIBLE);
                primaryOfficeView.invalidate();
            }
            else {
                primaryOfficeView.setVisibility(View.GONE);
            }

            currentLocationView.isOn = false;
            currentLocationView.primaryValue = "Unavailable";
            currentLocationView.invalidate();
        }
    }

    private static boolean isNotBlank( String str ) {
        return str != null && !str.isEmpty();
    }
}
