package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 10/15/14.
 */
public class TagSetsTable extends AbstractTable {

    public static final String TABLE_NAME = "tag_sets";

    public static final String CLM_NAME = "name";

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(CLM_NAME).append(" TEXT, ")
                .append(");");

        db.execSQL(createSql.toString());
        db.execSQL("CREATE INDEX department_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
