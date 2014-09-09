package com.triaged.badge.ui.messaging;


import android.app.Fragment;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.models.Contact;
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

        View root = inflater.inflate(R.layout.fragment_menu, container, false);
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

        return root;
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
