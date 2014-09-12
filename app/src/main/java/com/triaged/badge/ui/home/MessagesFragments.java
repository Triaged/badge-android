package com.triaged.badge.ui.home;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.triaged.badge.app.R;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.ui.home.adapters.MyMessagesAdapter;
import com.triaged.badge.ui.messaging.MessagingActivity;
import com.triaged.badge.ui.notification.Notifier;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;

public class MessagesFragments extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    @InjectView(R.id.messages_list) ListView messagesList;

    @InjectView(R.id.no_messages_image) ImageView noMessagesImage;
    @InjectView(R.id.no_messages_title) TextView noMessagesTitle;
    @InjectView(R.id.no_messages_info)  TextView noMessagesInfo;

    protected MyMessagesAdapter adapter;

    @OnItemClick(R.id.messages_list)
    void messageItemClicked(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), MessagingActivity.class);
        intent.putExtra(MessagingActivity.THREAD_ID_EXTRA, ((MyMessagesAdapter.ViewHolder) view.getTag()).threadId);
        intent.putExtra(MessagingActivity.THREAD_NAME_EXTRA, ((MyMessagesAdapter.ViewHolder) view.getTag()).name.getText().toString());
        startActivity(intent);
    }

    @OnClick(R.id.compose_button)
    void compose() {
        Intent intent = new Intent(getActivity(), MessageNewActivity.class);
        startActivity(intent);
    }

    public static MessagesFragments newInstance() {
        MessagesFragments fragment = new MessagesFragments();
        return fragment;
    }
    public MessagesFragments() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_messages, container, false);
        ButterKnife.inject(this, root);

        adapter = new MyMessagesAdapter(getActivity(), null);
        messagesList.setAdapter(adapter);
        checkListEmptiness();

        getLoaderManager().initLoader(0, null, this);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Notifier.clearNotifications(getActivity());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), MessageProvider.CONTENT_URI,
                null, MessagesTable.COLUMN_MESSAGES_THREAD_HEAD + " = 1", null,
                MessagesTable.COLUMN_MESSAGES_TIMESTAMP + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            adapter.swapCursor(data);
            checkListEmptiness();

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    protected void checkListEmptiness() {
        if (adapter.getCount() == 0) {
            noMessagesImage.setVisibility(View.VISIBLE);
            noMessagesTitle.setVisibility(View.VISIBLE);
            noMessagesInfo.setVisibility(View.VISIBLE);
        } else {
            noMessagesImage.setVisibility(View.GONE);
            noMessagesTitle.setVisibility(View.GONE);
            noMessagesInfo.setVisibility(View.GONE);
        }
    }
}
