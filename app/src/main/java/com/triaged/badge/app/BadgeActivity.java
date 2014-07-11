package com.triaged.badge.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * All app activities should inherit from this super
 * class, which implements global functionality.
 *
 * @author Created by Will on 7/7/14.
 */
public abstract class BadgeActivity extends Activity {

    protected LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver logoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        localBroadcastManager = LocalBroadcastManager.getInstance( this );
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction( DataProviderService.LOGGED_OUT_ACTION );
        logoutListener =  new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                logout();
            }
        };
        localBroadcastManager.registerReceiver( logoutListener, intentFilter );
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver( logoutListener );
    }

    /**
     * Override this method in the activity impl to change activity
     * behavior when a logged out state is detected.
     *
     * Default behavior is to close the activity.
     */
    protected void logout() {
        finish();
    }
}
