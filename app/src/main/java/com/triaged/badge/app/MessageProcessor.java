package com.triaged.badge.app;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.triaged.badge.database.helper.MessageHelper;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.BThreadProvider;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.provider.ThreadUserProvider;
import com.triaged.badge.database.table.BThreadUserTable;
import com.triaged.badge.database.table.BThreadsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.events.MessageForFayEvent;
import com.triaged.badge.events.NewMessageEvent;
import com.triaged.badge.models.BThread;
import com.triaged.badge.models.Message;
import com.triaged.badge.models.Receipt;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.utils.SharedPreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
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

    private MessageProcessor() {}


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


    /**
     * If thread doesn't exist yet, save it, and any unsaved
     * messages as well.
     * <p/>
     * If message that has been sent to us is one of our pending
     * messages, mark it as acknowledged, broadcast it, and sync
     * timestamp/id with server.
     *
     * @param bThread   A badgeThread Object.
     * @param broadcast if true ,send local broadcast if thread contains new messages, otherwise,
     *                  assume they are historical
     */
    public void upsertThreadAndMessages(final BThread bThread, final boolean broadcast) {
        long mostRecentMsgTimestamp = SharedPreferencesHelper.instance().getLong(SyncManager.MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0);

        // Insert thread into database.
        ContentValues cv = new ContentValues();
        cv.put(BThreadsTable.CLM_BTHREAD_ID, bThread.getId());
        if (bThread.isMuted() != null) cv.put(BThreadsTable.CLM_IS_MUTED, bThread.isMuted());
        cv.put(BThreadsTable.CLM_USERS_KEY, userIdArrayToKey(bThread.getUserIds()));
        if (bThread.getUserIds().length == 2) {
            cv.put(BThreadsTable.CLM_NAME, createThreadName(bThread.getUserIds()));
        } else if (bThread.getName() != null){
            cv.put(BThreadsTable.CLM_NAME, bThread.getName());
        }
        mContext.getContentResolver().insert(BThreadProvider.CONTENT_URI, cv);

        // For each user into this thread,
        // put a record into thread_user junction table.
        ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>(bThread.getMessages().length);
        for (int userId : bThread.getUserIds()) {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put(BThreadUserTable.CLM_USER_ID, userId);
            contentValues.put(BThreadUserTable.CLM_THREAD_ID, bThread.getId());
            dbOperations.add(ContentProviderOperation
                    .newInsert(ThreadUserProvider.CONTENT_URI)
                    .withValues(contentValues).build());
        }
        try {
            mContext.getContentResolver().applyBatch(ThreadUserProvider.AUTHORITY, dbOperations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        dbOperations.clear();

        for (Message message : bThread.getMessages()) {
            // Insert the message into the database.
            ContentValues contentValues = MessageHelper.toContentValue(message, bThread.getId());
            dbOperations.add(ContentProviderOperation.newInsert(
                    MessageProvider.CONTENT_URI).
                    withValues(contentValues).
                    build());

            //TODO: why nano? those zeros at the end of timestamp
            // does not make it unique number!
            long timestamp = (long) (message.getTimestamp() * 1000000d) /* nanos */;
            if (timestamp > mostRecentMsgTimestamp) {
                mostRecentMsgTimestamp = timestamp;
            }

            // If I am not the author of this message,
            // create a receipt and put into database for further use.
            if (contentValues.getAsInteger(MessagesTable.CLM_AUTHOR_ID) != App.accountId()) {
                ContentValues receiptValues = new ContentValues();
                receiptValues.put(ReceiptTable.CLM_MESSAGE_ID, contentValues.getAsString(MessagesTable.CLM_ID));
                receiptValues.put(ReceiptTable.CLM_THREAD_ID, bThread.getId());
                receiptValues.put(ReceiptTable.CLM_USER_ID, App.accountId());
                receiptValues.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.NOT_SYNCED);
                mContext.getContentResolver().insert(ReceiptProvider.CONTENT_URI, receiptValues);
                // Announce on event-but that we have new message
                EventBus.getDefault().post(new NewMessageEvent(bThread.getId(),
                        contentValues.getAsString(MessagesTable.CLM_ID)));
            }
        }
        try {
            mContext.getContentResolver().applyBatch(MessageProvider.AUTHORITY, dbOperations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }

        // If I'm honest, this switch isn't intended to be used this way,
        // but the idea here is only update the timestamp on history sync
        // so that all messages will eventually be dl'd no matter what.
        if (!broadcast) {
            SharedPreferencesHelper.instance()
                    .putLong(SyncManager.MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, mostRecentMsgTimestamp)
                    .commit();
        }

        // Get id of most recent msg.
        Cursor messages = mContext.getContentResolver().query(MessageProvider.CONTENT_URI, null,
                MessagesTable.CLM_THREAD_ID + "=?",
                new String[]{bThread.getId()},
                MessagesTable.CLM_TIMESTAMP + " ASC");
        if (messages.moveToLast()) {
            String mostRecentGuid = messages.getString(messages.getColumnIndex(MessagesTable.CLM_GUID));
            final String mostRecentId = messages.getString(messages.getColumnIndex(MessagesTable.CLM_ID));
            if ("Inf".equals(mostRecentGuid)) {
                // Dang! Crash the app to get a report.
                App.uiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException("Crashing app. Couldn't set head of thread " + bThread.getId()
                                + " because message guid came back 'Inf' message id is " + mostRecentId);
                    }
                });
            }
            messages.close();

        } else {
            messages.close();
        }
    }

    /**
     * SYNCHRONOUSLY creates a thread using the REST badge api.
     * <strong>ONLY CALL THIS FROM A BACKGROUND TASK</strong>
     *
     * @param recipientIds
     * @return
     */
    public String createThreadSync(final Integer[] recipientIds) throws JSONException,  RemoteException, OperationApplicationException {
        String threadKey = userIdArrayToKey(recipientIds);
        Cursor cursor = mContext.getContentResolver().query(BThreadProvider.CONTENT_URI,
                new String[]{BThreadsTable.CLM_BTHREAD_ID},
                BThreadsTable.CLM_USERS_KEY + "=?",
                new String[]{threadKey},
                null);
        if (cursor.moveToFirst()) {
            return cursor.getString(0);
        } else {
            JSONObject postBody = new JSONObject();
            JSONObject messageThread = new JSONObject();
            JSONArray userIds = new JSONArray();
            postBody.put("message_thread", messageThread);
            for (int i : recipientIds) {
                userIds.put(i);
            }
            messageThread.put("user_ids", userIds);
            TypedJsonString typedJsonString = new TypedJsonString(postBody.toString());
            BThread resultThread;
            resultThread = RestService.instance().messaging().createMessageThread(typedJsonString);
            ContentValues cv = new ContentValues();
            cv.put(BThreadsTable.CLM_BTHREAD_ID, resultThread.getId());
            cv.put(BThreadsTable.CLM_USERS_KEY, threadKey);
            cv.put(BThreadsTable.CLM_IS_MUTED, false);
            cv.put(BThreadsTable.CLM_NAME, createThreadName(recipientIds));
            mContext.getContentResolver().insert(BThreadProvider.CONTENT_URI, cv);

            ArrayList<ContentProviderOperation> dbOperations =
                    new ArrayList<ContentProviderOperation>(resultThread.getUserIds().length);
            for (int participantId : resultThread.getUserIds()) {
                dbOperations.add(ContentProviderOperation.newInsert(
                        ThreadUserProvider.CONTENT_URI)
                        .withValue(BThreadUserTable.CLM_THREAD_ID, resultThread.getId())
                        .withValue(BThreadUserTable.CLM_USER_ID, participantId)
                        .build());
            }
            mContext.getContentResolver().applyBatch(ThreadUserProvider.AUTHORITY, dbOperations);
            return resultThread.getId();
        }
    }

    private String createThreadName(Integer[] recipientIds) {
        if (recipientIds.length == 2) {
            String[] name = new String[2];
            for (int userId : recipientIds) {
                if (userId == App.accountId()) continue;
                name = UserHelper.getUserName(mContext, userId);
            }
            return String.format("%s %s", name[0], name[1]);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int userId : recipientIds) {
                if (userId == App.accountId()) continue;
                String firstName = UserHelper.getUserName(mContext, userId)[0];
                if (firstName != null) {
                    stringBuilder.append(firstName).append(", ");
                }
            }
            if (stringBuilder.length() > 2) {
                stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Sort the ids in the json array and join them with a comma.
     *
     * @param userIdArr json array of user ids
     * @return a comma delimited string of sorted ids from userIdArr
     */
    private static String userIdArrayToKey(Integer[] userIdArr) {
        Arrays.sort(userIdArr);
        StringBuilder delimString = new StringBuilder();
        String delim = "";
        for (int userId : userIdArr) {
            delimString.append(delim).append(userId);
            delim = ",";
        }
        return delimString.toString();
    }

}
