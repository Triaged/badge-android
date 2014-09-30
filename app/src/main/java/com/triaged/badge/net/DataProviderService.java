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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.DatabaseHelper;
import com.triaged.badge.database.helper.DepartmentHelper;
import com.triaged.badge.database.helper.MessageHelper;
import com.triaged.badge.database.helper.OfficeLocationHelper;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.ContactProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.provider.ThreadProvider;
import com.triaged.badge.database.provider.ThreadUserProvider;
import com.triaged.badge.database.table.ContactsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessageThreadsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.OfficeLocationsTable;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.database.table.ThreadUserTable;
import com.triaged.badge.events.LogedinSuccessfully;
import com.triaged.badge.events.NewMessageEvent;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.BadgeThread;
import com.triaged.badge.models.Company;
import com.triaged.badge.models.Contact;
import com.triaged.badge.models.Department;
import com.triaged.badge.models.Message;
import com.triaged.badge.models.OfficeLocation;
import com.triaged.badge.models.Receipt;
import com.triaged.badge.models.User;
import com.triaged.badge.net.api.RestService;
import com.triaged.badge.net.mime.TypedJsonString;
import com.triaged.badge.receivers.GCMReceiver;
import com.triaged.badge.receivers.LogoutReceiver;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static final String MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY = "latestMsgTimestampPrefsKey";

    public static final String DB_UPDATED_ACTION = "com.triage.badge.DB_UPDATED";
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

    protected static final String QUERY_CONTACT_SQL =
            String.format("SELECT contact.*, office.%s %s, department.%s %s, manager.%s %s, manager.%s %s FROM" +
                            " %s contact LEFT OUTER JOIN %s department ON contact.%s = department.%s LEFT OUTER JOIN %s manager" +
                            " ON contact.%s = manager.%s LEFT OUTER JOIN %s office ON contact.%s = office.%s  WHERE contact.%s = ?",
                    OfficeLocationsTable.CLM_NAME,
                    DatabaseHelper.JOINED_OFFICE_NAME,
                    DepartmentsTable.CLM_NAME,
                    DatabaseHelper.JOINED_DEPARTMENT_NAME,
                    ContactsTable.CLM_FIRST_NAME,
                    DatabaseHelper.JOINED_MANAGER_FIRST_NAME,
                    ContactsTable.CLM_LAST_NAME,
                    DatabaseHelper.JOINED_MANAGER_LAST_NAME,
                    ContactsTable.TABLE_NAME,
                    DepartmentsTable.TABLE_NAME,
                    ContactsTable.CLM_DEPARTMENT_ID,
                    DepartmentsTable.COLUMN_ID,
                    ContactsTable.TABLE_NAME,
                    ContactsTable.CLM_MANAGER_ID,
                    ContactsTable.COLUMN_ID,
                    OfficeLocationsTable.TABLE_NAME,
                    ContactsTable.CLM_PRIMARY_OFFICE_LOCATION_ID,
                    OfficeLocationsTable.COLUMN_ID,
                    ContactsTable.COLUMN_ID
            );

    protected volatile Contact loggedInUser;
    protected ScheduledExecutorService sqlThread;
    protected DatabaseHelper databaseHelper;
    protected SQLiteDatabase database = null;
    protected long lastSynced;
    protected SharedPreferences prefs;
    protected Handler handler;
    protected volatile boolean initialized;

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

        lastSynced = SharedPreferencesUtil.getLong(R.string.pref_last_sync_key, 0);
        sqlThread = Executors.newSingleThreadScheduledExecutor();
        databaseHelper = new DatabaseHelper(this);
        handler = new Handler();
        localBinding = new LocalBinding();

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
        database = null;
    }

    /**
     * Get a single contact from the server and add it to the db.
     * <p/>
     * Only used when called a GCM message is received instructing the client to update itself.
     */
    protected void getSingleContact(final int syncId) {
        RestService.instance().badge().getUser(syncId +"", new Callback<User>() {
            @Override
            public void success(User user, Response response) {
                getContentResolver().insert(ContactProvider.CONTENT_URI, UserHelper.toContentValue(user));
            }

            @Override
            public void failure(RetrofitError error) {
                App.gLogger.e(error);
            }
        });
    }

    protected void getSingleOffice(final int syncId) {
        RestService.instance().badge().getOfficeLocation(syncId + "", new Callback<OfficeLocation>() {
            @Override
            public void success(OfficeLocation officeLocation, Response response) {
                getContentResolver().insert(OfficeLocationProvider.CONTENT_URI,
                        OfficeLocationHelper.toContentValue(officeLocation));
            }

            @Override
            public void failure(RetrofitError error) {
                App.gLogger.e(error);
            }
        });
    }

    protected void getSingleDepartment(final int syncId) {
        RestService.instance().badge().getDepartment(syncId + "", new Callback<Department>() {
            @Override
            public void success(Department department, Response response) {
                ContentValues values = new ContentValues();
                values.put(DepartmentsTable.COLUMN_ID, department.id);
                values.put(DepartmentsTable.CLM_NAME, department.name);
                values.put(DepartmentsTable.CLM_CONTACTS_NUMBER, department.usersCount);
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
        long previousSync = lastSynced;
        lastSynced = System.currentTimeMillis();
        SharedPreferencesUtil.store(R.string.pref_last_sync_key, lastSynced);

        RestService.instance().badge().getCompany((previousSync - 60000) + "" /* one minute of buffer */, new Callback<Company>() {
            @Override
            public void success(Company company, Response response) {
                ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>();
                for (User user : company.getUsers()) {
                    dbOperations.add(ContentProviderOperation.newInsert(
                            ContactProvider.CONTENT_URI).withValues(UserHelper.toContentValue(user)).build());
                }
                try {
                    getContentResolver().applyBatch(ContactProvider.AUTHORITY, dbOperations);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                error.printStackTrace();
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
        if (info == null || !info.isConnected()) {// TODO listen for network becoming available so we can sync then.
            return;
        }
        lastSynced = System.currentTimeMillis();
        SharedPreferencesUtil.store(R.string.pref_last_sync_key, lastSynced);

        RestService.instance().badge().getCompany("0", new Callback<Company>() {
            @Override
            public void success(Company company, Response response) {
//                    getContentResolver().delete(ContactProvider.CONTENT_URI, null, null);
                getContentResolver().delete(DepartmentProvider.CONTENT_URI, null, null);
                getContentResolver().delete(OfficeLocationProvider.CONTENT_URI, null, null);
                try {
                    ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>();
                    for (User user : company.getUsers()) {
                        dbOperations.add(ContentProviderOperation.newInsert(
                                ContactProvider.CONTENT_URI).withValues(UserHelper.toContentValue(user)).build());
                    }
                    getContentResolver().applyBatch(ContactProvider.AUTHORITY, dbOperations);
                    dbOperations.clear();

                    for (Department department: company.getDepartments()) {
                        dbOperations.add(ContentProviderOperation.newInsert(
                                DepartmentProvider.CONTENT_URI).withValues(
                                DepartmentHelper.toContentValue(department)).build());
                    }
                    getContentResolver().applyBatch(DepartmentProvider.AUTHORITY, dbOperations);
                    dbOperations.clear();

                    for (OfficeLocation officeLocation: company.getOfficeLocations()) {
                        dbOperations.add(ContentProviderOperation.newInsert(
                                OfficeLocationProvider.CONTENT_URI).withValues(
                                OfficeLocationHelper.toContentValue(officeLocation)).build());
                    }
                    SharedPreferencesUtil.store(R.string.pref_does_fetched_company_already, true);
                    getContentResolver().applyBatch(OfficeLocationProvider.AUTHORITY, dbOperations);
                    dbOperations.clear();

                    SharedPreferencesUtil.store(R.string.pref_does_fetched_company_already, true);
                    loggedInUser = getContact(SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1));
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                App.gLogger.e(error);
            }
        });
    }

    protected void syncCompanyAsync() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                syncCompany();
            }
        });
    }

    /**
     * Query the db to get a contact given an id. Always returns the
     * latest and greatest local device data.
     *
     * @param contactId, an integer
     * @return a Contact
     */
    protected Contact getContact(Integer contactId) {
        if (database != null) {
            Cursor cursor = database.rawQuery(QUERY_CONTACT_SQL, new String[]{contactId + ""});
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
                    int loggedInContactId = SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1);

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
                    if (App.accountId() > 0) {
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
                            if (pInfo.versionCode > SharedPreferencesUtil.getInteger(R.string.pref_app_previous_version_key, -1)) {
                                SharedPreferencesUtil.store(R.string.pref_app_previous_version_key, pInfo.versionCode);
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
                try {
                    long timestamp = System.currentTimeMillis() * 1000 /* nano */;
                    // GUID
                    final String guid = UUID.randomUUID().toString();
                    //msgValues.put( CompanySQLiteHelper.CLM_ID, null );
                    msgValues.put(MessagesTable.CLM_TIMESTAMP, timestamp);
                    msgValues.put(MessagesTable.CLM_BODY, message);
                    msgValues.put(MessagesTable.CLM_THREAD_ID, threadId);
                    msgValues.put(MessagesTable.CLM_AUTHOR_ID, loggedInUser.id);
                    msgValues.put(MessagesTable.CLM_IS_READ, 1);
                    msgValues.put(MessagesTable.CLM_GUID, guid);
                    msgValues.put(MessagesTable.CLM_ACK, MSG_STATUS_PENDING);
                    getContentResolver().insert(MessageProvider.CONTENT_URI, msgValues);

                    sendMessageToFaye(timestamp, guid, threadId, message);
                } catch (JSONException e) {
                    // Realllllllly shouldn't happen.
                    App.gLogger.e("Severe bug, JSON exception parsing user id array from prefs", e);
                    return;
                }
                Intent newMsgIntent = new Intent(NEW_MSG_ACTION);
                newMsgIntent.putExtra(THREAD_ID_EXTRA, threadId);
                newMsgIntent.putExtra(IS_INCOMING_MSG_EXTRA, false);

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
                Cursor msgCursor = getContentResolver().query(MessageProvider.CONTENT_URI,
                        null, MessagesTable.CLM_GUID + " =?",
                        new String[]{guid}, null);
                if (msgCursor.moveToFirst() && msgCursor.getInt(msgCursor.getColumnIndex(MessagesTable.CLM_ACK)) == MSG_STATUS_PENDING) {
                    ContentValues values = new ContentValues();
                    values.put(MessagesTable.CLM_ACK, MSG_STATUS_FAILED);


                    int rowsUpdated = getContentResolver().update(MessageProvider.CONTENT_URI, values,
                            MessagesTable.CLM_GUID + " =?",
                            new String[] { guid});

//                    int rowsUpdated = database.update(
//                            MessagesTable.TABLE_NAME,
//                            values,
//                            String.format("%s = ?", MessagesTable.CLM_GUID),
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
                Cursor msgCursor = getContentResolver().query(MessageProvider.CONTENT_URI,
                        null, MessagesTable.CLM_GUID + " =?",
                        new String[]{guid}, null);
                if (msgCursor.moveToFirst()) {
                    // Flip back to pending status.
                    ContentValues msgValues = new ContentValues();
                    msgValues.put(MessagesTable.CLM_ACK, MSG_STATUS_PENDING);
                    getContentResolver().update(MessageProvider.CONTENT_URI, msgValues,
                            MessagesTable.CLM_GUID + " =?",
                            new String[] { guid});

//                    database.update(MessagesTable.TABLE_NAME, msgValues, String.format("%s = ?", MessagesTable.CLM_GUID), new String[]{guid});
                    String threadId = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.CLM_THREAD_ID));
                    String body = msgCursor.getString(msgCursor.getColumnIndex(MessagesTable.CLM_BODY));
                    long timestamp = msgCursor.getLong(msgCursor.getColumnIndex(MessagesTable.CLM_TIMESTAMP));
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
        long since = (prefs.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0) - 10000000) / 1000000l; /* 10 seconds of buffer */
        RestService.instance().messaging().getMessages(since + "", new Callback<BadgeThread[]>() {
            @Override
            public void success(BadgeThread[] badgeThreads, Response response) {
                for(BadgeThread bThread: badgeThreads) {
                    upsertThreadAndMessages(bThread, false);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                App.gLogger.e(error);
            }
        });
    }

    /**
     * If thread doesn't exist yet, save it, and any unsaved
     * messages as well.
     * <p/>
     * If message that has been sent to us is one of our pending
     * messages, mark it as acknowledged, broadcast it, and sync
     * timestamp/id with server.
     *
     * @param bThread   A badgeThread Object.
     * @param broadcast if true ,send local broadcast if thread contains new messages, otherwise,
     *                  assume they are historical
     */
    protected void upsertThreadAndMessages(final BadgeThread bThread, final boolean broadcast) {
        long mostRecentMsgTimestamp = prefs.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0);

        // Insert thread into database.
        ContentValues cv = new ContentValues();
        cv.put(MessageThreadsTable.COLUMN_ID, bThread.getId());
        cv.put(MessageThreadsTable.CLM_IS_MUTED, bThread.isMuted());
        cv.put(MessageThreadsTable.CLM_USERS_KEY, userIdArrayToKey(bThread.getUserIds()));
        if (bThread.getName() != null) cv.put(MessageThreadsTable.CLM_NAME, bThread.getName());
        getContentResolver().insert(ThreadProvider.CONTENT_URI, cv);

        // For each user into this thread,
        // put a record into thread_user junction table.
        ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>(bThread.getMessages().length);
        for (int userId : bThread.getUserIds()) {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put(ThreadUserTable.CLM_USER_ID, userId);
            contentValues.put(ThreadUserTable.CLM_THREAD_ID, bThread.getId());
            dbOperations.add(ContentProviderOperation
                    .newInsert(ThreadUserProvider.CONTENT_URI)
                    .withValues(contentValues).build());
        }
        try {
            getContentResolver().applyBatch(ThreadUserProvider.AUTHORITY, dbOperations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        dbOperations.clear();

        for (Message message : bThread.getMessages()) {
            // Insert the message into the database.
            ContentValues contentValues = MessageHelper.toContentValue(message, bThread.getId());
            dbOperations.add(ContentProviderOperation.newInsert(
                    MessageProvider.CONTENT_URI).
                    withValues(contentValues).
                    build());

            //TODO: why nano? those zeros at the end of timestamp
            // does not make it unique number!
            long timestamp = (long) (message.getTimestamp() * 1000000d) /* nanos */;
            if (timestamp > mostRecentMsgTimestamp) {
                mostRecentMsgTimestamp = timestamp;
            }


            // If I am not the author of this message,
            // create a receipt and put into database for further use.
            if (contentValues.getAsInteger(MessagesTable.CLM_AUTHOR_ID) != loggedInUser.id) {
                ContentValues receiptValues = new ContentValues();
                receiptValues.put(ReceiptTable.CLM_MESSAGE_ID, contentValues.getAsString(MessagesTable.CLM_ID));
                receiptValues.put(ReceiptTable.CLM_THREAD_ID, bThread.getId());
                receiptValues.put(ReceiptTable.CLM_USER_ID, loggedInUser.id);
                receiptValues.put(ReceiptTable.COLUMN_SYNC_STATUS, Receipt.NOT_SYNCED);
                getContentResolver().insert(ReceiptProvider.CONTENT_URI, receiptValues);
                // Announce on event-but that we have new message
                EventBus.getDefault().post(new NewMessageEvent(bThread.getId(),
                        contentValues.getAsString(MessagesTable.CLM_ID)));
            }
        }
        try {
            getContentResolver().applyBatch(MessageProvider.AUTHORITY, dbOperations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }


        // If I'm honest, this switch isn't intended to be used this way,
        // but the idea here is only update the timestamp on history sync
        // so that all messages will eventually be dl'd no matter what.
        if (!broadcast) {
            prefs.edit().putLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, mostRecentMsgTimestamp).commit();
        }

        // Get id of most recent msg.
        Cursor messages = getContentResolver().query(MessageProvider.CONTENT_URI, null,
                MessagesTable.CLM_THREAD_ID + "=?",
                new String[]{bThread.getId()},
                MessagesTable.CLM_TIMESTAMP + " ASC");
        if (messages.moveToLast()) {
            String mostRecentGuid = messages.getString(messages.getColumnIndex(MessagesTable.CLM_GUID));
            final String mostRecentId = messages.getString(messages.getColumnIndex(MessagesTable.CLM_ID));
            if ("Inf".equals(mostRecentGuid)) {
                // Dang! Crash the app to get a report.
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException("Crashing app. Couldn't set head of thread " + bThread.getId()
                                + " because message guid came back 'Inf' message id is " + mostRecentId);
                    }
                });
            }
            messages.close();

        } else {
            messages.close();
        }
    }

    /**
     * SYNCHRONOUSLY creates a thread using the REST badge api.
     * <strong>ONLY CALL THIS FROM A BACKGROUND TASK</strong>
     *
     * @param recipientIds
     * @return
     */
    protected String createThreadSync(final Integer[] recipientIds) throws JSONException, IOException, RemoteException, OperationApplicationException {
        String threadKey = userIdArrayToKey(recipientIds);
        Cursor cursor = getContentResolver().query(ThreadProvider.CONTENT_URI,
                new String[]{MessageThreadsTable.COLUMN_ID},
                MessageThreadsTable.CLM_USERS_KEY + "=?",
                new String[]{threadKey},
                null);
        if (cursor.moveToFirst()) {
            return cursor.getString(0);
        } else {
            JSONObject postBody = new JSONObject();
            JSONObject messageThread = new JSONObject();
            JSONArray userIds = new JSONArray();
            postBody.put("message_thread", messageThread);
            for (int i : recipientIds) {
                userIds.put(i);
            }
            messageThread.put("user_ids", userIds);
            TypedJsonString typedJsonString = new TypedJsonString(postBody.toString());
            BadgeThread resultThread = RestService.instance().messaging().createMessageThread(typedJsonString);
            if (resultThread != null) {
                ContentValues cv = new ContentValues();
                cv.put(MessageThreadsTable.COLUMN_ID, resultThread.getId());
                cv.put(MessageThreadsTable.CLM_USERS_KEY, threadKey);
                cv.put(MessageThreadsTable.CLM_IS_MUTED, false);
                cv.put(MessageThreadsTable.CLM_NAME, createThreadName(recipientIds));
                getContentResolver().insert(ThreadProvider.CONTENT_URI, cv);

                ArrayList<ContentProviderOperation> dbOperations =
                        new ArrayList<ContentProviderOperation>(resultThread.getUserIds().length);
                for (int participantId : resultThread.getUserIds()) {
                    dbOperations.add(ContentProviderOperation.newInsert(
                            ThreadUserProvider.CONTENT_URI)
                            .withValue(ThreadUserTable.CLM_THREAD_ID, resultThread.getId())
                            .withValue(ThreadUserTable.CLM_USER_ID, participantId)
                            .build());
                }
                getContentResolver().applyBatch(ThreadUserProvider.AUTHORITY, dbOperations);
                return resultThread.getId();
            } else {
                throw new IOException("Problem with creating thread due to network issue");
            }
        }
    }

    private String createThreadName(Integer[] recipientIds) {
        if (recipientIds.length == 2) {
            String[] name = new String[2];
            for (int userId : recipientIds) {
                if (userId == App.accountId()) continue;
                name = UserHelper.getUserName(this, recipientIds[0]);
            }
            return String.format("%s %s", name[0], name[1]);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int userId : recipientIds) {
                if (userId == App.accountId()) continue;
                String firstName = UserHelper.getUserName(this, userId)[0];
                if (firstName != null) {
                    stringBuilder.append(firstName).append(", ");
                }
            }
            if (stringBuilder.length() > 2) {
                stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Sort the ids in the json array and join them with a comma.
     *
     * @param userIdArr json array of user ids
     * @return a comma delimited string of sorted ids from userIdArr
     */
    private static String userIdArrayToKey(Integer[] userIdArr) {
        Arrays.sort(userIdArr);
        StringBuilder delimString = new StringBuilder();
        String delim = "";
        for (int userId : userIdArr) {
            delimString.append(delim).append(userId);
            delim = ",";
        }
        return delimString.toString();
    }

    /**
     * Construct JSONObject of user data to send with Mixpanel event tracking
     */
    protected JSONObject getBasicMixpanelData() {
        return new JSONObject();
    }

    public void onEvent(LogedinSuccessfully logedinSuccessfully) {
        loggedInUser = getContact(SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1));
        syncCompanyAsync();
        syncMessagesAsync();
    }

    public void onEvent(UpdateAccountEvent updateAccountEvent) {
        loggedInUser = getContact(SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1));
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
         * Reports whether the database is initialized and ready to return data.
         *
         * @return true if data is available
         */
        public boolean isInitialized() {
            return initialized;
        }

        /**
         * @return null of not logged in, contact representing user acct otherwise.
         */
        public Contact getLoggedInUser() {
            return loggedInUser;
        }


        /**
         * @see DataProviderService#getBasicMixpanelData() (int)
         */
        public JSONObject getBasicMixpanelData() {
            return DataProviderService.this.getBasicMixpanelData();
        }

        public void refreshContact(int contactId) {
            DataProviderService.this.getSingleContact(contactId);
        }

        /**
         * @see DataProviderService#upsertThreadAndMessages(com.triaged.badge.models.BadgeThread, boolean)
         */
        public void upsertThreadAndMessagesAsync(final BadgeThread badgeThread) {
            sqlThread.submit(new Runnable() {
                @Override
                public void run() {
                    DataProviderService.this.upsertThreadAndMessages(badgeThread, true);
                }
            });
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
        public String createThreadSync(Integer[] userIds) throws JSONException, IOException, RemoteException, OperationApplicationException {
            return DataProviderService.this.createThreadSync(userIds);
        }

        /**
         *
         * @param threadId
         * @return
         */
        public String getRecipientNames(String threadId) {
            Cursor cursor = getContentResolver().query(ThreadUserProvider.CONTENT_URI_CONTACT_INFO,
                    null, ThreadUserTable.CLM_THREAD_ID + "=?",
                    new String[]{threadId},
                    null);
            StringBuilder names = new StringBuilder();
            if (cursor.moveToFirst()) {
                do {
                    names.append(cursor.getString(cursor.getColumnIndexOrThrow(ContactsTable.CLM_FIRST_NAME))).append(",");
                } while (cursor.moveToNext());
                // Delete the last comma
                names.deleteCharAt(names.length() - 1);
            }
            return names.toString();
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
         * @see DataProviderService#partialSyncContactsAsync()
         */
        public void partialSyncContactsAsync() {
            DataProviderService.this.partialSyncContactsAsync();
        }

    }

}
