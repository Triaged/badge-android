package com.triaged.badge.database.table;

import android.database.sqlite.SQLiteDatabase;

/**
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


    protected static final String CREATE_OFFICE_LOCATIONS_TABLE_SQL = String.format(
            "create table %s (%s  integer primary key, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text);",
            TABLE_NAME,
            COLUMN_ID,
            COLUMN_OFFICE_LOCATION_NAME,
            COLUMN_OFFICE_LOCATION_ADDRESS,
            COLUMN_OFFICE_LOCATION_CITY,
            COLUMN_OFFICE_LOCATION_STATE,
            COLUMN_OFFICE_LOCATION_ZIP,
            COLUMN_OFFICE_LOCATION_COUNTRY,
            COLUMN_OFFICE_LOCATION_LAT,
            COLUMN_OFFICE_LOCATION_LNG
    );

    protected static final String DROP_OFFICElOCATIONS_TABLE_SQL = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_OFFICE_LOCATIONS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_OFFICElOCATIONS_TABLE_SQL);
    }
}
