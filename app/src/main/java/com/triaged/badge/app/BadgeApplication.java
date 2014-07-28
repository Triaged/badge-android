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

import com.crashlytics.android.Crashlytics;
import com.google.common.eventbus.Subscribe;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.codeweaver.faye.BusProvider;
import org.codeweaver.faye.Client;
import org.codeweaver.faye.event.ConnectedEvent;
import org.json.JSONObject;

/**
 * Custom implementation of the Android Application class that sets up global services and
 * plugins such as Google Analytics and Crashlytics.
 *
 * Created by Will on 7/7/14.
 */
public class BadgeApplication extends Application {

    private static final String TAG = BadgeApplication.class.getName();
    public static final String MIXPANEL_TOKEN = "b9c753b3560536492eba971a53213f5f";

    public volatile DataProviderService.LocalBinding dataProviderServiceBinding = null;
    public ServiceConnection dataProviderServiceConnnection = null;

    public Foreground appForeground;
    public Foreground.Listener foregroundListener;


    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.start(this);
        BusProvider.getInstance().register(this);

        final Intent fayeServiceIntent = new Intent( getApplicationContext(), FayeService.class );
        appForeground = Foreground.get(this);
        foregroundListener = new Foreground.Listener() {
            @Override
            public void onBecameForeground() {
                MixpanelAPI mixpanelAPI = MixpanelAPI.getInstance(BadgeApplication.this, MIXPANEL_TOKEN);
                JSONObject props = new JSONObject();
                mixpanelAPI.track("appForeground", props);
                mixpanelAPI.flush();
                startService( fayeServiceIntent );
            }

            @Override
            public void onBecameBackground() {
                stopService( fayeServiceIntent );
            }
        };
        appForeground.addListener(foregroundListener);

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
        appForeground.removeListener(foregroundListener);
        super.onTerminate();
    }
}
