package com.triaged.badge.ui.home;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.base.BackButtonActivity;
import com.triaged.badge.ui.home.adapters.ContactsWithoutHeadingsAdapter;
import com.triaged.badge.ui.profile.AbstractProfileActivity;
import com.triaged.badge.ui.profile.MyProfileActivity;
import com.triaged.badge.ui.profile.OtherProfileActivity;

/**
 * Contacts for a given department
 * <p/>
 * Created by Will on 7/17/14.
 */
public class ContactsForDepartmentActivity extends BackButtonActivity {

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
                int userId = dataProviderServiceBinding.getLoggedInUser().id;
                int clickedId = adapter.getCachedContact(position).id;
                Intent intent;
                if (userId == clickedId) {
                    intent = new Intent(ContactsForDepartmentActivity.this, MyProfileActivity.class);
                } else {
                    intent = new Intent(ContactsForDepartmentActivity.this, OtherProfileActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra(AbstractProfileActivity.PROFILE_ID_EXTRA, clickedId);
                startActivity(intent);
            }
        });

        Bundle extra = getIntent().getExtras();
        String departmentName = extra.getString(DEPARTMENT_NAME_EXTRA);
        departmentId = extra.getInt(DEPARTMENT_ID_EXTRA);
        backButton.setText(departmentName);

    }

    @Override
    protected void onDatabaseReady() {
        Cursor deptCursor = dataProviderServiceBinding.getContactsByDepartmentCursor(departmentId);
        adapter = new ContactsWithoutHeadingsAdapter(this, deptCursor, dataProviderServiceBinding);
        contactsForDepartmentList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
    }


    @Override
    protected void onDestroy() {
        adapter.destroy();
        super.onDestroy();
    }
}
