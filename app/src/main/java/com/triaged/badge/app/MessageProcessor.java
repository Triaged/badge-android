package com.triaged.badge.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.events.MessageForFayEvent;
import com.triaged.badge.models.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

/**
 * Created by Sadegh Kazemy on 10/1/14.
 */
public class MessageProcessor {
    private static MessageProcessor ourInstance = new MessageProcessor();

    public static final String NEW_MSG_ACTION = "com.triage.badge.NEW_MSG";
    public static final String THREAD_ID_EXTRA = "threadId";
    public static final String IS_INCOMING_MSG_EXTRA = "isIncomingMessage";

    private Context mContext = App.context();
    ScheduledExecutorService sqlThread = Executors.newSingleThreadScheduledExecutor();

    public static MessageProcessor getInstance() {
        return ourInstance;
    }

    private MessageProcessor() {
    }


    /**
     * Create a message in the database and send it async to faye
     * for sending over websocket.
     * <p/>
     * New messages become the thread head and are read by default.
     * The lifecycle for message status is pending, sent, or error.
     *
     * @param threadId
     * @param message
     */
    public void sendMessage(final String threadId, final String message) {
        ContentValues msgValues = new ContentValues();
        try {
            long timestamp = System.currentTimeMillis() * 1000 /* nano */;
            // GUID
            final String guid = UUID.randomUUID().toString();
            //msgValues.put( CompanySQLiteHelper.CLM_ID, null );
            msgValues.put(MessagesTable.CLM_TIMESTAMP, timestamp);
            msgValues.put(MessagesTable.CLM_BODY, message);
            msgValues.put(MessagesTable.CLM_THREAD_ID, threadId);
            msgValues.put(MessagesTable.CLM_AUTHOR_ID, App.accountId());
            msgValues.put(MessagesTable.CLM_IS_READ, 1);
            msgValues.put(MessagesTable.CLM_GUID, guid);
            msgValues.put(MessagesTable.CLM_ACK, Message.MSG_STATUS_PENDING);
            mContext.getContentResolver().insert(MessageProvider.CONTENT_URI, msgValues);

            sendMessageToFaye(timestamp, guid, threadId, message);
        } catch (JSONException e) {
            // Realllllllly shouldn't happen.
            App.gLogger.e("Severe bug, JSON exception parsing user id array from prefs", e);
            return;
        }
        Intent newMsgIntent = new Intent(NEW_MSG_ACTION);
        newMsgIntent.putExtra(THREAD_ID_EXTRA, threadId);
        newMsgIntent.putExtra(IS_INCOMING_MSG_EXTRA, false);

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(newMsgIntent);
    }

    protected void sendMessageToFaye(long timestamp, final String guid, final String threadId, final String message) throws JSONException {
        final JSONObject msgWrapper = new JSONObject();
        JSONObject msg = new JSONObject();
        msgWrapper.put("message", msg);
        msg.put("author_id", App.accountId());
        msg.put("body", message);
        msg.put("timestamp", timestamp);
        msg.put("guid", guid);
        msgWrapper.put("guid", guid);
        // Bind/unbind every time so that the service doesn't live past
        // stopService()
        EventBus.getDefault().post(new MessageForFayEvent(threadId, msgWrapper));

        sqlThread.schedule(new Runnable() {
            @Override
            public void run() {
                Cursor msgCursor = mContext.getContentResolver().query(MessageProvider.CONTENT_URI,
                        null,
                        MessagesTable.CLM_GUID + " =?",
                        new String[]{guid},
                        null);
                if (msgCursor.moveToFirst() && msgCursor.getInt(msgCursor.getColumnIndex(MessagesTable.CLM_ACK)) == Message.MSG_STATUS_PENDING) {
                    ContentValues values = new ContentValues();
                    values.put(MessagesTable.CLM_ACK, Message.MSG_STATUS_FAILED);

                    int rowsUpdated = mContext.getContentResolver().update(MessageProvider.CONTENT_URI, values,
                            MessagesTable.CLM_GUID + " =?",
                            new String[] { guid});
                    if (rowsUpdated == 1) {
                        App.uiHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Message could not be sent.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }, 30, TimeUnit.SECONDS);

    }

    /**
     * Try to send a message again that's already saved in the DB.
     *
     * @param guid message guid
     */
    public void retryMessage(final String guid) {
        Cursor msgCursor = mContext.getContentResolver().query(MessageProvider.CONTENT_URI,
                null, MessagesTable.CLM_GUID + " =?",
                new String[]{guid}, null);
        if (msgCursor.moveToFirst()) {
            // Flip back to pending status.
            ContentValues msgValues = new ContentValues();
            msgValues.put(MessagesTable.CLM_ACK, Message.MSG_STATUS_PENDING);
            mContext.getContentResolver().update(MessageProvider.CONTENT_URI, msgValues,
                    MessagesTable.CLM_GUID + " =?",
                    new String[] { guid});

            String threadId = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.CLM_THREAD_ID));
            String body = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.CLM_BODY));
            long timestamp = msgCursor.getLong(msgCursor.getColumnIndex(MessagesTable.CLM_TIMESTAMP));
            msgCursor.close();

            try {
                sendMessageToFaye(timestamp, guid, threadId, body);
            } catch (JSONException e) {
                App.gLogger.e("JSON exception preparing message to send to faye", e);
            }
        } else {
            App.gLogger.w("UI wanted to retry message with guid " + guid + " but that message can't be found.");
        }
    }

}
