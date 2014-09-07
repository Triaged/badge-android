package com.triaged.badge.ui.home;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.ui.home.adapters.MyContactAdapter;
import com.triaged.badge.ui.home.adapters.MyDepartmentAdapter;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */

public class ContactsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    private final static int CONTACTS_LOAD_ID = 1000;
    private final static int DEPARTMENTS_LOAD_ID = 2000;


    @InjectView(R.id.contacts_list) StickyListHeadersListView contactsListView;
    @InjectView(R.id.departments_list) ListView departmentsListView;
    @InjectView(R.id.contacts_departments_tab) View bottomTabsWrapper;
    @InjectView(R.id.contacts_tab) View contactsTab;
    @InjectView(R.id.departments_tab) View departmentsTab;

    @OnClick(R.id.contacts_tab)
    void tabContactsSelected() {
        departmentsListView.setVisibility(View.INVISIBLE);
        contactsListView.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.departments_tab)
    void tabDepartmentsSelected() {
        departmentsListView.setVisibility(View.VISIBLE);
        contactsListView.setVisibility(View.INVISIBLE);
    }


    MyContactAdapter contactsAdapter;
    MyDepartmentAdapter departmentAdapter;

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

        contactsAdapter = new MyContactAdapter(getActivity(), null);
        departmentAdapter = new MyDepartmentAdapter(getActivity(), null);

        contactsListView.setAdapter(contactsAdapter);
        departmentsListView.setAdapter(departmentAdapter);

        getLoaderManager().initLoader(CONTACTS_LOAD_ID, null, this);
        getLoaderManager().initLoader(DEPARTMENTS_LOAD_ID, null, this);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CONTACTS_LOAD_ID) {
            return new CursorLoader(getActivity(), ContactProvider.CONTENT_URI,
                    null, ContactsTable.COLUMN_CONTACT_IS_ARCHIVED + " = 0", null,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME);

        } else if (id == DEPARTMENTS_LOAD_ID) {
            return new CursorLoader(getActivity(), DepartmentProvider.CONTENT_URI, null,
                    DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS + " > 0 ",
                    null,
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == CONTACTS_LOAD_ID) {
            contactsAdapter.swapCursor(data);

        } else if (loader.getId() == DEPARTMENTS_LOAD_ID) {
            departmentAdapter.swapCursor(data);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
