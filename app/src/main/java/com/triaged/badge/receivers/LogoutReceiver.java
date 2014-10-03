package com.triaged.badge.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.triaged.badge.app.App;
import com.triaged.badge.database.DatabaseHelper;
import com.triaged.badge.location.LocationTrackingService;
import com.triaged.utils.GeneralUtils;
import com.triaged.utils.SharedPreferencesHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Handle a logout request.
 * <p/>
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class LogoutReceiver extends BroadcastReceiver {

    public static final String ACTION_LOGOUT = "com.triaged.badge.LOGOUT";
    public static final String RESTART_APP_EXTRA = "restart_app_extra";

    public LogoutReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO: need to send check out of office request to the server
        //TODO: need to send unregister request to the server

        LocationTrackingService.justClearAlarm(context);
        // Stop running services
        GeneralUtils.stopAllRunningServices(context);

        // Clear application data
        SharedPreferencesHelper.instance().clearSharedPref();
        DatabaseHelper.deleteDatabase();
        MixpanelAPI.getInstance(context, App.MIXPANEL_TOKEN).clearSuperProperties();

//        // Schedule to start application if needed.
//        Bundle bundle = intent.getExtras();
//        if (bundle != null) {
//            if (bundle.getBoolean(RESTART_APP_EXTRA)) {
//                Intent loginIntent = new Intent(context, OnboardingActivity.class);
//                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(loginIntent);
//            }
//        }
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
               System.exit(0);
            }
        }, 1000);
    }

}
