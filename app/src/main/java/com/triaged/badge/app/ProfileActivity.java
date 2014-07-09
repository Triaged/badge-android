package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ProfileContactInfoView;
import com.triaged.badge.app.views.ProfileCurrentLocationView;
import com.triaged.badge.app.views.ProfileManagesAdapter;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;

/**
 * Created by Will on 7/7/14.
 */
public class ProfileActivity extends BadgeActivity implements ActionBar.TabListener {

    private static final String LOG = ProfileActivity.class.getName();

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private ServiceConnection dataProviderServiceConnnection = null;
    private Contact contact = null;
    private TextView profileName = null;
    private TextView profileTitle = null;
    private ImageView profileImage = null;
    private ProfileContactInfoView emailView = null;
    private ProfileContactInfoView officePhoneView = null;
    private ProfileContactInfoView cellPhoneView = null;
    private ProfileContactInfoView birthDateView = null;
    private ProfileContactInfoView startDateView = null;
    private ProfileCurrentLocationView currentLocationView = null;

    private ListView manangesListView = null;
    private ProfileManagesAdapter managesAdapter;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IF MY PROFILE
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_unselected).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_unselected).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_selected).setTabListener(this), true);

        setContentView(R.layout.activity_profile);
        profileName = (TextView) findViewById(R.id.profile_name);
        profileTitle = (TextView) findViewById(R.id.profile_title);
        emailView = (ProfileContactInfoView) findViewById(R.id.profile_email);
        officePhoneView = (ProfileContactInfoView) findViewById(R.id.profile_office_phone);
        cellPhoneView = (ProfileContactInfoView) findViewById(R.id.profile_cell_phone);
        birthDateView = (ProfileContactInfoView) findViewById(R.id.profile_birth_date);
        startDateView = (ProfileContactInfoView) findViewById(R.id.profile_start_date);
        currentLocationView = (ProfileCurrentLocationView) findViewById(R.id.profile_current_location);
        profileImage = (ImageView)findViewById( R.id.profile_image );
        manangesListView = (ListView) findViewById(R.id.manages_list);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        Intent intent = getIntent();
        int id = intent.getIntExtra("PROFILE_ID", 0);
        contact = dataProviderServiceBinding.getContact(id);

    }

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
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 0) {
              // tab.setIcon(R.drawable.messages_selected);
//            Intent intent = new Intent(ProfileActivity.this, ContactsActivity.class);
//            startActivity(intent);
        } else if (tab.getPosition() == 1) {
            tab.setIcon(R.drawable.contacts_selected);
            Intent intent = new Intent(ProfileActivity.this, ContactsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 2) {
            tab.setIcon(R.drawable.profile_unselected);
            Intent intent = new Intent(ProfileActivity.this, ContactsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    private void setupProfile() {
        manangesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", managesAdapter.getCachedContact(position).id);
                startActivity(intent);
            }
        });
        dataProviderServiceBinding.setLargeContactImage( contact, profileImage );
        if (contact != null) {
            profileName.setText(contact.name);
            profileTitle.setText(contact.jobTitle);
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
            managesAdapter = new ProfileManagesAdapter(getBaseContext(), managedContactsCursor, dataProviderServiceBinding);
            manangesListView.setAdapter(managesAdapter);
            currentLocationView.isOn = false;
            currentLocationView.primaryValue = "Unavailable";
            currentLocationView.invalidate();
        }
    }
}
