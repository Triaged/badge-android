package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentsTable extends AbstractTable {

    public static final String TABLE_NAME = "departments";

    public static final String COLUMN_DEPARTMENT_NAME = "name";
    public static final String COLUMN_DEPARTMENT_NUM_CONTACTS = "num_contacts";

    @Override
    public void onCreate(SQLiteDatabase db) {

        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(COLUMN_DEPARTMENT_NAME).append(" TEXT, ")
                .append(COLUMN_DEPARTMENT_NUM_CONTACTS).append(" INTEGER")
                .append(");");

        db.execSQL(createSql.toString());

        db.execSQL("CREATE INDEX department_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX department_num_contacts_index ON " + TABLE_NAME + "(" + COLUMN_DEPARTMENT_NUM_CONTACTS + ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
