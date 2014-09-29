package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentsTable extends AbstractTable {

    public static final String TABLE_NAME = "departments";

    public static final String CLM_NAME = "name";
    public static final String CLM_CONTACTS_NUMBER = "contacts_number";

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(CLM_NAME).append(" TEXT, ")
                .append(CLM_CONTACTS_NUMBER).append(" INTEGER")
                .append(");");

        db.execSQL(createSql.toString());
        db.execSQL("CREATE INDEX department_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX department_num_contacts_index ON " + TABLE_NAME + "(" + CLM_CONTACTS_NUMBER + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
