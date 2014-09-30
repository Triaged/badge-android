package com.triaged.badge.ui.profile;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.ui.base.BackButtonActivity;
import com.triaged.badge.ui.home.adapters.UserAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Allow the logged in user to select their manager.
 * <p/>
 * Created by Will on 7/14/14.
 * Revised by Sadegh on 9/26/14.
 */
public class OnboardingReportingToActivity extends BackButtonActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String MGR_NAME_EXTRA = "mgrName";
    private UserAdapter contactsAdapter;
    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private String mSearchTerm = null;

    @InjectView(R.id.contacts_list) StickyListHeadersListView contactsListView;
    @InjectView(R.id.search_bar) EditText searchBar;
    @InjectView(R.id.clear_search) ImageButton clearSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_reporting_to);
        ButterKnife.inject(this);

        backButton.setText("Reporting To");
        contactsAdapter = new UserAdapter(this, null, R.layout.item_contact_no_msg);
        contactsListView.setAdapter(contactsAdapter);
        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserAdapter.ViewHolder holder = (UserAdapter.ViewHolder) view.getTag();
                Intent intent = new Intent(OnboardingReportingToActivity.this, OnboardingPositionActivity.class);
                intent.putExtra(MGR_NAME_EXTRA, holder.name);
                setResult(holder.contactId, intent);
                finish();
            }
        });

        searchBar.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newFilter = !TextUtils.isEmpty(s) ? String.valueOf(s) : null;
                if (s == null || s.length() == 0) {
                    clearSearch.setVisibility(View.INVISIBLE);
                }
                // Don't do anything if the filter is empty
                if (mSearchTerm == null && newFilter == null) {
                    return;
                }
                // Don't do anything if the new filter is the same as the current filter
                if (mSearchTerm != null && mSearchTerm.equals(newFilter)) {
                    return;
                }
                clearSearch.setVisibility(View.VISIBLE);
                // Updates current filter to new filter
                mSearchTerm = newFilter;
                // Restarts the loader. This triggers onCreateLoader(), which builds the
                // necessary content Uri from mSearchTerm.
                getLoaderManager().restartLoader(0, null, OnboardingReportingToActivity.this);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        clearSearch.setOnClickListener(new View.OnClickListener() {
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
                if (heightDiff > (densityMultiplier * 75)) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    searchBar.setCursorVisible(true);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    searchBar.setCursorVisible(false);
                }
            }
        });

        getLoaderManager().initLoader(0, savedInstanceState, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mSearchTerm == null) {
            return new CursorLoader(this, UserProvider.CONTENT_URI, null,
                    UsersTable.COLUMN_ID + " <>?",
                    new String[]{App.accountId()+""},
                    null);
        } else {
            String filterString = "%" + mSearchTerm + "%";
            return new CursorLoader(this, UserProvider.CONTENT_URI, null,
                    UsersTable.COLUMN_ID + "<> ? AND ("
                            + UsersTable.CLM_LAST_NAME + " LIKE ? OR "
                            + UsersTable.CLM_FIRST_NAME + " LIKE ?)  AND "
                            + UsersTable.CLM_IS_ARCHIVED + " = 0",
                    new String[] { App.accountId() + "" , filterString, filterString},
                    UsersTable.CLM_FIRST_NAME);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        contactsAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
