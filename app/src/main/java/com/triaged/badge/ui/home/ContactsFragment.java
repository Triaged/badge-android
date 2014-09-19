package com.triaged.badge.ui.home;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.ui.home.adapters.MyContactAdapter;
import com.triaged.badge.ui.home.adapters.MyDepartmentAdapter;
import com.triaged.badge.ui.profile.OtherProfileActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */

public class ContactsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    private final static int CONTACTS_TAB_ID = 1000;
    private final static int DEPARTMENTS_TAB_ID = 2000;

    private MyContactAdapter contactsAdapter;
    private MyDepartmentAdapter departmentAdapter;
    private String mSearchTerm = null;
    private int currentTab = CONTACTS_TAB_ID;

    private Typeface medium = null;
    private Typeface regular = null;


    @InjectView(R.id.contacts_list) StickyListHeadersListView contactsListView;
    @InjectView(R.id.departments_list) ListView departmentsListView;
    @InjectView(R.id.contacts_departments_tab) View bottomTabsWrapper;
    @InjectView(R.id.contacts_tab) Button contactsTab;
    @InjectView(R.id.departments_tab) Button departmentsTab;
    @InjectView(R.id.search_bar) EditText searchBar;
    @InjectView(R.id.clear_search) View clearSearch;


    @OnClick(R.id.contacts_tab)
    void tabContactsSelected() {
        if (currentTab != CONTACTS_TAB_ID) {
            clearSearch();
            currentTab = CONTACTS_TAB_ID;
            departmentsListView.setVisibility(View.INVISIBLE);
            contactsListView.setVisibility(View.VISIBLE);
            contactsTab.setTypeface(medium);
            departmentsTab.setTypeface(regular);
            searchBar.setHint(getActivity().getString(R.string.search_contacts));
        }
    }

    @OnClick(R.id.departments_tab)
    void tabDepartmentsSelected() {
        if (currentTab != DEPARTMENTS_TAB_ID) {
            clearSearch();
            currentTab = DEPARTMENTS_TAB_ID;
            departmentsListView.setVisibility(View.VISIBLE);
            contactsListView.setVisibility(View.INVISIBLE);
            departmentsTab.setTypeface(medium);
            contactsTab.setTypeface(regular);
            searchBar.setHint(getActivity().getString(R.string.search_departments));
        }
    }

    @OnClick(R.id.clear_search)
    void clearSearch() {
        if (mSearchTerm != null) {
            mSearchTerm = null;
            getLoaderManager().restartLoader(currentTab, null, this);
        }
        searchBar.setText("");
    }


    public static ContactsFragment newInstance() {
        ContactsFragment fragment = new ContactsFragment();
        return fragment;
    }
    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_contacts, container, false);
        ButterKnife.inject(this, root);

        contactsAdapter = new MyContactAdapter(getActivity(), null, R.layout.item_contact_with_msg);
        departmentAdapter = new MyDepartmentAdapter(getActivity(), null);

        contactsListView.setAdapter(contactsAdapter);
        departmentsListView.setAdapter(departmentAdapter);

        getLoaderManager().initLoader(CONTACTS_TAB_ID, null, this);
        getLoaderManager().initLoader(DEPARTMENTS_TAB_ID, null, this);

        medium = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Medium.ttf");
        regular = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Regular.ttf");

        searchBar.setHint(getActivity().getString(R.string.search_contacts));
        contactsTab.setTypeface(medium);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (App.dataProviderServiceBinding.getLoggedInUser() != null) {
                    int clickedId = ((MyContactAdapter.ViewHolder) view.getTag()).contactId;
                    if (clickedId != App.dataProviderServiceBinding.getLoggedInUser().id) {
                        Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.putExtra("PROFILE_ID", clickedId);
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
                MyDepartmentAdapter.ViewHolder viewHolder = (MyDepartmentAdapter.ViewHolder) view.getTag();
                Intent intent = new Intent(getActivity(), ContactsForDepartmentActivity.class);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_ID_EXTRA, viewHolder.id);
                intent.putExtra(ContactsForDepartmentActivity.DEPARTMENT_NAME_EXTRA, viewHolder.name);
                startActivity(intent);
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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
                getLoaderManager().restartLoader(currentTab, null, ContactsFragment.this);
            }

            @Override public void afterTextChanged(Editable s) { }
        });

        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACTS_TAB_ID) {
            if (mSearchTerm == null) {
                return new CursorLoader(getActivity(), ContactProvider.CONTENT_URI,
                        null, ContactsTable.COLUMN_CONTACT_IS_ARCHIVED + " = 0", null,
                        ContactsTable.COLUMN_CONTACT_FIRST_NAME);
            } else {
                String filterString = "%" + mSearchTerm + "%";
                return new CursorLoader(getActivity(), ContactProvider.CONTENT_URI,
                        null,
                        ContactsTable.COLUMN_CONTACT_LAST_NAME + " LIKE ? OR " +
                        ContactsTable.COLUMN_CONTACT_FIRST_NAME + " LIKE ?  AND " +
                        ContactsTable.COLUMN_CONTACT_IS_ARCHIVED + " = 0"
                        , new String[] { filterString, filterString},
                        ContactsTable.COLUMN_CONTACT_FIRST_NAME);
            }

        } else if (id == DEPARTMENTS_TAB_ID) {
            if (mSearchTerm == null) {
                return new CursorLoader(getActivity(), DepartmentProvider.CONTENT_URI, null,
                        DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS + " > 0 ",
                        null,
                        DepartmentsTable.COLUMN_DEPARTMENT_NAME);
            } else {
                return new CursorLoader(getActivity(), DepartmentProvider.CONTENT_URI, null,
                        DepartmentsTable.COLUMN_DEPARTMENT_NAME + " LIKE ? AND " +
                        DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS + " > 0 ",
                        new String[] {"%" + mSearchTerm + "%"},
                        DepartmentsTable.COLUMN_DEPARTMENT_NAME);
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

}
