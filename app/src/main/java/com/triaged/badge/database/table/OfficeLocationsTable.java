package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class OfficeLocationsTable extends AbstractTable {


    public static final String TABLE_NAME = "office_locations";

    public static final String COLUMN_OFFICE_LOCATION_NAME = "name";
    public static final String COLUMN_OFFICE_LOCATION_ADDRESS = "address";
    public static final String COLUMN_OFFICE_LOCATION_CITY = "city";
    public static final String COLUMN_OFFICE_LOCATION_STATE = "state";
    public static final String COLUMN_OFFICE_LOCATION_ZIP = "zip";
    public static final String COLUMN_OFFICE_LOCATION_COUNTRY = "country";
    public static final String COLUMN_OFFICE_LOCATION_LAT = "latitude";
    public static final String COLUMN_OFFICE_LOCATION_LNG = "longitude";


    @Override
    public void onCreate(SQLiteDatabase db) {

        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE ").append(TABLE_NAME).append("(")
                .append(COLUMN_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(COLUMN_OFFICE_LOCATION_NAME).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_ADDRESS).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_CITY).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_STATE).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_ZIP).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_COUNTRY).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_LAT).append(" TEXT, ")
                .append(COLUMN_OFFICE_LOCATION_LNG).append(" TEXT")
                .append(");");

        db.execSQL(createSql.toString());

        db.execSQL("CREATE INDEX office_location_id_index ON " + TABLE_NAME + "(" + COLUMN_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
