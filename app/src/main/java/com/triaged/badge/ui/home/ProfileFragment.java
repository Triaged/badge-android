package com.triaged.badge.ui.home;


import android.app.LoaderManager;
import android.content.ContentUris;
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
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.models.User;
import com.triaged.badge.ui.base.MixpanelFragment;
import com.triaged.badge.ui.base.views.ButtonWithFont;
import com.triaged.badge.ui.base.views.ProfileContactInfoView;
import com.triaged.badge.ui.base.views.ProfileCurrentLocationView;
import com.triaged.badge.ui.base.views.ProfileManagesUserView;
import com.triaged.badge.ui.base.views.ProfileReportsToView;
import com.triaged.badge.ui.profile.ProfileActivity;
import com.triaged.utils.GeneralUtils;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

public class ProfileFragment extends MixpanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String CONTACT_ID_ARG = "contact_id_arg";
    private static final int CONTACT_LOAD_REQUEST_ID = 0;
    private static final int MANAGES_LOAD_REQUEST_ID = 1;

    private long mContactId;
    protected User mCurrentUser = null;

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
    @InjectView(R.id.profile_root_view) LinearLayout profileRootView;
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
        intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_ID_EXTRA, mCurrentUser.getDepartmentId());
        intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_NAME_EXTRA, departmentView.getText().toString());
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
    public static ProfileFragment newInstance(long userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putLong(CONTACT_ID_ARG, userId);
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
            mContactId = getArguments().getLong(CONTACT_ID_ARG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.inject(this, root);
        getLoaderManager().initLoader(CONTACT_LOAD_REQUEST_ID, null, this);
        getLoaderManager().initLoader(MANAGES_LOAD_REQUEST_ID, null, this);
        return root;
    }


    /**
     * Repopulates profile information on creation and on new intent.
     */
    @DebugLog
    protected void setupProfile() {
        if (mCurrentUser == null) {
            //TODO:
            // Shouldn't happen, but if so show some error
            // also send a request to retrieve this user from server
            return;
        }
        profileName.setText(mCurrentUser.getFullName());
        profileTitle.setText(mCurrentUser.getEmployeeInfo().getJobTitle());
        bindAvatarView();
        bindDepartmentView();
        bindEmailView();
        bindOfficePhoneView();
        bindMobile();
        bindWebsiteView();
        bindLinkedInView();
        bindBirthDayView();
        bindOfficeLocationView();
        bindStartDateView();
        bindOfficeView();
        bindBossView();

        JSONObject props = App.dataProviderServiceBinding.getBasicMixpanelData();
        try {
            props.put("user_id", String.valueOf(mCurrentUser.getId()));
            mixpanel.track("profile_viewed", props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void bindAvatarView() {
        if (mCurrentUser.getAvatarFaceUrl() != null) {
            profileImage.setVisibility(View.VISIBLE);
            missingProfileImage.setVisibility(View.GONE);
//                dataProviderServiceBinding.setLargeContactImage( user, profileImage );
            ImageLoader.getInstance().displayImage(mCurrentUser.getAvatarFaceUrl(), profileImage);

        } else {
            missingProfileImage.setVisibility(View.VISIBLE);
            profileImage.setVisibility(View.GONE);
            missingProfileImage.setText(mCurrentUser.initials());
        }
    }

    private void bindDepartmentView() {
        if (mCurrentUser.getDepartmentId() > 0) {
            Cursor depCursor = getActivity().getContentResolver().query(
              ContentUris.withAppendedId(DepartmentProvider.CONTENT_URI, mCurrentUser.getDepartmentId()),
                    new String[] {DepartmentsTable.CLM_NAME},
                    null, null, null
            );
            if (depCursor.moveToFirst()) {
                String departmentName = depCursor.getString(0);
                departmentView.setVisibility(View.VISIBLE);
                departmentHeader.setVisibility(View.VISIBLE);
                departmentView.setText(departmentName);
            }
        } else {
            departmentView.setVisibility(View.GONE);
            departmentHeader.setVisibility(View.GONE);
        }
    }

    private void bindEmailView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmail())) {
            emailView.setVisibility(View.VISIBLE);
            emailView.primaryValue = mCurrentUser.getEmail();
            emailView.secondaryValue = "Email";
            emailView.invalidate();
        } else {
            emailView.setVisibility(View.GONE);
        }
    }

    private void bindOfficePhoneView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getOfficePhone())) {
            officePhoneView.setVisibility(View.VISIBLE);
            officePhoneView.primaryValue = mCurrentUser.getEmployeeInfo().getOfficePhone();
            officePhoneView.secondaryValue = "Office";
            officePhoneView.invalidate();
        } else {
            officePhoneView.setVisibility(View.GONE);
        }
    }

    private void bindMobile() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getCellPhone())) {
            cellPhoneView.setVisibility(View.VISIBLE);
            cellPhoneView.primaryValue = mCurrentUser.getEmployeeInfo().getCellPhone();
            cellPhoneView.secondaryValue = "Mobile";
            cellPhoneView.invalidate();
        } else {
            cellPhoneView.setVisibility(View.GONE);
        }
    }

    private void bindWebsiteView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getWebsite())) {
            websiteView.setVisibility(View.VISIBLE);
            websiteView.primaryValue = mCurrentUser.getEmployeeInfo().getWebsite();
            websiteView.secondaryValue = "Website";
            websiteView.invalidate();
        } else {
            websiteView.setVisibility(View.GONE);
        }
    }

    private void bindLinkedInView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getLinkedin())) {
            linkedinView.setVisibility(View.VISIBLE);
            linkedinView.primaryValue = mCurrentUser.getEmployeeInfo().getLinkedin();
            linkedinView.secondaryValue = "LinkedIn";
            linkedinView.invalidate();
        } else {
            linkedinView.setVisibility(View.GONE);
        }
    }

    private void bindBirthDayView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getBirthDate())) {
            birthDateView.setVisibility(View.VISIBLE);
            birthDateView.primaryValue = mCurrentUser.getEmployeeInfo().getBirthDate();
            birthDateView.secondaryValue = "Birthday";
            birthDateView.invalidate();
        } else {
            birthDateView.setVisibility(View.GONE);
        }
    }

    private void bindOfficeLocationView() {
        if (mCurrentUser.isSharingLocation()) {
            availabilityHeader.setVisibility(View.VISIBLE);
            currentLocationView.setVisibility(View.VISIBLE);
            String officeLocationName =  OfficeLocationHelper.getOfficeLocationName(getActivity(), mCurrentUser.currentOfficeLocationId() + "");
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
    }

    private void bindStartDateView() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getJobStartDate())) {
            startDateView.setVisibility(View.VISIBLE);
            startDateView.primaryValue = mCurrentUser.getEmployeeInfo().getJobStartDate();
            startDateView.secondaryValue = "Start Date";
            startDateView.invalidate();
        } else {
            startDateView.setVisibility(View.GONE);
        }
    }

    private void bindOfficeView() {
        Cursor cursor = getActivity().getContentResolver().query(
                ContentUris.withAppendedId(OfficeLocationProvider.CONTENT_URI, mCurrentUser.currentOfficeLocationId()),
                new String[]{OfficeLocationsTable.CLM_NAME}, null, null, null);
        if (cursor.moveToFirst()) {
            primaryOfficeView.primaryValue = cursor.getString(0);
            primaryOfficeView.secondaryValue = "Primary Office";
            primaryOfficeView.setVisibility(View.VISIBLE);
            primaryOfficeView.invalidate();
        } else {
            primaryOfficeView.setVisibility(View.GONE);
        }
    }

    private void bindBossView() {
        Cursor cursor = getActivity().getContentResolver().query(
                ContentUris.withAppendedId(UserProvider.CONTENT_URI, mCurrentUser.getManagerId()),
                null,null, null, null);
        if (cursor.moveToFirst()) {
            User boss = UserHelper.fromCursor(cursor);
            bossView.userId = App.accountId();
            bossView.setupView(boss);
            bossView.noPhotoThumb.setText(boss.initials());
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
                        Intent intent = new Intent(getActivity(), ProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra(ProfileActivity.PROFILE_ID_EXTRA, (int) bossView.profileId);
                        startActivity(intent);
                    }
                }
            });
            if (boss.getAvatarFaceUrl() != null) {
                ImageLoader.getInstance().displayImage(boss.getAvatarFaceUrl(), bossView.thumbImage, new SimpleImageLoadingListener() {
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
    }

    @DebugLog
    private void replaceAndCreateManagedContacts(Cursor reportsCursor) {
        int indexOfHeader = profileRootView.indexOfChild(managesHeader) + 1;
        // REMOVE OLD VIEWS
        for (int i = 0; i < numberManagedByPrevious; i++) {
            View v = profileRootView.getChildAt(indexOfHeader);
            if (v instanceof ProfileManagesUserView) {
                profileRootView.removeView(v);
            }
        }
        numberManagedByPrevious = reportsCursor.getCount();

        int iterator = 0;
        final int userId = App.accountId();
        if (reportsCursor.moveToFirst()) {
            managesHeader.setVisibility(View.VISIBLE);
            do {
                final ProfileManagesUserView newView = (ProfileManagesUserView) LayoutInflater.from(getActivity())
                        .inflate(R.layout.item_manages_contact, profileRootView, false);

                User newContact = UserHelper.fromCursor(reportsCursor);
                newView.userId = userId;
                newView.setupView(newContact);
                newView.noPhotoThumb.setText(newContact.initials());
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
                            Intent intent = new Intent(getActivity(), ProfileActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            intent.putExtra(ProfileActivity.PROFILE_ID_EXTRA, (int) newView.profileId);
                            startActivity(intent);
                        }
                    }
                });
                if (newContact.getAvatarFaceUrl() != null) {
//                dataProviderServiceBinding.setSmallContactImage(newContact, newView.thumbImage, newView.noPhotoThumb);
                    ImageLoader.getInstance().displayImage(newContact.getAvatarFaceUrl(), newView.thumbImage, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            newView.noPhotoThumb.setVisibility(View.GONE);
                        }
                    });
                }
                profileRootView.addView(newView, indexOfHeader + iterator);
                iterator++;
            } while (reportsCursor.moveToNext());
        } else {
            managesHeader.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACT_LOAD_REQUEST_ID) {
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(UserProvider.CONTENT_URI, mContactId),
                    null,  null, null, null);
        } else {
            return new CursorLoader(getActivity(), UserProvider.CONTENT_URI,
                    null, UsersTable.CLM_MANAGER_ID + "=?",
                    new String[]{mContactId + ""}, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CONTACT_LOAD_REQUEST_ID) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                mCurrentUser = UserHelper.fromCursor(data);
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
