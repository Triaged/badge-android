package com.triaged.badge.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.data.Contact;

import java.util.ArrayList;


public class ContactsActivity extends BadgeActivity {

    private ListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;
    private ArrayList<Contact> contactsList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactsListView = (ListView) findViewById(R.id.contacts_list);
        contactsList = new ArrayList<Contact>();
        contactsAdapter = new ContactsAdapter(this, R.layout.item_contact, contactsList);
        contactsListView.setAdapter(contactsAdapter);
    }

}