package com.triaged.badge.database.helper;

import android.content.ContentValues;

import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.models.Message;
import com.triaged.badge.net.DataProviderService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sadegh Kazemy on 9/9/14.
 */
public class MessageHelper {
    public static ContentValues setMessageContentValuesFromJSON(String threadId, JSONObject msg) throws JSONException {
        ContentValues msgValues = new ContentValues();
        msgValues.put(MessagesTable.COLUMN_MESSAGES_ACK, DataProviderService.MSG_STATUS_ACKNOWLEDGED);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_ID, msg.getString("id"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_FROM_ID, msg.getInt("author_id"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_ID, threadId);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_BODY, msg.getString("body"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_TIMESTAMP, (long) (msg.getDouble("timestamp") * 1000000d));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_GUID, msg.getString("guid"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_IS_READ, 0);
        return msgValues;
    }

    public static ContentValues toContentValue(Message message, String threadId) {
        ContentValues values = new ContentValues();
        values.put(MessagesTable.COLUMN_MESSAGES_ACK, DataProviderService.MSG_STATUS_ACKNOWLEDGED);
        values.put(MessagesTable.COLUMN_MESSAGES_ID, message.getId());
        values.put(MessagesTable.COLUMN_MESSAGES_FROM_ID, message.getAuthorId());
        values.put(MessagesTable.COLUMN_MESSAGES_THREAD_ID, threadId);
        values.put(MessagesTable.COLUMN_MESSAGES_BODY, message.getBody());
        values.put(MessagesTable.COLUMN_MESSAGES_TIMESTAMP, (long) (message.getTimestamp() * 1000000d));
        values.put(MessagesTable.COLUMN_MESSAGES_GUID, message.getGuid());
        values.put(MessagesTable.COLUMN_MESSAGES_IS_READ, 0);
        return values;
    }
}
