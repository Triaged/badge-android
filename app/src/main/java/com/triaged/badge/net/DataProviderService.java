package com.triaged.badge.net;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.app.App;
import com.triaged.badge.database.DatabaseHelper;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.DiskLruCache;
import com.triaged.badge.receivers.GCMReceiver;
import com.triaged.badge.receivers.LogoutReceiver;
import com.triaged.badge.ui.entrance.LoginActivity;

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
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public static final String MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY = "latestMsgTimestampPrefsKey";

    public static final String DB_UPDATED_ACTION = "com.triage.badge.DB_UPDATED";
    public static final String MSGS_UPDATED_ACTION = "com.triage.badge.MSGS_UPDATED";
    public static final String DB_AVAILABLE_ACTION = "com.triage.badge.DB_AVAILABLE";
    public static final String LOGGED_OUT_ACTION = "com.triage.badge.LOGGED_OUT";
    public static final String NEW_MSG_ACTION = "com.triage.badge.NEW_MSG";
    public static final String MSG_STATUS_CHANGED_ACTION = "com.triage.badge.MSG_STATUS_CHANGED";

    public static final String THREAD_ID_EXTRA = "threadId";
    public static final String MESSAGE_ID_EXTRA = "messageId";
    public static final String MESSAGE_BODY_EXTRA = "messageBody";
    public static final String MESSAGE_FROM_EXTRA = "messageFrom";
    public static final String IS_INCOMING_MSG_EXTRA = "isIncomingMessage";

    public static final int MSG_STATUS_PENDING = 0;
    public static final int MSG_STATUS_ACKNOWLEDGED = 1;
    public static final int MSG_STATUS_FAILED = 2;

    protected static final String QUERY_ALL_CONTACTS_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s ORDER BY contact.%s;",
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME
            );

    protected static final String QUERY_DEPARTMENT_CONTACTS_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department" +
                            " ON contact.%s = department.%s WHERE contact.%s = ? AND contact.%s = 0 ORDER BY contact.%s;",
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    ContactsTable.COLUMN_CONTACT_IS_ARCHIVED,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME
            );

    protected static final String QUERY_CONTACT_SQL =
            String.format("SELECT contact.*, office.%s %s, department.%s %s, manager.%s %s, manager.%s %s FROM" +
                            " %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s LEFT OUTER JOIN %s manager" +
                            " ON contact.%s = manager.%s LEFT OUTER JOIN %s office ON contact.%s = office.%s  WHERE contact.%s = ?",
                    OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME,
                    DatabaseHelper.JOINED_OFFICE_NAME,
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME,
                    DatabaseHelper.JOINED_MANAGER_FIRST_NAME,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME,
                    DatabaseHelper.JOINED_MANAGER_LAST_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_MANAGER_ID,
                    ContactsTable.COLUMN_ID,
                    OfficeLocationsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID,
                    OfficeLocationsTable.COLUMN_ID,
                    ContactsTable.COLUMN_ID
            );
    protected static final String QUERY_CONTACTS_WITH_EXCEPTION_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department" +
                            " ON contact.%s = department.%s WHERE contact.%s != ? AND contact.is_archived = 0  ORDER BY %s;",
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.COLUMN_ID,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME
            );
    protected static final String QUERY_MANAGED_CONTACTS_SQL =
            String.format("SELECT contact.*, department.%s %s FROM %s contact LEFT OUTER JOIN %s department" +
                            " ON  contact.%s = department.%s WHERE contact.%s = ? ORDER BY %s;",
                    DepartmentsTable.COLUMN_DEPARTMENT_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.COLUMN_CONTACT_MANAGER_ID,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME
            );
    protected static final String QUERY_THREADS_SQL =
            String.format("SELECT * from %s WHERE %s = 1 order by %s DESC",
                    MessagesTable.TABLE_NAME,
                    MessagesTable.COLUMN_MESSAGES_THREAD_HEAD,
                    MessagesTable.COLUMN_MESSAGES_TIMESTAMP
            );
    protected static final String QUERY_MESSAGES_SQL =
            String.format("SELECT message.*, contact.%s, contact.%s, contact.%s from %s message LEFT OUTER JOIN %s contact" +
                            " ON message.%s = contact.%s WHERE message.%s = ? order by message.%s ASC",
                    ContactsTable.COLUMN_CONTACT_AVATAR_URL,
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME,
                    ContactsTable.COLUMN_CONTACT_LAST_NAME,
                    MessagesTable.TABLE_NAME,
                    ContactsTable.TABLE_NAME,
                    MessagesTable.COLUMN_MESSAGES_FROM_ID,
                    ContactsTable.COLUMN_ID,
                    MessagesTable.COLUMN_MESSAGES_THREAD_ID,
                    MessagesTable.COLUMN_MESSAGES_TIMESTAMP
            );


    protected static final String QUERY_MESSAGE_SQL =
            String.format("SELECT * FROM %s WHERE %s = ? OR %s = ?",
                    MessagesTable.TABLE_NAME,
                    MessagesTable.COLUMN_MESSAGES_ID,
                    MessagesTable.COLUMN_MESSAGES_GUID);

    protected static final String QUERY_ALL_DEPARTMENTS_SQL = String.format(
            "SELECT * FROM %s WHERE %s > ? ORDER BY %s;",
            DepartmentsTable.TABLE_NAME,
            DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS,
            DepartmentsTable.COLUMN_DEPARTMENT_NAME);

    protected static final String CLEAR_DEPARTMENTS_SQL = String.format("DELETE FROM %s;", DepartmentsTable.TABLE_NAME);
    protected static final String CLEAR_CONTACTS_SQL = String.format("DELETE FROM %s;", ContactsTable.TABLE_NAME);
    protected static final String CLEAR_OFFICE_LOCATIONS_SQL = String.format("DELETE FROM %s;", OfficeLocationsTable.TABLE_NAME);
    protected static final String CLEAR_MESSAGES_SQL = String.format("DELETE FROM %s;", MessagesTable.TABLE_NAME);
    protected static final String QUERY_ALL_OFFICES_SQL = String.format("SELECT *  FROM %s ORDER BY %s;", OfficeLocationsTable.TABLE_NAME, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME);
    protected static final String QUERY_OFFICE_LOCATION_SQL = String.format("SELECT %s FROM %s WHERE %s = ?", OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME, OfficeLocationsTable.TABLE_NAME, OfficeLocationsTable.COLUMN_ID);

    protected static final String LAST_SYNCED_PREFS_KEY = "lastSyncedOn";
    public static final String API_TOKEN_PREFS_KEY = "apiToken";
    protected static final String LOGGED_IN_USER_ID_PREFS_KEY = "loggedInUserId";
    protected static final String INSTALLED_VERSION_PREFS_KEY = "installedAppVersion";

    protected static final String[] EMPTY_STRING_ARRAY = new String[]{};
    protected static final String[] DEPTS_WITH_CONTACTS_SQL_ARGS = new String[]{"0"};
    protected static final String[] ALL_DEPTS_SQL_ARGS = new String[]{"-1"};


    protected static final String SERVICE_ANDROID = "android";


    protected volatile Contact loggedInUser;
    protected ScheduledExecutorService sqlThread;
    protected DatabaseHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected ApiClient apiClient;
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

    private BroadcastReceiver syncGCMReceiver;

    public static void logCursor(Cursor c) {
        while (c.moveToNext()) {
            Log.d(LOG_TAG, "-------------------------------------------------------------------");
            for (String column : c.getColumnNames()) {
                Log.d(LOG_TAG, String.format("%s: %s", column, c.getString(c.getColumnIndex(column))));
            }
            Log.d(LOG_TAG, "-------------------------------------------------------------------");
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                try {
                    File cacheDir = params[0];
                    mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                    mDiskCacheStarting = false; // Finished initialization
                    mDiskCacheLock.notifyAll(); // Wake any waiting threads
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Couldn't open disk image cache.", e);
                }
            }
            return null;
        }
    }


    /**
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

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        String apiToken = prefs.getString(API_TOKEN_PREFS_KEY, "");
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadScheduledExecutor();
        databaseHelper = new DatabaseHelper(this);
        apiClient = new ApiClient(apiToken);
        contactList = new ArrayList(250);
        handler = new Handler();
        localBinding = new LocalBinding();
        mimeTypeMap = MimeTypeMap.getSingleton();

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        int cacheSize = maxMemory / 8;
        thumbCache = new LruCache<String, Bitmap>(cacheSize) {
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

        syncGCMReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // PARSE INTENT
                final String syncType = intent.getStringExtra(GCMReceiver.SYNC_GCM_DATA_TYPE_KEY);
                int syncId = intent.getIntExtra(GCMReceiver.SYNC_GCM_DATA_ID_KEY, -1);

                if (syncType.equals("user")) {
                    // Request contact from server, add it to the db.
                    getSingleContact(syncId);
                } else if (syncType.equals("office_location")) {
                    // Request office from server, add it to the db.
                    getSingleOffice(syncId);
                } else if (syncType.equals("department")) {
                    // Request department from server, add it to the db.
                    getSingleDepartment(syncId);
                }
            }
        };

        IntentFilter syncActionFilter = new IntentFilter(GCMReceiver.SYNC_GCM_RECEIVED);
        syncActionFilter.addAction(GCMReceiver.SYNC_GCM_RECEIVED);
        localBroadcastManager.registerReceiver(syncGCMReceiver, syncActionFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(syncGCMReceiver);
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.getConnectionManager().shutdown();
        httpClient.getConnectionManager().shutdown();
        if (!mDiskCacheStarting && mDiskLruCache != null) {
            try {
                mDiskLruCache.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "IOException closing disk cache", e);
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
        prefs.edit().putLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0).commit();
    }

    /**
     * Get a single contact from the server and add it to the db.
     * <p/>
     * Only used when called a GCM message is received instructing the client to update itself.
     */
    protected void getSingleContact(final int syncId) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = apiClient.getContact(syncId);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        ContentValues values = new ContentValues();
                        JSONObject user = parseJSONResponse(response.getEntity());
                        setContactDBValesFromJSON(user, values);
                        Contact newContact = getContact(user.getInt("id"));
                        if (newContact == null) {
                            database.insert(ContactsTable.TABLE_NAME, null, values);
                        } else {
                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{user.getString("id")});
                        }
                        values.clear();
                    }
                } catch (IOException e) {
                    App.gLogger.e("Network issue getting single contact");
                } catch (JSONException e) {
                    App.gLogger.e("Couldn't understand response from Badge server", e);
                }
            }
        });
    }

    protected void getSingleOffice(final int syncId) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = apiClient.getOffice(syncId);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        ContentValues values = new ContentValues();
                        JSONObject location = parseJSONResponse(response.getEntity());
                        setOfficeLocationDBValuesFromJSON(location, values);
                        database.insert(OfficeLocationsTable.TABLE_NAME, null, values);
                        values.clear();

                    }
                } catch (IOException e) {
                    App.gLogger.e("Network issue getting single office");
                } catch (JSONException e) {
                    App.gLogger.e("Couldn't understand response from Badge server", e);
                }
            }
        });
    }

    protected void getSingleDepartment(final int syncId) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = apiClient.getDepartment(syncId);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        ContentValues values = new ContentValues();
                        JSONObject dept = parseJSONResponse(response.getEntity());
                        setDepartmentBValuesFromJSON(dept, values);
                        database.insert(DepartmentsTable.TABLE_NAME, null, values);
                        values.clear();
                    }
                } catch (IOException e) {
                    App.gLogger.e("Network issue getting single department");
                } catch (JSONException e) {
                    App.gLogger.e("Couldn't understand response from Badge server", e);
                }
            }
        });
    }

    /**
     * Partial sync of contacts, only download/save contacts that have changed
     * since the last company sync.
     * <p/>
     * Used when app foregrounds.
     */
    protected void partialSyncContactsAsync() {
        // Sanity check, if we've synced within 2 minutes, don't bother.
        if (lastSynced > System.currentTimeMillis() - 120000 || database == null) {
            return;
        }

        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    long previousSync = lastSynced;
                    lastSynced = System.currentTimeMillis();
                    prefs.edit().putLong(LAST_SYNCED_PREFS_KEY, lastSynced).commit();

                    HttpResponse response = apiClient.downloadCompanyRequest(previousSync - 60000 /* one minute of buffer */);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        JSONArray companyArr = parseJSONArrayResponse(response.getEntity());
                        JSONObject companyObj = companyArr.getJSONObject(0);
                        JSONArray contactsArr = companyObj.getJSONArray("users");
                        ContentValues values = new ContentValues();
                        int contactsLength = contactsArr.length();
                        for (int i = 0; i < contactsLength; i++) {
                            JSONObject newContact = contactsArr.getJSONObject(i);
                            setContactDBValesFromJSON(newContact, values);
                            Contact c = getContact(newContact.getInt("id"));
                            if (c == null) {
                                database.insert(ContactsTable.TABLE_NAME, null, values);
                            } else {
                                database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{newContact.getString("id")});
                            }
                            values.clear();
                        }

                        localBroadcastManager.sendBroadcast(new Intent(DB_UPDATED_ACTION));
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                    }
                } catch (IOException e) {
                    App.gLogger.e("Network issue doing partial contact sync. ");
                } catch (JSONException e) {
                    App.gLogger.e("Couldn't understand response from Badge server", e);
                }
            }
        });
    }

    /**
     * Syncs company info from the cloud to the device.
     * <p/>
     * Notifies listeners via local broadcast that data has been updated with the {@link #DB_UPDATED_ACTION}
     *
     * @param db
     */
    protected void syncCompany(SQLiteDatabase db) {
        ConnectivityManager cMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cMgr.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            // TODO listen for network becoming available so we can sync then.

            return;
        }

        boolean updated = false;
        lastSynced = System.currentTimeMillis();
        prefs.edit().putLong(LAST_SYNCED_PREFS_KEY, lastSynced).commit();
        try {
            db.beginTransaction();
            HttpResponse response = apiClient.downloadCompanyRequest(0 /* Get all contacts */);
            ensureNotUnauthorized(response);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    db.execSQL(CLEAR_CONTACTS_SQL);
                    db.execSQL(CLEAR_DEPARTMENTS_SQL);
                    db.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);

                    JSONObject companyObj = parseJSONResponse(response.getEntity());
                    ContentValues values = new ContentValues();

                    JSONArray contactsArr = companyObj.getJSONArray("users");
                    int contactsLength = contactsArr.length();
                    for (int i = 0; i < contactsLength; i++) {
                        JSONObject newContact = contactsArr.getJSONObject(i);
                        setContactDBValesFromJSON(newContact, values);
                        db.insert(ContactsTable.TABLE_NAME, null, values);
                        values.clear();
                    }

                    if (companyObj.has("uses_departments") && companyObj.getBoolean("uses_departments")) {
                        JSONArray deptsArr = companyObj.getJSONArray("departments");
                        int deptsLength = deptsArr.length();
                        for (int i = 0; i < deptsLength; i++) {
                            JSONObject dept = deptsArr.getJSONObject(i);
                            setDepartmentBValuesFromJSON(dept, values);
                            db.insert(DepartmentsTable.TABLE_NAME, null, values);
                            values.clear();
                        }
                    }

                    if (companyObj.has("office_locations")) {
                        JSONArray locations = companyObj.getJSONArray("office_locations");
                        int locationsLength = locations.length();
                        for (int i = 0; i < locationsLength; i++) {
                            JSONObject location = locations.getJSONObject(i);
                            setOfficeLocationDBValuesFromJSON(location, values);
                            db.insert(OfficeLocationsTable.TABLE_NAME, null, values);
                            values.clear();
                        }
                    }

                    loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));

                } else {
                    App.gLogger.e("Got status " + statusCode + " from API. Handle this appropriately!");
                }
            } finally {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
            }

            db.setTransactionSuccessful();
            updated = true;
        } catch (IOException e) {
            App.gLogger.e("IO exception downloading company that should be handled more softly than this.", e);
        } catch (JSONException e) {
            App.gLogger.e("JSON from server not formatted correctly. Either we shouldn't have expected JSON or this is an api bug.", e);
        } finally {
            db.endTransaction();
        }
        if (updated && initialized) {
            localBroadcastManager.sendBroadcast(new Intent(DB_UPDATED_ACTION));
        }
    }


    private void setContactDBValesFromJSON(JSONObject json, ContentValues values) throws JSONException {
        values.put(ContactsTable.COLUMN_ID, json.getInt("id"));
        setStringContentValueFromJSONUnlessNull(json, "last_name", values, ContactsTable.COLUMN_CONTACT_LAST_NAME);
        setStringContentValueFromJSONUnlessNull(json, "first_name", values, ContactsTable.COLUMN_CONTACT_FIRST_NAME);
        setStringContentValueFromJSONUnlessNull(json, "avatar_face_url", values, ContactsTable.COLUMN_CONTACT_AVATAR_URL);
        setStringContentValueFromJSONUnlessNull(json, "email", values, ContactsTable.COLUMN_CONTACT_EMAIL);
        setIntContentValueFromJSONUnlessBlank(json, "manager_id", values, ContactsTable.COLUMN_CONTACT_MANAGER_ID);
        setIntContentValueFromJSONUnlessBlank(json, "primary_office_location_id", values, ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID);
        setIntContentValueFromJSONUnlessBlank(json, "current_office_location_id", values, ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID);
        setIntContentValueFromJSONUnlessBlank(json, "department_id", values, ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID);
        setBooleanContentValueFromJSONUnlessBlank(json, "archived", values, ContactsTable.COLUMN_CONTACT_IS_ARCHIVED);

        if (json.has("sharing_office_location") && !json.isNull("sharing_office_location")) {
            int sharingInt = json.getBoolean("sharing_office_location") ? Contact.SHARING_LOCATION_TRUE : Contact.SHARING_LOCATION_FALSE;
            values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, sharingInt);
        } else {
            values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, Contact.SHARING_LOCATION_UNAVAILABLE);
        }
        if (json.has("employee_info")) {
            JSONObject employeeInfo = json.getJSONObject("employee_info");
            setStringContentValueFromJSONUnlessNull(employeeInfo, "job_title", values, ContactsTable.COLUMN_CONTACT_JOB_TITLE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "job_start_date", values, ContactsTable.COLUMN_CONTACT_START_DATE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "birth_date", values, ContactsTable.COLUMN_CONTACT_BIRTH_DATE);
            // This comes in as iso 8601 GMT date.. but we save "August 1" or whatever
            String birthDateStr = values.getAsString(ContactsTable.COLUMN_CONTACT_BIRTH_DATE);
            if (birthDateStr != null) {
                values.put(ContactsTable.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthDateString(birthDateStr));
            }
            String startDateStr = values.getAsString(ContactsTable.COLUMN_CONTACT_START_DATE);
            if (startDateStr != null) {
                values.put(ContactsTable.COLUMN_CONTACT_START_DATE, Contact.convertStartDateString(startDateStr));
            }
            setStringContentValueFromJSONUnlessNull(employeeInfo, "cell_phone", values, ContactsTable.COLUMN_CONTACT_CELL_PHONE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "office_phone", values, ContactsTable.COLUMN_CONTACT_OFFICE_PHONE);
        }
    }

    private void setOfficeLocationDBValuesFromJSON(JSONObject json, ContentValues values) throws JSONException {
        values.put(OfficeLocationsTable.COLUMN_ID, json.getInt("id"));
        setStringContentValueFromJSONUnlessNull(json, "name", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME);
        setStringContentValueFromJSONUnlessNull(json, "street_address", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ADDRESS);
        setStringContentValueFromJSONUnlessNull(json, "city", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_CITY);
        setStringContentValueFromJSONUnlessNull(json, "state", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_STATE);
        setStringContentValueFromJSONUnlessNull(json, "zip_code", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ZIP);
        setStringContentValueFromJSONUnlessNull(json, "country", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_COUNTRY);
        setStringContentValueFromJSONUnlessNull(json, "latitude", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LAT);
        setStringContentValueFromJSONUnlessNull(json, "longitude", values, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LNG);
    }


    private void setDepartmentBValuesFromJSON(JSONObject json, ContentValues values) throws JSONException {
        values.put(DepartmentsTable.COLUMN_ID, json.getInt("id"));
        values.put(DepartmentsTable.COLUMN_DEPARTMENT_NAME, json.getString("name"));
        values.put(DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS, json.getString("users_count"));
    }

    /**
     * Pulls an int from json and sets it as a content value if the key exists and is not a null literal.
     *
     * @param json   json object possibly containing key
     * @param key    key in to json object where value should live
     * @param values database values to add to if the key exists
     * @param column column name to set in database values
     * @throws JSONException
     */
    private static void setStringContentValueFromJSONUnlessNull(JSONObject json, String key, ContentValues values, String column) throws JSONException {
        if (!json.isNull(key)) {
            values.put(column, json.getString(key));
        }
    }

    /**
     * Pulls an int from json and sets it as a content value if the key exists and is not the empty string.
     *
     * @param json   json object possibly containing key
     * @param key    key in to json object where value should live
     * @param values database values to add to if the key exists
     * @param column column name to set in database values
     * @throws JSONException
     */
    private static void setIntContentValueFromJSONUnlessBlank(JSONObject json, String key, ContentValues values, String column) throws JSONException {
        if (!json.isNull(key) && !"".equals(json.getString(key))) {
            values.put(column, json.getInt(key));
        }
    }


    /**
     * Pulls an int from json and sets it as a content value if the key exists and is not the empty string.
     *
     * @param json   json object possibly containing key
     * @param key    key in to json object where value should live
     * @param values database values to add to if the key exists
     * @param column column name to set in database values
     * @throws JSONException
     */
    private static void setBooleanContentValueFromJSONUnlessBlank(JSONObject json, String key, ContentValues values, String column) throws JSONException {
        if (!json.isNull(key) && !"".equals(json.getString(key))) {
            values.put(column, json.getBoolean(key));
        }
    }

    /**
     * Return a cursor to a set of contact rows that the given id manages.
     * All columns included.
     * <p/>
     * Caller must close the Cursor when no longer needed.
     *
     * @param contactId manager id
     * @return db cursor
     */
    protected Cursor getContactsManaged(int contactId) {
        if (database != null) {
            return database.rawQuery(QUERY_MANAGED_CONTACTS_SQL, new String[]{String.valueOf(contactId)});
        }
        throw new IllegalStateException("getContactsManaged() called before database available.");
    }

    /**
     * Return only contacts in a given department. Same fields returned as {@link #getContactsCursor()}
     *
     * @param departmentId
     * @return
     */
    protected Cursor getContactsByDepartmentCursor(int departmentId) {
        if (database != null) {
            return database.rawQuery(QUERY_DEPARTMENT_CONTACTS_SQL, new String[]{String.valueOf(departmentId)});
        }
        throw new IllegalStateException("getContactsByDepartmentCursor() called before database available.");
    }

    /**
     * Query the db to get a contact given an id. Always returns the
     * latest and greatest local device data.
     *
     * @param contactId, an integer
     * @return a Contact
     */
    protected Contact getContact(int contactId) {
        if (database != null) {
            Cursor cursor = database.rawQuery(QUERY_CONTACT_SQL, new String[]{String.valueOf(contactId)});
            try {
                if (cursor.moveToFirst()) {
                    Contact contact = new Contact();
                    contact.fromCursor(cursor);
                    return contact;
                }
                return null;
            } finally {
                cursor.close();
            }
        }
        throw new IllegalStateException("getContact() called before database available.");
    }

    /**
     * Draws the contact's thumb as a bitmap in to the specified image view.
     * <p/>
     * First, a small in memory cache is consulted to see if the bitmap is available, and if
     * so, the bitmap is synchronously drawn in to the image view.
     * <p/>
     * If not, a much larger disk cache is consulted asynchronously, and if it is, the bitmap is decoded
     * and stored in the memory cache before being drawn back on the main thread.
     * <p/>
     * If not in the disk cache, as a last resort, the image is downloaded in the BG and
     * placed in to the disk and memory caches.
     * <p/>
     * If a placeholder view is specified, it will be hidden
     *
     * @param avatarUrl       url of avatar img
     * @param thumbImageView  the view to set the image on.
     * @param placeholderView null or a view that should be hidden once the image has been set.
     */
    protected void setSmallContactImage(String avatarUrl, View thumbImageView, View placeholderView) {
        if (avatarUrl == null) {
            return;
        }
        Bitmap b = thumbCache.get(avatarUrl);
        if (b != null) {
            // Hooray!
            assignBitmapToView(b, thumbImageView);
            if (placeholderView != null) {
                placeholderView.setVisibility(View.GONE);
            }
        } else {
            new LoadImageAsyncTask(avatarUrl, thumbImageView, placeholderView, thumbCache).execute();
        }
    }

    /**
     * Downloads the image each time it's called and sets the bitmap resource on the imageview.
     * <p/>
     * TODO This should use the same disk cache as the small contact image once that's implemented.
     *
     * @param c         contact
     * @param imageView
     */
    protected void setLargeContactImage(Contact c, ImageView imageView) {
        new LoadImageAsyncTask(c.avatarUrl, imageView, null).execute();
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
        throw new IllegalStateException("getContactsCursor() called before database available.");
    }

    /**
     * Query the db to get a cursor to all contacts except for
     * the logged in user.
     * <p/>
     * Caller must close the cursor when finished.
     *
     * @return a cursor to all contact rows minus 1
     */
    protected Cursor getContactsCursorExcludingLoggedInUser() {
        if (database != null) {
            return database.rawQuery(QUERY_CONTACTS_WITH_EXCEPTION_SQL, new String[]{String.valueOf(loggedInUser.id)});
        }
        throw new IllegalStateException("getContactsCursorExcludingLoggedInUser() called before database available.");
    }

    /**
     * Query the db to get a cursor to the full list of departments
     *
     * @return a cursor to all dept rows
     */
    protected Cursor getDepartmentCursor(boolean onlyThoseWithContacts) {
        if (database != null) {
            String[] args = onlyThoseWithContacts ? DEPTS_WITH_CONTACTS_SQL_ARGS : ALL_DEPTS_SQL_ARGS;
            return database.rawQuery(QUERY_ALL_DEPARTMENTS_SQL, args);
        }
        throw new IllegalStateException("getDepartmentCursor() called before database available.");
    }

    /**
     * @return cursor to all office location rows
     */
    protected Cursor getOfficeLocationsCursor() {
        if (database != null) {
            return database.rawQuery(QUERY_ALL_OFFICES_SQL, EMPTY_STRING_ARRAY);
        }
        throw new IllegalStateException("getOfficeLocationsCursor() called before database available.");
    }


    /**
     * @param locationId id of the office
     * @return Name of the office.
     */
    private String getOfficeLocationName(int locationId) {
        if (database != null) {
            Cursor cursor = database.rawQuery(QUERY_OFFICE_LOCATION_SQL, new String[]{String.valueOf(locationId)});
            if (cursor.moveToFirst()) {
                String name = Contact.getStringSafelyFromCursor(cursor, OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME);
                cursor.close();
                return name;
            } else {
                return null;
            }
        }
        throw new IllegalStateException("getOfficeLocationName() called before database available.");
    }

    /**
     * Helper that sets a bitmap to a plain ole {@link android.widget.ImageView}
     *
     * @param b
     * @param v
     */
    protected void assignBitmapToView(Bitmap b, View v) {
        if (v instanceof ImageView) {
            ((ImageView) v).setImageBitmap(b);
        }
    }

    /**
     * When the service detects that there is no active user
     * or api token, it calls this function.
     * <p/>
     * This launches the login activity in a new task and sends a local
     * broadcast so that activities can listen and kill themselves.
     * <p/>
     * This should only be called on the sql thread.
     */
    protected void loggedOut(boolean restartApplication) {

        Intent logoutIntent = new Intent(LogoutReceiver.ACTION_LOGOUT);
        logoutIntent.putExtra(LogoutReceiver.RESTART_APP_EXTRA, restartApplication);
        DataProviderService.this.sendBroadcast(logoutIntent);


//        if (loggedInUser != null && loggedInUser.currentOfficeLocationId > 0 && !"".equals(apiClient.apiToken)) {
//            // User initiated logout, make sure they don't get "stuck"
//            checkOutOfOfficeSynchronously(loggedInUser.currentOfficeLocationId);
//        }
//        loggedInUser = null;
//        prefs.edit().clear().commit();
//
//        // Stop tracking location
//        LocationTrackingService.clearAlarm(localBinding, this);
//
//        // Wipe DB, we're not logged in anymore.
//        database.execSQL(CLEAR_CONTACTS_SQL);
//        database.execSQL(CLEAR_DEPARTMENTS_SQL);
//        database.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);
//        database.execSQL(CLEAR_MESSAGES_SQL);
//        notifyUILoggedOut();
//
//
//        // Stop the messaging service.
//        Intent fayeIntent = new Intent(this, FayeService.class);
//        stopService(fayeIntent);
//
//
//        // Report the event to mixpanel
//        MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(DataProviderService.this, App.MIXPANEL_TOKEN);
//        mixpanelAPI.clearSuperProperties();
//        apiClient.apiToken = "";
    }

    private void notifyUILoggedOut() {
        // Start the login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Tell other activities to close.
        localBroadcastManager.sendBroadcast(new Intent(LOGGED_OUT_ACTION));
    }

    protected void changePassword(final String currentPassword, final String newPassword, final String newPasswordConfirmation, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                JSONObject postBody = new JSONObject();
                try {
                    JSONObject user = new JSONObject();
                    postBody.put("user", user);
                    user.put("current_password", currentPassword);
                    user.put("password", newPassword);
                    user.put("password_confirmation", newPasswordConfirmation);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for change password", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.changePasswordRequest(postBody);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (response.getEntity() != null) {
                        response.getEntity().consumeContent();
                    }
                    if (statusCode == HttpStatus.SC_OK) {
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        fail("Got unexpected response from server. Please contact Badge HQ.", saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue changing password, please check your connection and try again.", saveCallback);
                }

            }
        });
    }

    protected void requestResetPassword(final String email, final AsyncSaveCallback saveCallback) {
        JSONObject postBody = new JSONObject();
        try {
            JSONObject user = new JSONObject();
            postBody.put("email", email);
        } catch (JSONException e) {
            App.gLogger.e("JSON exception creating post body for forgot password", e);
            fail("Unexpected issue, please contact Badge HQ", saveCallback);
            return;
        }

        try {
            HttpResponse response = apiClient.requestResetPasswordRequest(postBody);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
            if (statusCode == HttpStatus.SC_OK) {
                if (saveCallback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            saveCallback.saveSuccess(-1);
                        }
                    });
                }
            } else {
                fail("Got unexpected response from server. Please contact Badge HQ.", saveCallback);
            }
        } catch (IOException e) {
            fail("There was a network issue requesting to reset password, please check your connection and try again.", saveCallback);
        }
    }

    /**
     * This method is for when a user elects to log out. It DELETES /devices/:id/sign_out
     * and on success wipes local data on the phone and removes tokens.
     */
    protected void unregisterDevice() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DataProviderService.this);
                    int deviceId = prefs.getInt(REGISTERED_DEVICE_ID_PREFS_KEY, -1);
                    if (deviceId != -1) {
                        HttpResponse response = apiClient.unregisterDeviceRequest(deviceId);
                        ensureNotUnauthorized(response);
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }

                    }
                } catch (IOException e) {
                    App.gLogger.e("Wasn't able to delete device on api", e);
                } finally {
                    // Do this regardless of whether we can communicate with the cloud or not.
                    loggedOut(false);
                }
            }
        });
    }

    /**
     * Posts to /devices to register upon login.
     *
     * @param pushToken GCM device registration
     */
    protected void registerDevice(final String pushToken) {

        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                int androidVersion = android.os.Build.VERSION.SDK_INT;

                JSONObject postData = new JSONObject();
                JSONObject deviceData = new JSONObject();
                try {
                    postData.put("device", deviceData);
                    deviceData.put("token", pushToken);
                    deviceData.put("os_version", androidVersion);
                    deviceData.put("service", SERVICE_ANDROID);
                    deviceData.put("application_id", Settings.Secure.getString(DataProviderService.this.getContentResolver(),
                            Settings.Secure.ANDROID_ID));
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for device registration", e);
                    return;
                }

                try {
                    HttpResponse response = apiClient.registerDeviceRequest(postData);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                        // Get new department id
                        JSONObject newDevice = parseJSONResponse(response.getEntity());

                        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(DataProviderService.this).edit();
                        prefsEditor.putInt(REGISTERED_DEVICE_ID_PREFS_KEY, newDevice.getInt("id"));
                        prefsEditor.commit();
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                    }
                } catch (IOException e) {
                    // We'll try again next time the app starts.
                    App.gLogger.e("IOException trying to register device with badge HQ", e);
                } catch (JSONException e) {
                    App.gLogger.e("Response from Badge HQ wasn't parseable, sad panda", e);
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
     * @param loginCallback if non null, {@link DataProviderService.LoginCallback#loginFailed(String)} on this obj will be called on auth failure.
     */
    protected void loginAsync(final String email, final String password, final LoginCallback loginCallback) {
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
                    App.gLogger.e("JSON exception creating post body for login", e);
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
                            App.gLogger.e("JSON exception parsing error response from 401.", e);
                            fail("Login failed.");
                        }
                    } else if (statusCode == HttpStatus.SC_OK) {
                        try {
                            JSONObject account = parseJSONResponse(response.getEntity());
                            apiClient.apiToken = account.getString("authentication_token");
                            loggedInUser = new Contact();
                            loggedInUser.fromJSON(account.getJSONObject("current_user"));
                            prefs.edit().putInt(COMPANY_ID_PREFS_KEY, account.getInt("company_id")).
                                    putString(API_TOKEN_PREFS_KEY, apiClient.apiToken).
                                    putString(COMPANY_NAME_PREFS_KEY, account.getString("company_name")).
                                    putInt(LOGGED_IN_USER_ID_PREFS_KEY, loggedInUser.id).commit();

                            JSONObject props = constructMixpanelSuperProperties();
                            MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(DataProviderService.this, App.MIXPANEL_TOKEN);
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
                            syncMessagesSync();
                            startService(new Intent(DataProviderService.this, FayeService.class));
                        } catch (JSONException e) {
                            App.gLogger.e("JSON exception parsing login success.", e);
                            fail("Credentials were OK, but the response couldn't be understood. Please notify Badge HQ.");
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        App.gLogger.e("Unexpected http response code " + statusCode + " from api.");
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
    protected boolean ensureNotUnauthorized(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            loggedOut(true);
            return false;
        }
        return true;
    }

    /**
     * Updates a contact's info via the api and broadcasts
     * {@link #DB_UPDATED_ACTION} locally if successful.
     *
     * @param contactId
     */
    protected void refreshContact(final int contactId) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = apiClient.getContact(contactId);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == HttpStatus.SC_OK) {
                        try {
                            JSONObject contact = parseJSONResponse(response.getEntity());
                            ContentValues values = new ContentValues();
                            setContactDBValesFromJSON(contact, values);
                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(contactId)});
                            localBroadcastManager.sendBroadcast(new Intent(DB_UPDATED_ACTION));
                        } catch (JSONException e) {
                            Log.w(LOG_TAG, "Couldn't refresh contact due to malformed or unexpected JSON response.", e);
                        }
                    } else if (response.getEntity() != null) {
                        Log.w(LOG_TAG, "Response from /users/id was " + response.getStatusLine().getReasonPhrase());
                        response.getEntity().consumeContent();
                    }

                } catch (IOException e) {
                    Log.w(LOG_TAG, "Couldn't refresh contact due to network issue.");
                }
            }
        });
    }

    /**
     * Changes current user's office to this office id, when location svcs determine
     * they are there. If a current office is already set, does nothing.
     *
     * @param officeId
     */
    protected void checkInToOffice(final int officeId) {
        if (loggedInUser.currentOfficeLocationId <= 0) {
            sqlThread.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        HttpResponse response = apiClient.checkinRequest(officeId);
                        //ensureNotUnauthorized( response );
                        int status = response.getStatusLine().getStatusCode();
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        if (status == HttpStatus.SC_OK) {
                            ContentValues values = new ContentValues();
                            values.put(ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, officeId);
                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                            loggedInUser.currentOfficeLocationId = officeId;
                        } else {
                            Log.w(LOG_TAG, "Server responded with " + status + " trying to check out of location.");
                        }
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Couldn't check out in to office due to IOException " + officeId);
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
                    checkOutOfOfficeSynchronously(officeId);
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
    protected void checkOutOfOfficeSynchronously(int officeId) {
        try {
            HttpResponse response = apiClient.checkoutRequest(officeId);
            //ensureNotUnauthorized(response);
            int status = response.getStatusLine().getStatusCode();
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
            if (status == HttpStatus.SC_OK) {
                ContentValues values = new ContentValues();
                values.put(ContactsTable.COLUMN_CONTACT_CURRENT_OFFICE_LOCATION_ID, -1);
                database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
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
     * <p/>
     * Notifies listeners via the {@link #DB_AVAILABLE_ACTION} when the database is ready for use.
     */
    protected void initDatabase() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    database = databaseHelper.getWritableDatabase();

                    int loggedInContactId = prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1);
                    if (loggedInContactId > 0) {
                        // If we're logged in, and the database isn't empty, the UI should be ready to go.
                        if ((loggedInUser = getContact(loggedInContactId)) != null) {
                            initialized = true;
                            localBroadcastManager.sendBroadcast(new Intent(DB_AVAILABLE_ACTION));
                        }
                    } else {
                        // If there's no logged in user, nothing else will happen so we're done here.
                        initialized = true;
                        localBroadcastManager.sendBroadcast(new Intent(DB_AVAILABLE_ACTION));
                    }

                    // If there's a logged in user, sync the whole company.
                    if (!apiClient.apiToken.isEmpty()) {
                        syncCompany(database);

                        // Edge case avoided here, user was unauthorized
                        // and now db is empty, this is null.
                        if (loggedInUser != null) {
                            syncMessagesSync();

                        }

                        // If we had to sync the company first (it was dropped
                        // due to DB upgrade or whatever) noooooow we can
                        // let the UI know we're initialized.
                        if (!initialized) {
                            initialized = true;
                            localBroadcastManager.sendBroadcast(new Intent(DB_AVAILABLE_ACTION));
                        }


                        // Check if this is the first boot of a new install
                        // If it is, since we're logged in, if the user hasnt
                        // disabled location tracking, start the tracking service.
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            if (pInfo.versionCode > prefs.getInt(INSTALLED_VERSION_PREFS_KEY, -1)) {
                                prefs.edit().putInt(INSTALLED_VERSION_PREFS_KEY, pInfo.versionCode).commit();
                                if (prefs.getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true)) {
                                    // startService( new Intent( getApplicationContext(), LocationTrackingService.class ) );
                                    saveSharingLocationAsync(true, new DataProviderService.AsyncSaveCallback() {
                                        @Override
                                        public void saveSuccess(int newId) {
                                            prefs.edit().putBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true).commit();
                                            LocationTrackingService.scheduleAlarm(DataProviderService.this);
                                        }

                                        @Override
                                        public void saveFailed(String reason) {
                                            Log.d(LOG_TAG, "Initial attempt to save sharing location pref failed due to: " + reason);
                                        }
                                    });

                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            // Look at all the fucks I give!
                        }

                    } else {
//                        notifyUILoggedOut();
                    }
                } catch (Throwable t) {
                    App.gLogger.e("UNABLE TO GET DATABASE", t);
                }
            }
        });
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
     * @param saveCallback    null or a callback that will be invoked on the main thread on success or failure
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
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("first_name", firstName);
                    data.put("last_name", lastName);
                    data.put("department_id", departmentId);
                    data.put("manager_id", managerId);
                    data.put("primary_office_location_id", primaryOfficeId);

                    employeeInfo.put("birth_date", birthDateString);
                    employeeInfo.put("cell_phone", cellPhone);
                    employeeInfo.put("job_title", jobTitle);
                    employeeInfo.put("office_phone", officePhone);
                    employeeInfo.put("job_start_date", startDateString);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }


                try {
                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        JSONObject account = null;


                        // OK now send avatar if there was a new one specified
                        if (newAvatarFile != null) {
                            HttpResponse avatarResponse = apiClient.uploadNewAvatar(newAvatarFile);
                            int avatarStatusCode = avatarResponse.getStatusLine().getStatusCode();
                            if (avatarStatusCode == HttpStatus.SC_OK) {
                                // avatar response should have both profile changes and avatar change.
                                account = parseJSONResponse(avatarResponse.getEntity());
                            } else {
                                if (avatarResponse.getEntity() != null) {
                                    avatarResponse.getEntity().consumeContent();
                                }

                                fail("Save avatar response was '" + avatarResponse.getStatusLine().getReasonPhrase() + "'", saveCallback);
                            }
                        }

                        if (account == null) {
                            account = parseJSONResponse(response.getEntity());
                        }
                        // Update local data.
                        ContentValues values = new ContentValues();
                        setContactDBValesFromJSON(account.getJSONObject("current_user"), values);


                        database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                } catch (JSONException e) {
                    fail("We didn't understand the server response, please contact Badge HQ.", saveCallback);
                }
            }
        });
    }

    /**
     * Save whether the user wants to share their location or not.
     * <p/>
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param sharingLocation
     * @param saveCallback
     */
    protected void saveSharingLocationAsync(final boolean sharingLocation, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    user.put("user", data);
                    data.put("sharing_office_location", sharingLocation);

                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized(response);
                    if (response.getEntity() != null) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        ContentValues values = new ContentValues();
                        values.put(ContactsTable.COLUMN_CONTACT_SHARING_OFFICE_LOCATION, sharingLocation ? 1 : 0);

                        database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }
            }
        });
    }

    /**
     * Save first name, last name, cell phone, and birth date in local DB
     * and PATCH these values on account in the cloud.
     * <p/>
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param firstName
     * @param lastName
     * @param birthDateString
     * @param cellPhone
     * @param saveCallback    null or a callback that will be invoked on the main thread on success or failure
     */
    protected void saveBasicProfileDataAsync(final String firstName, final String lastName, final String birthDateString, final String cellPhone, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }


                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();

                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("first_name", firstName);
                    data.put("last_name", lastName);
                    employeeInfo.put("birth_date", birthDateString);
                    employeeInfo.put("cell_phone", cellPhone);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for basic profile data", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }


                try {
                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized(response);
                    if (response.getEntity() != null) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put(ContactsTable.COLUMN_CONTACT_FIRST_NAME, firstName);
                        values.put(ContactsTable.COLUMN_CONTACT_LAST_NAME, lastName);
                        values.put(ContactsTable.COLUMN_CONTACT_CELL_PHONE, cellPhone);
                        values.put(ContactsTable.COLUMN_CONTACT_BIRTH_DATE, birthDateString);

                        if (birthDateString != null) {
                            values.put(ContactsTable.COLUMN_CONTACT_BIRTH_DATE, Contact.convertBirthDateString(birthDateString));
                        }
                        //values.put( CompanySQLiteHelper.COL)
                        database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }
            }

        });
    }

    /**
     * Saves the id of the primary location for the logged in user locally
     * and via the API.
     * <p/>
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param primaryLocation
     * @param saveCallback
     */
    protected void savePrimaryLocationAsync(final int primaryLocation, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
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
                    App.gLogger.e("JSON exception creating post body for create department", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.patchAccountRequest(postData);
                    ensureNotUnauthorized(response);
                    if (response.getEntity() != null) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put(ContactsTable.COLUMN_CONTACT_PRIMARY_OFFICE_LOCATION_ID, primaryLocation);
                        //values.put( CompanySQLiteHelper.COL)
                        database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }

            }
        });
    }

    /**
     * Create a new department and persist it to the database. Database row
     * only created if api create option successful.
     *
     * @param department   name of new department
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void createNewDepartmentAsync(final String department, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }

                JSONObject postData = new JSONObject();
                JSONObject departmentData = new JSONObject();
                try {
                    postData.put("department", departmentData);
                    departmentData.put("name", department);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for create department", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.createDepartmentRequest(postData);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                        // Get new department id
                        JSONObject newDepartment = parseJSONResponse(response.getEntity());

                        // Update local data.
                        ContentValues values = new ContentValues();
                        final int departmentId = newDepartment.getInt("id");
                        values.put(DepartmentsTable.COLUMN_ID, departmentId);
                        values.put(DepartmentsTable.COLUMN_DEPARTMENT_NAME, newDepartment.getString("name"));
                        values.put(DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS, newDepartment.getInt("users_count"));
                        database.insert(DepartmentsTable.TABLE_NAME, null, values);
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(departmentId);
                                }
                            });
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                } catch (JSONException e) {
                    fail("We didn't understand the server response, please contact Badge HQ.", saveCallback);
                }
            }
        });

    }

    /**
     * Create a new office location via the API and save it to the local db if successful
     * <p/>
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
    protected void createNewOfficeLocationAsync(final String address, final String city, final String state, final String zip, final String country, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    fail("Database not ready yet. Please report to Badge HQ", saveCallback);
                    return;
                }

                // { “office_location” : { “street_address” : , “city” : , “zip_code” : , “state” : , “country” : , } }
                JSONObject postData = new JSONObject();
                JSONObject locationData = new JSONObject();
                try {
                    postData.put("office_location", locationData);
                    locationData.put("street_address", address);
                    locationData.put("city", city);
                    locationData.put("zip_code", zip);
                    locationData.put("state", state);
                    locationData.put("country", country);
                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating post body for create office location", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {
                    HttpResponse response = apiClient.createLocationRequest(postData);
                    ensureNotUnauthorized(response);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                        // Get new department id
                        JSONObject newOffice = parseJSONResponse(response.getEntity());

                        // Update local data.
                        ContentValues values = new ContentValues();
                        final int officeLocationId = newOffice.getInt("id");
                        values.put(OfficeLocationsTable.COLUMN_ID, officeLocationId);
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_NAME, newOffice.getString("name"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ADDRESS, newOffice.getString("street_address"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_CITY, newOffice.getString("city"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_STATE, newOffice.getString("state"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_ZIP, newOffice.getString("zip_code"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_COUNTRY, newOffice.getString("country"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LAT, newOffice.getString("latitude"));
                        values.put(OfficeLocationsTable.COLUMN_OFFICE_LOCATION_LNG, newOffice.getString("longitude"));
                        database.insert(OfficeLocationsTable.TABLE_NAME, null, values);
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(officeLocationId);
                                }
                            });
                        }
                    } else {
                        if (response.getEntity() != null) {
                            response.getEntity().consumeContent();
                        }
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                } catch (JSONException e) {
                    fail("We didn't understand the server response, please contact Badge HQ.", saveCallback);
                }
            }
        });


    }

    /**
     * Saves department, job title, and manager info.
     * <p/>
     * Operation is atomic, local values will not save if the account
     * can't be updated in the cloud.
     *
     * @param jobTitle
     * @param departmentId
     * @param managerId
     * @param saveCallback null or a callback that will be invoked on the main thread on success or failure
     */
    protected void savePositionProfileDataAsync(final String jobTitle, final int departmentId, final int managerId, final AsyncSaveCallback saveCallback) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                if (database == null) {
                    if (saveCallback != null) {
                        saveCallback.saveFailed("Database not ready yet. Please report to Badge HQ");
                    }
                    return;
                }

                // Wrap entire operation in the transaction so if syncing over http fails
                // the tx will roll back.

                JSONObject user = new JSONObject();
                try {
                    JSONObject data = new JSONObject();
                    JSONObject employeeInfo = new JSONObject();
                    user.put("user", data);
                    data.put("employee_info_attributes", employeeInfo);
                    data.put("department_id", departmentId);
                    data.put("manager_id", managerId);
                    employeeInfo.put("job_title", jobTitle);

                } catch (JSONException e) {
                    App.gLogger.e("JSON exception creating patch body for position profile data", e);
                    fail("Unexpected issue, please contact Badge HQ", saveCallback);
                    return;
                }

                try {

                    HttpResponse response = apiClient.patchAccountRequest(user);
                    ensureNotUnauthorized(response);
                    if (response.getEntity() != null) {
                        response.getEntity().consumeContent();
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        // Update local data.
                        ContentValues values = new ContentValues();
                        values.put(ContactsTable.COLUMN_CONTACT_JOB_TITLE, jobTitle);
                        values.put(ContactsTable.COLUMN_CONTACT_DEPARTMENT_ID, departmentId);
                        values.put(ContactsTable.COLUMN_CONTACT_MANAGER_ID, managerId);
                        database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
                        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
                        if (saveCallback != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    saveCallback.saveSuccess(-1);
                                }
                            });
                        }
                    } else {
                        fail("Server responded with " + response.getStatusLine().getReasonPhrase(), saveCallback);
                    }
                } catch (IOException e) {
                    fail("There was a network issue saving, please check your connection and try again.", saveCallback);
                }
            }
        });
    }

    /**
     * Create a message in the database and send it async to faye
     * for sending over websocket.
     * <p/>
     * New messages become the thread head and are read by default.
     * The lifecycle for message status is pending, sent, or error.
     *
     * @param threadId
     * @param message
     */
    protected void sendMessageAsync(final String threadId, final String message) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                ContentValues msgValues = new ContentValues();
                database.beginTransaction();
                try {
                    clearThreadHead(threadId, msgValues);
                    long timestamp = System.currentTimeMillis() * 1000 /* nano */;
                    // GUID
                    final String guid = UUID.randomUUID().toString();
                    JSONArray userIds = new JSONArray(prefs.getString(threadId, "[]"));
                    //msgValues.put( CompanySQLiteHelper.COLUMN_MESSAGES_ID, null );
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_TIMESTAMP, timestamp);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_BODY, message);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_AVATAR_URL, userIdArrayToAvatarUrl(userIds));
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_HEAD, 1);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_ID, threadId);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_FROM_ID, loggedInUser.id);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_IS_READ, 1);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_GUID, guid);
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_PARTICIPANTS, userIdArrayToNames(userIds));
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_ACK, MSG_STATUS_PENDING);
                    database.insert(MessagesTable.TABLE_NAME, null, msgValues);
                    database.setTransactionSuccessful();
                    sendMessageToFaye(timestamp, guid, threadId, message);

                } catch (JSONException e) {
                    // Realllllllly shouldn't happen.
                    App.gLogger.e("Severe bug, JSON exception parsing user id array from prefs", e);
                    return;
                } finally {
                    database.endTransaction();
                }
                Intent newMsgIntent = new Intent(NEW_MSG_ACTION);
                newMsgIntent.putExtra(THREAD_ID_EXTRA, threadId);
                newMsgIntent.putExtra(IS_INCOMING_MSG_EXTRA, false);
                //newMsgIntent.putExtra( MESSAGE_ID_EXTRA, mess)
                //newMsgIntent.putExtra( )
                localBroadcastManager.sendBroadcast(newMsgIntent);
            }


        });
    }

    protected void sendMessageToFaye(long timestamp, final String guid, final String threadId, final String message) throws JSONException {
        final JSONObject msgWrapper = new JSONObject();
        JSONObject msg = new JSONObject();
        msgWrapper.put("message", msg);
        msg.put("author_id", loggedInUser.id);
        msg.put("body", message);
        msg.put("timestamp", timestamp);
        msg.put("guid", guid);
        msgWrapper.put("guid", guid);
        // Bind/unbind every time so that the service doesn't live past
        // stopService()
        ServiceConnection fayeServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                FayeService.LocalBinding fayeServiceBinding = (FayeService.LocalBinding) service;
                fayeServiceBinding.sendMessage(threadId, msgWrapper);

                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // Derp
            }
        };
        if (!bindService(new Intent(DataProviderService.this, FayeService.class), fayeServiceConnection, BIND_AUTO_CREATE)) {
            unbindService(fayeServiceConnection);
        }
        sqlThread.schedule(new Runnable() {
            @Override
            public void run() {
                Cursor msgCursor = database.rawQuery(QUERY_MESSAGE_SQL, new String[]{"foo", guid});
                if (msgCursor.moveToFirst() && msgCursor.getInt(msgCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_ACK)) == MSG_STATUS_PENDING) {
                    ContentValues values = new ContentValues();
                    values.put(MessagesTable.COLUMN_MESSAGES_ACK, MSG_STATUS_FAILED);
                    int rowsUpdated = database.update(
                            MessagesTable.TABLE_NAME,
                            values,
                            String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_GUID),
                            new String[]{guid}
                    );
                    if (rowsUpdated == 1) {
                        Intent ackIntent = new Intent(MSG_STATUS_CHANGED_ACTION);
                        ackIntent.putExtra(MESSAGE_ID_EXTRA, guid);
                        ackIntent.putExtra(THREAD_ID_EXTRA, threadId);
                        localBroadcastManager.sendBroadcast(ackIntent);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(DataProviderService.this, "Message could not be sent.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }, 4, TimeUnit.SECONDS);

    }

    /**
     * Try to send a message again that's already saved in the DB.
     *
     * @param guid message guid
     */
    protected void retryMessageAsync(final String guid) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                Cursor msgCursor = database.rawQuery(QUERY_MESSAGE_SQL, new String[]{"foo", guid});
                if (msgCursor.moveToFirst()) {
                    // Flip back to pending status.
                    ContentValues msgValues = new ContentValues();
                    msgValues.put(MessagesTable.COLUMN_MESSAGES_ACK, MSG_STATUS_PENDING);
                    database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_GUID), new String[]{guid});
                    String threadId = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_THREAD_ID));
                    String body = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_BODY));
                    long timestamp = msgCursor.getLong(msgCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_TIMESTAMP));
                    msgCursor.close();

                    Intent statusChangeIntent = new Intent(MSG_STATUS_CHANGED_ACTION);
                    statusChangeIntent.putExtra(MESSAGE_ID_EXTRA, guid);
                    statusChangeIntent.putExtra(THREAD_ID_EXTRA, threadId);
                    localBroadcastManager.sendBroadcast(statusChangeIntent);

                    try {
                        sendMessageToFaye(timestamp, guid, threadId, body);
                    } catch (JSONException e) {
                        App.gLogger.e("JSON exception preparing message to send to faye", e);
                    }
                } else {
                    Log.w(LOG_TAG, "UI wanted to retry message with guid " + guid + " but that message can't be found.");
                }

            }
        });
    }

    /**
     * Marks the head msg of this thread as read.
     *
     * @param threadId
     */
    protected void markAsRead(final String threadId) {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(MessagesTable.COLUMN_MESSAGES_IS_READ, 1);
                database.update(
                        MessagesTable.TABLE_NAME,
                        values,
                        String.format("%s = ? AND %s = 1", MessagesTable.COLUMN_MESSAGES_THREAD_ID, MessagesTable.COLUMN_MESSAGES_THREAD_HEAD),
                        new String[]{threadId}
                );
            }
        });
    }

    protected void syncMessagesAsync() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                syncMessagesSync();
            }
        });
    }

    protected void syncMessagesSync() {
        try {
            HttpResponse response = apiClient.getMessageHistory(prefs.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0) - 10000000 /* 10 seconds of buffer */, loggedInUser.id);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                JSONArray msgResponse = parseJSONArrayResponse(response.getEntity());
                int numThreads = msgResponse.length();
                for (int i = 0; i < numThreads; i++) {
                    JSONObject thread = msgResponse.getJSONObject(i);
                    upsertThreadAndMessages(thread, false);
                    Intent updateIntent = new Intent(MSGS_UPDATED_ACTION);
                    updateIntent.putExtra(THREAD_ID_EXTRA, thread.getString("id"));
                    localBroadcastManager.sendBroadcast(updateIntent);

                }
            } else {
                if (response.getEntity() != null) {
                    response.getEntity().consumeContent();
                }
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "Can't sync message history at the moment.");
        } catch (JSONException e) {
            App.gLogger.e("Severe issue, message history response unparseable.", e);
        }
    }

    /**
     * If thread doesn't exist yet, save it, and any unsaved
     * messages as well.
     * <p/>
     * If message that has been sent to us is one of our pending
     * messages, mark it as acknowledged, broadcast it, and sync
     * timestamp/id with server.
     *
     * @param thread    faye thread json object
     * @param broadcast if true ,send local broadcast if thread contains new messages, otherwise,
     *                  assume they are historical
     */
    protected void upsertThreadAndMessages(final JSONObject thread, final boolean broadcast) {
        String threadId;
        database.beginTransaction();
        try {
            threadId = thread.getString("id");
            JSONArray userIds = thread.getJSONArray("user_ids");

            String userIdsList = userIdArrayToKey(userIds);

            prefs.edit().putString(userIdsList, threadId).putString(threadId, userIds.toString()).commit();

            JSONArray msgArray = thread.getJSONArray("messages");
            int numMessages = msgArray.length();
            ContentValues msgValues = new ContentValues();
            msgValues.clear();
            long mostRecentMsgTimestamp = prefs.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0);
            for (int i = 0; i < numMessages; i++) {
                JSONObject msg = msgArray.getJSONObject(i);
                long timestamp = (long) (msg.getDouble("timestamp") * 1000000d) /* nanos */;
                if (timestamp > mostRecentMsgTimestamp) {
                    mostRecentMsgTimestamp = timestamp;
                }
                String messageId = msg.getString("id");
                String guid = msg.getString("guid");
                String[] messageSelector = new String[]{messageId, guid};
                Cursor msgCursor = database.rawQuery(QUERY_MESSAGE_SQL, messageSelector);
                if (msgCursor.getCount() > 0) {
                    msgCursor.moveToFirst();
                    if (msgCursor.getInt(msgCursor.getColumnIndex(MessagesTable.COLUMN_MESSAGES_ACK)) != MSG_STATUS_ACKNOWLEDGED) {
                        msgValues.put(MessagesTable.COLUMN_MESSAGES_ACK, MSG_STATUS_ACKNOWLEDGED);
                        msgValues.put(MessagesTable.COLUMN_MESSAGES_ID, messageId);
                        msgValues.put(MessagesTable.COLUMN_MESSAGES_TIMESTAMP, timestamp);

                        database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ? OR %s = ?", MessagesTable.COLUMN_MESSAGES_ID, MessagesTable.COLUMN_MESSAGES_GUID), messageSelector);
                        // Callback that msg is confirmed?
                        Intent ackIntent = new Intent(MSG_STATUS_CHANGED_ACTION);
                        ackIntent.putExtra(MESSAGE_ID_EXTRA, messageId);
                        ackIntent.putExtra(THREAD_ID_EXTRA, threadId);
                        localBroadcastManager.sendBroadcast(ackIntent);
                    }
                } else {
                    setMessageContentValuesFromJSON(threadId, msg, msgValues);
                    database.insert(MessagesTable.TABLE_NAME, null, msgValues);
                    if (broadcast) {
                        Intent newMessageIntent = new Intent(NEW_MSG_ACTION);
                        Contact fromContact = getContact(msgValues.getAsInteger(MessagesTable.COLUMN_MESSAGES_FROM_ID));
                        newMessageIntent.putExtra(THREAD_ID_EXTRA, threadId);
                        newMessageIntent.putExtra(IS_INCOMING_MSG_EXTRA, true);
                        if (fromContact != null) {
                            newMessageIntent.putExtra(MESSAGE_FROM_EXTRA, fromContact.firstName);
                        } else {
                            newMessageIntent.putExtra(MESSAGE_FROM_EXTRA, "Someone");
                        }

                        newMessageIntent.putExtra(MESSAGE_BODY_EXTRA, msgValues.getAsString(MessagesTable.COLUMN_MESSAGES_BODY));
                        localBroadcastManager.sendBroadcast(newMessageIntent);
                    }

                }
                msgValues.clear();
            }
            // If I'm honest, this switch isn't intended to be used this way,
            // but the idea here is only update the timestamp on history sync
            // so that all messages will eventually be dl'd no matter what.
            if (!broadcast) {
                prefs.edit().putLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, mostRecentMsgTimestamp).commit();
            }

            // Get id of most recent msg.
            Cursor messages = getMessages(threadId);
            if (messages.moveToLast()) {
                String mostRecentGuid = messages.getString(messages.getColumnIndex(MessagesTable.COLUMN_MESSAGES_GUID));
                final String mostRecentId = messages.getString(messages.getColumnIndex(MessagesTable.COLUMN_MESSAGES_ID));
                if ("Inf".equals(mostRecentGuid)) {
                    // Dang! Crash the app to get a report.
                    final String finalThreadId = threadId;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            throw new RuntimeException("Crashing app. Couldn't set head of thread " + finalThreadId + " because message guid came back 'Inf' message id is " + mostRecentId);
                        }
                    });
                }
                messages.close();
                // Unset thread head on all thread messages.
                clearThreadHead(threadId, msgValues);

                msgValues.put(MessagesTable.COLUMN_MESSAGES_AVATAR_URL, userIdArrayToAvatarUrl(userIds));
                msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_PARTICIPANTS, userIdArrayToNames(userIds));
                msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_HEAD, 1);
                database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_GUID), new String[]{mostRecentGuid});
            } else {
                messages.close();
            }
            database.setTransactionSuccessful();
        } catch (JSONException e) {
            App.gLogger.e("Malformed JSON back from faye.");
        } catch (Throwable wtf) {
            App.gLogger.e("Couldn't insert message and it's causing all kinds of problems.", wtf);
        } finally {
            database.endTransaction();
        }
    }

    /**
     * SYNCHRONOUSLY creates a thread using the REST badge api.
     * <strong>ONLY CALL THIS FROM A BACKGROUND TASK</strong>
     *
     * @param recipientIds
     * @return
     */
    protected String createThreadSync(final Integer[] recipientIds) throws JSONException, IOException {
        String threadKey;
        JSONObject postBody = new JSONObject();
        JSONObject messageThread = new JSONObject();
        JSONArray userIds = new JSONArray();

        postBody.put("message_thread", messageThread);
        for (int i : recipientIds) {
            userIds.put(i);
        }
        messageThread.put("user_ids", userIds);
        threadKey = userIdArrayToKey(userIds);

        String existingThreadId = prefs.getString(threadKey, "");
        if ("".equals(existingThreadId)) {
            HttpResponse response = apiClient.createThreadRequest(postBody, loggedInUser.id);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
                JSONObject thread = parseJSONResponse(response.getEntity());
                String threadId = thread.getString("id");
                prefs.edit().putString(threadKey, threadId).putString(threadId, userIds.toString()).commit();
                return threadId;
            } else {
                if (response.getEntity() != null) {
                    response.getEntity().consumeContent();
                }
                // fail( "Unexpected response from the server.", saveCallback );
            }
        } else {
            return existingThreadId;
        }
        return null;
    }

    protected void clearThreadHead(String threadId, ContentValues msgValues) {
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_HEAD, 0);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_AVATAR_URL, (String) null);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_PARTICIPANTS, (String) null);
        database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_THREAD_ID), new String[]{threadId});
        msgValues.clear();
    }

    /**
     * Get a cursor to the list of active thread ids with most
     * recent thread first.
     *
     * @return
     */
    protected Cursor getThreads() {
        if (database != null) {
            return database.rawQuery(QUERY_THREADS_SQL, EMPTY_STRING_ARRAY);
        }
        throw new IllegalStateException("getThreads() called before database available.");
    }

    protected Cursor getMessages(String threadId) {
        if (database != null) {
            return database.rawQuery(QUERY_MESSAGES_SQL, new String[]{threadId});
        }
        throw new IllegalStateException("getMessages() called before database available.");

    }

    private static void setMessageContentValuesFromJSON(String threadId, JSONObject msg, ContentValues msgValues) throws JSONException {
        msgValues.put(MessagesTable.COLUMN_MESSAGES_ACK, MSG_STATUS_ACKNOWLEDGED);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_ID, msg.getString("id"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_FROM_ID, msg.getInt("author_id"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_ID, threadId);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_BODY, msg.getString("body"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_TIMESTAMP, (long) (msg.getDouble("timestamp") * 1000000d));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_GUID, msg.getString("guid"));
        msgValues.put(MessagesTable.COLUMN_MESSAGES_IS_READ, 0);
    }

    /**
     * Get the avatar url for the first user id in the list that's not mine.
     *
     * @param userIdArr
     * @return
     * @throws JSONException
     */
    private String userIdArrayToAvatarUrl(JSONArray userIdArr) throws JSONException {
        int numUsers = userIdArr.length();
        for (int i = 0; i < numUsers; i++) {
            int userId = userIdArr.getInt(i);
            if (userId != loggedInUser.id) {
                Contact c = getContact(userId);
                return c.avatarUrl;
            }
        }
        return null;
    }

    /**
     * Sort the ids in the json array and join them with a comma.
     *
     * @param userIdArr json array of user ids
     * @return a comma delimited string of sorted ids from userIdArr
     */
    private static String userIdArrayToKey(JSONArray userIdArr) throws JSONException {
        int size = userIdArr.length();
        ArrayList<Integer> userIdList = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            userIdList.add(userIdArr.getInt(i));
        }
        Collections.sort(userIdList);
        StringBuilder delimString = new StringBuilder();
        String delim = "";
        for (Integer userId : userIdList) {
            delimString.append(delim).append(userId);
            delim = ",";
        }
        return delimString.toString();
    }

    /**
     * Look up the contact corresponding to each id and join
     * their names in a comma separated list, excluding the logged
     * in user's own name
     *
     * @param userIdArr json array of user ids
     * @return a comma delimited string of unsorted contact names
     * @throws JSONException
     */
    private String userIdArrayToNames(JSONArray userIdArr) throws JSONException {
        StringBuilder names = new StringBuilder();
        String delim = "";
        int numUsers = userIdArr.length();
        StringBuilder firstNames = new StringBuilder();
        int validNames = 0;
        for (int i = 0; i < numUsers; i++) {
            int userId = userIdArr.getInt(i);
            if (userId != loggedInUser.id) {
                Contact c = getContact(userId);
                if (c != null) {
                    names.append(delim).append(c.name);
                    firstNames.append(delim).append(c.firstName);
                    validNames++;
                    delim = ", ";
                }
            }
        }
        if (validNames > 1) {
            return firstNames.toString();
        } else {
            return names.toString();
        }
    }

    /**
     * Utility method for any async save operation to invoke the fail method
     * on the provided callback when things go awry.
     * <p/>
     * Invokes on the main thread.
     *
     * @param reason
     * @param saveCallback
     */
    protected void fail(final String reason, final AsyncSaveCallback saveCallback) {
        if (saveCallback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    saveCallback.saveFailed(reason);
                }
            });
        }
    }

    /**
     * Construct JSONObject of user data to send with Mixpanel event tracking
     */
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
        } catch (JSONException e) {
            Log.w(LOG_TAG, "Couldn't construct mix panel super property json");
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
         * @see DataProviderService#getContactsByDepartmentCursor(int)
         */
        public Cursor getContactsByDepartmentCursor(int departmentId) {
            return DataProviderService.this.getContactsByDepartmentCursor(departmentId);
        }

        /**
         * @see DataProviderService#getContactsCursorExcludingLoggedInUser()
         */
        public Cursor getContactsCursorExcludingLoggedInUser() {
            return DataProviderService.this.getContactsCursorExcludingLoggedInUser();
        }

        /**
         * @see DataProviderService#getContact(int)
         */
        public Contact getContact(int contactId) {
            return DataProviderService.this.getContact(contactId);
        }

        /**
         * @see DataProviderService#setSmallContactImage(String, android.view.View, android.view.View)
         */
        public void setSmallContactImage(Contact c, View thumbImageView, View placeholderView) {
            DataProviderService.this.setSmallContactImage(c.avatarUrl, thumbImageView, placeholderView);
        }

        /**
         * @see DataProviderService#setSmallContactImage(String, android.view.View, android.view.View)
         */
        public void setSmallContactImage(String avatarUrl, View thumbImageView, View placeholderView) {
            DataProviderService.this.setSmallContactImage(avatarUrl, thumbImageView, placeholderView);
        }

        /**
         * @see DataProviderService#setLargeContactImage(com.triaged.badge.models.Contact, android.widget.ImageView)
         */
        public void setLargeContactImage(Contact c, ImageView imageView) {
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
         * @see DataProviderService#getContactsManaged(int)
         */
        public Cursor getContactsManaged(int contactId) {
            return DataProviderService.this.getContactsManaged(contactId);
        }

        /**
         * @see DataProviderService#loginAsync(String, String, DataProviderService.LoginCallback)
         */
        public void loginAsync(String email, String password, LoginCallback loginCallback) {
            DataProviderService.this.loginAsync(email, password, loginCallback);
        }

        /**
         * @return null of not logged in, contact representing user acct otherwise.
         */
        public Contact getLoggedInUser() {
            return loggedInUser;
        }

        /**
         * @see DataProviderService#saveBasicProfileDataAsync(String, String, String, String, DataProviderService.AsyncSaveCallback)
         */
        public void saveBasicProfileDataAsync(String firstName, String lastName, String birthDateString, String cellPhone, AsyncSaveCallback saveCallback) {
            DataProviderService.this.saveBasicProfileDataAsync(firstName, lastName, birthDateString, cellPhone, saveCallback);
        }

        /**
         * @see DataProviderService#savePositionProfileDataAsync(String, int, int, DataProviderService.AsyncSaveCallback)
         */
        public void savePositionProfileDataAsync(String jobTitle, int departmentId, int managerId, AsyncSaveCallback saveCallback) {
            DataProviderService.this.savePositionProfileDataAsync(jobTitle, departmentId, managerId, saveCallback);
        }

        /**
         * @see DataProviderService#savePrimaryLocationAsync(int, DataProviderService.AsyncSaveCallback)
         */
        public void savePrimaryLocationASync(int primaryLocation, AsyncSaveCallback saveCallback) {
            DataProviderService.this.savePrimaryLocationAsync(primaryLocation, saveCallback);
        }


        /**
         * @see DataProviderService#saveAllProfileDataAsync(String, String, String, String, String, int, int, int, String, String, byte[], DataProviderService.AsyncSaveCallback)
         */
        public void saveAllProfileDataAsync(String firstName, String lastName, String cellPhone, String officePhone, String jobTitle, int departmentId, int managerId, int primaryOfficeId, String startDateString, String birthDateString, byte[] newAvatarFile, AsyncSaveCallback saveCallback) {
            DataProviderService.this.saveAllProfileDataAsync(firstName, lastName, cellPhone, officePhone, jobTitle, departmentId, managerId, primaryOfficeId, startDateString, birthDateString, newAvatarFile, saveCallback);
        }

        /**
         * @see DataProviderService#getOfficeLocationName(int)
         */
        public String getOfficeLocationName(int locationId) {
            return DataProviderService.this.getOfficeLocationName(locationId);
        }

        /*
         * @see DataProviderService#getDepartmentCursor(boolean)
         */
        public Cursor getDepartmentCursor(boolean onlyNonEmptyDepts) {
            return DataProviderService.this.getDepartmentCursor(onlyNonEmptyDepts);
        }

        /**
         * @see DataProviderService#getOfficeLocationsCursor() ()
         */
        public Cursor getOfficeLocationsCursor() {
            return DataProviderService.this.getOfficeLocationsCursor();
        }

        /**
         * @see DataProviderService#loggedOut(boolean)
         */
        public void logout() {
            DataProviderService.this.unregisterDevice();
        }

        /**
         * @see DataProviderService#createNewDepartmentAsync(String, DataProviderService.AsyncSaveCallback)
         */
        public void createNewDepartmentAsync(String department, AsyncSaveCallback saveCallback) {
            DataProviderService.this.createNewDepartmentAsync(department, saveCallback);
        }

        /**
         * @see DataProviderService#createNewOfficeLocationAsync(String, String, String, String, String, DataProviderService.AsyncSaveCallback)
         */
        public void createNewOfficeLocationAsync(String address, String city, String state, String zip, String country, AsyncSaveCallback saveCallback) {
            DataProviderService.this.createNewOfficeLocationAsync(address, city, state, zip, country, saveCallback);
        }

        /**
         * @see DataProviderService#registerDevice(String)
         */
        public void registerDevice(String pushToken) {
            DataProviderService.this.registerDevice(pushToken);
        }

        /**
         * @see DataProviderService#checkInToOffice(int)
         */
        public void checkInToOffice(int officeId) {
            DataProviderService.this.checkInToOffice(officeId);
        }

        /**
         * @see DataProviderService#checkOutOfOfficeAsync(int)
         */
        public void checkOutOfOffice(int officeId) {
            DataProviderService.this.checkOutOfOfficeAsync(officeId);
        }

        /**
         * @see DataProviderService#changePassword(String, String, String, DataProviderService.AsyncSaveCallback)
         */
        public void changePassword(String oldPassword, String newPassword, String newPasswordConfirmation, AsyncSaveCallback saveCallback) {
            DataProviderService.this.changePassword(oldPassword, newPassword, newPasswordConfirmation, saveCallback);
        }

        /**
         * @see DataProviderService#getBasicMixpanelData() (int)
         */
        public JSONObject getBasicMixpanelData() {
            return DataProviderService.this.getBasicMixpanelData();
        }

        public void refreshContact(int contactId) {
            DataProviderService.this.refreshContact(contactId);
        }

        /**
         * @see DataProviderService#upsertThreadAndMessages(org.json.JSONObject, boolean)
         */
        public void upsertThreadAndMessagesAsync(final JSONObject thread) {
            sqlThread.submit(new Runnable() {
                @Override
                public void run() {
                    DataProviderService.this.upsertThreadAndMessages(thread, true);
                }
            });
        }

        /**
         * @see DataProviderService#getThreads()
         */
        public Cursor getThreads() {
            return DataProviderService.this.getThreads();
        }

        /**
         * @see DataProviderService#getMessages(String)
         */
        public Cursor getMessages(String threadId) {
            return DataProviderService.this.getMessages(threadId);
        }

        /**
         * @see DataProviderService#sendMessageAsync(String, String)
         */
        public void sendMessageAsync(String threadId, String body) {
            DataProviderService.this.sendMessageAsync(threadId, body);
        }

        /**
         * @see DataProviderService#createThreadSync(Integer[])
         */
        public String createThreadSync(Integer[] userIds) throws JSONException, IOException {
            return DataProviderService.this.createThreadSync(userIds);
        }

        /**
         * Provide a way to use {@link #userIdArrayToNames(org.json.JSONArray)}
         * from the UI.
         *
         * @param threadId
         * @return
         */
        public String getRecipientNames(String threadId) {
            try {
                return userIdArrayToNames(new JSONArray(prefs.getString(threadId, "[]")));
            } catch (JSONException e) {
                return null;
            }
        }

        /**
         * @see DataProviderService#markAsRead(String)
         */
        public void markAsRead(String threadId) {
            DataProviderService.this.markAsRead(threadId);
        }

        /**
         * @see DataProviderService#syncMessagesAsync()
         */
        public void syncMessagesAsync() {
            DataProviderService.this.syncMessagesAsync();
        }

        /**
         * @see DataProviderService#retryMessageAsync(String)
         */
        public void retryMessageAsync(String guid) {
            DataProviderService.this.retryMessageAsync(guid);
        }

        /**
         * @see DataProviderService#requestResetPassword(String, DataProviderService.AsyncSaveCallback)
         */
        public void requestResetPassword(String email, AsyncSaveCallback saveCallback) {
            DataProviderService.this.requestResetPassword(email, saveCallback);
        }

        /**
         * @see DataProviderService#partialSyncContactsAsync()
         */
        public void partialSyncContactsAsync() {
            DataProviderService.this.partialSyncContactsAsync();
        }

        /**
         * See DataProviderService#saveSharingLocationAsync
         */
        public void saveSharingLocationAsync(boolean sharingLocation, AsyncSaveCallback saveCallback) {
            DataProviderService.this.saveSharingLocationAsync(sharingLocation, saveCallback);
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
            this(url, thumbView, placeholderView, null);
        }

        /**
         * Task will save images in a memory cache.
         *
         * @param url             img url.
         * @param thumbView       view on which to set the bitmap once downloaded.
         * @param placeholderView view to hide if we successfully download the image and set it on the view.
         * @param memoryCache     cache to put image in to after downloading.
         */
        protected LoadImageAsyncTask(String url, View thumbView, View placeholderView, LruCache<String, Bitmap> memoryCache) {
            this.urlStr = url;
            this.thumbView = thumbView;
            this.memoryCache = memoryCache;
            this.placeholderView = placeholderView;
        }

        protected String getUrlHash() {
            return String.valueOf(urlStr.hashCode());
        }

        protected Bitmap loadBitmapFromDisk() {
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
                        if (snapshot != null) {
                            BufferedInputStream stream = new BufferedInputStream(snapshot.getInputStream(0));
                            Bitmap bitmap = BitmapFactory.decodeStream(stream);
                            stream.close();
                            return bitmap;
                        }
                    }
                }
            } catch (IOException e) {
                // OK... ?
                App.gLogger.e("Error reading from disk cache", e);
            }
            return null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Is it in disk cache?

            Bitmap bitmap = loadBitmapFromDisk();


            if (bitmap == null) {
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
                                if (mDiskLruCache != null && !mDiskCacheStarting) {
                                    DiskLruCache.Editor editor = mDiskLruCache.edit(getUrlHash());
                                    OutputStream out = editor.newOutputStream(0);
                                    if (out != null) {
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
                        App.gLogger.e("Either we got a bad URL from the api or we did something stupid", e);
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Network issue reading image data", e);
                    }
                }
            }

            if (bitmap != null) {
                final Bitmap scaledBitmap = thumbView.getWidth() > 0 ? Bitmap.createScaledBitmap(bitmap, thumbView.getWidth(), thumbView.getHeight(), false) : bitmap;

                if (memoryCache != null) {
                    memoryCache.put(urlStr, scaledBitmap);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        assignBitmapToView(scaledBitmap, thumbView);
                        if (placeholderView != null) {
                            placeholderView.setVisibility(View.GONE);
                        }

                    }
                });

            } else {
                Log.w(LOG_TAG, "Bitmap from " + urlStr + " was null. No network? Bad image data?");
            }

            return null;
        }
    }

    /**
     * Given an http response entity, parse in to a json object.
     *
     * @param entity http response body
     * @return parsed json object
     * @throws IOException   if network stream can't be read.
     * @throws JSONException if there's an error parsing json.
     */
    protected static JSONObject parseJSONResponse(HttpEntity entity) throws IOException, JSONException {
        ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream(1024 /* 256 k */);
        entity.writeTo(jsonBuffer);
        jsonBuffer.close();
        String json = jsonBuffer.toString("UTF-8");
        return new JSONObject(json);
    }

    /**
     * Given an http response entity, parse in to a json object.
     *
     * @param entity http response body
     * @return parsed json object
     * @throws IOException   if network stream can't be read.
     * @throws JSONException if there's an error parsing json.
     */
    protected static JSONArray parseJSONArrayResponse(HttpEntity entity) throws IOException, JSONException {
        ByteArrayOutputStream jsonBuffer = new ByteArrayOutputStream(1024 /* 256 k */);
        entity.writeTo(jsonBuffer);
        jsonBuffer.close();
        String json = jsonBuffer.toString("UTF-8");
        return new JSONArray(json);
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
        public void loginFailed(String reason);

        /**
         * Login was successful, {@link DataProviderService.LocalBinding#getLoggedInUser()}
         * is now guaranteed to return non null.
         *
         * @param user the now logged in user
         */
        public void loginSuccess(Contact user);
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
        public void saveSuccess(int newId);

        /**
         * Save encountered an issue.
         *
         * @param reason human readable reason for user messaging
         */
        public void saveFailed(String reason);
    }
}
