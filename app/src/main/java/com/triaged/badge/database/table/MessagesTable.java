package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class MessagesTable extends AbstractTable {


    public static final String TABLE_NAME = "messages";

    public static final String COLUMN_MESSAGES_ID = "server_msg_id";
    public static final String COLUMN_MESSAGES_BODY = "body";
    public static final String COLUMN_MESSAGES_THREAD_ID = "thread_id";
    public static final String COLUMN_MESSAGES_FROM_ID = "from_user_id";
    public static final String COLUMN_MESSAGES_TIMESTAMP = "timestamp";
    public static final String COLUMN_MESSAGES_THREAD_HEAD = "thread_head";
    public static final String COLUMN_MESSAGES_THREAD_PARTICIPANTS = "thread_names";
    public static final String COLUMN_MESSAGES_AVATAR_URL = "avatar_url";
    public static final String COLUMN_MESSAGES_ACK = "message_acknowledged";
    public static final String COLUMN_MESSAGES_IS_READ = "is_read";
    public static final String COLUMN_MESSAGES_GUID = "guid";


    @Override
    public void onCreate(SQLiteDatabase db) {

        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(COLUMN_MESSAGES_ID).append(" TEXT, ")
                .append(COLUMN_MESSAGES_THREAD_ID).append(" TEXT, ")
                .append(COLUMN_MESSAGES_FROM_ID).append(" INTEGER, ")
                .append(COLUMN_MESSAGES_BODY).append(" TEXT, ")
                .append(COLUMN_MESSAGES_TIMESTAMP).append(" INTEGER, ")
                .append(COLUMN_MESSAGES_ACK).append(" INTEGER, ")
                .append(COLUMN_MESSAGES_THREAD_HEAD).append(" INTEGER, ")
                .append(COLUMN_MESSAGES_THREAD_PARTICIPANTS).append(" TEXT, ")
                .append(COLUMN_MESSAGES_AVATAR_URL).append(" TEXT, ")
                .append(COLUMN_MESSAGES_IS_READ).append(" INTEGER, ")
                .append(COLUMN_MESSAGES_GUID).append(" TEXT")
                .append(");");

        db.execSQL(createSql.toString());

        db.execSQL("CREATE INDEX message_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX message_thread_id_index ON " + TABLE_NAME + "(" + COLUMN_MESSAGES_THREAD_ID + ");");
        db.execSQL("CREATE INDEX message_thread_head_index ON " + TABLE_NAME + "(" + COLUMN_MESSAGES_THREAD_HEAD + ");");
        db.execSQL("CREATE INDEX message_thread_body_index ON " + TABLE_NAME + "(" + COLUMN_MESSAGES_BODY + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
