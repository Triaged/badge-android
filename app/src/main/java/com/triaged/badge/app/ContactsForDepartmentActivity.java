package com.triaged.badge.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;

/**
 * Contacts for a given department
 *
 * Created by Will on 7/17/14.
 */
public class ContactsForDepartmentActivity extends BadgeActivity {

    private ListView contactsForDepartmentList;
    private ContactsAdapterWithoutHeadings adapter;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contacts_for_department);

        BadgeApplication app = (BadgeApplication) getApplication();
        dataProviderServiceBinding = app.dataProviderServiceBinding;

        TextView backButton;
        backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

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
        adapter = new ContactsAdapterWithoutHeadings(this, deptCursor, dataProviderServiceBinding);
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