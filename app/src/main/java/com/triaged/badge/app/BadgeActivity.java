package com.triaged.badge.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * All app activities should inherit from this super class, which implements global functionality.
 *
 * Created by Will on 7/7/14.
 */
public abstract class BadgeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
