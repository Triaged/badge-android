package com.triaged.badge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Push notifications are sent when the app
 * isn't in the foreground and a msg is received.
 *
 * Sync requests are sent as broadcasts.
 *
 * @author Created by jc on 7/31/14.
 */
public class GCMReceiver extends BroadcastReceiver {

    private static int mId = 1;
    public static final String SYNC_GCM_RECEIVED = "com.triage.badge.SYNC_GCM_RECEIVED";
    public static final String SYNC_GCM_DATA_TYPE_KEY = "data_type";
    public static final String SYNC_GCM_DATA_ID_KEY = "data_id";

    @Override
    public void onReceive( final Context context, Intent intent) {

        // If a message is received, display a notification
        String message = intent.getStringExtra( "message" );
        if (message != null) {
            String threadId = intent.getStringExtra( "thread_id" );
            String from = intent.getStringExtra( "author_name" );
            Notifier.newNotification( context, from, message, threadId );
        }

        // If a sync type is received, send a broadcast with the data
        String syncType = intent.getStringExtra( "type" );
        if (syncType != null) {
            // Send broadcast to DataProviderService
            int syncId = Integer.parseInt(intent.getStringExtra("id"));
            Intent broadcastIntent = new Intent(SYNC_GCM_RECEIVED);
            broadcastIntent.putExtra(SYNC_GCM_DATA_TYPE_KEY, syncType);
            broadcastIntent.putExtra(SYNC_GCM_DATA_ID_KEY, syncId);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }

    }
}
