package com.triaged.badge.net;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.database.helper.DepartmentHelper;
import com.triaged.badge.database.helper.MessageHelper;
import com.triaged.badge.database.helper.OfficeLocationHelper;
import com.triaged.badge.database.helper.UserHelper;
import com.triaged.badge.database.provider.BThreadProvider;
import com.triaged.badge.database.provider.DepartmentProvider;
import com.triaged.badge.database.provider.MessageProvider;
import com.triaged.badge.database.provider.OfficeLocationProvider;
import com.triaged.badge.database.provider.ReceiptProvider;
import com.triaged.badge.database.provider.ThreadUserProvider;
import com.triaged.badge.database.provider.UserProvider;
import com.triaged.badge.database.table.BThreadUserTable;
import com.triaged.badge.database.table.BThreadsTable;
import com.triaged.badge.database.table.DepartmentsTable;
import com.triaged.badge.database.table.MessagesTable;
import com.triaged.badge.database.table.ReceiptTable;
import com.triaged.badge.events.NewMessageEvent;
import com.triaged.badge.events.UpdateAccountEvent;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.models.BThread;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

    public static final String MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY = "latestMsgTimestampPrefsKey";

    public static final String LOGGED_OUT_ACTION = "com.triage.badge.LOGGED_OUT";

    public static final String MESSAGE_BODY_EXTRA = "messageBody";
    public static final String MESSAGE_FROM_EXTRA = "messageFrom";

    protected volatile Contact loggedInUser;
    protected ScheduledExecutorService sqlThread;
    protected long lastSynced;
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

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        lastSynced = SharedPreferencesUtil.getLong(R.string.pref_last_sync_key, 0);
        sqlThread = Executors.newSingleThreadScheduledExecutor();
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
                getContentResolver().insert(UserProvider.CONTENT_URI, UserHelper.toContentValue(user));
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
        if (lastSynced > System.currentTimeMillis() - 120000 ) {
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
                            UserProvider.CONTENT_URI).withValues(UserHelper.toContentValue(user)).build());
                }
                try {
                    getContentResolver().applyBatch(UserProvider.AUTHORITY, dbOperations);
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
        try {
            Company company = RestService.instance().badge().getCompany("0");
//            getContentResolver().delete(ContactProvider.CONTENT_URI, null, null);
            getContentResolver().delete(DepartmentProvider.CONTENT_URI, null, null);
            getContentResolver().delete(OfficeLocationProvider.CONTENT_URI, null, null);
            try {
                ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>();
                for (User user : company.getUsers()) {
                    dbOperations.add(ContentProviderOperation.newInsert(
                            UserProvider.CONTENT_URI).withValues(UserHelper.toContentValue(user)).build());
                }
                getContentResolver().applyBatch(UserProvider.AUTHORITY, dbOperations);
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

        } catch (RetrofitError e) {
            App.gLogger.e(e);
        }
    }

    /**
     * Query the db to get a contact given an id. Always returns the
     * latest and greatest local device data.
     *
     * @param contactId, an integer
     * @return a Contact
     */
    protected Contact getContact(Integer contactId) {
       Cursor cursor = getContentResolver().query(
               ContentUris.withAppendedId(UserProvider.CONTENT_URI_FULL_INFO, contactId ),
               null, null, null, null);
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
     */
    protected void initDatabase() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int loggedInContactId = SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1);

                    if (loggedInContactId > 0) {
                        // If we're logged in, and the database isn't empty, the UI should be ready to go.
                        if ((loggedInUser = getContact(loggedInContactId)) != null) {
                            initialized = true;
                        }
                    } else {
                        // If there's no logged in user, nothing else will happen so we're done here.
                        initialized = true;
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
                        }

                        // Check if this is the first boot of a new install
                        // If it is, since we're logged in, if the user hasnt
                        // disabled location tracking, start the tracking service.
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            if (pInfo.versionCode > SharedPreferencesUtil.getInteger(R.string.pref_app_previous_version_key, -1)) {
                                SharedPreferencesUtil.store(R.string.pref_app_previous_version_key, pInfo.versionCode);
                                if (SharedPreferencesUtil.getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true)) {
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

    protected void syncMessagesSync() {
        long since = (SharedPreferencesUtil.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0) - 10000000) / 1000000l; /* 10 seconds of buffer */
        try {
            BThread[] bThreads = RestService.instance().messaging().getMessages(since + "");
            for (BThread bThread : bThreads) {
                upsertThreadAndMessages(bThread, false);
            }
        } catch (RetrofitError error) {
            App.gLogger.e(error);
        }
    }

    protected void syncMessagesAsync() {
        sqlThread.submit(new Runnable() {
            @Override
            public void run() {
                syncMessagesSync();
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
    protected void upsertThreadAndMessages(final BThread bThread, final boolean broadcast) {
        long mostRecentMsgTimestamp = SharedPreferencesUtil.getLong(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, 0);

        // Insert thread into database.
        ContentValues cv = new ContentValues();
        cv.put(BThreadsTable.COLUMN_ID, bThread.getId());
        cv.put(BThreadsTable.CLM_IS_MUTED, bThread.isMuted());
        cv.put(BThreadsTable.CLM_USERS_KEY, userIdArrayToKey(bThread.getUserIds()));
        if (bThread.getUserIds().length == 2) {
            cv.put(BThreadsTable.CLM_NAME, createThreadName(bThread.getUserIds()));
        } else if (bThread.getName() != null){
            cv.put(BThreadsTable.CLM_NAME, bThread.getName());
        }
        getContentResolver().insert(BThreadProvider.CONTENT_URI, cv);

        // For each user into this thread,
        // put a record into thread_user junction table.
        ArrayList<ContentProviderOperation> dbOperations = new ArrayList<ContentProviderOperation>(bThread.getMessages().length);
        for (int userId : bThread.getUserIds()) {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put(BThreadUserTable.CLM_USER_ID, userId);
            contentValues.put(BThreadUserTable.CLM_THREAD_ID, bThread.getId());
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
            SharedPreferencesUtil.store(MOST_RECENT_MSG_TIMESTAMP_PREFS_KEY, mostRecentMsgTimestamp);
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
    protected String createThreadSync(final Integer[] recipientIds) throws JSONException,  RemoteException, OperationApplicationException {
        String threadKey = userIdArrayToKey(recipientIds);
        Cursor cursor = getContentResolver().query(BThreadProvider.CONTENT_URI,
                new String[]{BThreadsTable.COLUMN_ID},
                BThreadsTable.CLM_USERS_KEY + "=?",
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
            BThread resultThread;
            resultThread = RestService.instance().messaging().createMessageThread(typedJsonString);
            ContentValues cv = new ContentValues();
            cv.put(BThreadsTable.COLUMN_ID, resultThread.getId());
            cv.put(BThreadsTable.CLM_USERS_KEY, threadKey);
            cv.put(BThreadsTable.CLM_IS_MUTED, false);
            cv.put(BThreadsTable.CLM_NAME, createThreadName(recipientIds));
            getContentResolver().insert(BThreadProvider.CONTENT_URI, cv);

            ArrayList<ContentProviderOperation> dbOperations =
                    new ArrayList<ContentProviderOperation>(resultThread.getUserIds().length);
            for (int participantId : resultThread.getUserIds()) {
                dbOperations.add(ContentProviderOperation.newInsert(
                        ThreadUserProvider.CONTENT_URI)
                        .withValue(BThreadUserTable.CLM_THREAD_ID, resultThread.getId())
                        .withValue(BThreadUserTable.CLM_USER_ID, participantId)
                        .build());
            }
            getContentResolver().applyBatch(ThreadUserProvider.AUTHORITY, dbOperations);
            return resultThread.getId();
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
         * @see DataProviderService#upsertThreadAndMessages(com.triaged.badge.models.BThread, boolean)
         */
        public void upsertThreadAndMessagesAsync(final BThread bThread) {
            sqlThread.submit(new Runnable() {
                @Override
                public void run() {
                    DataProviderService.this.upsertThreadAndMessages(bThread, true);
                }
            });
        }

        /**
         * @see DataProviderService#createThreadSync(Integer[])
         */
        public String createThreadSync(Integer[] userIds) throws JSONException, RemoteException, OperationApplicationException {
            return DataProviderService.this.createThreadSync(userIds);
        }

        /**
         * @see DataProviderService#syncMessagesAsync()
         */
        public void syncMessagesAsync() {
            DataProviderService.this.syncMessagesAsync();
        }

        /**
         * @see DataProviderService#partialSyncContactsAsync()
         */
        public void partialSyncContactsAsync() {
            DataProviderService.this.partialSyncContactsAsync();
        }

    }

}
