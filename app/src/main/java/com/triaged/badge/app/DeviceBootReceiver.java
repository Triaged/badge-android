package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Fires when the app starts and starts tracking COARSE
 * location updates INFREQUENTLY to avoid burning battery.
 *
 * If the user opts out, it won't start the service.
 *
 * @author Created by jc on 7/17/14.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        if( prefs.getBoolean( LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true ) && !"".equals( prefs.getString( DataProviderService.API_TOKEN_PREFS_KEY, "" ) ) ) {
            context.startService( new Intent( context, LocationTrackingService.class ) );
        }
    }
}
