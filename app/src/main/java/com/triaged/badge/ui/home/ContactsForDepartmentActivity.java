package com.triaged.badge.ui.home;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.ui.base.BackButtonActivity;
import com.triaged.badge.ui.home.adapters.ContactsWithoutHeadingsAdapter;
import com.triaged.badge.ui.profile.ProfileActivity;

/**
 * Contacts for a given department
 * <p/>
 * Created by Will on 7/17/14.
 * Revised by Sadegh on 9/26/14.
 */
public class ContactsForDepartmentActivity extends BackButtonActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String DEPARTMENT_NAME_EXTRA = "DEPARTMENT_NAME";
    public static final String DEPARTMENT_ID_EXTRA = "DEPARTMENT_ID";

    private int departmentId;

    private ListView contactsForDepartmentList;
    private ContactsWithoutHeadingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contacts_for_department);

        contactsForDepartmentList = (ListView) findViewById(R.id.contacts_for_department_list);
        contactsForDepartmentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int clickedId = ((ContactsWithoutHeadingsAdapter.ViewHolder)view.getTag()).id;
                Intent intent;
                if (clickedId == dataProviderServiceBinding.getLoggedInUser().id) {
                    //TODO, use profile fragment
                    intent = new Intent(ContactsForDepartmentActivity.this, MainActivity.class);
//                    intent = new Intent(ContactsForDepartmentActivity.this, MyProfileActivity.class);
                } else {
                    intent = new Intent(ContactsForDepartmentActivity.this, ProfileActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra(ProfileActivity.PROFILE_ID_EXTRA, clickedId);
                startActivity(intent);
            }
        });

        Bundle extra = getIntent().getExtras();
        String departmentName = extra.getString(DEPARTMENT_NAME_EXTRA);
        departmentId = extra.getInt(DEPARTMENT_ID_EXTRA);
        backButton.setText(departmentName);

        adapter = new ContactsWithoutHeadingsAdapter(this, null);
        contactsForDepartmentList.setAdapter(adapter);
        getLoaderManager().initLoader(0, savedInstanceState, this);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, ContactProvider.CONTENT_URI,
                null, ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID + "=?",
                new String[]{departmentId+""}, ContactsTable.COLUMN_CONTACT_FIRST_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
    }
}
