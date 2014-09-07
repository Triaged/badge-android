package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
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


    protected static final String CREATE_MESSAGES_TABLE_SQL = String.format(
            "create table %s (%s integer primary key autoincrement, %s text, %s text," +
                    " %s integer, %s text, %s integer, %s integer, %s integer, %s text, %s text, %s integer, %s text);",
            TABLE_NAME,
            COLUMN_ID,
            COLUMN_MESSAGES_ID,
            COLUMN_MESSAGES_THREAD_ID,
            COLUMN_MESSAGES_FROM_ID,
            COLUMN_MESSAGES_BODY,
            COLUMN_MESSAGES_TIMESTAMP,
            COLUMN_MESSAGES_ACK,
            COLUMN_MESSAGES_THREAD_HEAD,
            COLUMN_MESSAGES_THREAD_PARTICIPANTS,
            COLUMN_MESSAGES_AVATAR_URL,
            COLUMN_MESSAGES_IS_READ,
            COLUMN_MESSAGES_GUID
    );

    protected static final String DROP_MESSAGES_TABLE_SQL = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MESSAGES_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_MESSAGES_TABLE_SQL);
    }

}
