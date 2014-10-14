package com.triaged.badge.ui.home;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.app.SyncManager;
import com.triaged.badge.ui.home.adapters.DepartmentAdapter;
import com.triaged.badge.ui.home.adapters.UserAdapter;
import com.triaged.badge.ui.profile.ProfileActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */

public class UsersFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int CONTACTS_TAB_ID = 1000;
    private final static int DEPARTMENTS_TAB_ID = 2000;

    private UserAdapter contactsAdapter;
    private DepartmentAdapter departmentAdapter;
    private String mSearchTerm = null;
    private int currentTab = CONTACTS_TAB_ID;

    @InjectView(R.id.contacts_list) StickyListHeadersListView contactsListView;
    @InjectView(R.id.departments_list) ListView departmentsListView;
    @InjectView(R.id.contacts_departments_tab) View bottomTabsWrapper;
    @InjectView(R.id.contacts_tab) Button contactsTab;
    @InjectView(R.id.departments_tab) Button departmentsTab;


    @OnClick(R.id.contacts_tab)
    void tabContactsSelected() {
        if (currentTab != CONTACTS_TAB_ID) {
            clearSearch();
            currentTab = CONTACTS_TAB_ID;
            selectActiveTab();
            departmentsListView.setVisibility(View.INVISIBLE);
            contactsListView.setVisibility(View.VISIBLE);
        }
    }

    private void selectActiveTab() {
        if (currentTab == CONTACTS_TAB_ID) {
            contactsTab.setSelected(true);
            departmentsTab.setSelected(false);
        } else {
            departmentsTab.setSelected(true);
            contactsTab.setSelected(false);
        }
    }

    @OnClick(R.id.departments_tab)
    void tabDepartmentsSelected() {
        if (currentTab != DEPARTMENTS_TAB_ID) {
            clearSearch();
            currentTab = DEPARTMENTS_TAB_ID;
            selectActiveTab();
            departmentsListView.setVisibility(View.VISIBLE);
            contactsListView.setVisibility(View.INVISIBLE);
        }
    }

    void clearSearch() {
        if (mSearchTerm != null) {
            mSearchTerm = null;
            mSearchView.setQuery("", false);
            getLoaderManager().restartLoader(currentTab, null, this);
        }
    }


    public static UsersFragment newInstance() {
        UsersFragment fragment = new UsersFragment();
        return fragment;
    }
    public UsersFragment() { }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_contacts, container, false);
        ButterKnife.inject(this, root);
        setHasOptionsMenu(true);

        selectActiveTab();

        contactsAdapter = new UserAdapter(getActivity(), null, R.layout.item_contact_with_msg);
        departmentAdapter = new DepartmentAdapter(getActivity(), null);

        contactsListView.setAdapter(contactsAdapter);
        departmentsListView.setAdapter(departmentAdapter);

        getLoaderManager().initLoader(CONTACTS_TAB_ID, null, this);
        getLoaderManager().initLoader(DEPARTMENTS_TAB_ID, null, this);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (SyncManager.getMyUser() != null) {
                    int clickedId = ((UserAdapter.ViewHolder) view.getTag()).contactId;
                    if (clickedId != SyncManager.getMyUser().id) {
                        Intent intent = new Intent(getActivity(), ProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra(ProfileActivity.PROFILE_ID_EXTRA, clickedId);
                        startActivity(intent);
                    } else {
                        // Normally won't happen.
                    }

                }
            }
        });

        departmentsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DepartmentAdapter.ViewHolder viewHolder = (DepartmentAdapter.ViewHolder) view.getTag();
                Intent intent = new Intent(getActivity(), ContactsForDepartmentActivity.class);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_ID_EXTRA, viewHolder.id);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_NAME_EXTRA, viewHolder.name);
                startActivity(intent);
            }
        });
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACTS_TAB_ID) {
            if (mSearchTerm == null) {
                return new CursorLoader(getActivity(), UserProvider.CONTENT_URI,
                        null, UsersTable.CLM_IS_ARCHIVED + " = 0", null,
                        UsersTable.CLM_FIRST_NAME);
            } else {
                String filterString = "%" + mSearchTerm + "%";
                return new CursorLoader(getActivity(), UserProvider.CONTENT_URI,
                        null,
                        UsersTable.CLM_LAST_NAME + " LIKE ? OR " +
                        UsersTable.CLM_FIRST_NAME + " LIKE ?  AND " +
                        UsersTable.CLM_IS_ARCHIVED + " = 0"
                        , new String[] { filterString, filterString},
                        UsersTable.CLM_FIRST_NAME);
            }

        } else if (id == DEPARTMENTS_TAB_ID) {
            if (mSearchTerm == null) {
                return new CursorLoader(getActivity(), DepartmentProvider.CONTENT_URI, null,
                        DepartmentsTable.CLM_CONTACTS_NUMBER + " > 0 ",
                        null,
                        DepartmentsTable.CLM_NAME);
            } else {
                return new CursorLoader(getActivity(), DepartmentProvider.CONTENT_URI, null,
                        DepartmentsTable.CLM_NAME + " LIKE ? AND " +
                        DepartmentsTable.CLM_CONTACTS_NUMBER + " > 0 ",
                        new String[] {"%" + mSearchTerm + "%"},
                        DepartmentsTable.CLM_NAME);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CONTACTS_TAB_ID) {
            contactsAdapter.swapCursor(data);

        } else if (loader.getId() == DEPARTMENTS_TAB_ID) {
            departmentAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    SearchView mSearchView;
    MenuItem mSearchMenuItem;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.users_fragment, menu);

        final Menu fMenu = menu;
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                fMenu.findItem(R.id.action_create_group).setVisible(false);
                ((MainActivity) getActivity()).viewPager.setPagingEnabled(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                fMenu.findItem(R.id.action_create_group).setVisible(true);

                if (!TextUtils.isEmpty(mSearchTerm)) {
                    mSearchTerm = null;
                    getLoaderManager().restartLoader(currentTab, null, UsersFragment.this);
                }

                ((MainActivity) getActivity()).viewPager.setPagingEnabled(true);
                return true;
            }
        });
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
//        mSearchView.setQueryHint(getString(R.string.search_contacts_hint_text));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override public boolean onQueryTextChange(String newText) {
                String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
                if (mSearchTerm == null && newFilter == null) {
                    return true;
                }
                if (mSearchTerm != null && mSearchTerm.equals(newFilter)) {
                    return true;
                }
                mSearchTerm = newFilter;
                getLoaderManager().restartLoader(currentTab, null, UsersFragment.this);
                return true;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
