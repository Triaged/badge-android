package com.triaged.badge.ui.messaging;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.triaged.badge.models.Receipt;
import com.triaged.badge.app.SyncManager;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.api.requests.ReceiptsReportRequest;
import com.triaged.badge.ui.base.MixpanelFragment;
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
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessagingFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MessagingFragment extends MixpanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // the fragment initialization thread_id parameter.
    private static final String ARG_THREAD_ID = "thread_id";

    private String mThreadId;
    private MessagingAdapter adapter;
    private int soleCounterpartId = 0;
    private int userCount = 2;
    private boolean reportsReceipts = false;

    //    @InjectView(R.id.post_box_wrapper) RelativeLayout postBoxWrapper;
    @InjectView(R.id.send_now_button) ImageButton sendButton;
    @InjectView(R.id.message_listview) ListView threadList;
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

        if (getArguments() != null) {
            mThreadId = getArguments().getString(ARG_THREAD_ID);
        }
        markMessagesAsRead();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_messaging, container, false);
        ButterKnife.inject(this, root);

        adapter = new MessagingAdapter(getActivity(), null);
        threadList.setAdapter(adapter);
        sendButton.setEnabled(false);

        getLoaderManager().initLoader(0, null, this);
        return root;
    }


    @Override
    public void onResume() {
        Notifier.clearNotifications(getActivity());
        super.onResume();
    }

    @Override
    public void onStop() {
        markMessagesAsRead();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //TODO: must optimized the columns projection
        return new CursorLoader(getActivity(), MessageProvider.CONTENT_URI_WITH_CONTACTS_INFO,
                null, MessagesTable.CLM_THREAD_ID + " =?",
                new String[] { mThreadId},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        if (!reportsReceipts) {
            reportsReceipts = true;
            prepareAndSendReceipts();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    private void prepareAndSendReceipts() {
        ReceiptHelper.setTimestamp(getActivity(), mThreadId);
        List<Receipt> receiptList = ReceiptHelper.fetchAllReceiptReportCandidates(App.context());
        if (receiptList.size() > 0) {
            ReceiptsReportRequest receiptsReportRequest = new ReceiptsReportRequest(receiptList);
            RestService.instance().messaging().reportReceipts(receiptsReportRequest, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    ReceiptHelper.setAllSeenReceiptsSync(App.context());
                }

                @Override
                public void failure(RetrofitError error) {

                }
            });
        }

    }

    private void markMessagesAsRead() {
        ContentValues values = new ContentValues();
        values.put(MessagesTable.CLM_IS_READ, 1);
        getActivity().getContentResolver().update(MessageProvider.CONTENT_URI, values,
                MessagesTable.CLM_THREAD_ID + " =?",
                new String[] { mThreadId});
    }


    public void onEvent(final NewMessageEvent newMessageEvent) {
        if (mThreadId.equals(newMessageEvent.threadId)) {

            final Receipt receipt = new Receipt();
            receipt.setThreadId(newMessageEvent.threadId);
            receipt.setMessageId(newMessageEvent.messageId);
            receipt.setUserId(SyncManager.getMyUser().id + "");
            receipt.setTimestamp(System.currentTimeMillis() + "");
            receipt.setSyncStatus(Receipt.SYNCED);

            ArrayList<Receipt> receiptArrayList = new ArrayList<Receipt>(1);
            receiptArrayList.add(receipt);
            ReceiptsReportRequest receiptsReportRequest = new ReceiptsReportRequest(receiptArrayList);

            RestService.instance().messaging().reportReceipts(receiptsReportRequest, new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    App.gLogger.i(response.getReason());

                    ContentValues receiptSyncedValues = new ContentValues(2);
                    receiptSyncedValues.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.SYNCED);
                    receiptSyncedValues.put(ReceiptTable.CLM_SEEN_TIMESTAMP, receipt.getTimestamp());

                    getActivity().getContentResolver().update(ReceiptProvider.CONTENT_URI,
                            receiptSyncedValues,
                            ReceiptTable.CLM_MESSAGE_ID + "=? AND "
                                    + ReceiptTable.CLM_USER_ID + "=?",
                            new String[]{newMessageEvent.messageId, SyncManager.getMyUser().id + ""});
                }

                @Override
                public void failure(RetrofitError error) {
                    App.gLogger.e(error.getMessage());
                    ContentValues receiptSyncedValues = new ContentValues(1);
                    receiptSyncedValues.put(ReceiptTable.CLM_SEEN_TIMESTAMP, receipt.getTimestamp());

                    getActivity().getContentResolver().update(ReceiptProvider.CONTENT_URI,
                            receiptSyncedValues,
                            ReceiptTable.CLM_MESSAGE_ID + "=? AND "
                                    + ReceiptTable.CLM_USER_ID + "=?",
                            new String[]{newMessageEvent.messageId, SyncManager.getMyUser().id + ""});
                }
            });
        }
    }
}
