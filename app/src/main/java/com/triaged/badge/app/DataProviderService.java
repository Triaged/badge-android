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
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.triaged.badge.app.views.ProfileManagesUserView;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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

    public static final String DB_UPDATED_INTENT = "com.triage.badge.DB_UPDATED";
    public static final String DB_AVAILABLE_INTENT = "com.triage.badge.DB_AVAILABLE";
    public static final String LOGGED_OUT_ACTION = "com.triage.badge.LOGGED_OUT";

    protected static final String QUERY_ALL_CONTACTS_SQL = String.format("SELECT * FROM %s ORDER BY %s;", CompanySQLiteHelper.TABLE_CONTACTS, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME );
    protected static final String SELECT_MANAGED_CONTACTS_SQL = String.format( "SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ?", CompanySQLiteHelper.COLUMN_CONTACT_ID, CompanySQLiteHelper.COLUMN_CONTACT_FIRST_NAME, CompanySQLiteHelper.COLUMN_CONTACT_LAST_NAME, CompanySQLiteHelper.COLUMN_CONTACT_AVATAR_URL, CompanySQLiteHelper.COLUMN_CONTACT_JOB_TITLE, CompanySQLiteHelper.TABLE_CONTACTS, CompanySQLiteHelper.COLUMN_CONTACT_MANAGER_ID );

    protected static final String LAST_SYNCED_PREFS_KEY = "lastSyncedOn";
    protected static final String API_TOKEN_PREFS_KEY = "apiToken";
    protected static final String[] EMPTY_STRING_ARRAY = new String[] { };



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
    protected volatile boolean loggedIn;

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
        if( "".equals( apiToken ) ) {
            loggedOut();
        }
        else {
            loggedIn = true;
        }
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadExecutor();
        databaseHelper = new CompanySQLiteHelper( this );
        apiClient = new BadgeApiClient( apiToken ); // "8ekayof3x1P5kE_LvPFv"
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
        apiClient.shutdown();
        httpClient.getConnectionManager().shutdown();
        database = null;
    }



    /**
     * Syncs company info from the cloud to the device.
     *
     * Notifies listeners via local broadcast that data has been updated.
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
            databaseHelper.clearContacts();
            if( !apiClient.downloadCompany(db, lastSynced) ) {
                loggedOut();
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
            localBroadcastManager.sendBroadcast( new Intent( DB_UPDATED_INTENT ) );
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
        return null;
    }

    protected void assignBitmapToView( Bitmap b, View v ) {
        if( v instanceof ImageView ) {
            ((ImageView)v).setImageBitmap( b );
        }
        else if( v instanceof ProfileManagesUserView  ) {
            ((ProfileManagesUserView)v).setBitmap( b );
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
        loggedIn = false;
        prefs.edit().remove( API_TOKEN_PREFS_KEY ).commit();
        Intent intent = new Intent( this, LoginActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
        localBroadcastManager.sendBroadcast( new Intent( LOGGED_OUT_ACTION ) );
    }

    /**
     * Attempt to create a new persistent app session by exchanging email
     * and password credentials for an api token over the network.
     *
     * @param email
     * @param password
     * @param loginFailedCallback if non null, {@link com.triaged.badge.app.DataProviderService.LoginFailedCallback#loginFailed(String)} on this obj will be called on auth failure.
     */
    protected void loginAsync( String email, String password, LoginFailedCallback loginFailedCallback ) {
        new LoginTask( email, password, loginFailedCallback ).execute();
    }

    /**
     * Get a writable database and do an incremental sync of new data from the cloud.
     *
     * Notifies listeners via the {@link #DB_AVAILABLE_INTENT} when the database is ready for use.
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
                    localBroadcastManager.sendBroadcast( new Intent( DB_AVAILABLE_INTENT ) );

                    if ( loggedIn ) {
                        syncCompany(database);
                    }

                } catch (Throwable t) {
                    Log.e(LOG_TAG, "UNABLE TO GET DATABASE", t);
                }
            }
        }  );
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


        public void loginAsync( String email, String password, LoginFailedCallback loginFailedCallback ) {
            DataProviderService.this.loginAsync( email, password, loginFailedCallback );
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

    protected class LoginTask extends AsyncTask<Void, Void, Void> {
        private String email;
        private String password;
        private LoginFailedCallback loginFailedCallback;

        public LoginTask(String email, String password, LoginFailedCallback loginFailedCallback) {
            this.email = email;
            this.password = password;
            this.loginFailedCallback = loginFailedCallback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                HttpResponse response = apiClient.executeLogin(email, password);
                int statusCode = response.getStatusLine().getStatusCode();
                if( statusCode == HttpStatus.SC_UNAUTHORIZED ) {
                    try {
                        JSONObject errorObj = parseJSONResponse(response.getEntity());
                        String error = errorObj.getJSONArray("errors").getString(0);
                        fail( error );
                    }
                    catch( JSONException e ) {
                        Log.e( LOG_TAG, "JSON exception parsing error response from 401.", e );
                        fail( "Login failed." );
                    }
                }
                else if ( statusCode == HttpStatus.SC_OK ) {
                    try {
                        JSONObject account = parseJSONResponse(response.getEntity());
                        apiClient.apiToken = account.getString( "authentication_token" );
                        prefs.edit().putString( API_TOKEN_PREFS_KEY, apiClient.apiToken ).commit();
                        sqlThread.submit( new Runnable() {
                            @Override
                            public void run() {
                                syncCompany( database );
                            }
                        } );

                    }
                    catch( JSONException e ) {
                        Log.e( LOG_TAG, "JSON exception parsing login success.", e );
                        fail( "The response wasn't understood. Please notify Badge HQ." );
                    }
                }
                else {
                    if( response.getEntity() != null ) {
                        response.getEntity().consumeContent();
                    }
                    Log.e( LOG_TAG, "Unexpected http response code " + statusCode + " from api." );
                    fail( "We didn't understand Badge's response. Please notify Badge HQ." );
                }
            }
            catch( IOException e ) {
                fail( "We had trouble connecting to Badge to authenticate. Check your phone's network connection and try again." );
            }
            return null;
        }

        private JSONObject parseJSONResponse( HttpEntity entity ) throws IOException, JSONException {
            ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream( 1024 /* 256 k */ );
            entity.writeTo( jsonBuffer );
            String json = jsonBuffer.toString("UTF-8");
            return new JSONObject( json );
        }

        private void fail( final String reason ) {
            if( loginFailedCallback != null ) {
                handler.post( new Runnable() {
                    @Override
                    public void run() {
                        loginFailedCallback.loginFailed( reason );
                    }
                } );
            }
        }
    }

    /**
     * Simple callback interface to handle login failure asynchronously
     */
    public interface LoginFailedCallback {
        /**
         * Called if login was unsuccessful. Always invoked on the main UI thread.
         *
         * @param reason Human readable message describing the failure.
         */
        public void loginFailed( String reason );
    }
}
