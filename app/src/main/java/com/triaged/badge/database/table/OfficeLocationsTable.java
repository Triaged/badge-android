package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class OfficeLocationsTable extends AbstractTable {


    public static final String TABLE_NAME = "office_locations";

    public static final String CLM_NAME = "name";
    public static final String CLM_ADDRESS = "address";
    public static final String CLM_CITY = "city";
    public static final String CLM_STATE = "state";
    public static final String CLM_ZIP = "zip";
    public static final String CLM_COUNTRY = "country";
    public static final String CLM_LAT = "latitude";
    public static final String CLM_LNG = "longitude";


    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(CLM_NAME).append(" TEXT, ")
                .append(CLM_ADDRESS).append(" TEXT, ")
                .append(CLM_CITY).append(" TEXT, ")
                .append(CLM_STATE).append(" TEXT, ")
                .append(CLM_ZIP).append(" TEXT, ")
                .append(CLM_COUNTRY).append(" TEXT, ")
                .append(CLM_LAT).append(" TEXT, ")
                .append(CLM_LNG).append(" TEXT")
                .append(");");

        db.execSQL(createSql.toString());
        db.execSQL("CREATE INDEX office_location_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
