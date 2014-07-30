package com.triaged.badge.app;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Will on 7/28/14.
 */
public class MessageNewActivity extends BadgeActivity {

    public static final String RECIPIENT_ID_EXTRA = "recipient_id";

    private StickyListHeadersListView contactsListView = null;
    private ContactsAdapter contactsAdapter = null;
    protected DataProviderService.LocalBinding dataProviderServiceBinding = null;
    private ContactsAdapterWithoutHeadings searchResultsAdapter = null;

    private EditText searchBar = null;
    private ImageButton clearButton = null;
    private ListView searchResultsList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflater = LayoutInflater.from(this);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View backButtonBar = inflater.inflate(R.layout.actionbar_new_message, null);

        TextView backButton = (TextView) backButtonBar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_messages_new);

        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MessageNewActivity.this, MessageShowActivity.class);
                intent.putExtra( RECIPIENT_ID_EXTRA, contactsAdapter.getCachedContact(position).id);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

        });

        searchResultsList = (ListView) findViewById(R.id.search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MessageNewActivity.this, MessageShowActivity.class);
                intent.putExtra(RECIPIENT_ID_EXTRA, contactsAdapter.getCachedContact(position).id);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        searchBar = (EditText) findViewById(R.id.search_bar);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = searchBar.getText().toString();
                if (text.length() > 0) {
                    clearButton.setVisibility(View.VISIBLE);
                    searchResultsAdapter.setFilter( text );
                    searchResultsList.setVisibility(View.VISIBLE);
                    contactsListView.setVisibility(View.GONE);

                } else {
                    clearButton.setVisibility(View.GONE);
                    contactsListView.setVisibility(View.VISIBLE);
                    searchResultsList.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        searchBar.addTextChangedListener(tw);

        clearButton = (ImageButton) findViewById(R.id.clear_search);

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        loadContacts();

    }

    @Override
    protected void onDestroy() {
        if( searchResultsAdapter != null ) {
            searchResultsAdapter.destroy();
        }
        if( contactsAdapter != null ) {
            contactsAdapter.destroy();
        }
        super.onDestroy();
    }

    private void loadContacts() {
        if( contactsAdapter != null ) {
            contactsAdapter.refresh();
        }
        else {
            contactsAdapter = new ContactsAdapter(this, dataProviderServiceBinding, R.layout.item_contact_no_msg);
            contactsListView.setAdapter(contactsAdapter);
        }
        if( searchResultsAdapter != null ) {
            searchResultsAdapter.refresh( dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser() );
        }
        else {
            searchResultsAdapter = new ContactsAdapterWithoutHeadings( this, dataProviderServiceBinding.getContactsCursorExcludingLoggedInUser(), dataProviderServiceBinding, false );
            searchResultsList.setAdapter( searchResultsAdapter );
        }
    }


}
