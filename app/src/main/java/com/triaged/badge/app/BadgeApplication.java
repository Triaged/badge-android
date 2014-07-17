package com.triaged.badge.app;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Custom implementation of the Android Application class that sets up global services and
 * plugins such as Google Analytics and Crashlytics.
 *
 * Created by Will on 7/7/14.
 */
public class BadgeApplication extends Application {

    private static final String TAG = BadgeApplication.class.getName();
    protected static final String INSTALLED_VERSION_PREFS_KEY = "installedAppVersion";

    public volatile DataProviderService.LocalBinding dataProviderServiceBinding = null;
    public ServiceConnection dataProviderServiceConnnection = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // Start service on app install/upgrade.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Has this version of the app ever been installed?
                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    if( pInfo.versionCode > prefs.getInt( INSTALLED_VERSION_PREFS_KEY, -1 ) ) {
                        prefs.edit().putInt(INSTALLED_VERSION_PREFS_KEY, pInfo.versionCode ).commit();
                        if( prefs.getBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ) ) {
                            startService( new Intent( getApplicationContext(), LocationTrackingService.class ) );
                        }
                    }
                }
                catch( PackageManager.NameNotFoundException e ) {
                    // Look at all the fucks I give!
                }

                return null;
            }
        }.execute();

        dataProviderServiceConnnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "SERVICE CONNECTED YAAAAY");
                dataProviderServiceBinding = (DataProviderService.LocalBinding) service;
                dataProviderServiceBinding.initDatabase();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "SERVICE DISCONNECTED BOOOOOO");
            }
        };

        if (!bindService(new Intent(this, DataProviderService.class), dataProviderServiceConnnection, BIND_AUTO_CREATE)) {
            Log.e( "SEVERE", "Couldn't bind to data provider service." );
            unbindService(dataProviderServiceConnnection);
        }
    }

    @Override
    public void onTerminate() {
        unbindService(dataProviderServiceConnnection);
        super.onTerminate();
    }
}
