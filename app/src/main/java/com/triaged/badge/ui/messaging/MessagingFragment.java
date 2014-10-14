package com.triaged.badge.ui.messaging;


import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.triaged.badge.app.App;
import com.triaged.badge.app.MessageProcessor;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.ReceiptHelper;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.events.NewMessageEvent;
import com.triaged.badge.events.ReceiptForFayEvent;
import com.triaged.badge.models.Receipt;
import com.triaged.badge.ui.base.MixpanelFragment;
import com.triaged.badge.ui.home.adapters.UserAdapter;
import com.triaged.badge.ui.notification.Notifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import de.greenrobot.event.EventBus;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessagingFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MessagingFragment extends MixpanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // the fragment initialization thread_id parameter.
    private static final String ARG_THREAD_ID = "thread_id";

    private static String mThreadId;
    private MessagingAdapter adapter;
    private int soleCounterpartId = 0;
    private int userCount = 2;
    private volatile boolean hasGeneratedReceipts = false;

    AlertDialog readyByDialog;
    ListView readyByListView;
    UserAdapter readyByAdapter;

    //    @InjectView(R.id.post_box_wrapper) RelativeLayout postBoxWrapper;
    @InjectView(R.id.send_now_button) ImageButton sendButton;
    @InjectView(R.id.message_listview) ListView messageListView;
    @InjectView(R.id.input_box) EditText postBox;

    @OnTextChanged(R.id.input_box)
    void onTextChanged(CharSequence s, int start, int before, int count) {

        if (TextUtils.isEmpty(s)) {
            sendButton.setEnabled(false);
        } else  {
            sendButton.setEnabled(true);
        }
    }

    @OnClick(R.id.send_now_button)
    void sendMessage() {
        String msg = postBox.getText().toString();
        if (!msg.equals("")) {
            MessageProcessor.getInstance().sendMessage(mThreadId, msg);
            postBox.setText("");
            JSONObject props = new JSONObject();
            try {
                props.put("recipient_id", mThreadId);
                mixpanel.track("message_sent", props);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param threadId Id of the the thread we want to show its messages.
     * @return A new instance of fragment MessagingFragment.
     */
    public static MessagingFragment newInstance(String threadId) {
        MessagingFragment fragment = new MessagingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_THREAD_ID, threadId);
        fragment.setArguments(args);
        return fragment;
    }
    public MessagingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Ready by:");
        readyByListView = new ListView(getActivity());
        readyByAdapter = new UserAdapter(getActivity(), null, R.layout.item_contact_no_msg);
        readyByListView.setAdapter(readyByAdapter);
        builder.setView(readyByListView);
        readyByDialog = builder.create();

        if (getArguments() != null) {
            mThreadId = getArguments().getString(ARG_THREAD_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_messaging, container, false);
        ButterKnife.inject(this, root);

        adapter = new MessagingAdapter(getActivity(), null);
        messageListView.setAdapter(adapter);
        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MessagingAdapter.MessageHolder holder = (MessagingAdapter.MessageHolder) view.getTag();
                if (holder.messageId != null) {
                    Cursor readByCursor = getActivity().getContentResolver().query(
                            ReceiptProvider.CONTENT_URI,
                            null,
                            ReceiptTable.CLM_MESSAGE_ID + "=?",
                            new String[]{holder.messageId},
                            null
                    );
                    readyByAdapter.swapCursor(readByCursor);
                    readyByDialog.show();
                }
            }
        });
        sendButton.setEnabled(false);
        getLoaderManager().initLoader(0, null, this);
        generateReceiptsAndMarkAsRead();
        return root;
    }


    @Override
    public void onResume() {
        Notifier.clearNotifications(getActivity());
        super.onResume();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        generateReceiptsAndMarkAsRead();
        super.onStop();
    }

    private void generateReceiptsAndMarkAsRead() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!hasGeneratedReceipts) {
                    generate();
                    sendReceipts();
                }
                markMessagesAsRead();
            }
        });
        thread.start();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                MessageProvider.CONTENT_URI_WITH_CONTACTS_INFO,
                null,
                MessagesTable.CLM_THREAD_ID + " =?",
                new String[] { mThreadId },
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void generate() {
        // Generate receipts for messages which are not set as read.
        Cursor cursor = App.context().getContentResolver().query(
                MessageProvider.CONTENT_URI,
                new String[]{MessagesTable.CLM_ID},
                MessagesTable.CLM_THREAD_ID + " =? AND "
                        + MessagesTable.CLM_IS_READ + " = 0 AND " +
                        MessagesTable.CLM_AUTHOR_ID + " != ?",
                new String[]{mThreadId, App.accountId() + ""},
                null
        );
        if (cursor.moveToFirst()) {
            ArrayList<ContentProviderOperation> insertOperations = new ArrayList<ContentProviderOperation>(cursor.getCount());
            do {
                ContentValues receiptValues = new ContentValues();
                receiptValues.put(ReceiptTable.CLM_MESSAGE_ID, cursor.getString(0));
                receiptValues.put(ReceiptTable.CLM_THREAD_ID, mThreadId);
                receiptValues.put(ReceiptTable.CLM_USER_ID, App.accountId());
                receiptValues.put(ReceiptTable.CLM_SEEN_TIMESTAMP, System.currentTimeMillis() / 1000f);
                receiptValues.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.NOT_SYNCED);
                insertOperations.add(ContentProviderOperation.newInsert(
                        ReceiptProvider.CONTENT_URI).withValues(receiptValues).build());
            } while (cursor.moveToNext());

            try {
                App.context().getContentResolver().applyBatch(ReceiptProvider.AUTHORITY, insertOperations);
                hasGeneratedReceipts = true;

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendReceipts() {
        // Report receipts to the server.
        List<Receipt> receiptList = ReceiptHelper.fetchAllReceiptReportCandidates(App.context());
        if (receiptList.size() > 0) {
            EventBus.getDefault().post(new ReceiptForFayEvent(receiptList));
        }
    }

    private void markMessagesAsRead() {
        ContentValues values = new ContentValues();
        values.put(MessagesTable.CLM_IS_READ, 1);
        App.context().getContentResolver().update(
                MessageProvider.CONTENT_URI,
                values,
                MessagesTable.CLM_THREAD_ID + " =?",
                new String[] { mThreadId }
        );
    }

    public static String threadId() {
        return mThreadId;
    }


    public void onEvent(final NewMessageEvent newMessageEvent) {
        if (mThreadId.equals(newMessageEvent.threadId)) {
            generate();
            sendReceipts();
        }
    }

    public void onEvent(LoadParticipantsEvent event) {
        adapter.setParticipantsNumber(event.participantsNumber);
    }
}
