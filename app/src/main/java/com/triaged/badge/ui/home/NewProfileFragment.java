package com.triaged.badge.ui.home;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.makeramen.RoundedImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.triaged.badge.app.App;
import com.triaged.badge.app.MessageProcessor;
import com.triaged.badge.app.R;
import com.triaged.badge.app.SyncManager;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.models.User;
import com.triaged.badge.ui.base.MixpanelFragment;
import com.triaged.badge.ui.base.views.ProfileContactInfoView;
import com.triaged.badge.ui.messaging.MessagingActivity;
import com.triaged.utils.GeneralUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import retrofit.RetrofitError;

public class NewProfileFragment extends MixpanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String USER_ID_ARG = "user_id_arg";
    private static final int CONTACT_LOAD_REQUEST_ID = 0;

    private int mUserId;
    private User mCurrentUser;

    @InjectView(R.id.avatar) RoundedImageView avatar;
    @InjectView(R.id.avatar_text) TextView avatarText;
    @InjectView(R.id.first_name) TextView firstName;
    @InjectView(R.id.last_name) TextView lastName;
    @InjectView(R.id.job_title) TextView jobTitle;
    @InjectView(R.id.email) TextView email;
    @InjectView(R.id.phone) TextView phone;
    @InjectView(R.id.office_phone) TextView officePhone;
    @InjectView(R.id.linkedin) TextView linkedin;
    @InjectView(R.id.website) TextView website;

    @InjectView(R.id.phone_row) View phoneRow;
    @InjectView(R.id.office_phone_row) View officePhoneRow;
    @InjectView(R.id.linkedin_row) View linkedinRow;
    @InjectView(R.id.website_row) View websiteRow;

    @OnClick(R.id.email_button)
    void openEmailClient() {
        if (!TextUtils.isEmpty(mCurrentUser.getEmail())) {
//            trackProfileButtonEvent("email");
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{mCurrentUser.getEmail()});
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "\n\n--\nsent via badge");
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        }
    }

    @OnClick({R.id.phone_call_button, R.id.office_call_button})
    void call(View view) {
        CharSequence phoneNumber = null;
        if (view.getId() == R.id.phone_call_button) {
            phoneNumber = phone.getText();
        } else if (view.getId() == R.id.office_call_button) {
            phoneNumber = officePhone.getText();
        }
        if (!TextUtils.isEmpty(phoneNumber)) {
//            trackProfileButtonEvent("phone");
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        }
    }

    @OnClick({R.id.linkedin_row, R.id.website_row})
    void openBrowser(View view) {
        CharSequence url = null;
        if (view.getId() == R.id.linkedin_row) {
            url = linkedin.getText();
        } else if (view.getId() == R.id.website_row) {
            url = website.getText();
        }
        if (!TextUtils.isEmpty(url)) {
            GeneralUtils.openWebsite(getActivity(), url.toString());
        }
    }




    public static NewProfileFragment newInstance(int userId) {
        NewProfileFragment fragment = new NewProfileFragment();
        Bundle args = new Bundle();
        args.putInt(USER_ID_ARG, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public NewProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserId = getArguments().getInt(USER_ID_ARG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_new_profile, container, false);
        ButterKnife.inject(this, root);
        getLoaderManager().restartLoader(CONTACT_LOAD_REQUEST_ID, null, this).forceLoad();
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACT_LOAD_REQUEST_ID) {
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(UserProvider.CONTENT_URI, mUserId),
                    null,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CONTACT_LOAD_REQUEST_ID) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                mCurrentUser = UserHelper.fromCursor(data);
                setupProfile();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @DebugLog
    protected void setupProfile() {
        if (mCurrentUser == null) {
            //TODO:
            // Shouldn't happen, but if so show some error
            // also send a request to retrieve this user from server
            return;
        }
        firstName.setText(mCurrentUser.getFirstName());
        lastName.setText(mCurrentUser.getLastName());
        getActivity().setTitle(String.format("%s %s", mCurrentUser.getFirstName(), mCurrentUser.getLastName()));

        jobTitle.setText(mCurrentUser.getEmployeeInfo().getJobTitle());

        setupMenuItems();

        bindAvatarView();

        email.setText(mCurrentUser.getEmail());

        if (TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getCellPhone())) {
            phoneRow.setVisibility(View.GONE);
        } else {
            phoneRow.setVisibility(View.VISIBLE);
            phone.setText(mCurrentUser.getEmployeeInfo().getCellPhone());
        }

        if (TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getOfficePhone())) {
            officePhoneRow.setVisibility(View.GONE);
        } else {
            officePhoneRow.setVisibility(View.VISIBLE);
            officePhone.setText(mCurrentUser.getEmployeeInfo().getOfficePhone());
        }


        if (TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getLinkedin())) {
            linkedinRow.setVisibility(View.GONE);
        } else {
            linkedinRow.setVisibility(View.VISIBLE);
            linkedin.setText(mCurrentUser.getEmployeeInfo().getLinkedin());
        }

        if (TextUtils.isEmpty(mCurrentUser.getEmployeeInfo().getWebsite())) {
            websiteRow.setVisibility(View.GONE);
        } else {
            websiteRow.setVisibility(View.VISIBLE);
            website.setText(mCurrentUser.getEmployeeInfo().getWebsite());
        }

        JSONObject props = new JSONObject();
        try {
            props.put("user_id", String.valueOf(mCurrentUser.getId()));
            mixpanel.track("profile_viewed", props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void bindAvatarView() {
        if (mCurrentUser.getAvatarFaceUrl() != null) {
            avatar.setVisibility(View.VISIBLE);
            avatarText.setVisibility(View.GONE);
            ImageLoader.getInstance().displayImage(mCurrentUser.getAvatarFaceUrl(), avatar);

        } else {
            avatarText.setVisibility(View.VISIBLE);
            avatar.setVisibility(View.GONE);
            avatarText.setText(mCurrentUser.initials());
        }
    }


    MenuItem settingAction;
    MenuItem sendMessageAction;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_fragment, menu);
        settingAction = menu.findItem(R.id.action_settings);
        sendMessageAction = menu.findItem(R.id.action_send_message);
        setupMenuItems();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;

            case R.id.action_send_message:
                openMessage();
                return true;

            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return false;
    }

    private void openMessage() {
        final Integer[] recipientIds = new Integer[]{ mUserId, SyncManager.getMyUser().id};
        Arrays.sort(recipientIds);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return MessageProcessor.getInstance().createThreadSync(recipientIds);
                } catch (RetrofitError e) {
                    App.toast("Network issue occurred. Try again later.");
                    App.gLogger.e(e);
                } catch (JSONException e) {
                    App.toast("Unexpected response from server.");
                } catch (OperationApplicationException e ) {
                    App.toast("Unexpected response from server.");
                } catch ( RemoteException e) {
                    App.toast("Unexpected response from server.");
                }
                return null;
            }

            @Override
            protected void onPostExecute(String threadId) {
                if (threadId != null) {
                    //TODO: should not create new activity,
                    // just update the thread id and refresh the fragment.
                    Intent intent = new Intent(getActivity(), MessagingActivity.class);
                    intent.putExtra(MessagingActivity.THREAD_ID_EXTRA, threadId);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    getActivity().startActivity(intent);
                } else {
                    App.toast("A problem occurred, please try later");
                }
            }
        }.execute();
    }

    private void openSettings() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupMenuItems() {
        if (settingAction != null && sendMessageAction != null)
            if (mUserId == App.accountId()) {
                settingAction.setVisible(true);
                sendMessageAction.setVisible(false);
            } else {

                settingAction.setVisible(false);
                sendMessageAction.setVisible(true);
            }
    }

}
