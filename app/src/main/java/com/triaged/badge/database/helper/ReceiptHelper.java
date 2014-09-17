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

    public static int setTimestamp(Context context, String threadId) {
        ContentValues cv = new ContentValues(1);
        cv.put(ReceiptTable.COLUMN_TIMESTAMP, System.currentTimeMillis() + "");
        int updateCount = context.getContentResolver().update(
                ReceiptProvider.CONTENT_URI, cv,
                ReceiptTable.COLUMN_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? AND " +
                        ReceiptTable.COLUMN_TIMESTAMP + " is NULL",
                new String[]{threadId + "", Receipt.NOT_SYNCED + "" }
        );
        return updateCount;
    }


    public static List<Receipt> fetchReceiptReportCandidates(Context context, String threadId) {
        Cursor cursor = context.getContentResolver().query(
                ReceiptProvider.CONTENT_URI, null,
                ReceiptTable.COLUMN_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{threadId + "", Receipt.NOT_SYNCED + ""},
                null);
        return Receipt.getCursorEntities(cursor);
    }


    public static int setReceiptSync(Context context, String threadId) {
        ContentValues cv = new ContentValues(1);
        cv.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.SYNCED);
        return  context.getContentResolver().update(
                ReceiptProvider.CONTENT_URI, cv,
                ReceiptTable.COLUMN_THREAD_ID + "=? AND " +
                        ReceiptTable.COLUMN_SYNC_STATUS + " =? ",
                new String[]{threadId + "", Receipt.NOT_SYNCED + ""}
        );
    }

}
