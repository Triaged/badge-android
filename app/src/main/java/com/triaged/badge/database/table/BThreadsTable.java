package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 9/29/14.
 */
public class BThreadsTable extends AbstractTable {

    public static final String TABLE_NAME = "bthreads";

    public static final String CLM_NAME = "name";
    public static final String CLM_IS_MUTED = "is_muted";
    public static final String CLM_USERS_KEY = "users_key";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "("
                        + COLUMN_ID + " INT PRIMARY KEY, "
                        + CLM_NAME + " TEXT, "
                        + CLM_USERS_KEY + " TEXT, "
                        + CLM_IS_MUTED + " BOOLEAN "
                        + ");"
        );
        db.execSQL("CREATE INDEX thread_table_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
