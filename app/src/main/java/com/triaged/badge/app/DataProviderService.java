package com.triaged.badge.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.triaged.badge.data.Contact;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public static final String COLUMN_CONTACT_ID = "_id";
    public static final String COLUMN_CONTACT_LAST_NAME = "last_name";
    public static final String COLUMN_CONTACT_FIRST_NAME = "first_name";
    public static final String COLUMN_CONTACT_AVATAR_URL= "avatar_url";
    public static final String CONTACTS_AVAILABLE_INTENT = "com.triage.badge.CONTACTS_AVAILABLE";

    private static final String SQL_DATABASE_NAME = "badge.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_DATABASE_SQL = String.format( "create table %s (%s  integer primary key autoincrement, %s text, %s text, %s text);", TABLE_CONTACTS, COLUMN_CONTACT_ID, COLUMN_CONTACT_FIRST_NAME, COLUMN_CONTACT_LAST_NAME, COLUMN_CONTACT_AVATAR_URL );
    private static final String QUERY_ALL_CONTACTS_SQL = String.format("SELECT * FROM %s ORDER BY %s;", TABLE_CONTACTS, COLUMN_CONTACT_LAST_NAME );

    protected ExecutorService sqlThread;
    protected CompanySQLiteHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected BadgeApiClient apiClient;
    protected ArrayList<Contact> contactList;
    protected Handler handler;
    protected volatile boolean initialized;

    private LocalBinding localBinding;

    private LocalBroadcastManager localBroadcastManager;


    @Override
    public IBinder onBind(Intent intent) {
        return localBinding;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences( this );
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadExecutor();
        databaseHelper = new CompanySQLiteHelper();
        apiClient = new BadgeApiClient();
        contactList = new ArrayList( 250 );
        handler = new Handler();
        localBinding = new LocalBinding();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                try {
                    database = databaseHelper.getWritableDatabase();
                    // TODO for now just making sure this runs at most once per half hour.
                    // In the future it should ask for any records modified since last sync
                    // every time.

                    if (lastSynced < System.currentTimeMillis() - 1800000) {
                        syncCompany(database);
                    }

                    initialized = true;

                    // Populate list of contacts.
                    readContacts(database);
                } catch (Throwable t) {
                    Log.e(LOG_TAG, "UNABLE TO GET DATABASE", t);
                }
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
        contactList.clear();
//        Cursor contacts = db.rawQuery( QUERY_ALL_CONTACTS_SQL, EMPTY_STRING_ARRAY );
//        while( contacts.moveToNext() ) {
//            Contact contact = new Contact();
//            contact.fromCursor( contacts );
//            contactList.add( contact );
//        }
        localBroadcastManager.sendBroadcast(new Intent(CONTACTS_AVAILABLE_INTENT));
    }

    protected void syncCompany( SQLiteDatabase db ) {
        lastSynced = System.currentTimeMillis();
        prefs.edit().putLong( LAST_SYNCED_PREFS_KEY, lastSynced ).commit();
        try {
            db.beginTransaction();
            db.execSQL( String.format( "DELETE FROM %s", TABLE_CONTACTS ) );
            apiClient.downloadCompany(db, lastSynced);
            db.setTransactionSuccessful();
        }
        catch( IOException e ) {
            Log.e( LOG_TAG, "IO exception downloading company that should be handled more softly than this.", e );
        }
        catch( JSONException e ) {
            Log.e( LOG_TAG, "JSON from server not formatted correctly. Either we shouldn't have expected JSON or this is an api bug.", e );
        }
        finally {
            db.endTransaction();
        }
    }

    protected class CompanySQLiteHelper extends SQLiteOpenHelper {

        public CompanySQLiteHelper() {
            super( DataProviderService.this, SQL_DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DATABASE_SQL);
            syncCompany( db );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL( "DROP TABLE IF EXISTS" + TABLE_CONTACTS + ";" );
            onCreate( db );
        }
    }

    public class LocalBinding extends Binder {
        public List<Contact> getContacts() {
            return contactList;
        }

        /**
         * Query the db to get a cursor to the latest set of all contacts.
         * Caller is responsible for closing the cursor when finished.
         *
         * @return a cursor to all contact rows
         */
        public Cursor getContactsCursor() {
            if( database != null ) {
                return database.rawQuery( QUERY_ALL_CONTACTS_SQL, EMPTY_STRING_ARRAY );
            }
            return null;
        }

        /**
         * Reports whether the database is initialized and ready to return data.
         *
         * @return true if data is available
         */
        public boolean isInitialized() {
            return initialized;
        }
    }
}
