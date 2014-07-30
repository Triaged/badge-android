package com.triaged.badge.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.views.ButtonWithFont;
import com.triaged.badge.app.views.ContactsAdapter;
import com.triaged.badge.app.views.ContactsAdapterWithoutHeadings;
import com.triaged.badge.app.views.CustomLayoutParams;
import com.triaged.badge.app.views.FlowLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private List<HashMap<Integer, String>> recipients = null;

    private FlowLayout userTagsWrapper = null;
    private CustomLayoutParams tagItemLayoutParams;
    private float densityMultiplier = 1;

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

        TextView nextButton = (TextView) backButtonBar.findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recipients.size() > 0) {
                    Intent intent = new Intent(MessageNewActivity.this, MessageShowActivity.class);
                    intent.putExtra(RECIPIENT_ID_EXTRA, recipients.get(0).get(0));
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    Toast.makeText(MessageNewActivity.this, "Please select a recipient", Toast.LENGTH_SHORT).show();
                }
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_messages_new);

        recipients = new ArrayList<HashMap<Integer, String>>();
        userTagsWrapper = (FlowLayout) findViewById(R.id.user_tags);

        contactsListView = (StickyListHeadersListView) findViewById(R.id.contacts_list);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                addRecipient(contactsAdapter.getCachedContact(position).id, contactsAdapter.getCachedContact(position).name);
            }

        });

        searchResultsList = (ListView) findViewById(R.id.search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                addRecipient(searchResultsAdapter.getCachedContact(position).id, searchResultsAdapter.getCachedContact(position).name);
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

        densityMultiplier = getResources().getDisplayMetrics().density;

        tagItemLayoutParams = new CustomLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagItemLayoutParams.setMargins(0, (int) (4 * densityMultiplier), (int) (4 * densityMultiplier), 0);
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

    private void addRecipient(final int contactId, final String contactName) {
        if (!recipients.contains(contactId)) {
            if (recipients.size() == 0) {
                userTagsWrapper.setVisibility(View.VISIBLE);
            }
            final HashMap<Integer, String> userHash = new HashMap<Integer, String>();
            userHash.put(contactId, contactName);
            recipients.add(userHash);
            LayoutInflater inflater = LayoutInflater.from(this);

            final ButtonWithFont newButton = (ButtonWithFont) inflater.inflate(R.layout.button_user_tag, null);
            newButton.setLayoutParams(tagItemLayoutParams);
            newButton.setTag(contactId);
            newButton.setText(contactName);
            newButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MessageNewActivity.this)
                            .setTitle("Remove " + contactName + "?")
                            .setMessage("Are you sure you want to remove " + contactName + "?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    recipients.remove(userHash);
                                    userTagsWrapper.removeView(newButton);
                                    if (recipients.size() == 0) {
                                        userTagsWrapper.setVisibility(View.GONE);
                                    }
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });
            userTagsWrapper.addView(newButton);
        }
    }

}
