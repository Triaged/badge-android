package com.triaged.badge.ui.home;


import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.OfficeLocationHelper;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.ui.base.MixpanelFragment;
import com.triaged.badge.ui.base.views.ButtonWithFont;
import com.triaged.badge.ui.base.views.ProfileContactInfoView;
import com.triaged.badge.ui.base.views.ProfileCurrentLocationView;
import com.triaged.badge.ui.base.views.ProfileManagesUserView;
import com.triaged.badge.ui.base.views.ProfileReportsToView;
import com.triaged.badge.ui.profile.OtherProfileActivity;
import com.triaged.utils.GeneralUtils;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ProfileFragment extends MixpanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String CONTACT_ID_ARG = "contact_id_arg";
    private static final int CONTACT_LOAD_REQUEST_ID = 0;
    private static final int MANAGES_LOAD_REQUEST_ID = 1;

    private int mContactId;
    protected Contact contact = null;

    @InjectView(R.id.profile_name) TextView profileName;
    @InjectView(R.id.profile_title)  TextView profileTitle;
    @InjectView(R.id.profile_image)  ImageView profileImage;
    @InjectView(R.id.missing_profile_image)  TextView missingProfileImage ;
    @InjectView(R.id.profile_department)  ButtonWithFont departmentView ;
    @InjectView(R.id.profile_email)  ProfileContactInfoView emailView ;
    @InjectView(R.id.profile_office_phone)  ProfileContactInfoView officePhoneView;
    @InjectView(R.id.profile_cell_phone)  ProfileContactInfoView cellPhoneView;
    @InjectView(R.id.profile_website)  ProfileContactInfoView websiteView;
    @InjectView(R.id.profile_linkedin)  ProfileContactInfoView linkedinView;

    @InjectView(R.id.profile_birth_date)  ProfileContactInfoView birthDateView;
    @InjectView(R.id.profile_primary_office)  ProfileContactInfoView primaryOfficeView;
    @InjectView(R.id.profile_start_date)  ProfileContactInfoView startDateView ;
    @InjectView(R.id.profile_current_location)  ProfileCurrentLocationView currentLocationView ;
    @InjectView(R.id.boss_view)  ProfileReportsToView bossView ;
    @InjectView(R.id.profile_heading_manages)  TextView managesHeader ;
    @InjectView(R.id.profile_heading_reports_to)  TextView bossHeader ;
    @InjectView(R.id.department_header)  TextView departmentHeader ;
    @InjectView(R.id.availability_header)  TextView availabilityHeader ;
    @InjectView(R.id.view_holder) LinearLayout viewHolder;
    @InjectView(R.id.profile_scrollview) ScrollView scrollView;

    @OnClick(R.id.settings_button)
    void openSettings(){
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @OnClick(R.id.profile_department)
    void onDepartmentClicked() {
        Intent intent = new Intent(getActivity(), ContactsForDepartmentActivity.class);
        intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_ID_EXTRA, contact.departmentId);
        intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_NAME_EXTRA, contact.departmentName);
        startActivity(intent);
    }

    @OnClick({R.id.profile_linkedin, R.id.profile_website})
    void openBrowser(View view) {
        String url = ((ProfileContactInfoView) view).primaryValue;
        GeneralUtils.openWebsite(getActivity(), url);
    }


    private int numberManagedByPrevious = 0;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param userId .
     * @return A new instance of fragment ProfileFragment.
     */
    public static ProfileFragment newInstance(int userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt(CONTACT_ID_ARG, userId);
        fragment.setArguments(args);
        return fragment;
    }
    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mContactId = getArguments().getInt(CONTACT_ID_ARG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.inject(this, root);

        contact =  App.dataProviderServiceBinding.getContact(mContactId);
        getLoaderManager().initLoader(CONTACT_LOAD_REQUEST_ID, null, this);
        getLoaderManager().initLoader(MANAGES_LOAD_REQUEST_ID, null, this);

        return root;
    }


    /**
     * Repopulates profile information on creation and on new intent.
     */
    protected void setupProfile() {
        if (contact != null) {
            if (contact.avatarUrl != null) {
                profileImage.setVisibility(View.VISIBLE);
                missingProfileImage.setVisibility(View.GONE);
//                dataProviderServiceBinding.setLargeContactImage( contact, profileImage );
                ImageLoader.getInstance().displayImage(contact.avatarUrl, profileImage);

            } else {
                missingProfileImage.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.GONE);
                missingProfileImage.setText(contact.initials);
            }
            profileName.setText(contact.name);
            profileTitle.setText(contact.jobTitle);
            if (!TextUtils.isEmpty(contact.departmentName)) {
                departmentView.setVisibility(View.VISIBLE);
                departmentHeader.setVisibility(View.VISIBLE);
                departmentView.setText(contact.departmentName);
            } else {
                departmentView.setVisibility(View.GONE);
                departmentHeader.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(contact.email)) {
                emailView.setVisibility(View.VISIBLE);
                emailView.primaryValue = contact.email;
                emailView.secondaryValue = "Email";
                emailView.invalidate();
            } else {
                emailView.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(contact.officePhone)) {
                officePhoneView.setVisibility(View.VISIBLE);
                officePhoneView.primaryValue = contact.officePhone;
                officePhoneView.secondaryValue = "Office";
                officePhoneView.invalidate();
            } else {
                officePhoneView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(contact.cellPhone)) {
                cellPhoneView.setVisibility(View.VISIBLE);
                cellPhoneView.primaryValue = contact.cellPhone;
                cellPhoneView.secondaryValue = "Mobile";
                cellPhoneView.invalidate();
            } else {
                cellPhoneView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(contact.website)) {
                websiteView.setVisibility(View.VISIBLE);
                websiteView.primaryValue = contact.website;
                websiteView.secondaryValue = "Website";
                websiteView.invalidate();
            } else {
                websiteView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(contact.linkedin)) {
                linkedinView.setVisibility(View.VISIBLE);
                linkedinView.primaryValue = contact.linkedin;
                linkedinView.secondaryValue = "LinkedIn";
                linkedinView.invalidate();
            } else {
                linkedinView.setVisibility(View.GONE);
            }


            if (!TextUtils.isEmpty(contact.birthDateString)) {
                birthDateView.setVisibility(View.VISIBLE);
                birthDateView.primaryValue = contact.birthDateString;
                birthDateView.secondaryValue = "Birthday";
                birthDateView.invalidate();
            } else {
                birthDateView.setVisibility(View.GONE);
            }

            if (contact.sharingOfficeLocation == Contact.SHARING_LOCATION_TRUE) {
                availabilityHeader.setVisibility(View.VISIBLE);
                currentLocationView.setVisibility(View.VISIBLE);
                int currentLocationId = contact.currentOfficeLocationId;
                String officeLocationName =  OfficeLocationHelper.getOfficeLocationName(getActivity(), currentLocationId + "");
                if (officeLocationName != null) {
                    currentLocationView.isOn = true;
                    currentLocationView.primaryValue = officeLocationName;
                } else {
                    currentLocationView.primaryValue = "Out of office";
                    currentLocationView.isOn = false;
                }
                currentLocationView.invalidate();
            } else {
                availabilityHeader.setVisibility(View.GONE);
                currentLocationView.setVisibility(View.GONE);
                currentLocationView.primaryValue = "Out of office";
                currentLocationView.isOn = false;
                currentLocationView.invalidate();
            }
            if (!TextUtils.isEmpty(contact.startDateString)) {
                startDateView.setVisibility(View.VISIBLE);
                startDateView.primaryValue = contact.startDateString;
                startDateView.secondaryValue = "Start Date";
                startDateView.invalidate();
            } else {
                startDateView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(contact.officeName)) {
                primaryOfficeView.primaryValue = contact.officeName;
                primaryOfficeView.secondaryValue = "Primary Office";
                primaryOfficeView.setVisibility(View.VISIBLE);
                primaryOfficeView.invalidate();
            } else {
                primaryOfficeView.setVisibility(View.GONE);
            }
            Contact boss;
            if (!TextUtils.isEmpty(contact.managerName) &&
                    (boss = App.dataProviderServiceBinding.getContact(contact.managerId)) != null) {

                bossView.userId = App.dataProviderServiceBinding.getLoggedInUser().id;
                bossView.setupView(boss);
                bossView.noPhotoThumb.setText(boss.initials);
                bossView.noPhotoThumb.setVisibility(View.VISIBLE);

                bossView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        JSONObject props = App.dataProviderServiceBinding.getBasicMixpanelData();
                        try {
                            props.put("manager_id", String.valueOf(bossView.profileId));
                            mixpanel.track("manager_tapped", props);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (bossView.userId == bossView.profileId) {
                            // Normally won't happen
                            scrollView.smoothScrollBy(0, 0);
                        } else {
                            Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            intent.putExtra("PROFILE_ID", bossView.profileId);
                            startActivity(intent);
                        }
                    }
                });
                if (boss.avatarUrl != null) {
                    ImageLoader.getInstance().displayImage(boss.avatarUrl, bossView.thumbImage, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            bossView.noPhotoThumb.setVisibility(View.GONE);
                        }
                    });
                }
                bossHeader.setVisibility(View.VISIBLE);
                bossView.setVisibility(View.VISIBLE);

                } else {
                    bossHeader.setVisibility(View.GONE);
                    bossView.setVisibility(View.GONE);
                }

                JSONObject props = App.dataProviderServiceBinding.getBasicMixpanelData();
                try {
                    props.put("user_id", String.valueOf(contact.id));
                    mixpanel.track("profile_viewed", props);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
    }

    private void replaceAndCreateManagedContacts(Cursor reportsCursor) {

        int indexOfHeader = viewHolder.indexOfChild(managesHeader) + 1;

        // REMOVE OLD VIEWS
        for (int i = 0; i < numberManagedByPrevious; i++) {
            View v = viewHolder.getChildAt(indexOfHeader);
            if (v instanceof ProfileManagesUserView) {
                viewHolder.removeView(v);
            }
        }

        numberManagedByPrevious = reportsCursor.getCount();

        int iterator = 0;
        final int userId = App.accountId();
        if (reportsCursor.moveToFirst()) do {
            final ProfileManagesUserView newView = (ProfileManagesUserView) LayoutInflater.from(getActivity())
                    .inflate(R.layout.item_manages_contact, viewHolder, false);
            Contact newContact = new Contact();
//            newContact = ContactsAdapter.getCachedContact(reportsCursor);
            newContact.fromCursor(reportsCursor);
            newView.userId = userId;
            newView.setupView(newContact);
            newView.noPhotoThumb.setText(newContact.initials);
            newView.noPhotoThumb.setVisibility(View.VISIBLE);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JSONObject props = App.dataProviderServiceBinding.getBasicMixpanelData();
                    try {
                        props.put("subordinate_id", String.valueOf(newView.profileId));
                        mixpanel.track("subordinate_tapped", props);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    if (userId == newView.profileId) {
                        // Normally won't happen
                        scrollView.smoothScrollBy(0, 0);
                    } else {
                        Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra("PROFILE_ID", newView.profileId);
                        startActivity(intent);
                    }
                }
            });
            if (newContact.avatarUrl != null) {
//                dataProviderServiceBinding.setSmallContactImage(newContact, newView.thumbImage, newView.noPhotoThumb);
                ImageLoader.getInstance().displayImage(newContact.avatarUrl, newView.thumbImage, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        newView.noPhotoThumb.setVisibility(View.GONE);
                    }
                });
            }
            viewHolder.addView(newView, indexOfHeader + iterator);
            iterator++;
        } while (reportsCursor.moveToNext());

        if (reportsCursor.getCount() > 0) {
            managesHeader.setVisibility(View.VISIBLE);
        } else {
            managesHeader.setVisibility(View.GONE);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACT_LOAD_REQUEST_ID) {
            return new CursorLoader(getActivity(), ContactProvider.CONTENT_URI,
                    null, ContactsTable.COLUMN_ID + "=?",
                    new String[]{mContactId + ""}, null);
        } else {
            return new CursorLoader(getActivity(), ContactProvider.CONTENT_URI,
                    null, ContactsTable.COLUMN_CONTACT_MANAGER_ID + "=?",
                    new String[]{mContactId + ""}, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CONTACT_LOAD_REQUEST_ID) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                contact = new Contact();
                contact.fromCursor(data);
                setupProfile();
            }
        } else {
            replaceAndCreateManagedContacts(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
