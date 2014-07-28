package com.triaged.badge.app;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;
import com.triaged.badge.data.DiskLruCache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

    public static final String REGISTERED_DEVICE_ID_PREFS_KEY = "badgeDeviceId";
    public static final String COMPANY_NAME_PREFS_KEY = "companyName";
    public static final String COMPANY_ID_PREFS_KEY = "companyId";

    public static final String DB_UPDATED_ACTION = "com.triage.badge.DB_UPDATED";
    public static final String DB_AVAILABLE_ACTION = "com.triage.badge.DB_AVAILABLE";
    public static final String LOGGED_OUT_ACTION = "com.triage.badge.LOGGED_OUT";


    protected static final String QUERY_ALL_CONTACTS_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s ORDER BY contact.%s;",
                CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME,
                CompanySQLiteHelper.JOINED_DEPARTMENT_NAME,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.TABLE_DEPARTMENTS,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME
            );

    protected static final String QUERY_DEPARTMENT_CONTACTS_SQL =
            String.format( "SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s WHERE contact.%s = ? ORDER BY contact.%s;",
                CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME,
                CompanySQLiteHelper.JOINED_DEPARTMENT_NAME,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.TABLE_DEPARTMENTS,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME
            );

    protected static final String QUERY_CONTACT_SQL =
            String.format( "SELECT contact.*, office.%s %s, department.%s %s, manager.%s %s, manager.%s %s FROM %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s LEFT OUTER JOIN %s manager ON contact.%s = manager.%s LEFT OUTER JOIN %s office ON contact.%s = office.%s  WHERE contact.%s = ?",
                CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME,
                CompanySQLiteHelper.JOINED_OFFICE_NAME,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME,
                CompanySQLiteHelper.JOINED_DEPARTMENT_NAME,
                CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME,
                CompanySQLiteHelper.JOINED_MANAGER_FIRST_NAME,
                CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME,
                CompanySQLiteHelper.JOINED_MANAGER_LAST_NAME,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.TABLE_DEPARTMENTS,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_ID,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_ID,
                CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS,
                CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID,
                CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_ID
            );
    protected static final String QUERY_CONTACTS_WITH_EXCEPTION_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s WHERE contact.%s != ? ORDER BY %s;",
                CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME,
                CompanySQLiteHelper.JOINED_DEPARTMENT_NAME,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.TABLE_DEPARTMENTS,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME
            );
    protected static final String QUERY_MANAGED_CONTACTS_SQL =
            String.format( "SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department ON  contact.%s = department.%s WHERE contact.%s = ? ORDER BY %s;",
                CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME,
                CompanySQLiteHelper.JOINED_DEPARTMENT_NAME,
                CompanySQLiteHelper.TABLE_CONTACTS,
                CompanySQLiteHelper.TABLE_DEPARTMENTS,
                CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_DEPARTMENT_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID,
                CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME
            );

    protected static final String QUERY_ALL_DEPARTMENTS_SQL = String.format( "SELECT * FROM %s WHERE %s > ? ORDER BY %s;", CompanySQLiteHelper.TABLE_DEPARTMENTS, CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS, CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME );
    protected static final String CLEAR_DEPARTMENTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_DEPARTMENTS );
    protected static final String CLEAR_CONTACTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_CONTACTS );
    protected static final String CLEAR_OFFICE_LOCATIONS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS );
    protected static final String QUERY_ALL_OFFICES_SQL = String.format( "SELECT *  FROM %s ORDER BY %s;", CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME );
    protected static final String QUERY_OFFICE_LOCATION_SQL = String.format( "SELECT %s FROM %s WHERE %s = ?", CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME, CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID );

    protected static final String LAST_SYNCED_PREFS_KEY = "lastSyncedOn";
    protected static final String API_TOKEN_PREFS_KEY = "apiToken";
    protected static final String LOGGED_IN_USER_ID_PREFS_KEY = "loggedInUserId";
    protected static final String INSTALLED_VERSION_PREFS_KEY = "installedAppVersion";

    protected static final String[] EMPTY_STRING_ARRAY = new String[] { };
    protected static final String[] DEPTS_WITH_CONTACTS_SQL_ARGS = new String[] { "0" };
    protected static final String[] ALL_DEPTS_SQL_ARGS = new String[] { "-1" };


    protected static final String SERVICE_ANDROID = "android";


    protected volatile Contact loggedInUser;
    protected ExecutorService sqlThread;
    protected CompanySQLiteHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected BadgeApiClient apiClient;
    protected ArrayList<Contact> contactList;
    protected Handler handler;
    protected volatile boolean initialized;

    protected HttpClient httpClient;
    protected MimeTypeMap mimeTypeMap;

    private LocalBinding localBinding;
    private LocalBroadcastManager localBroadcastManager;

    // Img cache related stuffs

    private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    protected LruCache<String, Bitmap> thumbCache;



    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                try {
                    File cacheDir = params[0];
                    mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                    mDiskCacheStarting = false; // Finished initialization
                    mDiskCacheLock.notifyAll(); // Wake any waiting threads
                }
                catch( IOException e ) {
                    Log.w( LOG_TAG, "Couldn't open disk image cache.", e );
                }
            }
            return null;
        }
    }


    /**
     *
     * @param context
     * @param uniqueName
     * @return
     */
    protected File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable() ? getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinding;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences( this );
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        String apiToken = prefs.getString( API_TOKEN_PREFS_KEY, "" );
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadExecutor();
        databaseHelper = new CompanySQLiteHelper( this );
        apiClient = new BadgeApiClient( apiToken );
        contactList = new ArrayList( 250 );
        handler = new Handler();
        localBinding = new LocalBinding();
        mimeTypeMap = MimeTypeMap.getSingleton();

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

        httpClient = new DefaultHttpClient();

        // Initialize disk cache on background thread
        File cacheDir = getDiskCacheDir(this, DISK_CACHE_SUBDIR);
        new InitDiskCacheTask().execute(cacheDir);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.getConnectionManager().shutdown();
        httpClient.getConnectionManager().shutdown();
        if( !mDiskCacheStarting && mDiskLruCache != null ) {
            try {
                mDiskLruCache.close();
            }
            catch( IOException e ) {
                Log.w( LOG_TAG, "IOException closing disk cache", e );
            }
        }
        database = null;
    }

    /**
     * Call this function to cause a full refresh of site data
     * next time we sync, rather than a delta. Primary use case
     * is when an app upgrade causes a DB schema change.
     */
    public void dataClearedCallback() {
        lastSynced = 0l;
    }

    /**
     * Syncs company info from the cloud to the device.
     *
     * Notifies listeners via local broadcast that data has been updated with the {@link #DB_UPDATED_ACTION}
     *
     * @param db
     */
    protected void syncCompany( SQLiteDatabase db ) {
        ConnectivityManager cMgr = (ConnectivityManager) getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo info = cMgr.getActiveNetworkInfo();
        if( info == null || !info.isConnected() ) {
            // TODO listen for network becoming available so we can sync then.

            return;
        }

        boolean updated = false;
        lastSynced = System.currentTimeMillis();
        prefs.edit().putLong( LAST_SYNCED_PREFS_KEY, lastSynced ).commit();
        try {
            db.beginTransaction();
            db.execSQL(CLEAR_CONTACTS_SQL);
            db.execSQL(CLEAR_DEPARTMENTS_SQL);
            db.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);
            HttpResponse response = apiClient.downloadCompanyRequest(lastSynced);
            ensureNotUnauthorized( response );
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {

                    ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream(256 * 1024 /* 256 k */);
                    response.getEntity().writeTo(jsonBuffer);


                    JSONArray companyArr = new JSONArray(jsonBuffer.toString("UTF-8"));
                    JSONObject companyObj = companyArr.getJSONObject(0);
                    // Allow immediate GC
                    jsonBuffer = null;
                    ContentValues values = new ContentValues();

                    JSONArray contactsArr = companyObj.getJSONArray("users");
                    int contactsLength = contactsArr.length();
                    for (int i = 0; i < contactsLength; i++) {
                        JSONObject newContact = contactsArr.getJSONObject(i);
                        setContactDBValesFromJSON( newContact, values );
                        db.insert(CompanySQLiteHelper.TABLE_CONTACTS, null, values);
                        values.clear();
                    }

                    if (companyObj.has("uses_departments") && companyObj.getBoolean("uses_departments")) {
                        JSONArray deptsArr = companyObj.getJSONArray("departments");
                        int deptsLength = deptsArr.length();
                        for (int i = 0; i < deptsLength; i++) {
                            JSONObject dept = deptsArr.getJSONObject( i );
                            values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_ID, dept.getInt("id"));
                            values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME, dept.getString("name"));
                            values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS, dept.getString( "users_count" ));
                            db.insert(CompanySQLiteHelper.TABLE_DEPARTMENTS, null, values);
                            values.clear();
                        }
                    }

                    if( companyObj.has( "office_locations" ) ) {
                        JSONArray locations = companyObj.getJSONArray( "office_locations" );
                        int locationsLength = locations.length();
                        for( int i = 0; i < locationsLength; i++ ) {
                            JSONObject location = locations.getJSONObject( i );
                            values.put( CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID, location.getInt( "id" ) );
                            setStringContentValueFromJSONUnlessNull( location, "name", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME );
                            setStringContentValueFromJSONUnlessNull( location, "street_address", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ADDRESS );
                            setStringContentValueFromJSONUnlessNull( location, "city", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_CITY );
                            setStringContentValueFromJSONUnlessNull( location, "state", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_STATE );
                            setStringContentValueFromJSONUnlessNull( location, "zip_code", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ZIP );
                            setStringContentValueFromJSONUnlessNull( location, "country", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_COUNTRY );
                            setStringContentValueFromJSONUnlessNull( location, "latitude", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LAT );
                            setStringContentValueFromJSONUnlessNull( location, "longitude", values, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LNG );
                            db.insert( CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS, null, values );
                            values.clear();
                        }
                    }

                    loggedInUser = getContact( prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 ) );

                }
                else {
                    Log.e(LOG_TAG, "Got status " + statusCode + " from API. Handle this appropriately!");
                }
            }
            finally {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
            }
            db.setTransactionSuccessful();
            updated = true;
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
        if( updated ) {
            localBroadcastManager.sendBroadcast( new Intent(DB_UPDATED_ACTION) );
        }

    }


    private void setContactDBValesFromJSON( JSONObject json, ContentValues values ) throws JSONException {
        values.put(CompanySQLiteHelper.COLUMN_CONTACT_ID, json.getInt("id"));
        setStringContentValueFromJSONUnlessNull(json, "last_name", values, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME);
        setStringContentValueFromJSONUnlessNull(json, "first_name", values, CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME);
        setStringContentValueFromJSONUnlessNull(json, "avatar_face_url", values, CompanySQLiteHelper.COLUMN_CONTACT_AVATAR_URL);
        setStringContentValueFromJSONUnlessNull(json, "email", values, CompanySQLiteHelper.COLUMN_CONTACT_EMAIL);
        setIntContentValueFromJSONUnlessBlank( json, "manager_id", values, CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID);
        setIntContentValueFromJSONUnlessBlank( json, "primary_office_location_id", values, CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID);
        setIntContentValueFromJSONUnlessBlank( json, "current_office_location_id", values, CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID);
        setIntContentValueFromJSONUnlessBlank( json, "department_id", values, CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID );
        if (json.has("sharing_office_location") && !json.isNull("sharing_office_location")) {
            int sharingInt = json.getBoolean("sharing_office_location") ? 1 : 0;
            values.put(CompanySQLiteHelper.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, sharingInt);
        }
        if (json.has("employee_info")) {
            JSONObject employeeInfo = json.getJSONObject("employee_info");
            setStringContentValueFromJSONUnlessNull(employeeInfo, "job_title", values, CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "job_start_date", values, CompanySQLiteHelper.COLUMN_CONTACT_START_DATE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "birth_date", values, CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE);
            // This comes in as iso 8601 GMT date.. but we save "August 1" or whatever
            String birthDateStr = values.getAsString( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE );
            if( birthDateStr != null ) {
                values.put( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthDateString(birthDateStr) );
            }
            String startDateStr = values.getAsString(CompanySQLiteHelper.COLUMN_CONTACT_START_DATE);
            if( startDateStr != null ) {
                values.put( CompanySQLiteHelper.COLUMN_CONTACT_START_DATE, Contact.convertStartDateString(startDateStr) );
            }
            setStringContentValueFromJSONUnlessNull(employeeInfo, "cell_phone", values, CompanySQLiteHelper.COLUMN_CONTACT_CELL_PHONE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "office_phone", values, CompanySQLiteHelper.COLUMN_CONTACT_OFFICE_PHONE);
        }

    }

    /**
     * Pulls an int from json and sets it as a content value if the key exists and is not a null literal.
     *
     * @param json json object possibly containing key
     * @param key key in to json object where value should live
     * @param values database values to add to if the key exists
     * @param column column name to set in database values
     * @throws JSONException
     */
    private static void setStringContentValueFromJSONUnlessNull(JSONObject json, String key, ContentValues values, String column) throws JSONException {
        if ( !json.isNull( key ) ) {
            values.put(column, json.getString(key));
        }
    }

    /**
     * Pulls an int from json and sets it as a content value if the key exists and is not the empty string.
     *
     * @param json json object possibly containing key
     * @param key key in to json object where value should live
     * @param values database values to add to if the key exists
     * @param column column name to set in database values
     * @throws JSONException
     */
    private static void setIntContentValueFromJSONUnlessBlank( JSONObject json, String key, ContentValues values, String column ) throws JSONException {
        if ( !json.isNull( key ) && !"".equals( json.getString( key ) ) ) {
            values.put( column, json.getInt(key));
        }
    }

    /**
     * Return a cursor to a set of contact rows that the given id manages.
     * All columns included.
     *
     * Caller must close the Cursor when no longer needed.
     *
     * @param contactId manager id
     * @return db cursor
     */
    protected Cursor getContactsManaged( int contactId ) {
        if( database != null  ) {
            return database.rawQuery(QUERY_MANAGED_CONTACTS_SQL, new String[] { String.valueOf( contactId ) } );
        }
        throw new IllegalStateException( "getContactsManaged() called before database available." );
    }

    /**
     * Return only contacts in a given department. Same fields returned as {@link #getContactsCursor()}
     * @param departmentId
     * @return
     */
    protected Cursor getContactsByDepartmentCursor(int departmentId) {
        if( database != null ) {
            return database.rawQuery( QUERY_DEPARTMENT_CONTACTS_SQL, new String[] { String.valueOf( departmentId ) } );
        }
        throw new IllegalStateException( "getContactsByDepartmentCursor() called before database available." );
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
            Cursor cursor = database.rawQuery( QUERY_CONTACT_SQL, new String[]{String.valueOf(contactId)});
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
        throw new IllegalStateException( "getContact() called before database available." );
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
     * If a placeholder view is specified, it will be hidden
     *
     * @param c contact
     * @param thumbImageView the view to set the image on.
     * @param placeholderView null or a view that should be hidden once the image has been set.
     */
    protected void setSmallContactImage( Contact c, View thumbImageView, View placeholderView ) {
        Bitmap b = thumbCache.get( c.avatarUrl );
        if( b != null ) {
            // Hooray!
            assignBitmapToView( b, thumbImageView );
            if( placeholderView != null ) {
                placeholderView.setVisibility( View.GONE );
            }
        }
        else {
            new LoadImageAsyncTask( c.avatarUrl, thumbImageView, placeholderView, thumbCache ).execute();
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
        new LoadImageAsyncTask( c.avatarUrl, imageView, null ).execute();
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
        throw new IllegalStateException( "getContactsCursor() called before database available." );
    }

    /**
     * Query the db to get a cursor to all contacts except for
     * the logged in user.
     *
     * Caller must close the cursor when finished.
     *
     * @return a cursor to all contact rows minus 1
     */
    protected Cursor getContactsCursorExcludingLoggedInUser() {
        if( database != null ) {
            return database.rawQuery(QUERY_CONTACTS_WITH_EXCEPTION_SQL, new String[] { String.valueOf( loggedInUser.id ) } );
        }
        throw new IllegalStateException( "getContactsCursorExcludingLoggedInUser() called before database available." );
    }

    /**
     * Query the db to get a cursor to the full list of departments
     *
     * @return a cursor to all dept rows
     */
    protected Cursor getDepartmentCursor( boolean onlyThoseWithContacts ) {
        if( database != null ) {
            String[] args = onlyThoseWithContacts ? DEPTS_WITH_CONTACTS_SQL_ARGS : ALL_DEPTS_SQL_ARGS;
            return database.rawQuery( QUERY_ALL_DEPARTMENTS_SQL, args );
        }
        throw new IllegalStateException( "getDepartmentCursor() called before database available." );
    }

    /**
     *
     * @return cursor to all office location rows
     */
    protected Cursor getOfficeLocationsCursor() {
        if( database != null ) {
            return database.rawQuery( QUERY_ALL_OFFICES_SQL, EMPTY_STRING_ARRAY );
        }
        throw new IllegalStateException( "getOfficeLocationsCursor() called before database available." );
    }


    /**
     * @param locationId id of the office
     * @return Name of the office.
     */
    private String getOfficeLocationName(int locationId) {
        if( database != null ) {
            Cursor cursor = database.rawQuery( QUERY_OFFICE_LOCATION_SQL, new String[] { String.valueOf( locationId ) } );
            cursor.moveToFirst();
            String name = Contact.getStringSafelyFromCursor( cursor, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME );
            cursor.close();
            return name;
        }
        throw new IllegalStateException( "getOfficeLocationName() called before database available." );
    }

    /**
     * Helper that sets a bitmap to a plain ole {@link android.widget.ImageView}
     *
     * @param b
     * @param v
     */
    protected void assignBitmapToView( Bitmap b, View v ) {
        if( v instanceof ImageView ) {
            ((ImageView)v).setImageBitmap( b );
        }
    }

    /**
     * When the service detects that there is no active user
     * or api token, it calls this function.
     *
     * This launches the login activity in a new task and sends a local
     * broadcast so that activities can listen and kill themselves.
     *
     * This should only be called on the sql thread.
     */
    protected void loggedOut() {
        if( loggedInUser != null && loggedInUser.currentOfficeLocationId > 0 && !"".equals( apiClient.apiToken ) ) {
            // User initiated logout, make sure they don't get "stuck"
            checkOutOfOfficeSynchronously(loggedInUser.currentOfficeLocationId);
        }
        loggedInUser = null;
        prefs.edit().
                remove( API_TOKEN_PREFS_KEY).
                remove( LOGGED_IN_USER_ID_PREFS_KEY ).
                remove( LAST_SYNCED_PREFS_KEY ).
                remove( COMPANY_ID_PREFS_KEY ).
                remove( COMPANY_NAME_PREFS_KEY ).
                remove( LoginActivity.PROPERTY_REG_ID ).
                remove( REGISTERED_DEVICE_ID_PREFS_KEY ).
                remove( LocationTrackingService.TRACK_LOCATION_PREFS_KEY ).commit();
        stopService( new Intent( this, LocationTrackingService.class ) );

        // Wipe DB, we're not logged in anymore.
        database.execSQL(CLEAR_CONTACTS_SQL);
        database.execSQL(CLEAR_DEPARTMENTS_SQL);
        database.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);
        Intent intent = new Intent( this, LoginActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );

        localBroadcastManager.sendBroadcast(new Intent(LOGGED_OUT_ACTION));
        MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance( DataProviderService.this, BadgeApplication.MIXPANEL_TOKEN);
        mixpanelAPI.clearSuperProperties();
    }

    protected void changePassword( final String currentPassword, final String newPassword, final String newPasswordConfirmation, final AsyncSaveCallback saveCallback ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                JSONObject postBody = new JSONObject();
                try {
                    JSONObject user = new JSONObject();
                    postBody.put( "user", user );
                    user.put( "current_password", currentPassword );
                    user.put( "password", newPassword );
                    user.put( "password_confirmation", newPasswordConfirmation );
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for change password", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }

                try {
                    HttpResponse response = apiClient.changePasswordRequest( postBody );
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    if( statusCode == HttpStatus.SC_OK  ) {
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( -1 );
                                }
                            });
                        }
                    }
                    else {
                        fail( "Got unexpected response from server. Please contact Badge HQ.", saveCallback );
                    }
                }
                catch( IOException e ) {
                    fail("There was a network issue changing password, please check your connection and try again.", saveCallback);
                }

            }
        });
    }

    /**
     * This method is for when a user elects to log out. It DELETES /devices/:id/sign_out
     * and on success wipes local data on the phone and removes tokens.
     *
     */
    protected void unregisterDevice() {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DataProviderService.this);
                    int deviceId = prefs.getInt(REGISTERED_DEVICE_ID_PREFS_KEY, -1);
                    // Do this regardless of whether we can communicate with the cloud or not.
                    loggedOut();
                    if( deviceId != -1 ) {
                        HttpResponse response = apiClient.unregisterDeviceRequest( deviceId );
                        ensureNotUnauthorized( response );
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }

                    }
                }
                catch( IOException e ) {
                    Log.e( LOG_TAG, "Wasn't able to delete device on api", e );
                }
            }
        } );
    }

    /**
     * Posts to /devices to register upon login.
     */
    protected void registerDevice() {

        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                String gcmRegId = prefs.getString(LoginActivity.PROPERTY_REG_ID, "");
                int androidVersion = android.os.Build.VERSION.SDK_INT;

                JSONObject postData = new JSONObject();
                JSONObject deviceData = new JSONObject();
                try {
                    postData.put( "device", deviceData);
                    deviceData.put( "token", gcmRegId );
                    deviceData.put( "os_version", androidVersion );
                    deviceData.put( "service", SERVICE_ANDROID );
                    deviceData.put( "guid", Settings.Secure.getString( DataProviderService.this.getContentResolver(),
                            Settings.Secure.ANDROID_ID ) );
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for device registration", e);
                    return;
                }

                try {
                    HttpResponse response = apiClient.registerDeviceRequest(postData);
                    ensureNotUnauthorized( response );
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                        // Get new department id
                        JSONObject newDevice = parseJSONResponse(response.getEntity());

                        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(DataProviderService.this).edit();
                        prefsEditor.putInt(REGISTERED_DEVICE_ID_PREFS_KEY, newDevice.getInt("id"));
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                    }
                } catch (IOException e) {
                    // We'll try again next time the app starts.
                    Log.e(LOG_TAG, "IOException trying to register device with badge HQ", e);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Response from Badge HQ wasn't parseable, sad panda", e);
                }

            }
        });
    }

    /**
     * Attempt to create a new persistent app session by exchanging email
     * and password credentials for an api token over the network.
     *
     * @param email
     * @param password
     * @param loginCallback if non null, {@link com.triaged.badge.app.DataProviderService.LoginCallback#loginFailed(String)} on this obj will be called on auth failure.
     */
    protected void loginAsync( final String email, final String password, final LoginCallback loginCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                JSONObject postData = new JSONObject();
                JSONObject creds = new JSONObject();

                try {
                    creds.put("email", email);
                    creds.put("password", password);
                    postData.put("user_login", creds);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "JSON exception creating post body for login", e);
                    fail("Unexpected issue, please contact Badge HQ");
                    return;
                }

                try {
                    HttpResponse response = apiClient.createSessionRequest(postData);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        try {
                            JSONObject errorObj = parseJSONResponse(response.getEntity());
                            String error = errorObj.getJSONArray("errors").getString(0);
                            fail(error);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "JSON exception parsing error response from 401.", e);
                            fail("Login failed.");
                        }
                    } else if (statusCode == HttpStatus.SC_OK) {
                        try {
                            JSONObject account = parseJSONResponse(response.getEntity());
                            apiClient.apiToken = account.getString("authentication_token");
                            loggedInUser = new Contact();
                            loggedInUser.fromJSON(account.getJSONObject("current_user"));
                            prefs.edit().putInt( COMPANY_ID_PREFS_KEY, account.getInt( "company_id" ) ).
                                    putString(API_TOKEN_PREFS_KEY, apiClient.apiToken).
                                    putString( COMPANY_NAME_PREFS_KEY, account.getString( "company_name" ) ).
                                    putInt(LOGGED_IN_USER_ID_PREFS_KEY, loggedInUser.id).commit();

                            JSONObject props = constructMixpanelSuperProperties();
                            MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance( DataProviderService.this, BadgeApplication.MIXPANEL_TOKEN);
                            mixpanelAPI.registerSuperProperties(props);

                            if (loginCallback != null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        loginCallback.loginSuccess(loggedInUser);
                                    }
                                });
                            }

                            syncCompany(database);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "JSON exception parsing login success.", e);
                            fail("Credentials were OK, but the response couldn't be understood. Please notify Badge HQ.");
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        Log.e(LOG_TAG, "Unexpected http response code " + statusCode + " from api.");
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase());
                    }
                } catch (IOException e) {
                    fail("We had trouble connecting to Badge to authenticate. Check your phone's network connection and try again.");
                }

            }

            private void fail(final String reason) {
                if (loginCallback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            loginCallback.loginFailed(reason);
                        }
                    });
                }
            }
        });
    }

    /**
     * Every time we hit the API, we should make sure the status code
     * returned wasn't unauthorized. If it was, we assume
     * the user has been logged out or termed or something and we reset
     * to logged out state.
     *
     * @param response
     */
    protected void ensureNotUnauthorized( HttpResponse response ) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            loggedOut();
        }
    }

    /**
     * Updates a contact's info via the api and broadcasts
     * {@link #DB_UPDATED_ACTION} locally if successful.
     *
     * @param contactId
     */
    protected void refreshContact( final int contactId ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = apiClient.getContact( contactId );
                    int statusCode = response.getStatusLine().getStatusCode();

                    if( statusCode == HttpStatus.SC_OK ) {
                        try {
                            JSONObject contact = parseJSONResponse(response.getEntity());
                            ContentValues values = new ContentValues();
                            setContactDBValesFromJSON( contact, values );
                            database.update( CompanySQLiteHelper.TABLE_CONTACTS, values, String.format( "%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID ), new String[] { String.valueOf( contactId) } );
                            localBroadcastManager.sendBroadcast( new Intent( DB_UPDATED_ACTION ) );
                        }
                        catch( JSONException e ) {
                            Log.w( LOG_TAG, "Couldn't refresh contact due to malformed or unexpected JSON response.", e );
                        }
                    }
                    else  if( response.getEntity() != null ) {
                        Log.w( LOG_TAG, "Response from /users/id was " + response.getStatusLine().getReasonPhrase() );
                        response.getEntity().consumeContent();
                    }

                }
                catch( IOException e ) {
                    Log.w( LOG_TAG, "Couldn't refresh contact due to network issue." );
                }
            }
        } );
    }

    /**
     * Changes current user's office to this office id, when location svcs determine
     * they are there. If a current office is already set, does nothing.
     *
     * @param officeId
     */
    protected void checkInToOffice(final int officeId) {
        if( loggedInUser.currentOfficeLocationId <= 0 ) {
            sqlThread.submit( new Runnable() {
                @Override
                public void run() {

                    try {
                        HttpResponse response = apiClient.checkinRequest( officeId );
                        ensureNotUnauthorized( response );
                        int status = response.getStatusLine().getStatusCode();
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }
                        if(  status == HttpStatus.SC_OK ) {
                            ContentValues values = new ContentValues();
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, officeId );
                            database.update( CompanySQLiteHelper.TABLE_CONTACTS, values, String.format( "%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID ), new String[] { String.valueOf( loggedInUser.id ) }  );
                            loggedInUser.currentOfficeLocationId = officeId;
                        }
                        else {
                            Log.w( LOG_TAG, "Server responded with " + status + " trying to check out of location." );
                        }
                    }
                    catch( IOException e ) {
                        Log.w( LOG_TAG, "Couldn't check out in to office due to IOException " + officeId );
                        // Rats. Next time?
                    }

                }
            });
        }
    }


    /**
     * Async wrapper for {@link #checkOutOfOfficeSynchronously(int)}
     * for when called from the UI.
     *
     * @param officeId
     */
    protected void checkOutOfOfficeAsync(final int officeId) {
        if (loggedInUser.currentOfficeLocationId == officeId) {
            sqlThread.submit(new Runnable() {
                @Override
                public void run() {
                    checkOutOfOfficeSynchronously( officeId );
                }
            });
        }
    }

    /**
     * If the user is currently "checked in" to this office,
     * clear that status because they are no longer close to it.
     *
     * @param officeId
     */
    protected void checkOutOfOfficeSynchronously(int officeId ) {
        try {
            HttpResponse response = apiClient.checkoutRequest(officeId);
            ensureNotUnauthorized(response);
            int status = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
            if (status == HttpStatus.SC_OK) {
                ContentValues values = new ContentValues();
                values.put(CompanySQLiteHelper.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, -1);
                database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
                loggedInUser.currentOfficeLocationId = -1;
            } else {
                Log.w(LOG_TAG, "Server responded with " + status + " trying to check out of location.");
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "Couldn't check out of office due to IOException " + officeId);
            // Rats. Next time?
        }
    }

    /**
     * Get a writable database and do an incremental sync of new data from the cloud.
     *
     * Notifies listeners via the {@link #DB_AVAILABLE_ACTION} when the database is ready for use.
     */
    protected void initDatabase() {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                try {
                    database = databaseHelper.getWritableDatabase();
                    // TODO for now just making sure this runs at most once per half hour.
                    // In the future it should ask for any records modified since last sync
                    // every time.
                    int loggedInContactId = prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 );
                    if( loggedInContactId > 0 ) {
                        loggedInUser = getContact(loggedInContactId);
                    }
                    initialized = true;
                    localBroadcastManager.sendBroadcast( new Intent(DB_AVAILABLE_ACTION) );

                    if ( !apiClient.apiToken.isEmpty() ) {
                        syncCompany(database);

                        // Check if this is the first boot of a new install
                        // If it is, since we're logged in, if the user hasnt
                        // disabled location tracking, start the tracking service.
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            if( pInfo.versionCode > prefs.getInt( INSTALLED_VERSION_PREFS_KEY, -1 ) ) {
                                prefs.edit().putInt(INSTALLED_VERSION_PREFS_KEY, pInfo.versionCode ).commit();
                                if( prefs.getBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true )  ) {
                                    startService( new Intent( getApplicationContext(), LocationTrackingService.class ) );
                                }
                            }
                        }
                        catch( PackageManager.NameNotFoundException e ) {
                            // Look at all the fucks I give!
                        }

                    }
                    else {
                        loggedOut();
                    }
                } catch (Throwable t) {
                    Log.e(LOG_TAG, "UNABLE TO GET DATABASE", t);
                }
            }
        }  );
    }

    /**
     * Update the user's entire profile at once.
     *
     * @param firstName
     * @param lastName
     * @param cellPhone
     * @param officePhone
     * @param jobTitle
     * @param departmentId
     * @param managerId
     * @param primaryOfficeId
     * @param startDateString
     * @param birthDateString
     * @param newAvatarFile
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void saveAllProfileDataAsync(
            final String firstName,
            final String lastName,
            final String cellPhone,
            final String officePhone,
            final String jobTitle,
            final int departmentId,
            final int managerId,
            final int primaryOfficeId,
            final String startDateString,
            final String birthDateString,
            final byte[] newAvatarFile,
            final AsyncSaveCallback saveCallback
    ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if( database == null ) {
                    fail( "Database not ready yet. Please report to Badge HQ", saveCallback );
                    return;
                }

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put( "user", data );
                    data.put( "employee_info_attributes", employeeInfo );
                    data.put( "first_name", firstName );
                    data.put( "last_name", lastName );
                    data.put( "department_id", departmentId );
                    data.put( "manager_id", managerId );
                    data.put( "primary_office_location_id", primaryOfficeId );

                    employeeInfo.put( "birth_date", birthDateString );
                    employeeInfo.put( "cell_phone", cellPhone );
                    employeeInfo.put( "job_title", jobTitle );
                    employeeInfo.put( "office_phone", officePhone );
                    employeeInfo.put( "job_start_date", startDateString );
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for basic profile data", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }


                try {
                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized( response );
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        JSONObject account = parseJSONResponse( response.getEntity() );
                        // Update local data.
                        ContentValues values = new ContentValues();
                        setContactDBValesFromJSON(account.getJSONObject("current_user"), values);


                        // OK now send avatar if there was a new one specified
                        if( newAvatarFile != null ) {
                            HttpResponse avatarResponse = apiClient.uploadNewAvatar( newAvatarFile );
                            int avatarStatusCode = avatarResponse.getStatusLine().getStatusCode();
                            if( avatarResponse.getEntity() != null ) {
                                avatarResponse.getEntity().consumeContent();
                            }
                            if( avatarStatusCode == HttpStatus.SC_OK  ) {

                            }
                            else {
                                fail("Save avatar response was '" + avatarResponse.getStatusLine().getReasonPhrase() + "'", saveCallback);
                            }
                        }

                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact( prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 ) );
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( -1 );
                                }
                            });
                        }
                    }
                    else {
                        if( response.getEntity() != null  ) {
                            response.getEntity().consumeContent();
                        }
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                }
                catch( IOException e ) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }
                catch( JSONException e ) {
                    fail("We didn't understand the server response, please contact Badge HQ.", saveCallback);
                }
            }
        } );
    }

    /**
     * Save first name, last name, cell phone, and birth date in local DB
     * and PATCH these values on account in the cloud.
     *
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param firstName
     * @param lastName
     * @param birthDateString
     * @param cellPhone
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void saveBasicProfileDataAsync( final String firstName, final String lastName, final String birthDateString, final String cellPhone, final AsyncSaveCallback saveCallback ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if( database == null ) {
                    fail( "Database not ready yet. Please report to Badge HQ", saveCallback );
                    return;
                }


                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put( "user", data );
                    data.put( "employee_info_attributes", employeeInfo );
                    data.put( "first_name", firstName );
                    data.put( "last_name", lastName );
                    employeeInfo.put( "birth_date", birthDateString );
                    employeeInfo.put( "cell_phone", cellPhone );
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for basic profile data", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }


                try {
                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized(response);
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME, firstName );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME, lastName );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_CELL_PHONE, cellPhone );
                        values.put(CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE, birthDateString);

                        if( birthDateString != null ) {
                            values.put( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthDateString(birthDateString) );
                        }
                        //values.put( CompanySQLiteHelper.COL)
                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact( prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 ) );
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( -1 );
                                }
                            });
                        }
                    }
                    else {
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                }
                catch( IOException e ) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }
            }

        } );
    }

    /**
     * Saves the id of the primary location for the logged in user locally
     * and via the API.
     *
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param primaryLocation
     * @param saveCallback
     */
    protected void savePrimaryLocationAsync(final int primaryLocation, final AsyncSaveCallback saveCallback) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }

                JSONObject postData = new JSONObject();
                JSONObject user = new JSONObject();
                try {
                    postData.put("user", user);
                    user.put("primary_office_location_id", primaryLocation);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "JSON exception creating post body for create department", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.patchAccountRequest(postData);
                    ensureNotUnauthorized( response );
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, primaryLocation );
                        //values.put( CompanySQLiteHelper.COL)
                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact( prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 ) );
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( -1 );
                                }
                            });
                        }
                    }
                    else {
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }
                        fail( "Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback );
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }

            }
        } );
    }

    /**
     * Create a new department and persist it to the database. Database row
     * only created if api create option successful.
     *
     * @param department name of new department
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void createNewDepartmentAsync( final String department, final AsyncSaveCallback saveCallback ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if( database == null ) {
                    fail( "Database not ready yet. Please report to Badge HQ", saveCallback );
                    return;
                }

                JSONObject postData = new JSONObject();
                JSONObject departmentData = new JSONObject();
                try {
                    postData.put("department", departmentData);
                    departmentData.put("name", department);
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for create department", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }

                try {
                    HttpResponse response = apiClient.createDepartmentRequest(postData);
                    ensureNotUnauthorized( response );
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED ) {
                        // Get new department id
                        JSONObject newDepartment = parseJSONResponse( response.getEntity() );

                        // Update local data.
                        ContentValues values = new ContentValues();
                        final int departmentId = newDepartment.getInt("id");
                        values.put( CompanySQLiteHelper.COLUMN_DEPARTMENT_ID, departmentId );
                        values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME, newDepartment.getString("name"));
                        values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS, newDepartment.getInt("contact_count"));
                        database.insert(CompanySQLiteHelper.TABLE_DEPARTMENTS, null, values);
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( departmentId );
                                }
                            });
                        }
                    }
                    else {
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }
                        fail( "Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback );
                    }
                }
                catch( IOException e ) {
                    fail( "There was a network issue saving, please check your connection and try again.", saveCallback );
                }
                catch( JSONException e ) {
                    fail( "We didn't understand the server response, please contact Badge HQ.", saveCallback );
                }
            }
        } );

    }

    /**
     * Create a new office location via the API and save it to the local db if successful
     *
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param address
     * @param city
     * @param state
     * @param zip
     * @param country
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure. if success, new office location id will be provided as arg
     */
    protected void createNewOfficeLocationAsync( final String address, final String city, final String state, final String zip, final String country, final AsyncSaveCallback saveCallback ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if( database == null ) {
                    fail( "Database not ready yet. Please report to Badge HQ", saveCallback );
                    return;
                }

                // { “office_location” : { “street_address” : , “city” : , “zip_code” : , “state” : , “country” : , } }
                JSONObject postData = new JSONObject();
                JSONObject locationData = new JSONObject();
                try {
                    postData.put("office_location", locationData );
                    locationData.put( "street_address", address );
                    locationData.put( "city", city );
                    locationData.put( "zip_code", zip );
                    locationData.put( "state", state );
                    locationData.put( "country", country );
                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating post body for create office location", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }

                try {
                    HttpResponse response = apiClient.createLocationRequest(postData);
                    ensureNotUnauthorized( response );
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED ) {
                        // Get new department id
                        JSONObject newOffice = parseJSONResponse( response.getEntity() );

                        // Update local data.
                        ContentValues values = new ContentValues();
                        final int officeLocationId = newOffice.getInt("id");
                        values.put( CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID, officeLocationId );
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME, newOffice.getString("name"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ADDRESS, newOffice.getString("street_address"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_CITY, newOffice.getString("city"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_STATE, newOffice.getString("state"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ZIP, newOffice.getString("zip_code"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_COUNTRY, newOffice.getString("country"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LAT, newOffice.getString("latitude"));
                        values.put(CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LNG, newOffice.getString("longitude"));
                        database.insert(CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS, null, values);
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( officeLocationId );
                                }
                            });
                        }
                    }
                    else {
                        if( response.getEntity() != null ) {
                            response.getEntity().consumeContent();
                        }
                        fail( "Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback );
                    }
                }
                catch( IOException e ) {
                    fail( "There was a network issue saving, please check your connection and try again.", saveCallback );
                }
                catch( JSONException e ) {
                    fail( "We didn't understand the server response, please contact Badge HQ.", saveCallback );
                }
            }
        } );


    }

    /**
     * Saves department, job title, and manager info.
     *
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param jobTitle
     * @param departmentId
     * @param managerId
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void savePositionProfileDataAsync( final String jobTitle, final int departmentId, final int managerId, final AsyncSaveCallback saveCallback ) {
        sqlThread.submit( new Runnable() {
            @Override
            public void run() {
                if( database == null ) {
                    if( saveCallback != null ) {
                        saveCallback.saveFailed( "Database not ready yet. Please report to Badge HQ" );
                    }
                    return;
                }

                // Wrap entire operation in the transaction so if syncing over http fails
                // the tx will roll back.

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();
                    user.put( "user", data );
                    data.put( "employee_info_attributes", employeeInfo );
                    data.put( "department_id", departmentId );
                    data.put( "manager_id", managerId );
                    employeeInfo.put( "job_title", jobTitle );

                }
                catch( JSONException e ) {
                    Log.e(LOG_TAG, "JSON exception creating patch body for position profile data", e);
                    fail( "Unexpected issue, please contact Badge HQ", saveCallback );
                    return;
                }

                try {

                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized( response );
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE, jobTitle );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_DEPARTMENT_ID, departmentId );
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID, managerId );
                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact( prefs.getInt( LOGGED_IN_USER_ID_PREFS_KEY, -1 ) );
                        if( saveCallback != null ) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess( -1 );
                                }
                            });
                        }
                    }
                    else {
                        fail( "Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback );
                    }
                }
                catch( IOException e ) {
                    fail( "There was a network issue saving, please check your connection and try again.", saveCallback );
                }
            }
        } );
    }

    /**
     * Utility method for any async save operation to invoke the fail method
     * on the provided callback when things go awry.
     *
     * Invokes on the main thread.
     *
     * @param reason
     * @param saveCallback
     */
    protected  void fail( final String reason, final AsyncSaveCallback saveCallback ) {
        if( saveCallback != null ) {
            handler.post( new Runnable() {
                @Override
                public void run() {
                    saveCallback.saveFailed( reason );
                }
            });
        }
    }

    /**
     * Construct JSONObject of user data to send with Mixpanel event tracking
     * */
    protected JSONObject getBasicMixpanelData() {
        return new JSONObject();
    }

    protected JSONObject constructMixpanelSuperProperties() {
        JSONObject mixpanelData = new JSONObject();
        try {
            mixpanelData.put("firstName", loggedInUser.firstName);
            mixpanelData.put("lastName", loggedInUser.lastName);
            mixpanelData.put("email", loggedInUser.email);
            mixpanelData.put("company.name", prefs.getString(COMPANY_NAME_PREFS_KEY, ""));
            mixpanelData.put("company.identifier", prefs.getInt(COMPANY_ID_PREFS_KEY, -1));
            return mixpanelData;
        }
        catch (JSONException e) {
            Log.w( LOG_TAG, "Couldn't construct mix panel super property json" );
        }
        return new JSONObject();
    }

    /**
     * Local, non rpc interface for this service.
     */
    public class LocalBinding extends Binder {
        /**
         * @see DataProviderService#initDatabase()
         */
        public void initDatabase() {
            DataProviderService.this.initDatabase();
        }

        /**
         * @see DataProviderService#getContactsCursor()
         */
        public Cursor getContactsCursor() {
            return DataProviderService.this.getContactsCursor();
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#getContactsByDepartmentCursor(int)
         */
        public Cursor getContactsByDepartmentCursor( int departmentId ) {
            return DataProviderService.this.getContactsByDepartmentCursor(departmentId);
        }

        /**
         * @see DataProviderService#getContactsCursorExcludingLoggedInUser()
         */
        public Cursor getContactsCursorExcludingLoggedInUser() {
           return DataProviderService.this.getContactsCursorExcludingLoggedInUser();
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#getContact(int)
         */
        public Contact getContact(int contactId) {
            return DataProviderService.this.getContact(contactId);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#setSmallContactImage(com.triaged.badge.data.Contact, android.view.View, android.view.View )
         */
        public void setSmallContactImage( Contact c, View thumbImageView, View placeholderView ) {
            DataProviderService.this.setSmallContactImage(c, thumbImageView, placeholderView );
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#setLargeContactImage(com.triaged.badge.data.Contact, android.widget.ImageView)
         */
        public void setLargeContactImage( Contact c, ImageView imageView ) {
            DataProviderService.this.setLargeContactImage(c, imageView);
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

        /**
         * @see com.triaged.badge.app.DataProviderService#loginAsync(String, String, com.triaged.badge.app.DataProviderService.LoginCallback)
         */
        public void loginAsync( String email, String password, LoginCallback loginCallback) {
            DataProviderService.this.loginAsync( email, password, loginCallback);
        }

        /**
         * @return null of not logged in, contact representing user acct otherwise.
         */
        public Contact getLoggedInUser( ) {
            return loggedInUser;
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#saveBasicProfileDataAsync(String, String, String, String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void saveBasicProfileDataAsync( String firstName, String lastName, String birthDateString, String cellPhone, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.saveBasicProfileDataAsync(firstName, lastName, birthDateString, cellPhone, saveCallback);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#savePositionProfileDataAsync(String, int, int, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void savePositionProfileDataAsync( String jobTitle, int departmentId, int managerId, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.savePositionProfileDataAsync(jobTitle, departmentId, managerId, saveCallback);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#savePrimaryLocationAsync(int, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void savePrimaryLocationASync( int primaryLocation, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.savePrimaryLocationAsync(primaryLocation, saveCallback);
        }


        /**
         * @see com.triaged.badge.app.DataProviderService#saveAllProfileDataAsync(String, String, String, String, String, int, int, int, String, String, byte[], com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void saveAllProfileDataAsync( String firstName, String lastName, String cellPhone, String officePhone, String jobTitle, int departmentId, int managerId, int primaryOfficeId, String startDateString, String birthDateString, byte[] newAvatarFile, AsyncSaveCallback saveCallback) {
            DataProviderService.this.saveAllProfileDataAsync(firstName, lastName, cellPhone, officePhone, jobTitle, departmentId, managerId, primaryOfficeId, startDateString, birthDateString, newAvatarFile, saveCallback);
        }

        /**
         * @see DataProviderService#getOfficeLocationName(int)
         */
        public String getOfficeLocationName( int locationId ) {
            return DataProviderService.this.getOfficeLocationName(locationId);
        }

        /*
         * @see DataProviderService#getDepartmentCursor(boolean)
         */
        public Cursor getDepartmentCursor( boolean onlyNonEmptyDepts ) {
            return DataProviderService.this.getDepartmentCursor( onlyNonEmptyDepts );
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#getOfficeLocationsCursor() ()
         */
        public Cursor getOfficeLocationsCursor() {
            return DataProviderService.this.getOfficeLocationsCursor();
        }

        /** @see DataProviderService#loggedOut()  */
        public void logout() {
            DataProviderService.this.unregisterDevice();
        }
        
        /**
         * @see com.triaged.badge.app.DataProviderService#createNewDepartmentAsync(String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void createNewDepartmentAsync( String department, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.createNewDepartmentAsync(department, saveCallback);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#createNewOfficeLocationAsync(String, String, String, String, String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void createNewOfficeLocationAsync( String address, String city, String state, String zip, String country, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.createNewOfficeLocationAsync(address, city, state, zip, country, saveCallback);
        }

        /**
         * @see DataProviderService#registerDevice()
         */
        public void registerDevice() {
            DataProviderService.this.registerDevice();
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#checkInToOffice(int)
         */
        public void checkInToOffice( int officeId  ) {
            DataProviderService.this.checkInToOffice(officeId);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#checkOutOfOfficeAsync(int)
         */
        public void checkOutOfOffice( int officeId ) {
            DataProviderService.this.checkOutOfOfficeAsync(officeId);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#changePassword(String, String, String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void changePassword( String oldPassword, String newPassword, String newPasswordConfirmation, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.changePassword(oldPassword, newPassword, newPasswordConfirmation, saveCallback);
        }

        /**
         * @see DataProviderService#getBasicMixpanelData() (int)
         */
        public JSONObject getBasicMixpanelData() {
            return DataProviderService.this.getBasicMixpanelData();
        }

        public void refreshContact( int contactId ) {
            DataProviderService.this.refreshContact( contactId );
        }
    }

    /**
     * Background task to fetch an image first from a disk cache,
     * and if not there yet, the server, set it as the resource
     * for an image view, and keep it in disk and mem cache for the future
     */
    private class LoadImageAsyncTask extends AsyncTask<Void, Void, Void> {
        private String urlStr = null;
        private View thumbView = null;
        private View placeholderView;
        private LruCache<String, Bitmap> memoryCache = null;


        /**
         * Task won't save images in a memory cache.
         *
         * @param url
         * @param thumbView
         */
        protected LoadImageAsyncTask(String url, View thumbView, View placeholderView) {
            this( url, thumbView, placeholderView, null );
        }

        /**
         * Task will save images in a memory cache.
         *
         * @param url img url.
         * @param thumbView view on which to set the bitmap once downloaded.
         * @param placeholderView view to hide if we successfully download the image and set it on the view.
         * @param memoryCache cache to put image in to after downloading.
         */
        protected LoadImageAsyncTask(String url, View thumbView, View placeholderView, LruCache<String, Bitmap> memoryCache) {
            this.urlStr = url;
            this.thumbView = thumbView;
            this.memoryCache = memoryCache;
            this.placeholderView = placeholderView;
        }

        protected String getUrlHash() {
            return String.valueOf( urlStr.hashCode() );
        }

        protected Bitmap loadBitmapFromDisk( ) {
            try {
                synchronized (mDiskCacheLock) {
                    // Wait while disk cache is started from background thread
                    while (mDiskCacheStarting) {
                        try {
                            mDiskCacheLock.wait();
                        } catch (InterruptedException e) {
                            return null;
                        }
                    }
                    if (mDiskLruCache != null) {
                        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(getUrlHash());
                        if( snapshot != null ) {
                            BufferedInputStream stream = new BufferedInputStream(snapshot.getInputStream(0));
                            Bitmap bitmap = BitmapFactory.decodeStream(stream);
                            stream.close();
                            return bitmap;
                        }
                    }
                }
            }
            catch( IOException e ) {
                // OK... ?
                Log.e( LOG_TAG, "Error reading from disk cache", e );
            }
            return null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Is it in disk cache?

            Bitmap bitmap = loadBitmapFromDisk();



            if( bitmap == null ) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {

                    try {
                        URI uri = new URI(urlStr);
                        HttpGet imageGet = new HttpGet(uri);
                        HttpHost host = new HttpHost(uri.getHost());
                        HttpResponse response = httpClient.execute(host, imageGet);
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            // Add full sized img to disk cache.
                            synchronized (mDiskCacheLock) {
                                if (mDiskLruCache != null && !mDiskCacheStarting ) {
                                    DiskLruCache.Editor editor = mDiskLruCache.edit(getUrlHash());
                                    OutputStream out = editor.newOutputStream(0);
                                    if( out != null ) {
                                        response.getEntity().writeTo(out);
                                        out.close();
                                        editor.commit();
                                        bitmap = loadBitmapFromDisk();
                                    }
                                }
                            }
                        } else {
                            if (response.getEntity() != null) {
                                response.getEntity().consumeContent();
                            }
                        }
                    } catch (URISyntaxException e) {
                        // Womp womp
                        Log.e(LOG_TAG, "Either we got a bad URL from the api or we did something stupid", e);
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Network issue reading image data", e);
                    }
                }
            }

            if( bitmap != null ) {
                final Bitmap scaledBitmap = thumbView.getWidth() > 0 ? Bitmap.createScaledBitmap(bitmap, thumbView.getWidth(), thumbView.getHeight(), false) : bitmap;

                if( memoryCache != null ) {
                    memoryCache.put(urlStr, scaledBitmap);
                }

                handler.post( new Runnable() {
                    @Override
                    public void run() {
                        assignBitmapToView(scaledBitmap, thumbView);
                        if( placeholderView != null ) {
                            placeholderView.setVisibility( View.GONE );
                        }

                    }
                });

            }
            else {
                Log.w( LOG_TAG, "Bitmap from " + urlStr + " was null. No network? Bad image data?" );
            }

            return null;
        }
    }

    /**
     * Given an http response entity, parse in to a json object.
     *
     * @param entity http response body
     * @return parsed json object
     * @throws IOException if network stream can't be read.
     * @throws JSONException if there's an error parsing json.
     */
    protected static JSONObject parseJSONResponse( HttpEntity entity ) throws IOException, JSONException {
        ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream( 1024 /* 256 k */ );
        entity.writeTo( jsonBuffer );
        jsonBuffer.close();
        String json = jsonBuffer.toString("UTF-8");
        return new JSONObject( json );
    }

    /**
     * Simple callback interface to handle login response asynchronously.
     * Callbacks are always invoked on main UI thread.
     */
    public interface LoginCallback {
        /**
         * Called if login was unsuccessful.
         *
         * @param reason Human readable message describing the failure.
         */
        public void loginFailed( String reason );

        /**
         * Login was successful, {@link com.triaged.badge.app.DataProviderService.LocalBinding#getLoggedInUser()}
         * is now guaranteed to return non null.
         *
         * @param user the now logged in user
         */
        public void loginSuccess( Contact user );
    }

    /**
     * All profile saves are async and take a callback argument.
     * If you need to be notified of the save result.
     */
    public interface AsyncSaveCallback {
        /**
         * Save has finished successfully.
         *
         * @param newId if save resulted in a new record, the id of the new record. otherwise -1
         */
        public void saveSuccess( int newId );

        /**
         * Save encountered an issue.
         *
         * @param reason human readable reason for user messaging
         */
        public void saveFailed( String reason );
    }
}
