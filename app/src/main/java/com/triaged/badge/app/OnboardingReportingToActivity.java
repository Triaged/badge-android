package com.triaged.badge.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Allow the logged in user to select their manager.
 *
 * Created by Will on 7/14/14.
 */
public class OnboardingReportingToActivity extends BackButtonActivity {

    public static final String MGR_NAME_EXTRA = "mgrName";
    private StickyListHeadersListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;
    private ContactsAdapterWithoutHeadings searchResultsAdapter = null;

    private EditText searchBar = null;
    private ImageButton clearButton = null;
    private ListView searchResultsList = null;

    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_reporting_to);
        backButton.setText("Reporting To");
        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(OnboardingReportingToActivity.this, OnboardingPositionActivity.class);
                intent.putExtra( MGR_NAME_EXTRA, contactsAdapter.getCachedContact(position).name);
                setResult( contactsAdapter.getCachedContact(position).id, intent );
                finish();
            }

        });

        searchResultsList = (ListView) findViewById(R.id.search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(OnboardingReportingToActivity.this, OnboardingPositionActivity.class);
                intent.putExtra( MGR_NAME_EXTRA, searchResultsAdapter.getCachedContact(position).name);
                setResult( contactsAdapter.getCachedContact(position).id, intent );
                finish();
            }
        });

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
                    searchResultsAdapter.setFilter( text );
                    searchResultsList.setVisibility(View.VISIBLE);
                    contactsListView.setVisibility(View.GONE);

                } else {
                    clearButton.setVisibility(View.GONE);
                    contactsListView.setVisibility(View.VISIBLE);
                    searchResultsList.setVisibility(View.GONE);
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

        densityMultiplier = getResources().getDisplayMetrics().density;
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
                    searchBar.setCursorVisible(true);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    searchBar.setCursorVisible(false);
                }
            }
        });

    }

    @Override
    protected void onDatabaseReady() {
        loadContacts();
    }

    private void loadContacts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final Cursor contactsCursor = dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser();
                final Cursor searchCursor = dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser();

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        if( searchResultsAdapter != null ) {
                            searchResultsAdapter.changeCursor( searchCursor );
                        }
                        else {
                            runOnUiThread( new Runnable() {
                                @Override
                                public void run() {
                                    searchResultsAdapter = new ContactsAdapterWithoutHeadings( OnboardingReportingToActivity.this, searchCursor, dataProviderServiceBinding, false );
                                    searchResultsList.setAdapter( searchResultsAdapter );
                                }
                            });
                        }
                        if( contactsAdapter != null ) {
                            contactsAdapter.changeCursor( contactsCursor );
                        }
                        else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    contactsAdapter = new ContactsAdapter(OnboardingReportingToActivity.this, dataProviderServiceBinding, contactsCursor, R.layout.item_contact_no_msg);
                                    contactsListView.setAdapter(contactsAdapter);
                                }
                            });
                        }

                    }
                });
                return null;
            }
        }.execute();
    }

    @Override
    protected void onDestroy() {
        if( searchResultsAdapter != null ) {
            searchResultsAdapter.destroy();
        }
        if( contactsAdapter != null ) {
            contactsAdapter.destroy();
        }
        super.onDestroy();
    }
}
