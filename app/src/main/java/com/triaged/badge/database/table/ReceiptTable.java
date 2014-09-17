package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 9/17/14.
 */
public class ReceiptTable extends AbstractTable {

    public static final String TABLE_NAME = "receipts";

    public static final String COLUMN_THREAD_ID = "thread_id";
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_SYNC_STATUS = "sync_status";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_THREAD_ID + " TEXT, "
                + COLUMN_MESSAGE_ID + " TEXT UNIQUE, "
                + COLUMN_USER_ID + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_SYNC_STATUS + " INTEGER, "
                + " UNIQUE(" + COLUMN_MESSAGE_ID + "," + COLUMN_USER_ID + ") ON CONFLICT REPLACE "
                + ");");

        db.execSQL("CREATE INDEX receipt_message_id_index ON " + TABLE_NAME + "(" + COLUMN_MESSAGE_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
