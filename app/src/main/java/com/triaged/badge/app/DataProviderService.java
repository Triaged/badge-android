package com.triaged.badge.app;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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

    protected static final String QUERY_ALL_DEPARTMENTS_SQL = String.format( "SELECT * FROM %s ORDER BY %s;", CompanySQLiteHelper.TABLE_DEPARTMENTS, CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME );
    protected static final String CLEAR_DEPARTMENTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_DEPARTMENTS );
    protected static final String CLEAR_CONTACTS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_CONTACTS );
    protected static final String CLEAR_OFFICE_LOCATIONS_SQL = String.format( "DELETE FROM %s;", CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS );
    protected static final String QUERY_ALL_OFFICES_SQL = String.format( "SELECT *  FROM %s ORDER BY %s;", CompanySQLiteHelper.TABLE_OFFICE_LOCATIONS, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME );

    protected static final String LAST_SYNCED_PREFS_KEY = "lastSyncedOn";
    protected static final String API_TOKEN_PREFS_KEY = "apiToken";
    protected static final String LOGGED_IN_USER_ID_PREFS_KEY = "loggedInUserId";
    protected static final String[] EMPTY_STRING_ARRAY = new String[] { };


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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.getConnectionManager().shutdown();
        httpClient.getConnectionManager().shutdown();
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
                            values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS, dept.getString( "contact_count" ));
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
                } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    loggedOut();
                } else {
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
            setStringContentValueFromJSONUnlessNull(employeeInfo, "start_date", values, CompanySQLiteHelper.COLUMN_CONTACT_START_DATE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "birth_date", values, CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE);
            // This comes in as iso 8601 GMT date.. but we save "August 1" or whatever
            String birthDateStr = values.getAsString( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE );
            if( birthDateStr != null ) {
                values.put( CompanySQLiteHelper.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthdayString( birthDateStr ) );
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
     * @param c contact
     * @param thumbImageView
     */
    protected void setSmallContactImage( Contact c, View thumbImageView ) {
        Bitmap b = thumbCache.get( c.avatarUrl );
        if( b != null ) {
            // Hooray!
            assignBitmapToView( b, thumbImageView );
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
    protected Cursor getDepartmentCursor() {
        if( database != null ) {
            return database.rawQuery( QUERY_ALL_DEPARTMENTS_SQL, EMPTY_STRING_ARRAY );
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
     */
    protected void loggedOut() {
        loggedInUser = null;
        prefs.edit().remove( API_TOKEN_PREFS_KEY ).remove(LOGGED_IN_USER_ID_PREFS_KEY ).commit();
        // Wipe DB, we're not logged in anymore.
        database.execSQL(CLEAR_CONTACTS_SQL);
        database.execSQL(CLEAR_DEPARTMENTS_SQL);
        database.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);
        Intent intent = new Intent( this, LoginActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
        localBroadcastManager.sendBroadcast(new Intent(LOGGED_OUT_ACTION));
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
                            prefs.edit().putString(API_TOKEN_PREFS_KEY, apiClient.apiToken).putInt(LOGGED_IN_USER_ID_PREFS_KEY, loggedInUser.id).commit();

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


                    initialized = true;
                    localBroadcastManager.sendBroadcast( new Intent(DB_AVAILABLE_ACTION) );

                    if ( !apiClient.apiToken.isEmpty() ) {
                        syncCompany(database);
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
     * @param newAvatarFileBase64Str
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
            final String newAvatarFileBase64Str,
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
                    if( newAvatarFileBase64Str != null ) {
                        data.put( "avatar", newAvatarFileBase64Str );
                    }

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
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        JSONObject account = parseJSONResponse( response.getEntity() );
                        // Update local data.
                        ContentValues values = new ContentValues();
                        setContactDBValesFromJSON( account, values );
                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
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
                    fail( "We didn't understand the server response, please contact Badge HQ.", saveCallback );
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
                        //values.put( CompanySQLiteHelper.COL)
                        database.update(CompanySQLiteHelper.TABLE_CONTACTS, values, String.format("%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID), new String[]{String.valueOf(loggedInUser.id)});
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
    protected void savePrimaryLocationASync( final int primaryLocation, final AsyncSaveCallback saveCallback ) {
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
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK ) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put( CompanySQLiteHelper.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, primaryLocation );
                        //values.put( CompanySQLiteHelper.COL)
                        database.update( CompanySQLiteHelper.TABLE_CONTACTS, values, String.format( "%s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID ), new String[] { String.valueOf( loggedInUser.id ) } );
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
                    HttpResponse response = apiClient.createDepartmentRequest( postData );

                    int statusCode = response.getStatusLine().getStatusCode();
                    if( statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED ) {
                        // Get new department id
                        JSONObject newDepartment = parseJSONResponse( response.getEntity() );

                        // Update local data.
                        ContentValues values = new ContentValues();
                        final int departmentId = newDepartment.getInt("id");
                        values.put( CompanySQLiteHelper.COLUMN_DEPARTMENT_ID, departmentId );
                        values.put(CompanySQLiteHelper.COLUMN_DEPARTMENT_NAME, newDepartment.getString("name"));
                        values.put( CompanySQLiteHelper.COLUMN_DEPARTMENT_NUM_CONTACTS, newDepartment.getInt( "contact_count" ) );
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
         * @see com.triaged.badge.app.DataProviderService#setSmallContactImage(com.triaged.badge.data.Contact, android.view.View)
         */
        public void setSmallContactImage( Contact c, View thumbImageView ) {
            DataProviderService.this.setSmallContactImage(c, thumbImageView);
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
            DataProviderService.this.saveBasicProfileDataAsync( firstName, lastName, birthDateString, cellPhone, saveCallback);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#savePositionProfileDataAsync(String, int, int, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void savePositionProfileDataAsync( String jobTitle, int departmentId, int managerId, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.savePositionProfileDataAsync(jobTitle, departmentId, managerId, saveCallback);
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#savePrimaryLocationASync(int, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void savePrimaryLocationASync( int primaryLocation, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.savePrimaryLocationASync( primaryLocation, saveCallback );
        }

        /**
         * @see DataProviderService#getDepartmentCursor()
         */
        public Cursor getDepartmentCursor() {
            return DataProviderService.this.getDepartmentCursor();
        }


        /**
         * @see com.triaged.badge.app.DataProviderService#getOfficeLocationsCursor() ()
         */
        public Cursor getOfficeLocationsCursor() {
            return DataProviderService.this.getOfficeLocationsCursor();
        }

        /** @see DataProviderService#loggedOut()  */
        public void logout() {
            DataProviderService.this.loggedOut();
        }
        
        /**
         * @see com.triaged.badge.app.DataProviderService#createNewDepartmentAsync(String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void createNewDepartmentAsync( String department, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.createNewDepartmentAsync( department, saveCallback );
        }

        /**
         * @see com.triaged.badge.app.DataProviderService#createNewOfficeLocationAsync(String, String, String, String, String, com.triaged.badge.app.DataProviderService.AsyncSaveCallback)
         */
        public void createNewOfficeLocationAsync( String address, String city, String state, String zip, String country, AsyncSaveCallback saveCallback ) {
            DataProviderService.this.createNewOfficeLocationAsync( address, city, state, zip, country, saveCallback );
        }
    }

    /**
     * Background task to fetch an image from the server, set it as the resource
     * for an image view, and optionally cache the image for future use.
     */
    private class DownloadImageTask extends AsyncTask<Void, Void, Void> {
        private String urlStr = null;
        private View thumbView = null;
        private LruCache<String, Bitmap> memoryCache = null;


        /**
         * Task won't save images in a memory cache.
         *
         * @param url
         * @param thumbView
         */
        protected DownloadImageTask( String url, View thumbView  ) {
            this( url, thumbView, null );
        }

        /**
         * Task will save images in a memory cache.
         *
         * @param url
         * @param thumbView
         * @param memoryCache cache to put image in to after downloading.
         */
        protected DownloadImageTask( String url, View thumbView, LruCache<String, Bitmap> memoryCache ) {
            this.urlStr = url;
            this.thumbView = thumbView;
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
                            //final Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, thumbView.getWidth(), thumbView.getHeight(), false);
                            handler.post( new Runnable() {
                                @Override
                                public void run() {
                                    assignBitmapToView( bitmap, thumbView );
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
