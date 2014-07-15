package com.triaged.badge.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ContactsAdapter;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Will on 7/14/14.
 */
public class OnboardingReportingToActivity extends BadgeActivity {

    private StickyListHeadersListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_reporting_to);
        TextView backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int userId = dataProviderServiceBinding.getLoggedInUser().id;
                int clickedId = contactsAdapter.getCachedContact(position).id;
                if (userId == clickedId) {
                    Toast.makeText(OnboardingReportingToActivity.this, "Please go back if you do not have a manager", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(OnboardingReportingToActivity.this, OnboardingPositionActivity.class);
                    intent.putExtra("REPORTS_TO_ID", contactsAdapter.getCachedContact(position).id);
                    intent.putExtra("REPORTS_TO_NAME", contactsAdapter.getCachedContact(position).name);
                    startActivity(intent);
                }
            }

        });
        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        loadContacts();
    }

    private void loadContacts() {
        if( contactsAdapter != null ) {
            contactsAdapter.refresh();
        }
        else {
            contactsAdapter = new ContactsAdapter(this, dataProviderServiceBinding, R.layout.item_contact_no_msg);
            contactsListView.setAdapter(contactsAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

}
