package com.triaged.badge.app;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ButtonWithFont;
import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ProfileContactInfoView;
import com.triaged.badge.app.views.ProfileCurrentLocationView;
import com.triaged.badge.app.views.ProfileManagesUserView;
import com.triaged.badge.app.views.ProfileReportsToView;
import com.triaged.badge.data.Contact;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic abstract class for my own profile and other profiles
 *
 * Created by Will on 7/7/14.
 */
public abstract class AbstractProfileActivity extends BadgeActivity  {

    private static final String LOG = AbstractProfileActivity.class.getName();
    public static final String PROFILE_ID_EXTRA = "PROFILE_ID";


    private ServiceConnection dataProviderServiceConnnection = null;
    protected Contact contact = null;
    private int contactId = 0;
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
    private ProfileReportsToView bossView = null;
    private TextView managesHeader = null;
    private TextView bossHeader = null;
    private TextView departmentHeader = null;
    private LayoutInflater inflater = null;
    private int numberManagedByPrevious = 0;

    private ArrayList<Integer> backStackIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        final Intent intent = getIntent();
        contactId = intent.getIntExtra(PROFILE_ID_EXTRA, 0);
        backStackIds = intent.getIntegerArrayListExtra("BACK_STACK_IDS");
        if (backStackIds == null) {
            backStackIds= new ArrayList<Integer>();
        }

        if( dataProviderServiceBinding.getLoggedInUser() != null && dataProviderServiceBinding.getLoggedInUser().id == contactId ) {
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
        managesHeader = (TextView) findViewById(R.id.profile_heading_manages);
        departmentHeader = (TextView) findViewById(R.id.department_header);
        bossHeader = (TextView) findViewById(R.id.profile_heading_reports_to);
        bossView = (ProfileReportsToView) findViewById(R.id.boss_view);

        departmentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AbstractProfileActivity.this, ContactsForDepartmentActivity.class);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_ID_EXTRA, contact.departmentId);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_NAME_EXTRA, contact.departmentName);
                startActivity(intent);
            }
        });

        inflater = LayoutInflater.from(this);

    }

    private void replaceAndCreateManagedContacts(Cursor reportsCursor) {

        LinearLayout viewHolder = (LinearLayout) findViewById(R.id.view_holder);

        int indexOfHeader = viewHolder.indexOfChild(managesHeader) + 1;

        // REMOVE OLD VIEWS
        for (int i = 0; i<numberManagedByPrevious; i++) {
            View v = viewHolder.getChildAt(indexOfHeader);
            if (v instanceof ProfileManagesUserView) {
                viewHolder.removeView(v);
            }
        }

        numberManagedByPrevious = reportsCursor.getCount();

        int iterator = 0;
        final int userId = dataProviderServiceBinding.getLoggedInUser().id;
        while (reportsCursor.moveToNext()) {
            final ProfileManagesUserView newView = (ProfileManagesUserView) inflater.inflate(R.layout.item_manages_contact, viewHolder, false);
            Contact newContact = new Contact();
            newContact = ContactsAdapter.getCachedContact(reportsCursor);
            newView.userId = userId;
            newView.setupView(newContact);
            newView.noPhotoThumb.setText(newContact.initials);
            newView.noPhotoThumb.setVisibility(View.VISIBLE);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
                    try {
                        props.put("subordinate_id", String.valueOf(newView.profileId));
                        mixpanel.track("subordinate_tapped", props);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent intent;
                    if (userId == newView.profileId) {
                        intent = new Intent(AbstractProfileActivity.this, MyProfileActivity.class);
                    } else {
                        intent = new Intent(AbstractProfileActivity.this, OtherProfileActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtra("PROFILE_ID", newView.profileId);
                    backStackIds.add(contact.id);
                    intent.putExtra("BACK_STACK_IDS", backStackIds);
                    startActivity(intent);
                }
            });
            if( newContact.avatarUrl != null ) {
                dataProviderServiceBinding.setSmallContactImage(newContact, newView.thumbImage, newView.noPhotoThumb);
            }
            viewHolder.addView(newView, indexOfHeader + iterator);
            iterator++;
        }

        if (reportsCursor.getCount() > 0) {
            managesHeader.setVisibility(View.VISIBLE);
        } else {
            managesHeader.setVisibility(View.GONE);
        }

    }

    /**
     * Subclasses must set content view
     */
    protected abstract void setContentViewLayout();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        contactId = intent.getIntExtra("PROFILE_ID", 0);
        backStackIds = intent.getIntegerArrayListExtra("BACK_STACK_IDS");
        if (backStackIds == null) {
            backStackIds = new ArrayList<Integer>();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
        contact = dataProviderServiceBinding.getContact(contactId);
        setupProfile();
        Cursor reportsCursor = getNewManagesContactsCursor();
        replaceAndCreateManagedContacts(reportsCursor);
    }

    @Override
    public void onBackPressed() {
        if (backStackIds.size() > 0) {
            int profileId = backStackIds.get(backStackIds.size() - 1);
            Intent intent;
            if (profileId == dataProviderServiceBinding.getLoggedInUser().id) {
                intent = new Intent(AbstractProfileActivity.this, MyProfileActivity.class);
            } else {
                intent = new Intent(AbstractProfileActivity.this, OtherProfileActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("PROFILE_ID", profileId);
            backStackIds.remove(backStackIds.size() - 1);
            intent.putExtra("BACK_STACK_IDS", backStackIds);
            startActivity(intent);
            if (this instanceof MyProfileActivity && !backStackIds.contains(profileId)) {
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected Cursor getNewManagesContactsCursor() {
        Cursor managedContactsCursor = dataProviderServiceBinding.getContactsManaged(contact.id);
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
                departmentHeader.setVisibility(View.VISIBLE);
                departmentView.setText(contact.departmentName);
            }
            else {
                departmentView.setVisibility(View.GONE);
                departmentHeader.setVisibility(View.GONE);
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

            int currentLocationId = contact.currentOfficeLocationId;
            String officeLocationName = dataProviderServiceBinding.getOfficeLocationName( currentLocationId );
            if( officeLocationName != null ) {
                currentLocationView.isOn = true;
                currentLocationView.primaryValue = dataProviderServiceBinding.getOfficeLocationName( currentLocationId );
            }
            else {
                currentLocationView.primaryValue = "Out of office";
                currentLocationView.isOn = false;
            }
            currentLocationView.invalidate();

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
            if (isNotBlank(contact.managerName)) {
                bossHeader.setVisibility(View.VISIBLE);

                final Contact boss = dataProviderServiceBinding.getContact(contact.managerId);
                bossView.userId = dataProviderServiceBinding.getLoggedInUser().id;
                bossView.setupView(boss);
                bossView.noPhotoThumb.setText(boss.initials);
                bossView.noPhotoThumb.setVisibility(View.VISIBLE);

                bossView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
                        try {
                            props.put("manager_id", String.valueOf(bossView.profileId));
                            mixpanel.track("manager_tapped", props);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent intent;
                        if (bossView.userId == bossView.profileId) {
                            intent = new Intent(AbstractProfileActivity.this, MyProfileActivity.class);
                        } else {
                            intent = new Intent(AbstractProfileActivity.this, OtherProfileActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra("PROFILE_ID", bossView.profileId);
                        backStackIds.add(contact.id);
                        intent.putExtra("BACK_STACK_IDS", backStackIds);
                        startActivity(intent);
                    }
                });
                if( boss.avatarUrl != null ) {
                    dataProviderServiceBinding.setSmallContactImage(boss, bossView.thumbImage, bossView.noPhotoThumb);
                }

                bossView.setVisibility(View.VISIBLE);

            } else {
                bossHeader.setVisibility(View.GONE);
                bossView.setVisibility(View.GONE);
            }

            JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
            try {
                props.put("user_id", String.valueOf(contact.id));
                mixpanel.track("profile_viewed", props);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isNotBlank( String str ) {
        return str != null && !str.isEmpty();
    }

}
