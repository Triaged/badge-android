package com.triaged.badge.app;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ProfileManagesAdapter;

/**
 * Contacts for a given department
 *
 * Created by Will on 7/17/14.
 */
public class ContactsForDepartmentActivity extends BackButtonActivity {

    private ListView contactsForDepartmentList;
    private ProfileManagesAdapter adapter;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

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
                intent.putExtra("PROFILE_ID", clickedId);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        int departmentId = intent.getIntExtra("DEPARTMENT_ID", 0);
        String departmentName = intent.getStringExtra("DEPARTMENT_NAME");
        backButton.setText(departmentName);
        Cursor deptCursor = dataProviderServiceBinding.getContactsByDepartmentCursor(departmentId);
        adapter = new ProfileManagesAdapter(this, deptCursor, dataProviderServiceBinding);
        contactsForDepartmentList.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
    }


}
