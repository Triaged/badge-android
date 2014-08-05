package com.triaged.badge.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Push notifications are sent when the app
 * isn't in the foreground and a msg is received.
 *
 * @author Created by jc on 7/31/14.
 */
public class GCMReceiver extends BroadcastReceiver {
    private static int mId = 1;

    @Override
    public void onReceive( final Context context, Intent intent) {
        // Log.i(GCMReceiver.class.getName(), "New GCM message: " + intent.getExtras().toString());
        // Show notification
        String message = intent.getStringExtra( "message" );
        String threadId = intent.getStringExtra( "thread_id" );
        String from = intent.getStringExtra( "author_name" );
        Notifier.newNotification( context, from, message, threadId );
    }
}
