package com.triaged.badge.app;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Profile Activity for other users (not the logged-in-user)
 *
 * Created by Will on 7/10/14.
 */
public class OtherProfileActivity extends AbstractProfileActivity {

    private TextView backButton = null;
    private ImageButton makeCallButton = null;
    private ImageButton newEmailButton = null;
    private BroadcastReceiver refreshReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflater = LayoutInflater.from(this);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View backButtonBar = inflater.inflate(R.layout.back_button_bar, null);
        backButton = (TextView) backButtonBar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        ImageButton newMessageButton = (ImageButton) findViewById(R.id.new_message_button);
        newMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(OtherProfileActivity.this, "New Message", Toast.LENGTH_SHORT).show();
                trackProfileButtonEvent("message");
            }
        });

        newEmailButton = (ImageButton) findViewById(R.id.new_email_button);
        newEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contact != null && contact.email != null) {
                    trackProfileButtonEvent("email");
                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{contact.email});

                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "\n\n--\nsent via badge");

                    OtherProfileActivity.this.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                }
            }
        });

        makeCallButton = (ImageButton) findViewById(R.id.call_button);
        makeCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contact != null && contact.cellPhone != null) {
                    trackProfileButtonEvent("phone");
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + contact.cellPhone));
                    startActivity(intent);
                }
            }
        });

        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                contact = dataProviderServiceBinding.getContact( contact.id );
                setupProfile();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Try to refresh contact
        localBroadcastManager.registerReceiver( refreshReceiver, new IntentFilter( DataProviderService.DB_UPDATED_ACTION ));
        dataProviderServiceBinding.refreshContact( contact.id );
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver( refreshReceiver );
    }

    @Override
    protected void setContentViewLayout() {
        setContentView(R.layout.activity_other_profile);
    }

    @Override
    protected void setupProfile() {
        super.setupProfile();
        backButton.setText(contact.name);
        if (contact.cellPhone == null) {
            makeCallButton.setVisibility(View.INVISIBLE);
        } else {
            makeCallButton.setVisibility(View.VISIBLE);
        }
        if (contact.email == null) {
            newEmailButton.setVisibility(View.INVISIBLE);
        } else {
            newEmailButton.setVisibility(View.VISIBLE);
        }
    }

    private void trackProfileButtonEvent(String eventType) {
        JSONObject props = dataProviderServiceBinding.getBasicMixpanelData();
        try {
            props.put("button", eventType);
            mixpanel.track("profile_button_touched", props);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}