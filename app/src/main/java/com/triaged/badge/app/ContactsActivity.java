package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 *
 * Primary activity for displaying a user's list of contacts and departments.
 *
 */

public class ContactsActivity extends BadgeActivity implements ActionBar.TabListener {

    private static final String TAG = ContactsActivity.class.getName();
    private StickyListHeadersListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;

    private Button contactsTabButton = null;
    private Button departmentsTabButton = null;

    private Typeface medium = null;
    private Typeface regular = null;

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private ServiceConnection dataProviderServiceConnnection = null;

    protected BroadcastReceiver receiver;

    private LocalBroadcastManager localBroadcastManager;
    private Cursor contactsCursor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setText("A").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("B").setTabListener(this), true);
        actionBar.addTab(actionBar.newTab().setText("C").setTabListener(this));

        setContentView(R.layout.activity_contacts);

        contactsTabButton = (Button) findViewById(R.id.contacts_tab);
        departmentsTabButton = (Button) findViewById(R.id.departments_tab);

        contactsTabButton.setSelected(true);
        departmentsTabButton.setSelected(false);

        medium = Typeface.createFromAsset( ContactsActivity.this.getAssets(), "Roboto-Medium.ttf" );
        regular = Typeface.createFromAsset( ContactsActivity.this.getAssets(), "Roboto-Regular.ttf" );

        contactsTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactsTabButton.setSelected(true);
                departmentsTabButton.setSelected(false);
                contactsTabButton.setTypeface(medium);
                departmentsTabButton.setTypeface(regular);
                contactsListView.setVisibility(View.VISIBLE);
            }
        });

        departmentsTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactsTabButton.setSelected(false);
                departmentsTabButton.setSelected(true);
                contactsTabButton.setTypeface(regular);
                departmentsTabButton.setTypeface(medium);
                contactsListView.setVisibility(View.INVISIBLE);
            }
        });

        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ContactsActivity.this, ProfileActivity.class);
                startActivity(intent);
            }

        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "CONTACTS AVAILABLE YAAAAY");
                setupContacts();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataProviderService.CONTACTS_AVAILABLE_INTENT);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, filter);

        dataProviderServiceConnnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "SERVICE CONNECTED YAAAAY");
                dataProviderServiceBinding = (DataProviderService.LocalBinding) service;
                setupContacts();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "SERVICE DISCONNECTED BOOOOOO");
            }
        };

        if (!bindService(new Intent(this, DataProviderService.class), dataProviderServiceConnnection, BIND_AUTO_CREATE)) {
            unbindService(dataProviderServiceConnnection);
        }

    }

    private void setupContacts() {
        if (dataProviderServiceBinding.isInitialized() ) {
            // SETUP CONTACTS
            contactsCursor = dataProviderServiceBinding.getContactsCursor();
            contactsAdapter = new ContactsAdapter(this, contactsCursor );
            contactsListView.setAdapter(contactsAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        if( contactsCursor != null ) {
            contactsCursor.close();
        }
        unbindService(dataProviderServiceConnnection);
        localBroadcastManager.unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        Log.d(TAG, "TAB SELECTED");
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        Log.d(TAG, "TAB UNSELECTED");
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        Log.d(TAG, "TAB RESELECTED");
    }
}