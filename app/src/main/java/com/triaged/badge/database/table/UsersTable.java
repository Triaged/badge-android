package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class UsersTable extends AbstractTable {

    public static final String TABLE_NAME = "users";

    public static final String CLM_LAST_NAME = "last_name";
    public static final String CLM_FIRST_NAME = "first_name";
    public static final String CLM_AVATAR_URL = "avatar_url";
    public static final String CLM_JOB_TITLE = "job_title";
    public static final String CLM_EMAIL = "email";
    public static final String CLM_START_DATE = "start_date";
    public static final String CLM_BIRTH_DATE = "birth_date";
    public static final String CLM_CELL_PHONE = "cell_phone";
    public static final String CLM_OFFICE_PHONE = "office_phone";
    public static final String CLM_WEBSITE = "website";
    public static final String CLM_LINKEDIN = "linkedin";
    public static final String CLM_MANAGER_ID = "manager_id";
    public static final String CLM_PRIMARY_OFFICE_LOCATION_ID = "primary_office_location_id";
    public static final String CLM_CURRENT_OFFICE_LOCATION_ID = "current_office_location_id";
    public static final String CLM_DEPARTMENT_ID = "department_id";
    public static final String CLM_SHARING_OFFICE_LOCATION = "sharing_office_location";
    public static final String CLM_IS_ARCHIVED = "is_archived";



    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(CLM_FIRST_NAME).append(" TEXT, ")
                .append(CLM_LAST_NAME).append(" TEXT, ")
                .append(CLM_AVATAR_URL).append(" TEXT, ")
                .append(CLM_JOB_TITLE).append(" TEXT, ")
                .append(CLM_EMAIL).append(" TEXT, ")
                .append(CLM_START_DATE).append(" TEXT, ")
                .append(CLM_BIRTH_DATE).append(" TEXT, ")
                .append(CLM_CELL_PHONE).append(" TEXT, ")
                .append(CLM_OFFICE_PHONE).append(" TEXT, ")
                .append(CLM_WEBSITE).append(" TEXT, ")
                .append(CLM_LINKEDIN).append(" TEXT, ")
                .append(CLM_MANAGER_ID).append(" INTEGER, ")
                .append(CLM_PRIMARY_OFFICE_LOCATION_ID).append(" INTEGER, ")
                .append(CLM_CURRENT_OFFICE_LOCATION_ID).append(" INTEGER, ")
                .append(CLM_DEPARTMENT_ID).append(" INTEGER, ")
                .append(CLM_SHARING_OFFICE_LOCATION).append(" INTEGER, ")
                .append(CLM_IS_ARCHIVED).append(" BOOLEAN")
                .append(");");

        db.execSQL(createSql.toString());
        db.execSQL("CREATE INDEX contact_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
        db.execSQL("CREATE INDEX contact_first_name_index ON " + TABLE_NAME + "(" + CLM_FIRST_NAME + ");");
        db.execSQL("CREATE INDEX contact_last_name_index ON " + TABLE_NAME + "(" + CLM_LAST_NAME + ");");
        db.execSQL("CREATE INDEX contact_manager_id_index ON " + TABLE_NAME + "(" + CLM_MANAGER_ID + ");");
        db.execSQL("CREATE INDEX contact_department_id_index ON " + TABLE_NAME + "(" + CLM_DEPARTMENT_ID + ");");
        db.execSQL("CREATE INDEX contact_is_archived_index ON " + TABLE_NAME + "(" + CLM_IS_ARCHIVED + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

}
