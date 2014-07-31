package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;
import com.triaged.badge.app.views.DepartmentsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 *
 * Primary activity for displaying a user's list of contacts and departments.
 *
 */
public class ContactsActivity extends BadgeActivity implements ActionBar.TabListener {

    private static boolean shouldRegister = true;

    private static final String TAG = ContactsActivity.class.getName();
    private StickyListHeadersListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;

    private ListView departmentsListView = null;
    private DepartmentsAdapter departmentsAdapter = null;
    private ContactsAdapterWithoutHeadings searchResultsAdapter = null;

    private Button contactsTabButton = null;
    private Button departmentsTabButton = null;

    private Typeface medium = null;
    private Typeface regular = null;

    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    protected boolean databaseReady;
    protected BroadcastReceiver receiver;
    protected BadgeApplication app;

    private LocalBroadcastManager localBroadcastManager;

    private EditText searchBar = null;
    private ImageButton clearButton = null;
    private LinearLayout contactsDepartmentsTab = null;
    private ListView searchResultsList = null;
    private RelativeLayout.LayoutParams departmentListViewParams;
    private int departmentListTopMargin = 0;
    private int departmentListBottomMargin = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.messages_unselected).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.contacts_selected).setTabListener(this), true);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.profile_unselected).setTabListener(this), false);

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
                departmentsListView.setVisibility( View.INVISIBLE );
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
                departmentsListView.setVisibility(View.VISIBLE);
            }
        });

        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

//        LayoutInflater inflater = LayoutInflater.from(this);
//        RelativeLayout searchBarView = (RelativeLayout) inflater.inflate(R.layout.include_search, null);
//        contactsListView.addHeaderView(searchBarView);

        searchResultsList = (ListView) findViewById(R.id.search_results_list);

        departmentListViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        float densityMultiplier = getResources().getDisplayMetrics().density;
        departmentListTopMargin = (int) (64 * densityMultiplier);
        departmentListBottomMargin = (int) (40 * densityMultiplier);

        searchBar = (EditText) findViewById(R.id.search_bar);
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = searchBar.getText().toString();
                if (text.length() > 0) {
                    clearButton.setVisibility(View.VISIBLE);
                    contactsDepartmentsTab.setVisibility(View.GONE);
                    if (contactsTabButton.isSelected()) {
                        searchResultsAdapter.setFilter( text );
                        searchResultsList.setVisibility(View.VISIBLE);
                        contactsListView.setVisibility(View.GONE);
                    }
                    else {
                        departmentListViewParams.setMargins(0, departmentListTopMargin, 0, 0);
                        departmentsListView.setLayoutParams(departmentListViewParams);
                        departmentsAdapter.setFilter( text );
                    }
                } else {
                    clearButton.setVisibility(View.GONE);
                    contactsDepartmentsTab.setVisibility(View.VISIBLE);
                    if (contactsTabButton.isSelected()) {
                        contactsListView.setVisibility(View.VISIBLE);
                        searchResultsList.setVisibility(View.GONE);
                    }
                    else {
                        departmentListViewParams.setMargins(0, departmentListTopMargin, 0, departmentListBottomMargin);
                        departmentsListView.setLayoutParams(departmentListViewParams);
                        departmentsAdapter.clearFilter();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        searchBar.addTextChangedListener(tw);

        clearButton = (ImageButton) findViewById(R.id.clear_search);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

        contactsDepartmentsTab = (LinearLayout) findViewById(R.id.contacts_departments_tab);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int userId = dataProviderServiceBinding.getLoggedInUser().id;
                int clickedId = contactsAdapter.getCachedContact(position).id;
                Intent intent;
                if (userId == clickedId) {
                    intent = new Intent(ContactsActivity.this, MyProfileActivity.class);
                } else {
                    intent = new Intent(ContactsActivity.this, OtherProfileActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", clickedId);
                startActivity(intent);
            }
        });

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int userId = dataProviderServiceBinding.getLoggedInUser().id;
                int clickedId = searchResultsAdapter.getCachedContact(position).id;
                Intent intent;
                if (userId == clickedId) {
                    intent = new Intent(ContactsActivity.this, MyProfileActivity.class);
                } else {
                    intent = new Intent(ContactsActivity.this, OtherProfileActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", clickedId);
                startActivity(intent);
            }
        });


        databaseReady = false;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if( intent.getAction().equals( DataProviderService.DB_AVAILABLE_ACTION) ) {
                    dataProviderServiceBinding = app.dataProviderServiceBinding;
                    databaseReadyCallback();
                }
                else if( intent.getAction().equals( DataProviderService.DB_UPDATED_ACTION) ) {
                    loadContactsAndDepartments();
                }
            }
        };


        departmentsListView = (ListView) findViewById(R.id.departments_list);

        departmentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ContactsActivity.this, ContactsForDepartmentActivity.class);
                intent.putExtra("DEPARTMENT_ID", departmentsAdapter.getItem(position).id);
                intent.putExtra("DEPARTMENT_NAME", departmentsAdapter.getItem(position).name);
                startActivity(intent);
            }
        });

        app = (BadgeApplication) getApplication();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataProviderService.DB_AVAILABLE_ACTION);
        filter.addAction(DataProviderService.DB_UPDATED_ACTION);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(receiver, filter);
        dataProviderServiceBinding = app.dataProviderServiceBinding;
        if( dataProviderServiceBinding != null && dataProviderServiceBinding.isInitialized() ) {
            databaseReadyCallback();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if( departmentsAdapter != null ) {
            departmentsAdapter.destroy();
        }
        if( searchResultsAdapter != null ) {
            searchResultsAdapter.destroy();
        }
        localBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 0) {
            tab.setIcon(R.drawable.messages_selected);
            Intent intent = new Intent(this, MessagesIndexActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (tab.getPosition() == 2) {
            tab.setIcon(R.drawable.profile_selected);
            Intent intent = new Intent( this, MyProfileActivity.class );
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra("PROFILE_ID", dataProviderServiceBinding.getLoggedInUser().id);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 1) {
            tab.setIcon(R.drawable.contacts_unselected);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

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
            lazyDeviceRegistration();
            loadContactsAndDepartments();
        }
    }

    protected void loadContactsAndDepartments() {
        if( dataProviderServiceBinding.getLoggedInUser() != null ) {
            if (contactsAdapter != null) {
                contactsAdapter.refresh();
            } else {
                contactsAdapter = new ContactsAdapter(this, dataProviderServiceBinding, R.layout.item_contact_with_msg);
                contactsListView.setAdapter(contactsAdapter);
            }

            if (departmentsAdapter != null) {
                departmentsAdapter.refresh();
            } else {
                departmentsAdapter = new DepartmentsAdapter(this, R.layout.item_department_with_count, dataProviderServiceBinding, true);
                departmentsListView.setAdapter(departmentsAdapter);
            }
            if (searchResultsAdapter != null) {
                searchResultsAdapter.refresh(dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser());
            } else {
                searchResultsAdapter = new ContactsAdapterWithoutHeadings(this, dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser(), dataProviderServiceBinding, false);
                searchResultsList.setAdapter(searchResultsAdapter);
            }
        }
    }

    /**
     * Every time we get to the contacts screen, do a quick check to see if we've registered the device yet.
     * If not, do it assuming the user is logged in!
     */
    private void lazyDeviceRegistration() {
        if( shouldRegister && dataProviderServiceBinding.getLoggedInUser() != null ) {
            String regId = getRegistrationId( this );
            if( regId.isEmpty() ) {
                // This will async generate a new reg id and
                // send it up to the cloud
                ensureGcmRegistration();
            }
            else {
                // Re-register device
                ((BadgeApplication) getApplication()).dataProviderServiceBinding.registerDevice( regId );
            }
            shouldRegister = false;
        }
    }

    @Override
    protected void logout() {
        shouldRegister = true;
        super.logout();
    }
}