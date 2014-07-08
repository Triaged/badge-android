package com.triaged.badge.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import com.triaged.badge.data.Contact;

/**
 * Created by Will on 7/7/14.
 */
public class ProfileActivity extends BadgeActivity {

    private static final String LOG = ProfileActivity.class.getName();

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private ServiceConnection dataProviderServiceConnnection = null;
    private Contact contact = null;
    private TextView contactName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        contactName = (TextView) findViewById(R.id.profile_name);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        Intent intent = getIntent();
        int id = intent.getIntExtra("PROFILE_ID", 0);
        contact = dataProviderServiceBinding.getContact(id);
        if (contact != null) {
            contactName.setText(contact.name);
        }
    }
}
