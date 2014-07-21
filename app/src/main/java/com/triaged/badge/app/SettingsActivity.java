package com.triaged.badge.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * User settings view
 *
 * Created by Will on 7/10/14.
 */
public class SettingsActivity extends BackButtonActivity {

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );

        setContentView(R.layout.activity_settings);
        backButton.setText("Settings");

        Button editProfileButton = (Button) findViewById(R.id.edit_profile_button);
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
        Button changePasswordButton = (Button) findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( SettingsActivity.this, ChangePasswordActivity.class );
                startActivity( intent );
            }
        });
        Button logoutButton = (Button) findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
                mixpanel.track("logout", props);

                dataProviderServiceBinding.logout();
            }
        });
        Button aboutButton = (Button) findViewById(R.id.about_button);
        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
                startActivity(browserIntent);
            }
        });
        Switch newMessageNotificationSwitch = (Switch) findViewById(R.id.new_message_switch);
        newMessageNotificationSwitch.setChecked(true);
        newMessageNotificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO: WHAT CHANGES HERE???
                Toast.makeText(SettingsActivity.this, "New Message Notification Switch Changed", Toast.LENGTH_SHORT).show();
            }
        });

        Switch broadcastOfficeLocationSwitch = (Switch) findViewById(R.id.office_location_switch);
        broadcastOfficeLocationSwitch.setChecked( prefs.getBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ) );
        broadcastOfficeLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if( isChecked ) {
                    prefs.edit().putBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ).commit();
                    startService(new Intent(SettingsActivity.this, LocationTrackingService.class ));
                }
                else {
                    prefs.edit().putBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, false ).commit();
                    stopService(new Intent(SettingsActivity.this, LocationTrackingService.class ) );
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

}
