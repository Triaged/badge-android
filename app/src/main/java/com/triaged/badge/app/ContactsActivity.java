package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.DepartmentsAdapter;

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

    private ListView departmentsListView = null;
    private DepartmentsAdapter departmentsAdapter = null;

    private Button contactsTabButton = null;
    private Button departmentsTabButton = null;

    private Typeface medium = null;
    private Typeface regular = null;

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    protected boolean databaseReady;
    protected BroadcastReceiver receiver;
    protected BadgeApplication app;

    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_unselected).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_selected).setTabListener(this), true);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_unselected).setTabListener(this));

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
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", contactsAdapter.getCachedContact(position).id);
                startActivity(intent);
            }

        });

        databaseReady = false;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if( intent.getAction().equals( DataProviderService.DB_AVAILABLE_INTENT ) ) {
                    databaseReadyCallback();
                }
                else if( intent.getAction().equals( DataProviderService.DB_UPDATED_INTENT ) ) {
                    loadContactsAndDepartments();
                }
            }
        };


        departmentsListView = (ListView) findViewById(R.id.departments_list);
        app = (BadgeApplication) getApplication();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataProviderService.DB_AVAILABLE_INTENT);
        filter.addAction(DataProviderService.DB_UPDATED_INTENT);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, filter);
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        if( dataProviderServiceBinding != null && dataProviderServiceBinding.isInitialized() ) {
            databaseReadyCallback();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
        ActionBar actionBar = getActionBar();
        actionBar.getTabAt(0).setIcon(R.drawable.messages_unselected);
        actionBar.getTabAt(1).setIcon(R.drawable.contacts_selected).select();
        actionBar.getTabAt(2).setIcon(R.drawable.profile_unselected);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( contactsAdapter != null ) {
            contactsAdapter.destroy();
        }
        localBroadcastManager.unregisterReceiver( receiver );
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


    /**
     * This callback should be invoked whenever the database is determined
     * to be ready, which may be asynchronous with activity create and start.
     *
     * No contacts db operation should occur until this has been called.
     */
    protected void databaseReadyCallback() {
        if( !databaseReady ) {
            databaseReady = true;
            // SETUP CONTACTS
            dataProviderServiceBinding = app.dataProviderServiceBinding;
            loadContactsAndDepartments();
        }
    }

    protected void loadContactsAndDepartments() {
        if( contactsAdapter != null ) {
            contactsAdapter.refresh();

            // Refresh departments
        }
        else {

            contactsAdapter = new ContactsAdapter(this, dataProviderServiceBinding);
            contactsListView.setAdapter(contactsAdapter);

            // SETUP DEPARTMENTS
            // get cursor
            // create adapter
            // set adapter
        }
    }
}