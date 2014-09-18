package com.triaged.badge.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.triaged.badge.database.table.ReceiptTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sadegh Kazemy on 9/17/14.
 */
public class Receipt {

    public static final int SYNCED = 100;
    public static final int SYNCING = 200;
    public static final int NOT_SYNCED = 300;

    String threadId;
    String messageId;
    String userId;
    String timestamp;
    int syncStatus;


    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncStatus) {
        this.syncStatus = syncStatus;
    }

    public static ContentValues generateContentValue(Receipt receipt) {
        ContentValues cv = new ContentValues(4);
        cv.put(ReceiptTable.COLUMN_THREAD_ID, receipt.threadId);
        cv.put(ReceiptTable.COLUMN_MESSAGE_ID, receipt.messageId);
        cv.put(ReceiptTable.COLUMN_USER_ID, receipt.userId);
        cv.put(ReceiptTable.COLUMN_SEEN_TIMESTAMP, receipt.timestamp);
        cv.put(ReceiptTable.COLUMN_SYNC_STATUS, receipt.syncStatus);
        return cv;
    }

    public static List<Receipt> getCursorEntities(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            return new ArrayList<Receipt>(0);
        } else {
            ArrayList<Receipt> resultList = new ArrayList<Receipt>(cursor.getCount());
            do {
                resultList.add(getCursorEntity(cursor));
            } while (cursor.moveToNext());
            return resultList;
        }
    }

    public static Receipt getCursorEntity(Cursor cursor) {
        Receipt receipt = new Receipt();
        receipt.setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(ReceiptTable.COLUMN_MESSAGE_ID)));
        receipt.setThreadId(cursor.getString(cursor.getColumnIndexOrThrow(ReceiptTable.COLUMN_THREAD_ID)));
        receipt.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(ReceiptTable.COLUMN_USER_ID)));
        receipt.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(ReceiptTable.COLUMN_SEEN_TIMESTAMP)));
        receipt.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(ReceiptTable.COLUMN_SYNC_STATUS)));
        return receipt;
    }
}