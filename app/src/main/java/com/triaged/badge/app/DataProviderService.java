package com.triaged.badge.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.triaged.badge.data.Contact;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This service abstracts access to contact and company
 * information. It can sync data from the cloud to the device
 * and provides stored data to the app.
 *
 * @author Created by jc on 7/7/14.
 */
public class DataProviderService extends Service {
    protected static final String LOG_TAG = DataProviderService.class.getName();

    protected static final String LAST_SYNCED_PREFS_KEY = "lastSyncedOn";
    protected static final String[] EMPTY_STRING_ARRAY = new String[] { };

    protected static final String TABLE_CONTACTS = "contacts";
    protected static final String COLUMN_CONTACT_ID = "_id";
    public static final String COLUMN_CONTACT_LAST_NAME = "last_name";
    public static final String COLUMN_CONTACT_FIRST_NAME = "first_name";
    public static final String COLUMN_CONTACT_AVATAR_URL= "avatar_url";

    private static final String SQL_DATABASE_NAME = "badge.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_DATABASE_SQL = String.format( "create table %s (%s  integer primary key autoincrement, %s text, %s text, %s text);", TABLE_CONTACTS, COLUMN_CONTACT_ID, COLUMN_CONTACT_FIRST_NAME, COLUMN_CONTACT_LAST_NAME, COLUMN_CONTACT_AVATAR_URL );

    protected ExecutorService sqlThread;
    protected CompanySQLiteHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected BadgeApiClient apiClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences( this );
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadExecutor();
        databaseHelper = new CompanySQLiteHelper();
        apiClient = new BadgeApiClient();
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                database = databaseHelper.getWritableDatabase();
                // TODO for now just making sure this runs at most once per half hour.
                // In the future it should ask for any records modified since last sync
                // every time.

                if( lastSynced < System.currentTimeMillis() - 1800000 ) {
                    syncCompany(database);
                }
                // Populate list of contacts.
                readContacts(database);
            }
        }  );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.shutdown();
        database = null;
    }

    protected void readContacts( SQLiteDatabase db ) {
        Cursor contacts = db.rawQuery( String.format("SELECT * FROM %s ORDER BY %s;", TABLE_CONTACTS, COLUMN_CONTACT_LAST_NAME ), EMPTY_STRING_ARRAY );
        ArrayList<Contact> contactList = new ArrayList( contacts.getCount() );
        while( contacts.moveToNext() ) {
            Contact contact = new Contact();
            contact.fromCursor( contacts );
            contactList.add( contact );
        }
    }

    protected void syncCompany( SQLiteDatabase db ) {
        lastSynced = System.currentTimeMillis();
        prefs.edit().putLong( LAST_SYNCED_PREFS_KEY, lastSynced ).commit();
        try {
            apiClient.downloadCompany(db, lastSynced);
        }
        catch( IOException e ) {
            Log.e( LOG_TAG, "IO exception downloading company that should be handled more softly than this.", e );
        }
    }

    protected class CompanySQLiteHelper extends SQLiteOpenHelper {

        public CompanySQLiteHelper() {
            super( DataProviderService.this, SQL_DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            database.execSQL(CREATE_DATABASE_SQL);
            syncCompany( db );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            database.execSQL( "DROP TABLE IF EXISTS" + TABLE_CONTACTS + ";" );
            onCreate( database );
        }
    }
}
