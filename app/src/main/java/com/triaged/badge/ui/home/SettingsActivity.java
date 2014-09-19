package com.triaged.badge.ui.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.triaged.badge.app.R;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.net.DataProviderService;
import com.triaged.badge.net.RestClient;
import com.triaged.badge.receivers.LogoutReceiver;
import com.triaged.badge.ui.base.BackButtonActivity;
import com.triaged.badge.ui.profile.ChangePasswordActivity;
import com.triaged.badge.ui.profile.EditProfileActivity;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONObject;

/**
 * User settings view
 * <p/>
 * Created by Will on 7/10/14.
 */
public class SettingsActivity extends BackButtonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

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

                JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
                mixpanel.track("logout", props);

                int deviceId = SharedPreferencesUtil.getInteger(R.string.pref_device_id_key, -1);
                if (deviceId > -1) {
                    RestClient.deviceApi.signOut(deviceId);
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
        broadcastOfficeLocationSwitch.setChecked(prefs.getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true));
        broadcastOfficeLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    dataProviderServiceBinding.saveSharingLocationAsync(true, new DataProviderService.AsyncSaveCallback() {
                        @Override
                        public void saveSuccess(int newId) {
                            prefs.edit().putBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true).commit();
                            LocationTrackingService.scheduleAlarm(SettingsActivity.this);
                        }

                        @Override
                        public void saveFailed(String reason) {
                            Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    dataProviderServiceBinding.saveSharingLocationAsync(false, new DataProviderService.AsyncSaveCallback() {
                        @Override
                        public void saveSuccess(int newId) {
                            prefs.edit().putBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, false).commit();
                            LocationTrackingService.clearAlarm(dataProviderServiceBinding, SettingsActivity.this);
                        }

                        @Override
                        public void saveFailed(String reason) {
                            Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
