package com.triaged.badge.ui.messaging;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
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
import com.triaged.badge.database.provider.BThreadProvider;
import com.triaged.badge.database.provider.ThreadUserProvider;
import com.triaged.badge.database.table.BThreadUserTable;
import com.triaged.badge.database.table.BThreadsTable;
import com.triaged.badge.database.table.UsersTable;
import com.triaged.badge.models.MessageThread;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.ui.home.adapters.UserAdapter;
import com.triaged.badge.net.api.requests.MessageBThreadRequest;
import com.triaged.badge.ui.profile.ProfileActivity;

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
public class MenuFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String ARG_THREAD_ID = "thread_id";

    private String mThreadId;
    UserAdapter adapter;

    @InjectView(R.id.participantsList) ListView participantsListView;
    View menuHeader;
    TextView groupNameTextView;
    CheckBox muteCheckBox;

    @OnItemClick(R.id.participantsList)
    void goToProfile(AdapterView<?> parent, View view, int position, long id) {
        if (id < 0) { // If the header-view clicked somehow, do nothing
            return;
        }
        int userId = ((UserAdapter.ViewHolder)view.getTag()).contactId;
        Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
        profileIntent.putExtra(ProfileActivity.PROFILE_ID_EXTRA, userId);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_messaging_right_menu, container, false);
        ButterKnife.inject(this, root);
        setupMenuListHeaderItems();

        adapter = new UserAdapter(getActivity(), null, R.layout.item_contact_with_msg);
        participantsListView.setAdapter(adapter);

        getLoaderManager().initLoader(0, savedInstanceState, this);
        return root;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                ThreadUserProvider.CONTENT_URI_CONTACT_INFO,
                null, BThreadUserTable.CLM_THREAD_ID + "=? AND " +
                UsersTable.CLM_IS_ARCHIVED + "=0 AND " +
                UsersTable.COLUMN_ID + "!=?",
                new String[]{mThreadId, App.accountId() + ""},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        if(data.getCount() < 2) {
            participantsListView.removeHeaderView(menuHeader);
        } else {
            participantsListView.addHeaderView(menuHeader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void setupMenuListHeaderItems() {
        menuHeader = LayoutInflater.from(getActivity()).inflate(R.layout.header_view_for_participants,
                participantsListView, false );

        groupNameTextView = (TextView) menuHeader.findViewById(R.id.group_name_text);
        muteCheckBox = (CheckBox) menuHeader.findViewById(R.id.mute_checkbox);

        View groupNameRow = menuHeader.findViewById(R.id.group_name_row);
        groupNameRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditGroupNameDialog();
            }
        });

        View groupMuteRow = menuHeader.findViewById(R.id.group_mute_row);
        groupMuteRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMute();
            }
        });
    }

    private void requestMute() {
        Callback<Response> muteCallback = new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                ContentValues cv = new ContentValues(1);
                cv.put(BThreadsTable.CLM_IS_MUTED, !muteCheckBox.isChecked());
                getActivity().getContentResolver().update(BThreadProvider.CONTENT_URI, cv,
                        BThreadsTable.COLUMN_ID + "=?", new String[]{mThreadId});
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getActivity(), "A problem occurred during sending the request.",
                        Toast.LENGTH_LONG).show();
            }
        };

        if (muteCheckBox.isChecked()) {
            RestService.instance().messaging().unmuteThread(mThreadId, muteCallback);
        } else {
            RestService.instance().messaging().muteThread(mThreadId, muteCallback);
        }
    }

    private void showEditGroupNameDialog() {
        final EditText input = new EditText(getActivity());
        if (!TextUtils.isEmpty(groupNameTextView.getText())) {
            input.setText(groupNameTextView.getText());
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
                        MessageBThreadRequest request = new MessageBThreadRequest(new MessageThread(newName));

                        RestService.instance().messaging().threadSetName(mThreadId, request, new Callback<Response>() {
                            @Override
                            public void success(Response response, Response response2) {
                                ContentValues cv = new ContentValues(1);
                                cv.put(BThreadsTable.CLM_NAME, input.getText().toString());
                                getActivity().getContentResolver().update(BThreadProvider.CONTENT_URI, cv,
                                        BThreadsTable.COLUMN_ID + "=?", new String[]{mThreadId});
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Toast.makeText(getActivity(), "A problem occurred during sending the request.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }


    public void setThreadName(String name) {
        groupNameTextView.setText(name);
    }

    public void setMute(boolean isMute){
        muteCheckBox.setChecked(isMute);
    }
}
