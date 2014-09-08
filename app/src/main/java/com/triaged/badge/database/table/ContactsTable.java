package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
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



    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(COLUMN_CONTACT_FIRST_NAME).append(" TEXT, ")
                .append(COLUMN_CONTACT_LAST_NAME).append(" TEXT, ")
                .append(COLUMN_CONTACT_AVATAR_URL).append(" TEXT, ")
                .append(COLUMN_CONTACT_JOB_TITLE).append(" TEXT, ")
                .append(COLUMN_CONTACT_EMAIL).append(" TEXT, ")
                .append(COLUMN_CONTACT_START_DATE).append(" TEXT, ")
                .append(COLUMN_CONTACT_BIRTH_DATE).append(" TEXT, ")
                .append(COLUMN_CONTACT_CELL_PHONE).append(" TEXT, ")
                .append(COLUMN_CONTACT_OFFICE_PHONE).append(" TEXT, ")
                .append(COLUMN_CONTACT_MANAGER_ID).append(" INTEGER, ")
                .append(COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID).append(" INTEGER, ")
                .append(COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID).append(" INTEGER, ")
                .append(COLUMN_CONTACT_DEPARTMENT_ID).append(" INTEGER, ")
                .append(COLUMN_CONTACT_SHARING_OFFICE_LOCATION).append(" INTEGER, ")
                .append(COLUMN_CONTACT_IS_ARCHIVED).append(" BOOLEAN")
                .append(");");

        db.execSQL(createSql.toString());

        db.execSQL("CREATE INDEX contact_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX contact_first_name_index ON " + TABLE_NAME + "(" + COLUMN_CONTACT_FIRST_NAME + ");");
        db.execSQL("CREATE INDEX contact_last_name_index ON " + TABLE_NAME + "(" + COLUMN_CONTACT_LAST_NAME + ");");
        db.execSQL("CREATE INDEX contact_manager_id_index ON " + TABLE_NAME + "(" + COLUMN_CONTACT_MANAGER_ID + ");");
        db.execSQL("CREATE INDEX contact_department_id_index ON " + TABLE_NAME + "(" + COLUMN_CONTACT_DEPARTMENT_ID + ");");
        db.execSQL("CREATE INDEX contact_is_archived_index ON " + TABLE_NAME + "(" + COLUMN_CONTACT_IS_ARCHIVED + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
