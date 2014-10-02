package com.triaged.badge.ui.home;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.App;
import com.triaged.badge.app.MessageProcessor;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.ui.base.BadgeActivity;
import com.triaged.badge.ui.base.views.ButtonWithFont;
import com.triaged.badge.ui.base.views.CustomLayoutParams;
import com.triaged.badge.ui.base.views.FlowLayout;
import com.triaged.badge.ui.home.adapters.UserAdapter;
import com.triaged.badge.ui.messaging.MessagingActivity;

import org.json.JSONException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.RetrofitError;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Will on 7/28/14.
 *
 * Revised by Sadegh on 9/26/14.
 */
public class MessageNewActivity extends BadgeActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private HashMap<Integer, String> recipients = null;
    private CustomLayoutParams tagItemLayoutParams;
    private float densityMultiplier = 1;
    private boolean keyboardVisible = false;
    private String mSearchTerm = null;
    private UserAdapter contactsAdapter = null;

    @InjectView(R.id.contacts_list) StickyListHeadersListView contactsListView;
    @InjectView(R.id.search_bar) EditText searchBar;
    @InjectView(R.id.clear_search) ImageButton clearSearch;
    @InjectView(R.id.user_tags) FlowLayout userTagsWrapper;
    @InjectView(R.id.activity_root) View rootView;

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
                    HashSet<Integer> userIdSet = new HashSet<Integer>(recipients.keySet());
                    userIdSet.add(App.dataProviderServiceBinding.getLoggedInUser().id);
                    final Integer[] recipientIds = userIdSet.toArray(new Integer[userIdSet.size()]);
                    Arrays.sort(recipientIds);
                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            try {
                                return MessageProcessor.getInstance().createThreadSync(recipientIds);
                            } catch (RetrofitError e) {
                                toastMessage("Network issue occurred. Try again later.");
                                App.gLogger.e(e);
                            } catch (JSONException e) {
                                toastMessage("Unexpected response from server.");
                            } catch (OperationApplicationException e ) {
                                toastMessage("Unexpected response from server.");
                            } catch ( RemoteException  e){
                                toastMessage("Unexpected response from server.");
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String threadId) {
                            if (threadId != null) {
                                Intent intent = new Intent(MessageNewActivity.this, MessagingActivity.class);
                                intent.putExtra(MessagingActivity.THREAD_ID_EXTRA, threadId);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            }
                        }
                    }.execute();
                } else {
                    Toast.makeText(MessageNewActivity.this, "Please select a recipient", Toast.LENGTH_SHORT).show();
                }
            }
        });

        actionBar.setCustomView(backButtonBar, layoutParams);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.activity_messages_new);
        ButterKnife.inject(this);

        recipients = new HashMap<Integer, String>();

        contactsAdapter = new UserAdapter(this, null, R.layout.item_contact_no_msg);
        contactsListView.setAdapter(contactsAdapter);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserAdapter.ViewHolder holder = (UserAdapter.ViewHolder) view.getTag();
                addRecipient(holder.contactId, holder.name);
                searchBar.setText("");
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newFilter = !TextUtils.isEmpty(s) ? String.valueOf(s) : null;

                if (s == null || s.length() == 0) {
                    clearSearch.setVisibility(View.INVISIBLE);
                }

                // Don't do anything if the filter is empty
                if (mSearchTerm == null && newFilter == null) {
                    return;
                }
                // Don't do anything if the new filter is the same as the current filter
                if (mSearchTerm != null && mSearchTerm.equals(newFilter)) {
                    return;
                }
                clearSearch.setVisibility(View.VISIBLE);
                // Updates current filter to new filter
                mSearchTerm = newFilter;
                // Restarts the loader. This triggers onCreateLoader(), which builds the
                // necessary content Uri from mSearchTerm.
                getLoaderManager().restartLoader(0, null, MessageNewActivity.this);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText("");
            }
        });

        densityMultiplier = getResources().getDisplayMetrics().density;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int rootViewHeight = rootView.getRootView().getHeight();
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                rootView.getWindowVisibleDisplayFrame(r);
                int heightDiff = rootViewHeight - (r.bottom - r.top);
                if (heightDiff > (densityMultiplier * 75)) { // if more than 75 dp, its probably a keyboard...
                    keyboardVisible = true;
                    searchBar.setCursorVisible(true);
                } else if (keyboardVisible) {
                    keyboardVisible = false;
                    searchBar.setCursorVisible(false);
                }
            }
        });

        tagItemLayoutParams = new CustomLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagItemLayoutParams.setMargins(0, (int) (4 * densityMultiplier), (int) (4 * densityMultiplier), 0);

        getLoaderManager().initLoader(0, savedInstanceState, this);
    }

    private void addRecipient(final int contactId, final String contactName) {
        if (!recipients.containsKey(contactId)) {
            if (recipients.size() == 0) {
                userTagsWrapper.setVisibility(View.VISIBLE);
            }
            recipients.put(contactId, contactName);
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
                                    recipients.remove(contactId);
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


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mSearchTerm == null) {
            return new CursorLoader(this, UserProvider.CONTENT_URI, null,
                    UsersTable.COLUMN_ID + " <>?",
                    new String[]{App.accountId()+""},
                    null);
        } else {
            String filterString = "%" + mSearchTerm + "%";
            return new CursorLoader(this, UserProvider.CONTENT_URI, null,
                    UsersTable.COLUMN_ID + "<> ? AND ("
                            + UsersTable.CLM_LAST_NAME + " LIKE ? OR "
                            + UsersTable.CLM_FIRST_NAME + " LIKE ?)  AND "
                            + UsersTable.CLM_IS_ARCHIVED + " = 0",
                    new String[] { App.accountId() + "" , filterString, filterString},
                    UsersTable.CLM_FIRST_NAME);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        contactsAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    private void toastMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
