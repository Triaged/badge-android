package com.triaged.badge.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.triaged.badge.app.DataProviderService;

/**
 * @author Created by jc on 7/10/14.
 */

public class CompanySQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_CONTACTS = "contacts";
    public static final String TABLE_DEPARTMENTS = "departments";
    public static final String TABLE_OFFICE_LOCATIONS = "office_locations";
    public static final String TABLE_MESSAGES = "messages";

    public static final String COLUMN_CONTACT_ID = "_id";
    public static final String COLUMN_CONTACT_LAST_NAME = "last_name";
    public static final String COLUMN_CONTACT_FIRST_NAME = "first_name";
    public static final String COLUMN_CONTACT_AVATAR_URL= "avatar_url";
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

    public static final String JOINED_DEPARTMENT_NAME = "department_name";
    public static final String JOINED_MANAGER_FIRST_NAME = "manager_first_name";
    public static final String JOINED_MANAGER_LAST_NAME = "manager_last_name";
    public static final String JOINED_OFFICE_NAME = "office_name";

    public static final String COLUMN_DEPARTMENT_ID = "_id";
    public static final String COLUMN_DEPARTMENT_NAME = "name";
    public static final String COLUMN_DEPARTMENT_NUM_CONTACTS = "num_contacts";

    public static final String COLUMN_OFFICE_LOCATION_ID = "_id";
    public static final String COLUMN_OFFICE_LOCATION_NAME = "name";
    public static final String COLUMN_OFFICE_LOCATION_ADDRESS = "address";
    public static final String COLUMN_OFFICE_LOCATION_CITY = "city";
    public static final String COLUMN_OFFICE_LOCATION_STATE = "state";
    public static final String COLUMN_OFFICE_LOCATION_ZIP = "zip";
    public static final String COLUMN_OFFICE_LOCATION_COUNTRY = "country";
    public static final String COLUMN_OFFICE_LOCATION_LAT = "latitude";
    public static final String COLUMN_OFFICE_LOCATION_LNG = "longitude";

    public static final String COLUMN_MESSAGES_ID = "_id";
    public static final String COLUMN_MESSAGES_BODY = "body";
    public static final String COLUMN_MESSAGES_THREAD_ID = "thread_id";
    public static final String COLUMN_MESSAGES_FROM_ID = "from_user_id";
    public static final String COLUMN_MESSAGES_ACK = "message_acknowledged";


    protected static final String SQL_DATABASE_NAME = "badge.db";
    protected static final int DATABASE_VERSION = 1;
    private static final String CREATE_CONTACTS_TABLE_SQL = String.format( "create table %s (%s  integer primary key autoincrement, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s integer, %s integer, %s integer, %s integer, %s integer );",
            TABLE_CONTACTS,
            COLUMN_CONTACT_ID,
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
            COLUMN_CONTACT_SHARING_OFFICE_LOCATION
    );

    protected static final String CREATE_DEPARTMENTS_TABLE_SQL = String.format( "create table %s (%s  integer primary key, %s text, %s integer );",
            TABLE_DEPARTMENTS,
            COLUMN_DEPARTMENT_ID,
            COLUMN_DEPARTMENT_NAME,
            COLUMN_DEPARTMENT_NUM_CONTACTS
    );

    protected static final String CREATE_OFFICE_LOCATIONS_TABLE_SQL = String.format( "create table %s (%s  integer primary key, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text);",
            TABLE_OFFICE_LOCATIONS,
            COLUMN_OFFICE_LOCATION_ID,
            COLUMN_OFFICE_LOCATION_NAME,
            COLUMN_OFFICE_LOCATION_ADDRESS,
            COLUMN_OFFICE_LOCATION_CITY,
            COLUMN_OFFICE_LOCATION_STATE,
            COLUMN_OFFICE_LOCATION_ZIP,
            COLUMN_OFFICE_LOCATION_COUNTRY,
            COLUMN_OFFICE_LOCATION_LAT,
            COLUMN_OFFICE_LOCATION_LNG
    );

    protected static final String CREATE_MESSAGES_TABLE_SQL = String.format( "create table %s (%s  integer primary key, %s integer, %s integer, %s text, %s integer);",
            TABLE_MESSAGES,
            COLUMN_MESSAGES_ID,
            COLUMN_MESSAGES_THREAD_ID,
            COLUMN_MESSAGES_FROM_ID,
            COLUMN_MESSAGES_BODY,
            COLUMN_MESSAGES_ACK
    );

    protected static final String DROP_CONTACTS_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_CONTACTS );
    protected static final String DROP_DEPARTMENTS_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_DEPARTMENTS );
    protected static final String DROP_OFFICE_LOCATIONS_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_OFFICE_LOCATIONS );
    protected static final String DROP_MESSAGES_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_MESSAGES );

    private SQLiteDatabase openDatabase;
    private DataProviderService dataProviderService;

    public CompanySQLiteHelper( DataProviderService dataProviderService ) {
        super( dataProviderService, SQL_DATABASE_NAME, null, DATABASE_VERSION);
        openDatabase = null;
        this.dataProviderService = dataProviderService;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL( CREATE_CONTACTS_TABLE_SQL );
        db.execSQL( CREATE_DEPARTMENTS_TABLE_SQL );
        db.execSQL( CREATE_OFFICE_LOCATIONS_TABLE_SQL );
        db.execSQL( CREATE_MESSAGES_TABLE_SQL );
        dataProviderService.dataClearedCallback();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL( DROP_CONTACTS_TABLE_SQL );
        db.execSQL( DROP_DEPARTMENTS_TABLE_SQL );
        db.execSQL( DROP_OFFICE_LOCATIONS_TABLE_SQL );
        db.execSQL( DROP_MESSAGES_TABLE_SQL );
        onCreate( db );
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        openDatabase = super.getWritableDatabase();
        return openDatabase;
    }
}
