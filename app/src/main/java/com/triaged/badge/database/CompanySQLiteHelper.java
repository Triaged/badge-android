package com.triaged.badge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.net.DataProviderService;

/**
 * @author Created by jc on 7/10/14.
 */

public class CompanySQLiteHelper extends SQLiteOpenHelper {

    public static final String JOINED_DEPARTMENT_NAME = "department_name";
    public static final String JOINED_MANAGER_FIRST_NAME = "manager_first_name";
    public static final String JOINED_MANAGER_LAST_NAME = "manager_last_name";
    public static final String JOINED_OFFICE_NAME = "office_name";


    protected static final String SQL_DATABASE_NAME = "badge.db";
    protected static final int DATABASE_VERSION = 21;


    private SQLiteDatabase openDatabase;
    private DataProviderService dataProviderService;

    public CompanySQLiteHelper(DataProviderService dataProviderService) {
        super(dataProviderService, SQL_DATABASE_NAME, null, DATABASE_VERSION);
        openDatabase = null;
        this.dataProviderService = dataProviderService;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        new ContactsTable().onCreate(db);
        new DepartmentsTable().onCreate(db);
        new MessagesTable().onCreate(db);
        new OfficeLocationsTable().onCreate(db);

        dataProviderService.dataClearedCallback();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new ContactsTable().onUpgrade(db, oldVersion, newVersion);
        new DepartmentsTable().onUpgrade(db, oldVersion, newVersion);
        new MessagesTable().onUpgrade(db, oldVersion, newVersion);
        new OfficeLocationsTable().onUpgrade(db, oldVersion, newVersion);

        onCreate(db);
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        openDatabase = super.getWritableDatabase();
        return openDatabase;
    }
}
