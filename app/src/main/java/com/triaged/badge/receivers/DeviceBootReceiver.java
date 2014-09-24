package com.triaged.badge.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.triaged.badge.app.R;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.badge.net.DataProviderService;
import com.triaged.utils.SharedPreferencesUtil;

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
        if (SharedPreferencesUtil.getBoolean(LocationTrackingService.TRACK_LOCATION_PREFS_KEY, true) &&
                !"".equals(SharedPreferencesUtil.getString(R.string.pref_api_token, ""))) {
            LocationTrackingService.scheduleAlarm(context);
        }
    }
}
