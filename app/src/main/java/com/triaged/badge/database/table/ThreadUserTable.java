package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 9/29/14.
 */
public class ThreadUserTable extends AbstractTable {

    public static String TABLE_NAME = "thread_user_junction";

    public static String CLM_THREAD_ID = "thread_id";
    public static String CLM_USER_ID = "user_id";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + "(" + CLM_THREAD_ID + " INTEGER NOT NULL, "
                + CLM_USER_ID + " INTEGER NOT NULL, "
                + "PRIMARY KEY (" + CLM_USER_ID + ", " + CLM_THREAD_ID + "), "
                + "FOREIGN KEY (" + CLM_THREAD_ID + ") REFERENCES "
                    + MessageThreadsTable.TABLE_NAME + "(" + MessageThreadsTable.COLUMN_ID + "),"
                + "FOREIGN KEY (" + CLM_USER_ID + ") REFERENCES "
                    + UsersTable.TABLE_NAME + "(" + UsersTable.COLUMN_ID + ")"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
