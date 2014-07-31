package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Push notifications are sent when the app
 * isn't in the foreground and a msg is received.
 *
 * @author Created by jc on 7/31/14.
 */
public class GCMReceiver extends BroadcastReceiver {
    @Override
    public void onReceive( final Context context, Intent intent) {
        // Show notification


    }
}
