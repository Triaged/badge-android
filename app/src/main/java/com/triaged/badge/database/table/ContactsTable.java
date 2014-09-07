package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class ContactsTable extends AbstractTable {

    public static final String TABLE_NAME = "contacts";

    public static final String COLUMN_CONTACT_LAST_NAME = "last_name";
    public static final String COLUMN_CONTACT_FIRST_NAME = "first_name";
    public static final String COLUMN_CONTACT_AVATAR_URL = "avatar_url";
    public static final String COLUMN_CONTACT_JOB_TITLE = "job_title";
    public static final String COLUMN_CONTACT_EMAIL = "email";
    public static final String COLUMN_CONTACT_START_DATE = "start_date";
    public static final String COLUMN_CONTACT_BIRTH_DATE = "birth_date";
    public static final String COLUMN_CONTACT_CELL_PHONE = "cell_phone";
    public static final String COLUMN_CONTACT_OFFICE_PHONE = "office_phone";
    public static final String COLUMN_CONTACT_MANAGER_ID = "manager_id";
    public static final String COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID = "primary_office_location_id";
    public static final String COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID = "current_office_location_id";
    public static final String COLUMN_CONTACT_DEPARTMENT_ID = "department_id";
    public static final String COLUMN_CONTACT_SHARING_OFFICE_LOCATION = "sharing_office_location";
    public static final String COLUMN_CONTACT_IS_ARCHIVED = "is_archived";


    private static final String CREATE_CONTACTS_TABLE_SQL = String.format("create table %s " +
                    "(%s  integer primary key autoincrement, %s text, %s text, %s text, %s text, %s text," +
                    " %s text, %s text, %s text, %s text, %s integer, %s integer, %s integer, %s integer, %s integer, %s boolean);",
            TABLE_NAME,
            COLUMN_ID,
            COLUMN_CONTACT_FIRST_NAME,
            COLUMN_CONTACT_LAST_NAME,
            COLUMN_CONTACT_AVATAR_URL,
            COLUMN_CONTACT_JOB_TITLE,
            COLUMN_CONTACT_EMAIL,
            COLUMN_CONTACT_START_DATE,
            COLUMN_CONTACT_BIRTH_DATE,
            COLUMN_CONTACT_CELL_PHONE,
            COLUMN_CONTACT_OFFICE_PHONE,
            COLUMN_CONTACT_MANAGER_ID,
            COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID,
            COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID,
            COLUMN_CONTACT_DEPARTMENT_ID,
            COLUMN_CONTACT_SHARING_OFFICE_LOCATION,
            COLUMN_CONTACT_IS_ARCHIVED
    );

    protected static final String DROP_CONTACTS_TABLE_SQL = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CONTACTS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_CONTACTS_TABLE_SQL);
    }

}
