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
    public static final String COLUMN_CONTACT_DEPARTMENT_NAME = "department_name";
    public static final String COLUMN_CONTACT_SHARING_OFFICE_LOCATION = "sharing_office_location";

    public static final String COLUMN_DEPARTMENT_ID = "_id";
    public static final String COLUMN_DEPARTMENT_NAME = "name";
    public static final String COLUMN_DEPARTMENT_NUM_CONTACTS = "num_contacts";


    protected static final String SQL_DATABASE_NAME = "badge.db";
    protected static final int DATABASE_VERSION = 1;
    private static final String CREATE_CONTACTS_TABLE_SQL = String.format( "create table %s (%s  integer primary key autoincrement, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s integer, %s integer, %s integer, %s integer, %s text, %s integer );",
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
            COLUMN_CONTACT_DEPARTMENT_NAME,
            COLUMN_CONTACT_SHARING_OFFICE_LOCATION
    );

    protected static final String CREATE_DEPARTMENTS_TABLE_SQL = String.format( "create table %s (%s  integer primary key, %s text, %s integer );",
            TABLE_DEPARTMENTS,
            COLUMN_DEPARTMENT_ID,
            COLUMN_DEPARTMENT_NAME,
            COLUMN_DEPARTMENT_NUM_CONTACTS
    );

    protected static final String DROP_CONTACTS_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_CONTACTS );
    protected static final String DROP_DEPARTMENTS_TABLE_SQL = String.format( "DROP TABLE IF EXISTS %s", TABLE_DEPARTMENTS );
    protected static final String CLEAR_CONTACTS_SQL = String.format("DELETE FROM %s", TABLE_CONTACTS);
    protected static final String CLEAR_DEPARTMENTS_SQL = String.format("DELETE FROM %s", TABLE_DEPARTMENTS );


    private SQLiteDatabase openDatabase = null;

    public CompanySQLiteHelper( Context context ) {
        super( context, SQL_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL( CREATE_CONTACTS_TABLE_SQL );
        db.execSQL( CREATE_DEPARTMENTS_TABLE_SQL );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL( DROP_CONTACTS_TABLE_SQL );
        db.execSQL( DROP_DEPARTMENTS_TABLE_SQL );
        onCreate( db );
    }

    public void clearContacts() {
        if (openDatabase != null) {
            openDatabase.execSQL( CLEAR_CONTACTS_SQL );
        }
        else {
            throw new IllegalStateException( "Can't access the database before getWritableDatabase() has been called." );
        }
    }

    public void clearDepartments() {
        if( openDatabase != null ) {
            openDatabase.execSQL( CLEAR_DEPARTMENTS_SQL );
        }
        else {
            throw new IllegalStateException( "Can't access the database before getWritableDatabase() has been called." );
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        openDatabase = super.getWritableDatabase();
        return openDatabase;
    }
}
