package com.triaged.badge.app.views;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.triaged.badge.data.Contact;

import java.util.ArrayList;

/**
 * Common ArrayAdapter for Contacts ListViews
 *
 * Created by Will on 7/7/14.
 */
public class ContactsAdapter extends ArrayAdapter<Contact> {

    public ContactsAdapter(Context context, int resource, ArrayList<Contact> contacts) {
        super(context, resource);
    }

}
