package com.triaged.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.inputmethod.InputMethodManager;

import com.triaged.badge.app.App;

import java.util.List;

/**
 * Provides some general utility methods for android.
 *
 * Created by Sadegh Kazemy on 9/7/14.
 */
public class GeneralUtils {

    private GeneralUtils() {
    }

    /**
     * Determine whether the device is connected to a network or not.
     *
     * @param context the context its caller.
     * @return true if the device is connected or connecting to a network.
     */
    public static boolean hasAvailableNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    /**
     * Dismiss the virtual keyboard if it is visible.
     *
     * @param activity the activity which is having the keyboard.
     */
    public static void dismissKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (activity.getWindow().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(activity.getWindow().getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Exit the app and start it again.
     *
     * @param context       Application context.
     * @param delayMilliSec Delay time with want to start application in milli seconds.
     */
    public static void scheduleForStart(Context context, int delayMilliSec) {
        if (delayMilliSec < 100) {
            delayMilliSec = 100;
        }

        Intent restartIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        PendingIntent intent = PendingIntent.getActivity(
                context, 0,
                restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delayMilliSec, intent);
    }


    /**
     * Stops all the running service for the current application
     *
     * @param context
     */
    public static void stopAllRunningServices(Context context) {

        ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo>
                runningServiceInfoList = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : runningServiceInfoList) {
            String serviceName = runningServiceInfo.service.getClassName();
            //TODO: should use context.getPackageName(),
            // but since the package in manifest is not well formatted, it ok for now.
            if (serviceName.contains("com.triaged.badge")) {
                try {
                    Class serviceClass = Class.forName(serviceName);
                    context.stopService(new Intent(context, serviceClass));
                } catch (ClassNotFoundException e) {
                    App.gLogger.e(e);
                }
            }
        }


    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static String getAppVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }


    /**
     * Opens a url with Android default browser
     * @param context Application context.
     * @param url The website address we want to open.
     */
    public static void openWebsite(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        i.setData(Uri.parse(url));
        context.startActivity(i);
    }
}
