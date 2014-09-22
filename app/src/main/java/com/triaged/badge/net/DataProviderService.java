package com.triaged.badge.net;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.DatabaseHelper;
import com.triaged.badge.database.helper.MessageHelper;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.events.NewMessageEvent;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.Department;
import com.triaged.badge.models.Receipt;
import com.triaged.badge.models.User;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.receivers.GCMReceiver;
import com.triaged.badge.receivers.LogoutReceiver;
import com.triaged.utils.SharedPreferencesUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

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
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME
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
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME
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
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME
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
                    ContactsTable.COLUMN_CONTACT_FIRST_NAME
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

    private BroadcastReceiver syncGCMReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return localBinding;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        String apiToken = prefs.getString(API_TOKEN_PREFS_KEY, "");
        lastSynced = prefs.getLong(LAST_SYNCED_PREFS_KEY, 0);
        sqlThread = Executors.newSingleThreadScheduledExecutor();
        databaseHelper = new DatabaseHelper(this);
        apiClient = new ApiClient(apiToken);
        contactList = new ArrayList<Contact>(250);
        handler = new Handler();
        localBinding = new LocalBinding();
        mimeTypeMap = MimeTypeMap.getSingleton();

        httpClient = new DefaultHttpClient();

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
        EventBus.getDefault().unregister(this);
        localBroadcastManager.unregisterReceiver(syncGCMReceiver);
        sqlThread.shutdownNow();
        databaseHelper.close();
        apiClient.getConnectionManager().shutdown();
        httpClient.getConnectionManager().shutdown();
        database = null;
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
                            getContentResolver().insert(ContactProvider.CONTENT_URI, values);
//                            database.insert(ContactsTable.TABLE_NAME, null, values);
                        } else {
                            getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                    ContactsTable.COLUMN_ID + " = ?",
                                    new String[]{user.getString("id")});
//                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID),
//                                    new String[]{user.getString("id")});
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
                        getContentResolver().insert(OfficeLocationProvider.CONTENT_URI, values);
//                                database.insert(OfficeLocationsTable.TABLE_NAME, null, values);
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
        RestService.instance().badge().getDepartment(syncId + "", new Callback<Department>() {
            @Override
            public void success(Department department, Response response) {
                ContentValues values = new ContentValues();
                values.put(DepartmentsTable.COLUMN_ID, department.id);
                values.put(DepartmentsTable.COLUMN_DEPARTMENT_NAME, department.name);
                values.put(DepartmentsTable.COLUMN_DEPARTMENT_NUM_CONTACTS, department.usersCount);
                getContentResolver().insert(DepartmentProvider.CONTENT_URI, values);
            }

            @Override
            public void failure(RetrofitError error) {
                App.gLogger.e("Network issue getting single department. error: " + error.getMessage());
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
                                getContentResolver().insert(ContactProvider.CONTENT_URI, values);
//                                database.insert(ContactsTable.TABLE_NAME, null, values);
                            } else {
                                getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                        ContactsTable.COLUMN_ID + " =?",
                                        new String[] { newContact.getString("id")});
//                                database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{newContact.getString("id")});
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
     */
    protected void syncCompany() {
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
//            db.beginTransaction();
            HttpResponse response = apiClient.downloadCompanyRequest(0 /* Get all contacts */);
            ensureNotUnauthorized(response);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
//                    getContentResolver().delete(ContactProvider.CONTENT_URI, null, null);
                    getContentResolver().delete(DepartmentProvider.CONTENT_URI, null, null);
                    getContentResolver().delete(OfficeLocationProvider.CONTENT_URI, null, null);

//                    db.execSQL(CLEAR_CONTACTS_SQL);
//                    db.execSQL(CLEAR_DEPARTMENTS_SQL);
//                    db.execSQL(CLEAR_OFFICE_LOCATIONS_SQL);

                    JSONObject companyObj = parseJSONResponse(response.getEntity());
                    JSONArray contactsArr = companyObj.getJSONArray("users");
                    int contactsLength = contactsArr.length();
                    ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>();
                    for (int i = 0; i < contactsLength; i++) {
                        JSONObject newContact = contactsArr.getJSONObject(i);
                        ContentValues values = new ContentValues();
                        setContactDBValesFromJSON(newContact, values);

                        getContentResolver().insert(ContactProvider.CONTENT_URI, values);
                        dbOperations.add(ContentProviderOperation.newInsert(
                                ContactProvider.CONTENT_URI).withValues(values).build());
                    }
                    getContentResolver().applyBatch(ContactProvider.AUTHORITY, dbOperations);
                    dbOperations.clear();

                    if (companyObj.has("uses_departments") && companyObj.getBoolean("uses_departments")) {
                        JSONArray deptsArr = companyObj.getJSONArray("departments");
                        int deptsLength = deptsArr.length();
                        for (int i = 0; i < deptsLength; i++) {
                            ContentValues values = new ContentValues();
                            JSONObject dept = deptsArr.getJSONObject(i);
                            setDepartmentBValuesFromJSON(dept, values);
                            dbOperations.add(ContentProviderOperation.newInsert(
                                    DepartmentProvider.CONTENT_URI).withValues(values).build());
                        }
                        getContentResolver().applyBatch(DepartmentProvider.AUTHORITY, dbOperations);
                        dbOperations.clear();
                    }

                    if (companyObj.has("office_locations")) {
                        JSONArray locations = companyObj.getJSONArray("office_locations");
                        int locationsLength = locations.length();
                        for (int i = 0; i < locationsLength; i++) {
                            ContentValues values = new ContentValues();
                            JSONObject location = locations.getJSONObject(i);
                            setOfficeLocationDBValuesFromJSON(location, values);
                            dbOperations.add(ContentProviderOperation.newInsert(
                                    OfficeLocationProvider.CONTENT_URI).withValues(values).build());
                        }
                        getContentResolver().applyBatch(OfficeLocationProvider.AUTHORITY, dbOperations);
                        dbOperations.clear();
                    }
                    SharedPreferencesUtil.store(R.string.pref_has_fetch_company, true);
                    loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));

                } else {
                    App.gLogger.e("Got status " + statusCode + " from API. Handle this appropriately!");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            } finally {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
            }

//            db.setTransactionSuccessful();
            updated = true;
        } catch (IOException e) {
            App.gLogger.e("IO exception downloading company that should be handled more softly than this.", e);
        } catch (JSONException e) {
            App.gLogger.e("JSON from server not formatted correctly. Either we shouldn't have expected JSON or this is an api bug.", e);
        } finally {
//            db.endTransaction();
        }
        if (updated && initialized) {
            localBroadcastManager.sendBroadcast(new Intent(DB_UPDATED_ACTION));
        }
    }

    protected void syncCompanyAsync() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                syncCompany();
            }
        });
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
            setStringContentValueFromJSONUnlessNull(employeeInfo, "website", values, ContactsTable.COLUMN_CONTACT_WEBSITE);
            setStringContentValueFromJSONUnlessNull(employeeInfo, "linkedin", values, ContactsTable.COLUMN_CONTACT_LINKEDIN);
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
     * When the service detects that there is no active user
     * or api token, it calls this function.
     * <p/>
     * This launches the login activity in a new task and sends a local
     * broadcast so that activities can listen and kill themselves.
     * <p/>
     * This should only be called on the sql thread.
     */
    protected void loggedOut() {

        Intent logoutIntent = new Intent(LogoutReceiver.ACTION_LOGOUT);
        logoutIntent.putExtra(LogoutReceiver.RESTART_APP_EXTRA, true);
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
     * Every time we hit the API, we should make sure the status code
     * returned wasn't unauthorized. If it was, we assume
     * the user has been logged out or termed or something and we reset
     * to logged out state.
     *
     * @param response
     */
    protected boolean ensureNotUnauthorized(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            loggedOut();
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
                            getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                    ContactsTable.COLUMN_ID + " =?",
                                    new String[]{contactId + ""});
//                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(contactId)});
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
                            getContentResolver().update(ContactProvider.CONTENT_URI, values,
                                    ContactsTable.COLUMN_ID + " =?",
                                    new String[] { loggedInUser.id + ""});
//                            database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
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

                getContentResolver().update(ContactProvider.CONTENT_URI, values,
                        ContactsTable.COLUMN_ID + " =?",
                        new String[] { loggedInUser + ""});

//                database.update(ContactsTable.TABLE_NAME, values, String.format("%s = ?", ContactsTable.COLUMN_ID), new String[]{String.valueOf(loggedInUser.id)});
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
                    database = databaseHelper.getReadableDatabase();

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
                        syncCompany();

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
//                                    saveSharingLocationAsync(true, new DataProviderService.AsyncSaveCallback() {
//                                        @Override
//                                        public void saveSuccess(int newId) {
//                                            prefs.edit().putBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true).commit();
//                                            LocationTrackingService.scheduleAlarm(DataProviderService.this);
//                                        }
//
//                                        @Override
//                                        public void saveFailed(String reason) {
//                                            Log.d(LOG_TAG, "Initial attempt to save sharing location pref failed due to: " + reason);
//                                        }
//                                    });

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
//                database.beginTransaction();
                try {
                    clearThreadHead(threadId);
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

                    getContentResolver().insert(MessageProvider.CONTENT_URI, msgValues);
//                            database.insert(MessagesTable.TABLE_NAME, null, msgValues);
//                    database.setTransactionSuccessful();
                    sendMessageToFaye(timestamp, guid, threadId, message);

                } catch (JSONException e) {
                    // Realllllllly shouldn't happen.
                    App.gLogger.e("Severe bug, JSON exception parsing user id array from prefs", e);
                    return;
                } finally {
//                    database.endTransaction();
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


                    int rowsUpdated = getContentResolver().update(MessageProvider.CONTENT_URI, values,
                            MessagesTable.COLUMN_MESSAGES_GUID + " =?",
                            new String[] { guid});

//                    int rowsUpdated = database.update(
//                            MessagesTable.TABLE_NAME,
//                            values,
//                            String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_GUID),
//                            new String[]{guid}
//                    );
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
                    getContentResolver().update(MessageProvider.CONTENT_URI, msgValues,
                            MessagesTable.COLUMN_MESSAGES_GUID + " =?",
                            new String[] { guid});

//                    database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_GUID), new String[]{guid});
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
//        database.beginTransaction();
        try {
            threadId = thread.getString("id");
            JSONArray userIds = thread.getJSONArray("user_ids");

            String userIdsList = userIdArrayToKey(userIds);

            prefs.edit().putString(userIdsList, threadId).putString(threadId, userIds.toString()).commit();

            JSONArray msgArray = thread.getJSONArray("messages");
            long mostRecentMsgTimestamp = prefs.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0);

            if (!thread.isNull("muted")) SharedPreferencesUtil.store("is_mute_" + threadId, thread.getBoolean("muted"));
            if (!thread.isNull("name")) SharedPreferencesUtil.store("name_" + threadId, thread.getString("name"));

            ArrayList<ContentProviderOperation> dbOperations =
                    new ArrayList<ContentProviderOperation>(msgArray.length());
            for (int i = 0; i < msgArray.length(); i++) {
                JSONObject msg = msgArray.getJSONObject(i);
                //TODO: why nano? those zeros at the end of timestamp
                // does not make it unique number!
                long timestamp = (long) (msg.getDouble("timestamp") * 1000000d) /* nanos */;
                if (timestamp > mostRecentMsgTimestamp) {
                    mostRecentMsgTimestamp = timestamp;
                }
                ContentValues contentValues = MessageHelper.setMessageContentValuesFromJSON(threadId, msg);
                dbOperations.add(ContentProviderOperation.newInsert(
                        MessageProvider.CONTENT_URI).
                        withValues(contentValues).
                        build());

                if (contentValues.getAsInteger(MessagesTable.COLUMN_MESSAGES_FROM_ID) != loggedInUser.id) {
                    ContentValues receiptValues = new ContentValues();
                    receiptValues.put(ReceiptTable.COLUMN_MESSAGE_ID, contentValues.getAsString(MessagesTable.COLUMN_MESSAGES_ID));
                    receiptValues.put(ReceiptTable.COLUMN_THREAD_ID, threadId);
                    receiptValues.put(ReceiptTable.COLUMN_USER_ID, loggedInUser.id);
                    receiptValues.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.NOT_SYNCED);

                    getContentResolver().insert(ReceiptProvider.CONTENT_URI, receiptValues);

                    EventBus.getDefault().post(new NewMessageEvent(threadId,
                            contentValues.getAsString(MessagesTable.COLUMN_MESSAGES_ID)));
                }
            }

            getContentResolver().applyBatch(MessageProvider.AUTHORITY, dbOperations);


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
                clearThreadHead(threadId);

                ContentValues msgValues = new ContentValues();
                msgValues.put(MessagesTable.COLUMN_MESSAGES_AVATAR_URL, userIdArrayToAvatarUrl(userIds));
                msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_PARTICIPANTS, userIdArrayToNames(userIds));
                msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_HEAD, 1);

                getContentResolver().update(MessageProvider.CONTENT_URI, msgValues,
                        MessagesTable.COLUMN_MESSAGES_GUID + " =?",
                        new String[] { mostRecentGuid});

            } else {
                messages.close();
            }
        } catch (JSONException e) {
            App.gLogger.e("Malformed JSON back from faye.");
        } catch (Throwable wtf) {
            App.gLogger.e("Couldn't insert message and it's causing all kinds of problems.", wtf);
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

    protected void clearThreadHead(String threadId) {
        ContentValues msgValues = new ContentValues();
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_HEAD, 0);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_AVATAR_URL, (String) null);
        msgValues.put(MessagesTable.COLUMN_MESSAGES_THREAD_PARTICIPANTS, (String) null);
        getContentResolver().update(MessageProvider.CONTENT_URI, msgValues,
                MessagesTable.COLUMN_MESSAGES_THREAD_ID + " =?",
                new String[] { threadId});

//        database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.COLUMN_MESSAGES_THREAD_ID), new String[]{threadId});
        msgValues.clear();
    }

    protected Cursor getMessages(String threadId) {
        if (database != null) {
            return database.rawQuery(QUERY_MESSAGES_SQL, new String[]{threadId});
        }
        throw new IllegalStateException("getMessages() called before database available.");

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

    public void onEvent(LogedinSuccessfully logedinSuccessfully) {

        String apiToken = SharedPreferencesUtil.getString(R.string.pref_api_token, "");
        apiClient = new ApiClient(apiToken);
        syncCompanyAsync();
        syncMessagesAsync();
    }

    public void onEvent(UpdateAccountEvent updateAccountEvent) {
        loggedInUser = getContact(prefs.getInt(LOGGED_IN_USER_ID_PREFS_KEY, -1));
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
         * @return null of not logged in, contact representing user acct otherwise.
         */
        public Contact getLoggedInUser() {
            return loggedInUser;
        }

        /**
         * Just a temporary method, for this refactoring
         */
        public void setLoggedInUser(User currentUser) {
            loggedInUser = new Contact();
            loggedInUser.id = currentUser.getId();
            loggedInUser.firstName = currentUser.getFirstName();
            loggedInUser.lastName = currentUser.getLastName();
            loggedInUser.avatarUrl = currentUser.getAvatarUrl();
            loggedInUser.linkedin = currentUser.getEmployeeInfo().getLinkedin();
            loggedInUser.website = currentUser.getEmployeeInfo().getWebsite();

            if (currentUser.getEmployeeInfo().getBirthDate() != null) {
                loggedInUser.birthDateString = Contact.convertBirthDateString(currentUser.getEmployeeInfo().getBirthDate());
            }
            loggedInUser.cellPhone = currentUser.getEmployeeInfo().getCellPhone();

            try {
                loggedInUser.currentOfficeLocationId = Integer.valueOf(currentUser.getCurrentOfficeLocaitonId());
            } catch (NumberFormatException e) {
                loggedInUser.currentOfficeLocationId = -1;
            }

            try {
                loggedInUser.departmentId = Integer.valueOf(currentUser.getDepartmentId());
            } catch (NumberFormatException e) {
                loggedInUser.departmentId = -1;
            }

            try {
                loggedInUser.managerId = Integer.valueOf(currentUser.getManagerId());
            } catch (NumberFormatException e) {
                loggedInUser.managerId = -1;
            }

            try {
                loggedInUser.primaryOfficeLocationId = Integer.valueOf(currentUser.getPrimaryOfficeLocationid());
            } catch (NumberFormatException e) {
                loggedInUser.primaryOfficeLocationId = -1;
            }

            loggedInUser.isArchived = currentUser.isArchived();
            loggedInUser.jobTitle = currentUser.getEmployeeInfo().getJobTitle();

            loggedInUser.name = currentUser.getFirstName() + " " + currentUser.getLastName();

            if (currentUser.getEmployeeInfo().getJobStartDate() != null) {
                loggedInUser.startDateString = Contact.convertStartDateString(currentUser.getEmployeeInfo().getJobStartDate());
            }
//            loggedInUser.sharingOfficeLocation = currentUser.isSharingOfficeLocation();
            loggedInUser.officePhone = currentUser.getEmployeeInfo().getOfficePhone();

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
