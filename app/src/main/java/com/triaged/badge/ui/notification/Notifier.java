package com.triaged.badge.ui.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.triaged.badge.app.R;
import com.triaged.badge.ui.home.MainActivity;
import com.triaged.badge.ui.messaging.MessagingActivity;
import com.triaged.utils.SharedPreferencesUtil;

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

    public static void newNotification(Context context, String from, String msg, String threadId) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        if (SharedPreferencesUtil.getBoolean("is_mute_" + threadId, false)) {
            return;
        }

        int numMessages = prefs.getInt(MESSAGE_COUNT_PREFS_KEY, 0) + 1;
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt(MESSAGE_COUNT_PREFS_KEY, numMessages);
        Set<String> senderNames = prefs.getStringSet(MESSAGE_SENDERS_PREFS_KEY, new HashSet<String>());
        senderNames.add(from);
        prefsEditor.putStringSet(MESSAGE_SENDERS_PREFS_KEY, senderNames);
        //Define sound URI
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (numMessages == 1) {
            Notification.Builder mBuilder =
                    new Notification.Builder(context)
                            // TODO we need the real icon
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("New message from " + from)
                            .setContentText(msg)
                            .setSound(soundUri)
                            .setAutoCancel(true);

            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(context, MessagingActivity.class);
            resultIntent.putExtra(MessagingActivity.THREAD_ID_EXTRA, threadId);
            resultIntent.putExtra(MessagingActivity.THREAD_NAME_EXTRA , from);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            if (android.os.Build.VERSION.SDK_INT > 15) {
                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            } else {
                @SuppressWarnings("deprecation")
                Notification notification = mBuilder.getNotification();
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }
        } else {
            StringBuilder namesBuilder = new StringBuilder();
            String delim = "";
            for (String name : senderNames) {
                namesBuilder.append(delim).append(name);
                delim = ", ";
            }
            Notification.Builder mBuilder =
                    new Notification.Builder(context)
                            // TODO we need the real icon
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(String.format("%d new messages", numMessages))
                            .setContentText(namesBuilder.toString())
                            .setSound(soundUri)
                            .setAutoCancel(true);

            // Creates an explicit intent for an Activity in your app
            //TODO should open messages list fragment,
            // or it could be better to open the exact thread.
            Intent resultIntent = new Intent(context, MainActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // We're always updating one consistent notification id. We'll never show more than one.


            if (android.os.Build.VERSION.SDK_INT > 15) {
                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            } else {
                @SuppressWarnings("deprecation")
                Notification notification = mBuilder.getNotification();
                mNotificationManager.notify(NOTIFICATION_ID, notification);
            }

        }
        prefsEditor.commit();

    }

    public static void clearNotifications(Context context) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        prefs.edit().remove(MESSAGE_COUNT_PREFS_KEY).remove(MESSAGE_SENDERS_PREFS_KEY).commit();
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}
