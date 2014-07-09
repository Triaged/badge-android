package com.triaged.badge.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.LruCache;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.triaged.badge.data.Contact;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

    public static final String CONTACTS_AVAILABLE_INTENT = "com.triage.badge.CONTACTS_AVAILABLE";

    private static final String SQL_DATABASE_NAME = "badge.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_DATABASE_SQL = String.format( "create table %s (%s  integer primary key autoincrement, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s text, %s integer, %s integer, %s integer, %s integer, %s integer );",
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
    private static final String QUERY_ALL_CONTACTS_SQL = String.format("SELECT * FROM %s ORDER BY %s;", TABLE_CONTACTS, COLUMN_CONTACT_LAST_NAME );
    private static final String SELECT_MANAGED_CONTACTS_SQL = String.format( "SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ?", COLUMN_CONTACT_ID, COLUMN_CONTACT_FIRST_NAME, COLUMN_CONTACT_LAST_NAME, COLUMN_CONTACT_AVATAR_URL, COLUMN_CONTACT_JOB_TITLE, TABLE_CONTACTS, COLUMN_CONTACT_MANAGER_ID );

    protected ExecutorService sqlThread;
    protected CompanySQLiteHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected BadgeApiClient apiClient;
    protected ArrayList<Contact> contactList;
    protected Handler handler;
    protected volatile boolean initialized;
    protected LruCache<String, Bitmap> thumbCache;
    protected HttpClient httpClient;
    protected MimeTypeMap mimeTypeMap;

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
        mimeTypeMap = MimeTypeMap.getSingleton();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        int cacheSize = maxMemory / 8;
        thumbCache = new LruCache<String, Bitmap>( cacheSize ) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

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
        httpClient = new DefaultHttpClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.shutdown();
        httpClient.getConnectionManager().shutdown();
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

    /**
     * Return a cursor to a set of contact rows that the given id manages.
     * Only name, avatar, and title columns are available in the returned cursor.
     *
     * Caller must close the Cursor when no longer needed.
     *
     * @param contactId manager id
     * @return db cursor
     */
    protected Cursor getContactsManaged( int contactId ) {
        if( database != null  ) {
            return database.rawQuery( SELECT_MANAGED_CONTACTS_SQL, new String[] { String.valueOf( contactId ) } );
        }
        return null;
    }

    /**
     * Query the db to get a contact given an id. Always returns the
     * latest and greatest local device data.
     *
     * @param contactId, an integer
     * @return a Contact
     */
    protected Contact getContact(int contactId) {
        if (database !=null ) {
            Cursor cursor = database.rawQuery("SELECT * FROM contacts WHERE _id=?", new String[]{String.valueOf(contactId)});
            try {
                if (cursor.moveToFirst()) {
                    Contact contact = new Contact();
                    contact.fromCursor(cursor);
                    return contact;
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Draws the contact's thumb as a bitmap in to the specified image view.
     *
     * First, a small in memory cache is consulted to see if the bitmap is available, and if
     * so, the bitmap is synchronously drawn in to the image view.
     *
     * If not, a much larger disk cache is consulted asynchronously, and if it is, the bitmap is decoded
     * and stored in the memory cache before being drawn back on the main thread.
     *
     * If not in the disk cache, as a last resort, the image is downloaded in the BG and
     * placed in to the disk and memory caches.
     *
     * @param c contact
     * @param thumbImageView
     */
    protected void setSmallContactImage( Contact c, ImageView thumbImageView ) {
        Bitmap b = thumbCache.get( c.avatarUrl );
        if( b != null ) {
            // Hooray!
            thumbImageView.setImageBitmap( b );
        }
        else {
            new DownloadImageTask( c.avatarUrl, thumbImageView, thumbCache ).execute();
        }
    }

    /**
     * Downloads the image each time it's called and sets the bitmap resource on the imageview.
     *
     * TODO This should use the same disk cache as the small contact image once that's implemented.
     *
     * @param c contact
     * @param imageView
     */
    protected void setLargeContactImage( Contact c, ImageView imageView ) {
        new DownloadImageTask( c.avatarUrl, imageView ).execute();
    }

    /**
     * Query the db to get a cursor to the latest set of all contacts.
     * Caller is responsible for closing the cursor when finished.
     *
     * @return a cursor to all contact rows
     */
    protected Cursor getContactsCursor() {
        if (database != null) {
            return database.rawQuery(QUERY_ALL_CONTACTS_SQL, EMPTY_STRING_ARRAY);
        }
        return null;
    }

    public class LocalBinding extends Binder {
        /**
         * @see DataProviderService#getContactsCursor()
         */
        public Cursor getContactsCursor() {
            return DataProviderService.this.getContactsCursor();
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#getContact(int)
         */
        public Contact getContact(int contactId) {
            return DataProviderService.this.getContact( contactId );
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#setSmallContactImage(com.triaged.badge.data.Contact, android.widget.ImageView)
         */
        public void setSmallContactImage( Contact c, ImageView thumbImageView ) {
            DataProviderService.this.setSmallContactImage(c, thumbImageView);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#setLargeContactImage(com.triaged.badge.data.Contact, android.widget.ImageView)
         */
        public void setLargeContactImage( Contact c, ImageView imageView ) {
            DataProviderService.this.setLargeContactImage( c, imageView );
        }

        /**
         * Reports whether the database is initialized and ready to return data.
         *
         * @return true if data is available
         */
        public boolean isInitialized() {
            return initialized;
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#getContactsManaged(int)
         */
        public Cursor getContactsManaged( int contactId ) {
            return DataProviderService.this.getContactsManaged( contactId );
        }
    }

    /**
     * Background task to fetch an image from the server, set it as the resource
     * for an image view, and optionally cache the image for future use.
     */
    private class DownloadImageTask extends AsyncTask<Void, Void, Void> {
        private String urlStr = null;
        private ImageView thumbImageView = null;
        private LruCache<String, Bitmap> memoryCache = null;


        /**
         * Task won't save images in a memory cache.
         *
         * @param url
         * @param thumbImageView
         */
        protected DownloadImageTask( String url, ImageView thumbImageView  ) {
            this( url, thumbImageView, null );
        }

        /**
         * Task will save images in a memory cache.
         *
         * @param url
         * @param thumbImageView
         * @param memoryCache cache to put image in to after downloading.
         */
        protected DownloadImageTask( String url, ImageView thumbImageView, LruCache<String, Bitmap> memoryCache ) {
            this.urlStr = url;
            this.thumbImageView = thumbImageView;
            this.memoryCache = memoryCache;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE );
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if( info != null && info.isConnected() ) {

                try {
                    URI uri = new URI(urlStr);
                    HttpGet imageGet = new HttpGet( uri );
                    HttpHost host = new HttpHost( uri.getHost() );
                    HttpResponse response = httpClient.execute(host, imageGet);
                    if( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ) {
                        InputStream imgStream = response.getEntity().getContent();
                        final Bitmap bitmap = BitmapFactory.decodeStream( imgStream );
                        imgStream.close();
                        if( bitmap != null ) {
                            //final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, thumbImageView.getWidth(), thumbImageView.getHeight(), false);
                            handler.post( new Runnable() {
                                @Override
                                public void run() {
                                    thumbImageView.setImageBitmap( bitmap );
                                }
                            } );

                            if( memoryCache != null ) {
                                memoryCache.put(urlStr, bitmap);
                            }
                        }
                        else {
                            Log.w( LOG_TAG, "Decoded bitmap from " + urlStr + " was null. Bad image data?" );
                        }
                    }
                    else {
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }
                    }
                }
                catch (URISyntaxException e) {
                    // Womp womp
                    Log.e(LOG_TAG, "Either we got a bad URL from the api or we did something stupid", e);
                }
                catch( IOException e ) {
                    Log.w( LOG_TAG, "Network issue reading image data" );
                }
            }
            return null;
        }
    }
}
