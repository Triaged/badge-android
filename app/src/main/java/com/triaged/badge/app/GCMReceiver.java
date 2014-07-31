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
        // Show notification
        String message = intent.getStringExtra( "message" );
        String threadId = intent.getStringExtra( "thread_id" );
        Notification.Builder mBuilder =
                new Notification.Builder( context )
                        // TODO we need the real icon
                        .setSmallIcon(R.drawable.ic_action_person)
                        .setContentTitle( "New Badge message" )
                        .setContentText( message )
                        .setAutoCancel( true );

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MessageShowActivity.class);
        resultIntent.putExtra( MessageShowActivity.THREAD_ID_EXTRA, threadId );
        PendingIntent resultPendingIntent = PendingIntent.getActivity( context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId++, mBuilder.build());
    }
}
