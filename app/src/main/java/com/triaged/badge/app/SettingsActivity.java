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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        Button changePasswordButton = (Button) findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( SettingsActivity.this, ChangePasswordActivity.class );
                startActivity( intent );
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"team@badge.co"});
                SettingsActivity.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });

        Switch broadcastOfficeLocationSwitch = (Switch) findViewById(R.id.office_location_switch);
        broadcastOfficeLocationSwitch.setChecked( prefs.getBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ) );
        broadcastOfficeLocationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if( isChecked ) {
                    prefs.edit().putBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ).commit();
                    LocationTrackingService.scheduleAlarm( SettingsActivity.this );
                }
                else {
                    prefs.edit().putBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, false ).commit();
                    LocationTrackingService.clearAlarm( dataProviderServiceBinding, SettingsActivity.this );
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
