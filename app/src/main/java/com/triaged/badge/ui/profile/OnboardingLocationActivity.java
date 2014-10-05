package com.triaged.badge.ui.profile;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.Contact;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.views.OnboardingDotsView;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.badge.ui.profile.adapters.OfficeLocationsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Allow the user to select a primary office location.
 * <p/>
 * Created by Will on 7/10/14.
 */
public class OnboardingLocationActivity extends BadgeActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    protected OfficeLocationsAdapter officeLocationsAdapter;

    @InjectView(R.id.continue_button) Button continueButton;
    @InjectView(R.id.office_locations_list) ListView officeLocationsList;
    ImageView noLocationCheck;

    @OnClick(R.id.continue_button)
    void continueClicked() {
        onContinue();
    }

    @OnItemClick(R.id.office_locations_list)
    void onLocationClicked(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            Cursor officeCursor = (Cursor) officeLocationsAdapter.getItem(position - 1);
            officeLocationsAdapter.usersOffice = Contact.getIntSafelyFromCursor(officeCursor, OfficeLocationsTable.COLUMN_ID);
            officeLocationsAdapter.usersOfficeName = Contact.getStringSafelyFromCursor(officeCursor, OfficeLocationsTable.CLM_NAME);
            officeLocationsAdapter.notifyDataSetChanged();
            noLocationCheck.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_location);
        ButterKnife.inject(this);

        officeLocationsAdapter = new OfficeLocationsAdapter(this, null, R.layout.item_office_location);
        officeLocationsList.setAdapter(officeLocationsAdapter);
        getLoaderManager().initLoader(0, savedInstanceState, this);

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
            }
        });

        Button addView = (Button) inflater.inflate(R.layout.item_add_new, null);
        addView.setText(getString(R.string.add_new_location));
        officeLocationsList.addFooterView(addView);
        addView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OnboardingLocationActivity.this, OnboardingMapActivity.class));
            }
        });
    }

    /**
     * Called when the "Continue" Button is clicked. Subclasses may override
     */
    protected void onContinue() {
        JSONObject postData = new JSONObject();
        JSONObject user = new JSONObject();
        try {
            postData.put("user", user);
            user.put("primary_office_location_id", officeLocationsAdapter.usersOffice);
        } catch (JSONException e) {
            App.gLogger.e("JSON exception creating post body for create department", e);
            return;
        }
        TypedJsonString typedJsonString = new TypedJsonString(postData.toString());
        RestService.instance().badge().updateAccount(typedJsonString, new Callback<Account>() {
            @Override
            public void success(Account account, Response response) {
                ContentValues values = new ContentValues();
                values.put(UsersTable.CLM_PRIMARY_OFFICE_LOCATION_ID,
                        officeLocationsAdapter.usersOffice);
                getContentResolver().update(UserProvider.CONTENT_URI, values,
                        UsersTable.COLUMN_ID + " =?",
                        new String[]{App.accountId() + ""});
                EventBus.getDefault().post(new UpdateAccountEvent());

                Intent intent = new Intent(OnboardingLocationActivity.this, MainActivity.class);
                startActivity(intent);
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(new Intent(ONBOARDING_FINISHED_ACTION));
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(OnboardingLocationActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, OfficeLocationProvider.CONTENT_URI,
                null, null, null, OfficeLocationsTable.CLM_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        officeLocationsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}