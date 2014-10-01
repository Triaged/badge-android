package com.triaged.badge.ui.home;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.User;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.receivers.LogoutReceiver;
import com.triaged.badge.ui.base.BackButtonActivity;
import com.triaged.badge.ui.profile.ChangePasswordActivity;
import com.triaged.badge.ui.profile.EditProfileActivity;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * User settings view
 * <p/>
 * Created by Will on 7/10/14.
 */
public class SettingsActivity extends BackButtonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        backButton.setText("Settings");

        Button editProfileButton = (Button) findViewById(R.id.edit_profile_button);
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        Button changePasswordButton = (Button) findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        Button logoutButton = (Button) findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JSONObject props = App.dataProviderServiceBinding.getBasicMixpanelData();
                mixpanel.track("logout", props);

                int deviceId = SharedPreferencesUtil.getInteger(R.string.pref_device_id_key, -1);
                if (deviceId > -1) {
                    RestService.instance().badge().signOut(deviceId);
                }

                /**
                 * send a broadcast for logout receiver to handle
                 * application data cleanup and exit.
                 */
                Intent logoutIntent = new Intent(LogoutReceiver.ACTION_LOGOUT);
                logoutIntent.putExtra(LogoutReceiver.RESTART_APP_EXTRA, false);
                sendBroadcast(logoutIntent);

            }
        });
        Button aboutButton = (Button) findViewById(R.id.about_button);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"team@badge.co"});
                SettingsActivity.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });

        Switch broadcastOfficeLocationSwitch = (Switch) findViewById(R.id.office_location_switch);
        broadcastOfficeLocationSwitch.setChecked(SharedPreferencesUtil.getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true));
        broadcastOfficeLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    user.put("user", data);
                    data.put("sharing_office_location", isChecked);

                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                    return;
                }
                TypedJsonString typedJsonString = new TypedJsonString(user.toString());
                RestService.instance().badge().updateAccount(typedJsonString, new Callback<Account>() {
                    @Override
                    public void success(Account account, Response response) {
                        SharedPreferencesUtil.store(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, isChecked);

                        ContentValues values = new ContentValues(1);
                        values.put(UsersTable.CLM_SHARING_OFFICE_LOCATION,
                                isChecked ? User.SHARING_LOCATION_ONE : User.SHARING_LOCATION_OFF);

                        getContentResolver().update(UserProvider.CONTENT_URI, values,
                                UsersTable.COLUMN_ID + " =?",
                                new String[]{App.accountId() + ""});

                        if (isChecked) {
                            LocationTrackingService.scheduleAlarm(SettingsActivity.this);
                        } else {
                            LocationTrackingService.clearAlarm(App.dataProviderServiceBinding, SettingsActivity.this);
                        }
                        EventBus.getDefault().post(new UpdateAccountEvent());
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Toast.makeText(SettingsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
