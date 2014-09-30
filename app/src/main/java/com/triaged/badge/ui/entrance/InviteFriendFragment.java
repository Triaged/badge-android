package com.triaged.badge.ui.entrance;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.ui.IRow;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 * A simple {@link Fragment} subclass.
 */
public class InviteFriendFragment extends Fragment {

    static String myAccountHost;
    static boolean isFromACompany = false;
    PhoneContactAdapter contactAdapter;

    @InjectView(R.id.contacts_listview) ListView contactsListView;
    @InjectView(R.id.progressBar) ProgressBar progressBar;


    public InviteFriendFragment() { }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_invite_friend, container, false);
        ButterKnife.inject(this, root);

        getActivity().getActionBar().show();

        isFromACompany = SharedPreferencesUtil.getBoolean(R.string.pref_is_a_company_email_key, false);
        String accountEmail = SharedPreferencesUtil.getString(R.string.pref_account_email_key, "");
        int atSignIndex = accountEmail.lastIndexOf('@');
        if (atSignIndex > 0) {
            myAccountHost = accountEmail.substring(atSignIndex + 1);
        } else {
            throw new IllegalStateException("Cannot find an email address associated with the current account!");
        }

        getLoaderManager().initLoader(0, savedInstanceState, new LoaderManager.LoaderCallbacks<List<IRow>>() {
            @Override
            public Loader<List<IRow>> onCreateLoader(int id, Bundle args) {
                return new ContactLoader(getActivity());
            }

            @Override
            public void onLoadFinished(Loader<List<IRow>> loader, List<IRow> data) {
                progressBar.setVisibility(View.GONE);
                contactAdapter = new PhoneContactAdapter(getActivity(), R.layout.row_phone_contact_invite, data);
                contactsListView.setAdapter(contactAdapter);
            }

            @Override
            public void onLoaderReset(Loader<List<IRow>> loader) {

            }
        }).forceLoad();
        return root;
    }


    private static boolean isColleague(String email) {
        int atSignIndex = email.indexOf('@');
        if (atSignIndex > 0) {
            if (myAccountHost.equals(email.substring(atSignIndex + 1))) return true;
        }
        return false;
    }

    @DebugLog
    public static List<IRow> createContactsList() {
        ContentResolver contentResolver = App.context().getContentResolver();
        String[] projection = {
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };
        Cursor contactsCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                projection, null, null, ContactsContract.Contacts.DISPLAY_NAME + " COLLATE NOCASE");

        if (contactsCursor.getCount() < 1) return new ArrayList<IRow>(0);

        List<IRow> contacts = new ArrayList<IRow>(contactsCursor.getCount());
        int colleagueCounter = 0;
        while (contactsCursor.moveToNext()) {
            String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
            String contactName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            Cursor emailCursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null);
            PhoneContact[] emailsOfContact = new PhoneContact[emailCursor.getCount()];

            for( int i = 0; emailCursor.moveToNext(); i++) {
                String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                PhoneContact newContact = new PhoneContact();
                newContact.id = contactId;
                newContact.name = contactName;
                newContact.email = email;
                if (contactName.equals(email)) {
                    newContact.hideSubtext = true;
                }
                if (isFromACompany && isColleague(email)) {
                    newContact.isColleague = true;
                    contacts.add(colleagueCounter, newContact);
                    colleagueCounter++;
                } else {
                    contacts.add(newContact);
                }
                emailsOfContact[i] = newContact;
            }
            emailCursor.close();

            if (Integer.parseInt(contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null);
                if (phoneCursor.moveToFirst()) {
                    String phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (emailsOfContact.length > 0) {
                        for (PhoneContact contact : emailsOfContact) {
                            contact.phone = phone;
                        }
                    } else {
                        PhoneContact newContact = new PhoneContact();
                        newContact.id = contactId;
                        newContact.name = contactName;
                        newContact.phone = phone;
                        if (!contactName.equals(phone)) {
                            contacts.add(newContact);
                        }
                    }
                    phoneCursor.close();
                }
            }
        }

        if (colleagueCounter > 0) {
            HeaderRow colleagueHeaderRow = new HeaderRow(myAccountHost);
            contacts.add(0, colleagueHeaderRow);

            HeaderRow othersHeaderRow = new HeaderRow("Other Contacts");
            contacts.add(colleagueCounter + 1, othersHeaderRow);
        } else {
            HeaderRow othersHeaderRow = new HeaderRow("Contacts");
            contacts.add(colleagueCounter, othersHeaderRow);
        }
        return  contacts;
    }

    static class ContactLoader extends AsyncTaskLoader<List<IRow>> {
        public ContactLoader(Context context) {
            super(context);
        }

        @Override
        public List<IRow> loadInBackground() {
            return createContactsList();
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.invite_friend_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next_button) {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
            return true;
        }
        return false;
    }

    static class PhoneContact implements IRow {
        String id;
        String name;
        String phone;
        String email;
        String avatarUri;
        boolean hasInvited;
        boolean isColleague;
        boolean hideSubtext;

        @Override
        public int getType() {
            return IRow.CONTENT_ROW;
        }
    }

    static class HeaderRow implements IRow {
        String name;

        HeaderRow(String name) {
            this.name = name;
        }

        @Override
        public int getType() {
            return IRow.HEADER_ROW;
        }
    }
}
