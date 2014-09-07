package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class DepartmentsTable extends AbstractTable {

    public static final String TABLE_NAME = "departments";

    public static final String COLUMN_DEPARTMENT_NAME = "name";
    public static final String COLUMN_DEPARTMENT_NUM_CONTACTS = "num_contacts";

    protected static final String CREATE_DEPARTMENTS_TABLE_SQL = String.format(
            "create table %s (%s  integer primary key, %s text, %s integer );",
            TABLE_NAME,
            COLUMN_ID,
            COLUMN_DEPARTMENT_NAME,
            COLUMN_DEPARTMENT_NUM_CONTACTS
    );

    protected static final String DROP_DEPARTMENTS_TABLE_SQL = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DEPARTMENTS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_DEPARTMENTS_TABLE_SQL);
    }

}
