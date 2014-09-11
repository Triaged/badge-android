package com.triaged.badge.ui.messaging;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.MessageThread;
import com.triaged.badge.net.api.MessageThreadApi;
import com.triaged.badge.net.api.requests.MessageThreadRequest;
import com.triaged.badge.ui.home.adapters.MyContactAdapter;
import com.triaged.badge.ui.profile.AbstractProfileActivity;
import com.triaged.badge.ui.profile.OtherProfileActivity;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MenuFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MenuFragment extends Fragment  {
    // the fragment initialization thread_id parameter.
    private static final String ARG_THREAD_ID = "thread_id";

    private String mThreadId;
    MyContactAdapter adapter;
    List<Contact> participants;
    MatrixCursor cursor;

    @InjectView(R.id.participantsList) ListView participantsListView;
    View menuHeader;
    TextView groupNameTextView;
    CheckBox muteCheckBox;

    @OnItemClick(R.id.participantsList)
    void goToProfile(AdapterView<?> parent, View view, int position, long id) {
        int contactId = ((MyContactAdapter.ViewHolder)view.getTag()).contactId;
        Intent profileIntent = new Intent(getActivity(), OtherProfileActivity.class);
        profileIntent.putExtra(AbstractProfileActivity.PROFILE_ID_EXTRA, contactId);
        startActivity(profileIntent);
    }



    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param threadId Id of the the thread we want to show its messages.
     * @return A new instance of fragment MessagingFragment.
     */
    public static MenuFragment newInstance(String threadId) {
        MenuFragment fragment = new MenuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THREAD_ID, threadId);
        fragment.setArguments(args);
        return fragment;
    }

    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mThreadId = getArguments().getString(ARG_THREAD_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_messaging_right_menu, container, false);
        ButterKnife.inject(this, root);

        String usersJsonString = SharedPreferencesUtil.getString(mThreadId, "[]");
        try {
            JSONArray users = new JSONArray(usersJsonString);

            cursor = new MatrixCursor(new String[] {
                    ContactsTable.COLUMN_ID,
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME,
                    ContactsTable.COLUMN_CONTACT_JOB_TITLE,
                    ContactsTable.COLUMN_CONTACT_AVATAR_URL
            }, users.length());

            participants = new ArrayList<Contact>(users.length());
            for (int i = 0; i < users.length(); i++) {
                Integer userId = users.getInt(i);
                if (userId != App.dataProviderServiceBinding.getLoggedInUser().id) {
                    final Contact c = App.dataProviderServiceBinding.getContact(userId);
                    if (c != null && !c.isArchived) {
                        participants.add(c);
                        addContactToCursor(c);
                    }
                }
            }

        } catch (JSONException e) {
            App.gLogger.e(e);
        }

        adapter = new MyContactAdapter(getActivity(), cursor, R.layout.item_contact_with_msg);
        participantsListView.setAdapter(adapter);

        setupMenuListHeaderItems();

        return root;
    }

    String groupName = null;
    private void setupMenuListHeaderItems() {
        menuHeader = LayoutInflater.from(getActivity()).inflate(R.layout.header_view_for_participants,
                participantsListView, false );
        participantsListView.addHeaderView(menuHeader);

        groupNameTextView = (TextView) menuHeader.findViewById(R.id.group_name_text);
        muteCheckBox = (CheckBox) menuHeader.findViewById(R.id.mute_checkbox);

        groupName = SharedPreferencesUtil.getString("name_" + mThreadId, "Group");
        groupNameTextView.setText(groupName);

        Boolean isMute = SharedPreferencesUtil.getBoolean("is_mute_" + mThreadId, false);
        muteCheckBox.setChecked(isMute);

        View groupNameRow = menuHeader.findViewById(R.id.group_name_row);
        if (participants.size() > 1) {
            groupNameRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditGroupNameDialog();
                }
            });
        } else {
            groupNameRow.setVisibility(View.GONE);
        }

        View groupMuteRow = menuHeader.findViewById(R.id.group_mute_row);
        groupMuteRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMute();
            }
        });
    }

    private void requestMute() {
        MessageThreadApi messageThreadApi = App.restAdapter.create(MessageThreadApi.class);
        Callback<Response> muteCallback = new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                muteCheckBox.toggle();
                SharedPreferencesUtil.store("is_mute_" + mThreadId, muteCheckBox.isChecked());
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getActivity(), "A problem occurred during sending the request.",
                        Toast.LENGTH_LONG).show();
            }
        };

        if (muteCheckBox.isChecked()) {
            messageThreadApi.unMute(mThreadId, muteCallback);
        } else {
            messageThreadApi.mute(mThreadId, muteCallback);
        }
    }

    private void showEditGroupNameDialog() {
        final EditText input = new EditText(getActivity());
        if (groupName != null) {
            input.setText(groupName);
        }
        input.setHint("Enter a name");

        new AlertDialog.Builder(getActivity())
                .setTitle("Group name")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(input.getText())) {
                            sendRenameRequest(input.getText().toString());

                        } else {
                            Toast.makeText(getActivity(), "Group name could not be empty!", Toast.LENGTH_LONG).show();
                        }
                    }

                    private void sendRenameRequest(String newName) {
                        MessageThreadRequest request = new MessageThreadRequest(new MessageThread(newName));
                        MessageThreadApi messageThreadApi = App.restAdapter.create(MessageThreadApi.class);
                        messageThreadApi.setName(mThreadId, request, new Callback<Response>() {
                                    @Override
                                    public void success(Response response, Response response2) {
                                        App.gLogger.i("response:: success:: " + response);
                                        groupName = input.getText().toString();
                                        SharedPreferencesUtil.store("name_" + mThreadId, groupName);
                                        groupNameTextView.setText(groupName);
                                        // Since right now we don't have table for thread,
                                        // should notify observers in the following way.
                                        getActivity().getContentResolver().notifyChange(MessageProvider.CONTENT_URI, null);
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        Toast.makeText(getActivity(), "A problem occurred during sending the request.", Toast.LENGTH_LONG).show();
                                        App.gLogger.e("response:: error", error);
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }


    private void addContactToCursor(Contact contact) {
        cursor.addRow(new Object[]{contact.id,
                contact.firstName,
                contact.lastName,
                contact.jobTitle,
                contact.avatarUrl
        });
    }


}
