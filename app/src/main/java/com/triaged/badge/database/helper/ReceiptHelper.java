package com.triaged.badge.database.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.models.Receipt;

import java.util.List;

/**
 * Created by Sadegh Kazemy on 9/17/14.
 */
public class ReceiptHelper {

    private ReceiptHelper() { }

    public static ContentValues fromReceipt(Receipt receipt) {
        ContentValues contentValues = new ContentValues(5);
        if (receipt.getThreadId() != null )
            contentValues.put(ReceiptTable.CLM_THREAD_ID, receipt.getThreadId());

        contentValues.put(ReceiptTable.CLM_MESSAGE_ID, receipt.getMessageId());
        contentValues.put(ReceiptTable.CLM_USER_ID, receipt.getUserId());
        contentValues.put(ReceiptTable.CLM_SEEN_TIMESTAMP, receipt.getTimestamp());
        contentValues.put(ReceiptTable.COLUMN_SYNC_STATUS, receipt.getSyncStatus());
        return contentValues;
    }

    public static int setTimestamp(Context context, String threadId) {
        ContentValues cv = new ContentValues(1);
        cv.put(ReceiptTable.CLM_SEEN_TIMESTAMP, System.currentTimeMillis() + "");
        int updateCount = context.getContentResolver().update(
                ReceiptProvider.CONTENT_URI, cv,
                ReceiptTable.CLM_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? AND " +
                        ReceiptTable.CLM_SEEN_TIMESTAMP + " is NULL",
                new String[]{threadId + "", Receipt.NOT_SYNCED + "" }
        );
        return updateCount;
    }


    public static List<Receipt> fetchReceiptReportCandidates(Context context, String threadId) {
        Cursor cursor = context.getContentResolver().query(
                ReceiptProvider.CONTENT_URI, null,
                ReceiptTable.CLM_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{threadId + "", Receipt.NOT_SYNCED + ""},
                null);
        return Receipt.getCursorEntities(cursor);
    }


    public static List<Receipt> fetchAllReceiptReportCandidates(Context context) {
        Cursor cursor = context.getContentResolver().query(
                ReceiptProvider.CONTENT_URI, null,
                ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{ Receipt.NOT_SYNCED + "" },
                ReceiptTable.CLM_THREAD_ID
        );
        return Receipt.getCursorEntities(cursor);
    }


    public static int setReceiptsSync(Context context, String threadId) {
        ContentValues cv = new ContentValues(1);
        cv.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.SYNCED);
        return  context.getContentResolver().update(
                ReceiptProvider.CONTENT_URI, cv,
                ReceiptTable.CLM_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{threadId + "", Receipt.NOT_SYNCED + ""}
        );
    }


    public static int setAllSeenReceiptsSync(Context context) {
        ContentValues cv = new ContentValues(1);
        cv.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.SYNCED);
        return  context.getContentResolver().update(
                ReceiptProvider.CONTENT_URI, cv,
                ReceiptTable.CLM_SEEN_TIMESTAMP + " IS NOT NULL AND "
                        + ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{ Receipt.NOT_SYNCED + ""}
        );
    }

}
