package com.triaged.badge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.OfficeLocationsTable;

/**
 * @author Created by jc on 7/10/14.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    protected static final String DATABASE_NAME = "badge.db";
    protected static final int DATABASE_VERSION = 21;

    private static Context mContext;

    public static final String JOINED_DEPARTMENT_NAME = "department_name";
    public static final String JOINED_MANAGER_FIRST_NAME = "manager_first_name";
    public static final String JOINED_MANAGER_LAST_NAME = "manager_last_name";
    public static final String JOINED_OFFICE_NAME = "office_name";

//    private DataProviderService dataProviderService;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        this.dataProviderService = dataProviderService;
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        new ContactsTable().onCreate(db);
        new DepartmentsTable().onCreate(db);
        new MessagesTable().onCreate(db);
        new OfficeLocationsTable().onCreate(db);

//        dataProviderService.dataClearedCallback();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new ContactsTable().onUpgrade(db, oldVersion, newVersion);
        new DepartmentsTable().onUpgrade(db, oldVersion, newVersion);
        new MessagesTable().onUpgrade(db, oldVersion, newVersion);
        new OfficeLocationsTable().onUpgrade(db, oldVersion, newVersion);

        onCreate(db);
    }


    public static void deleteDatabase() {
        mContext.deleteDatabase(DATABASE_NAME);
    }
}
