package com.triaged.badge.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Central repository so that different code paths
 * can notify using the same module.
 *
 * @author Created by jc on 8/1/14.
 */
public class Notifier {
    private static final int NOTIFICATION_ID = 9329203; // Nice round number
    private static final String MESSAGE_COUNT_PREFS_KEY = "numMessages";
    private static final String MESSAGE_SENDERS_PREFS_KEY = "messageSenders";

    private static SharedPreferences prefs = null;

    public static void newNotification( Context context, String from, String msg, String threadId ) {
        if( prefs == null ) {
            prefs = PreferenceManager.getDefaultSharedPreferences( context );
        }

        int numMessages = prefs.getInt( MESSAGE_COUNT_PREFS_KEY, 0 ) + 1;
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt( MESSAGE_COUNT_PREFS_KEY, numMessages );
        Set<String> senderNames = prefs.getStringSet( MESSAGE_SENDERS_PREFS_KEY, new HashSet<String>() );
        senderNames.add( from );
        prefsEditor.putStringSet( MESSAGE_SENDERS_PREFS_KEY, senderNames );
        if( numMessages == 1 ) {
            Notification.Builder mBuilder =
                    new Notification.Builder(context)
                            // TODO we need the real icon
                            .setSmallIcon(R.drawable.ic_action_person)
                            .setContentTitle("New message from " + from )
                            .setContentText(msg)
                            .setAutoCancel(true);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(context, MessageShowActivity.class);
            resultIntent.putExtra(MessageShowActivity.THREAD_ID_EXTRA, threadId);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
        else {
            StringBuilder namesBuilder = new StringBuilder();
            String delim = "";
            for( String name : senderNames ) {
                namesBuilder.append( delim ).append( name );
                delim = ", ";
            }
            Notification.Builder mBuilder =
                    new Notification.Builder(context)
                            // TODO we need the real icon
                            .setSmallIcon(R.drawable.ic_action_person)
                            .setContentTitle( String.format( "%d new messages", numMessages ) )
                            .setContentText( namesBuilder.toString() )
                            .setAutoCancel(true);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(context, MessagesIndexActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // We're always updating one consistent notification id. We'll never show more than one.
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        }
        prefsEditor.commit();

    }

    public static void clearNotifications( Context context ) {
        if( prefs == null ) {
            prefs = PreferenceManager.getDefaultSharedPreferences( context );
        }

        prefs.edit().remove(MESSAGE_COUNT_PREFS_KEY ).remove( MESSAGE_SENDERS_PREFS_KEY ).commit();
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel( NOTIFICATION_ID );
    }
}
