package com.triaged.badge.ui.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.triaged.badge.app.App;
import com.triaged.badge.app.R;
import com.triaged.badge.net.DataProviderService;
import com.triaged.badge.ui.notification.Notifier;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesUtil;

import java.io.IOException;

/**
 * All app activities should inherit from this super
 * class, which implements global functionality.
 *
 * @author Created by Will on 7/7/14.
 */
public abstract class BadgeActivity extends MixpanelActivity {

    protected static final String TAG = BadgeActivity.class.getName();

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    protected static int myUserId = SharedPreferencesUtil.getInteger(R.string.pref_my_user_id_key, -1);

    /**
     * Badge Sender ID. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    private static final String SENDER_ID = "466421541427";

    protected static final String ONBOARDING_FINISHED_ACTION = "onboardingFinished";

    protected LocalBroadcastManager localBroadcastManager;
    protected BroadcastReceiver databaseReadyReceiver;
    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    private BroadcastReceiver logoutListener;
    private BroadcastReceiver newMessageReceiver;
    IntentFilter newMessageIntentFilter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DataProviderService.LOGGED_OUT_ACTION);
        logoutListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                logout();
            }
        };

        localBroadcastManager.registerReceiver(logoutListener, intentFilter);

        newMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyNewMessage(intent);
            }
        };
        newMessageIntentFilter = new IntentFilter(DataProviderService.NEW_MSG_ACTION);

        databaseReadyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DataProviderService.DB_AVAILABLE_ACTION)) {
                    dataProviderServiceBinding = ((App) getApplication()).dataProviderServiceBinding;
                    onDatabaseReady();
                } else if (intent.getAction().equals(DataProviderService.DB_UPDATED_ACTION)) {
                    onDatabaseUpdated();
                }
            }
        };
        IntentFilter dbActionFilter = new IntentFilter(DataProviderService.DB_AVAILABLE_ACTION);
        dbActionFilter.addAction(DataProviderService.DB_UPDATED_ACTION);
        localBroadcastManager.registerReceiver(databaseReadyReceiver, dbActionFilter);
        dataProviderServiceBinding = ((App) getApplication()).dataProviderServiceBinding;
        if (dataProviderServiceBinding != null) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    onDatabaseReady();
                }
            });

        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(databaseReadyReceiver);
        localBroadcastManager.unregisterReceiver(logoutListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        localBroadcastManager.registerReceiver(newMessageReceiver, newMessageIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(newMessageReceiver);
    }

    /**
     * Implement this method to do any activity set up
     * that requires access to badge data.
     * <p/>
     * Do not attempt to access {@link #dataProviderServiceBinding}
     * until this has been called.
     * <p/>
     * This is always invoked after {@link android.app.Activity#onCreate(android.os.Bundle)}
     * <p/>
     * Default impl does nothing.
     */
    protected void onDatabaseReady() {
    }

    /**
     * Default impl does nothing. Override to be notified
     * when the database has been re-synced.
     */
    protected void onDatabaseUpdated() {

    }

    /**
     * Override this function to change behavior when
     * a message is received, default behavior is to notify
     * in the status bar and play alert/vibrate.
     *
     * @param intent
     */
    protected void notifyNewMessage(Intent intent) {
        if (intent.getBooleanExtra(DataProviderService.IS_INCOMING_MSG_EXTRA, false)) {
            String threadId = intent.getStringExtra(DataProviderService.THREAD_ID_EXTRA);
            String from = intent.getStringExtra(DataProviderService.MESSAGE_FROM_EXTRA);
            String message = intent.getStringExtra(DataProviderService.MESSAGE_BODY_EXTRA);
            Notifier.newNotification(this, from, message, threadId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Override this method in the activity impl to change activity
     * behavior when a logged out state is detected.
     * <p/>
     * Default behavior is to close the activity.
     */
    protected void logout() {
        finish();
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    protected String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            App.gLogger.i("Registration not found!!");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = GeneralUtils.getAppVersionCode(context);
        if (registeredVersion != currentVersion) {
            App.gLogger.i("App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(this.getClass().getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {

                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BadgeActivity.this);
                    String regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    Log.d(TAG, msg);

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(BadgeActivity.this, regid);
                    // Send it up to the api.
                    ((App) getApplication()).dataProviderServiceBinding.registerDevice(regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                // mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId   registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = GeneralUtils.getAppVersionCode(context);
        App.gLogger.i("Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * It registers for GCM through the app and tries to email the
     * reg id to founders@startupgiraffe.com, if it hasn't already been
     * registered.
     */
    protected void ensureGcmRegistration() {
        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            String regid = getRegistrationId(this);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            // TODO should probably disable the UI, but this isn't a priority
            App.gLogger.i("No valid Google Play Services APK found.");
        }

    }
}
