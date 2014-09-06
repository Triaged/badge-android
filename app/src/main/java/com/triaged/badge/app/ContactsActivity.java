package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;
import com.triaged.badge.app.views.DepartmentsAdapter;

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

    protected BadgeApplication app;

    private LocalBroadcastManager localBroadcastManager;

    private EditText searchBar = null;
    private ImageButton clearButton = null;
    private LinearLayout contactsDepartmentsTab = null;
    private ListView searchResultsList = null;
    private RelativeLayout.LayoutParams departmentListViewParams;
    private int departmentListTopMargin = 0;
    private int departmentListBottomMargin = 0;
    protected ProgressBar loadingSpinner;

    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private RelativeLayout.LayoutParams contactsListViewParams;
    private int contactsListTopMargin = 0;
    private int contactsListBottomMargin = 0;

    private Cursor contactsCursor;
    private Cursor searchCursor;
    private Cursor departmentsCursor;

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
        loadingSpinner = (ProgressBar) findViewById( R.id.loading_spinner );

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
                setSearchBarHint();
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
                setSearchBarHint();
            }
        });

        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

//        LayoutInflater inflater = LayoutInflater.from(this);
//        RelativeLayout searchBarView = (RelativeLayout) inflater.inflate(R.layout.include_search, null);
//        contactsListView.addHeaderView(searchBarView);

        searchResultsList = (ListView) findViewById(R.id.search_results_list);

        departmentListViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        densityMultiplier = getResources().getDisplayMetrics().density;
        departmentListTopMargin = (int) (64 * densityMultiplier);
        departmentListBottomMargin = (int) (40 * densityMultiplier);

        contactsListViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        contactsListTopMargin = (int) (48 * densityMultiplier);
        contactsListBottomMargin = (int) (40 * densityMultiplier);

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
                    if (contactsTabButton.isSelected()) {
                        searchResultsAdapter.setFilter( text );
                        searchResultsList.setVisibility(View.VISIBLE);
                        contactsListView.setVisibility(View.GONE);
                    }
                    else {
                        departmentsAdapter.setFilter( text );
                    }
                } else {
                    clearButton.setVisibility(View.GONE);
                    if (contactsTabButton.isSelected()) {
                        contactsListView.setVisibility(View.VISIBLE);
                        searchResultsList.setVisibility(View.GONE);
                    }
                    else {
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
                if (dataProviderServiceBinding.getLoggedInUser() != null) {
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
            }
        });

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (dataProviderServiceBinding.getLoggedInUser() != null) {
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
            }
        });




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

        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = activityRootView.getRootView().getHeight();

                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                activityRootView.getWindowVisibleDisplayFrame(r);

                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75) ) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    contactsDepartmentsTab.setVisibility(View.GONE);
                    contactsListViewParams.setMargins(0, contactsListTopMargin, 0, 0);
                    contactsListView.setLayoutParams(contactsListViewParams);
                    departmentListViewParams.setMargins(0, departmentListTopMargin, 0, 0);
                    departmentsListView.setLayoutParams(departmentListViewParams);
                    searchBar.setCursorVisible(true);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    contactsDepartmentsTab.setVisibility(View.VISIBLE);
                    contactsListViewParams.setMargins(0, contactsListTopMargin, 0, contactsListBottomMargin);
                    contactsListView.setLayoutParams(contactsListViewParams);
                    departmentListViewParams.setMargins(0, departmentListTopMargin, 0, departmentListBottomMargin);
                    departmentsListView.setLayoutParams(departmentListViewParams);
                    searchBar.setCursorVisible(false);
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getActionBar();
        actionBar.getTabAt(0).setIcon(R.drawable.messages_unselected);
        actionBar.getTabAt(1).setIcon(R.drawable.contacts_selected).select();
        actionBar.getTabAt(2).setIcon(R.drawable.profile_unselected);
        setSearchBarHint();
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
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if ( tab.getPosition() == 0) {
            tab.setIcon(R.drawable.messages_selected);
            Intent intent = new Intent(this, MessagesIndexActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        } else if (tab.getPosition() == 2) {
            if (dataProviderServiceBinding != null && dataProviderServiceBinding.getLoggedInUser() != null) {
                tab.setIcon(R.drawable.profile_selected);
                Intent intent = new Intent( this, MyProfileActivity.class );
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra("PROFILE_ID", dataProviderServiceBinding.getLoggedInUser().id);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
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
     * This callback is invoked once and only once
     * when the database is ready.
     */
    @Override
    protected void onDatabaseReady() {
        lazyDeviceRegistration();
        loadContactsAndDepartments();
    }

    /**
     * Refresh contact list on resync
     */
    @Override
    protected void onDatabaseUpdated() {
        loadContactsAndDepartments();
    }

    protected void loadContactsAndDepartments() {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if( dataProviderServiceBinding.getLoggedInUser() != null ) {
                    contactsCursor = dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser();
                    searchCursor = dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser();
                    departmentsCursor = dataProviderServiceBinding.getDepartmentCursor( true );
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (contactsCursor != null && searchCursor != null && departmentsCursor != null) {
                    if (contactsAdapter != null) {
                        contactsAdapter.changeCursor(contactsCursor);
                    } else {
                        contactsAdapter = new ContactsAdapter(ContactsActivity.this, dataProviderServiceBinding, contactsCursor, R.layout.item_contact_with_msg);
                        contactsListView.setAdapter(contactsAdapter);
                        loadingSpinner.setVisibility(View.GONE);
                    }

                    if (searchResultsAdapter != null) {
                        searchResultsAdapter.changeCursor(searchCursor);
                    } else {
                        searchResultsAdapter = new ContactsAdapterWithoutHeadings(ContactsActivity.this, searchCursor, dataProviderServiceBinding, false);
                        searchResultsList.setAdapter(searchResultsAdapter);
                    }

                    if (departmentsAdapter != null) {
                        departmentsAdapter.departmentsCursor = departmentsCursor;
                        departmentsAdapter.refresh();
                    } else {
                        departmentsAdapter = new DepartmentsAdapter(ContactsActivity.this, R.layout.item_department_with_count, dataProviderServiceBinding, departmentsCursor);
                        departmentsListView.setAdapter(departmentsAdapter);
                    }
                }
                super.onPostExecute(aVoid);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private void setSearchBarHint() {
        if (contactsTabButton.isSelected()) {
            searchBar.setHint("Search Contacts");
        } else {
            searchBar.setHint("Search Departments");
        }
    }
}