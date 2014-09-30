package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class MessagesTable extends AbstractTable {


    public static final String TABLE_NAME = "messages";

    public static final String CLM_ID = "server_msg_id";
    public static final String CLM_BODY = "body";
    public static final String CLM_THREAD_ID = "thread_id";
    public static final String CLM_AUTHOR_ID = "author_id";
    public static final String CLM_TIMESTAMP = "timestamp";
    public static final String CLM_ACK = "message_acknowledged";
    public static final String CLM_IS_READ = "is_read";
    public static final String CLM_GUID = "guid";

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(CLM_ID).append(" TEXT, ")
                .append(CLM_THREAD_ID).append(" TEXT, ")
                .append(CLM_AUTHOR_ID).append(" INTEGER, ")
                .append(CLM_BODY).append(" TEXT, ")
                .append(CLM_TIMESTAMP).append(" INTEGER, ")
                .append(CLM_ACK).append(" INTEGER, ")
                .append(CLM_IS_READ).append(" INTEGER, ")
                .append(CLM_GUID).append(" TEXT UNIQUE")
                .append(");");

        db.execSQL(createSql.toString());
        db.execSQL("CREATE INDEX message_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX message_thread_id_index ON " + TABLE_NAME + "(" + CLM_THREAD_ID + ");");
        db.execSQL("CREATE INDEX message_thread_body_index ON " + TABLE_NAME + "(" + CLM_BODY + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
