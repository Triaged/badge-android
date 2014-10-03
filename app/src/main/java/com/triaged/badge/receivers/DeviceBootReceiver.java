package com.triaged.badge.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.triaged.badge.app.R;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.utils.SharedPreferencesHelper;

/**
 * Fires when the app starts and starts tracking COARSE
 * location updates INFREQUENTLY to avoid burning battery.
 * <p/>
 * If the user opts out, it won't start the service.
 *
 * @author Created by jc on 7/17/14.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (SharedPreferencesHelper.instance().getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true) &&
                !"".equals(SharedPreferencesHelper.instance().getString(R.string.pref_api_token, ""))) {
            LocationTrackingService.scheduleAlarm(context);
        }
    }
}
